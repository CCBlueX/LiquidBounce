/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.integration.backend

import com.mojang.blaze3d.platform.InputConstants
import com.mojang.blaze3d.systems.RenderSystem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.future.await
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.BrowserReadyEvent
import net.ccbluex.liquidbounce.event.events.GameRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.ccbluex.liquidbounce.features.global.GlobalManager
import net.ccbluex.liquidbounce.integration.backend.backends.cef.CefBrowserBackend
import net.ccbluex.liquidbounce.integration.backend.backends.external.ExternalSystemBrowserBackend
import net.ccbluex.liquidbounce.integration.backend.browser.GlobalBrowserSettings
import net.ccbluex.liquidbounce.integration.interop.persistant.PersistentLocalStorage
import net.ccbluex.liquidbounce.integration.task.TaskManager
import net.ccbluex.liquidbounce.utils.client.clientLogger
import net.ccbluex.liquidbounce.utils.client.env
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY

/**
 * Overrides the player's choice.
 */
val browserBackend = env("LB_BROWSER_BACKEND", "net.ccbluex.liquidbounce.browser.backend")
var isBrowserDisabled = env("LB_BROWSER_SKIP", "net.ccbluex.liquidbounce.browser.skip")?.toBoolean()
    ?: false
val isBrowserAccelerationDisabled = env("LB_BROWSER_DISABLE_ACCELERATION",
    "net.ccbluex.liquidbounce.browser.disableAcceleration")?.toBoolean() ?: false

object BrowserBackendManager : EventListener {

    private val logger = clientLogger("BrowserBackendManager")

    val isInitialized: Boolean
        get() = backend?.isInitialized ?: false
    var backend: BrowserBackend? = null

    private const val DEFAULT_BACKEND = "cef"

    private val providers = linkedMapOf<String, BrowserBackendProvider>()

    val selectableBackends: List<BrowserBackendProvider>
        get() = providers.values.filter(BrowserBackendProvider::selectable)

    /**
     * Set while the player is asked.
     */
    var pendingSelection: CompletableDeferred<BrowserBackendProvider>? = null
        private set

    init {
        registerBackend(BrowserBackendProvider(
            DEFAULT_BACKEND,
            "Chromium",
            "The browser LiquidBounce comes with (Chromium)."
        ) { CefBrowserBackend() })
        registerBackend(BrowserBackendProvider(
            "external",
            "System browser",
            "Opens pages in the browser of the system.",
            selectable = false
        ) { ExternalSystemBrowserBackend() })
    }

    @AddonApi
    fun registerBackend(provider: BrowserBackendProvider) {
        check(providers.putIfAbsent(provider.id, provider) == null) {
            "Browser backend '${provider.id}' is already registered"
        }
    }

    internal fun unregisterBackend(provider: BrowserBackendProvider) {
        providers.remove(provider.id, provider)
    }

    fun init() {
        PersistentLocalStorage
    }

    /**
     * Makes the browser dependencies available and initializes the browser
     * when the dependencies are available.
     */
    fun makeDependenciesAvailable(taskManager: TaskManager) {
        if (isBrowserDisabled) {
            logger.warn("Environment variable 'LB_BROWSER_SKIP' is set to 'true'.")
            return
        }

        if (browserBackend == "none") {
            logger.warn("Environment variable 'LB_BROWSER_BACKEND' is set to 'none'.")
            isBrowserDisabled = true
            return
        }

        val provider = chosenBackend()
        if (provider != null) {
            use(provider, taskManager)
            return
        }

        // No backend is loaded until the player picks one
        val selection = CompletableDeferred<BrowserBackendProvider>()
        pendingSelection = selection
        logger.info("Asking which browser backend to use.")
        mc.execute { mc.gui.setScreen(BrowserSelectionScreen(selectableBackends, selection)) }
        taskManager.launch("Browser") {
            val picked = selection.await()
            pendingSelection = null

            GlobalBrowserSettings.backendId = picked.id
            ConfigSystem.store(GlobalManager)
            // Within the task, so the loading screen stays until the backend's tasks exist
            mc.submit { use(picked, taskManager) }.await()
        }
    }

    /**
     * Returns null when the player has to be asked.
     */
    private fun chosenBackend(): BrowserBackendProvider? {
        browserBackend?.let { id ->
            return providers[id] ?: error("Unknown browser backend: $id")
        }

        val selectable = selectableBackends
        if (selectable.size <= 1) {
            return selectable.firstOrNull() ?: providers.getValue(DEFAULT_BACKEND)
        }

        val shiftHeld = InputConstants.isKeyDown(InputConstants.KEY_LSHIFT) ||
            InputConstants.isKeyDown(InputConstants.KEY_RSHIFT)
        if (shiftHeld) {
            logger.info("Shift is held, asking for the browser backend.")
            return null
        }

        return selectable.firstOrNull { it.id == GlobalBrowserSettings.backendId }
    }

    private fun use(provider: BrowserBackendProvider, taskManager: TaskManager) {
        logger.info("Using the '${provider.id}' browser backend.")
        val browserBackend = provider.create()
        this.backend = browserBackend
        browserBackend.makeDependenciesAvailable(taskManager, ::start)
    }

    /**
     * Initializes the browser.
     */
    fun start() {
        // Ensure that the browser is available
        logger.info("Initializing browser...")

        // Ensure that the browser is started on the render thread
        RenderSystem.assertOnRenderThread()

        val browserBackend = backend ?: return
        browserBackend.start()

        if (isBrowserAccelerationDisabled) {
            logger.warn("Environment variable 'LB_BROWSER_DISABLE_ACCELERATION' is set to 'true'.")
        }
        GlobalBrowserSettings
        EventManager.callEvent(BrowserReadyEvent)
        logger.info("Successfully initialized browser.")
    }

    /**
     * Shuts down the browser.
     */
    fun stop() = runCatching {
        backend?.stop()
    }.onFailure {
        logger.error("Failed to shutdown browser.", it)
    }.onSuccess {
        logger.info("Successfully shutdown browser.")
    }

    /**
     * Causes an update of every browser by re-setting their viewport.
     */
    fun forceUpdate() = mc.execute {
        val browserBackend = backend ?: return@execute

        for (browser in browserBackend.browsers) {
            try {
                browser.viewport = browser.viewport
            } catch (e: Exception) {
                logger.error("Failed to update tab of '${browser.url}'", e)
            }
        }
    }

    @Suppress("unused")
    private val gameRenderHandler = handler<GameRenderEvent>(priority = FIRST_PRIORITY) {
        val browserBackend = backend ?: return@handler
        if (!browserBackend.isInitialized) {
            return@handler
        }

        browserBackend.update()
    }

}
