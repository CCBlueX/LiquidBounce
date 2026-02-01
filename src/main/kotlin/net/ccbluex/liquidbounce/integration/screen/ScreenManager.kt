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

package net.ccbluex.liquidbounce.integration.screen

import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.BrowserReadyEvent
import net.ccbluex.liquidbounce.event.events.ClientPlayerEffectEvent
import net.ccbluex.liquidbounce.event.events.FpsLimitEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.KeyboardKeyEvent
import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.event.events.VirtualScreenEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.suspendHandler
import net.ccbluex.liquidbounce.event.waitMatchesWithTimeout
import net.ccbluex.liquidbounce.features.misc.HideAppearance
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.integration.backend.BrowserBackendManager
import net.ccbluex.liquidbounce.integration.backend.browser.Browser
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserState
import net.ccbluex.liquidbounce.integration.backend.browser.GlobalBrowserSettings
import net.ccbluex.liquidbounce.integration.backend.browser.IntegrationBrowserSettings
import net.ccbluex.liquidbounce.integration.interop.ClientInteropServer
import net.ccbluex.liquidbounce.integration.screen.impl.CustomSharedMinecraftScreen
import net.ccbluex.liquidbounce.integration.screen.impl.CustomStandaloneMinecraftScreen
import net.ccbluex.liquidbounce.integration.screen.impl.InternetExplorerScreen
import net.ccbluex.liquidbounce.integration.task.TaskProgressScreen
import net.ccbluex.liquidbounce.integration.theme.Theme
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.utils.client.error.ErrorHandler
import net.ccbluex.liquidbounce.utils.client.error.QuickFix
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.TitleScreen
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.lwjgl.glfw.GLFW
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds

object ScreenManager : EventListener {

    private val logger: Logger = LogManager.getLogger("$CLIENT_NAME/ScreenManager")

    /**
     * The main browser will constantly be updated to display the current screen.
     *
     * May be null if the browser backend is not initialized. This can happen when
     * [BrowserBackendManager.isSkipping] is true.
     */
    var mainBrowser: Browser? = null
        private set
    val browserSettings = IntegrationBrowserSettings(0, ::restart)

    var theme: Theme? = null
        private set
    var screen: CustomScreen? = null
        private set

    /**
     * Acknowledgement is used to detect desyncs between the integration browser and the client.
     * It is reset when the client opens a new screen and confirmed when the integration browser
     * opens the same screen.
     *
     * If the acknowledgement is not confirmed after 500ms, the integration browser will be reloaded.
     */
    val screenAcknowledgement = ScreenAcknowledgement()

    private val standardCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR)

    internal val parent: Screen
        get() = mc.screen ?: TitleScreen()

    @Suppress("unused")
    private val handleBrowserReady = suspendHandler<BrowserReadyEvent>(
        priority = EventPriorityConvention.FIRST_PRIORITY
    ) {
        logger.info("Browser backend is ready. Initializing browser...")
        val browser = ThemeManager.openInputAwareImmediate(settings = browserSettings)

        waitUntilInitialized(browser)
        validateBrowserState(browser, true)
    }

    private suspend fun waitUntilInitialized(browser: Browser) {
        logger.info("Waiting for browser to be initialized...")
        // We currently proceed to go to the Minecraft Title Screen
        //   until this times out. [ErrorHandler.fatal] will kill the game anyway.
        if (waitMatchesWithTimeout<GameTickEvent>(timeout = 30.seconds) {
                browser.isInitialized && browser.state.isCompleted
            } == null) {
            ErrorHandler.fatal(
                error = IllegalStateException("Timed out waiting for integration browser to initialize."),
                quickFix = QuickFix.BROWSER_IS_NOT_RESPONDING
            )
        }
    }

    private suspend fun validateBrowserState(browser: Browser, allowTryOnceMore: Boolean) {
        // Validate browser state past wait.
        when (val state = browser.state) {
            is BrowserState.Success -> {
                this.mainBrowser = browser
                logger.info("Integration Browser $browser is ready.")
            }
            // Try ONCE MORE.
            is BrowserState.Failure if (allowTryOnceMore) -> {
                logger.warn("Failed to initialize integration browser. " +
                    "(code='${state.errorCode}', text='${state.errorText}', url='${state.failedUrl}')")
                browser.url = state.failedUrl
                waitUntilInitialized(browser)
                validateBrowserState(browser, false)
            }
            is BrowserState.Failure -> ErrorHandler.fatal(
                error = IllegalStateException(
                    "Failed to initialize integration browser. " +
                        "(code='${state.errorCode}', text='${state.errorText}', url='${state.failedUrl}')"
                ),
                quickFix = QuickFix.BROWSER_FAILED_TO_LOAD_UI
            )
            else -> ErrorHandler.fatal(
                error = IllegalStateException("Invalid browser state past wait"),
                quickFix = QuickFix.BROWSER_IS_NOT_RESPONDING
            )
        }
    }

    @Suppress("unused")
    fun openScreen(name: String) {
        openScreen(type = CustomScreenType.byName(name) ?: return)
    }

    fun openScreen(theme: Theme? = ThemeManager.theme, type: CustomScreenType) {
        if (theme == null) {
            logger.warn("Theme is null, can't open virtual screen.")
            return
        }

        // Check if the virtual screen is already open
        if (screen?.type == type) {
            return
        }

        if (this.theme != theme) {
            this.theme = theme
            ThemeManager.updateImmediate(mainBrowser, type)
        }

        val customScreen = CustomScreen(type).apply { screen = this }
        screenAcknowledgement.reset()
        EventManager.callEvent(
            VirtualScreenEvent(
                customScreen.type,
                action = VirtualScreenEvent.Action.OPEN
            )
        )
    }

    fun closeScreen() {
        val virtualScreen = screen ?: return

        screen = null
        screenAcknowledgement.reset()
        EventManager.callEvent(
            VirtualScreenEvent(
                virtualScreen.type,
                action = VirtualScreenEvent.Action.CLOSE
            )
        )
    }

    fun restart() {
        try {
            // [mainBrowser] may be null if the browser backend is not initialized.
            // That means we are likely still in the process of starting up.
            val mainBrowser = this.mainBrowser ?: return
            mainBrowser.close()
            this.mainBrowser = ThemeManager.openInputAwareImmediate(settings = browserSettings)
        } catch (e: Exception) {
            logger.error("Failed to restart browser backend for screen integration.", e)
        }

        try {
            ModuleClickGui.invalidate()
        } catch (e: Exception) {
            logger.error("Failed to restart ClickGUI browser integration.", e)
        }

        try {
            ModuleHud.reopen()
        } catch (e: Exception) {
            logger.error("Failed to restart HUD browser integration.", e)
        }
    }

    fun update() {
        val browser = mainBrowser ?: return
        logger.info(
            "Reloading integration browser ${browser.javaClass.simpleName} " +
                "to ${ThemeManager.getScreenLocation()}"
        )
        ThemeManager.updateImmediate(browser, screen?.type)
    }

    fun restoreOriginalScreen() {
        if (mc.screen is CustomSharedMinecraftScreen) {
            mc.setScreen((mc.screen as CustomSharedMinecraftScreen).originalScreen)
        }
    }

    /**
     * Handle opening new screens
     */
    @Suppress("unused")
    private val screenHandler = handler<ScreenEvent> { event ->
        // Set to default GLFW cursor
        GLFW.glfwSetCursor(mc.window.handle(), standardCursor)

        if (handleCurrentScreen(event.screen)) {
            event.cancelEvent()
        }
    }

    @Suppress("unused")
    private val screenUpdater = handler<GameTickEvent> {
        handleCurrentScreen(mc.screen)
    }

    @Suppress("unused")
    private val effectUpdateHandler = handler<GameTickEvent> {
        val player = mc.player ?: return@handler
        if (player.activeEffects.isNotEmpty()) {
            EventManager.callEvent(ClientPlayerEffectEvent(player.activeEffects.toList()))
        }
    }

    /**
     * Refresh integration browser when we change worlds, this can also mean we disconnect from a server
     * and go back to the main menu.
     */
    @Suppress("unused")
    private val worldChangeEvent = handler<WorldChangeEvent> {
        update()
    }

    @Suppress("unused")
    private val fpsLimitHandler = handler<FpsLimitEvent> { event ->
        if (this.mainBrowser == null || !browserSettings.syncGameFps || !isClientScreen(mc.screen)) {
            return@handler
        }

        event.fps = min(event.fps, browserSettings.currentFps)
    }

    @Suppress("unused")
    private val keyHandler = handler<KeyboardKeyEvent> { event ->
        val keyCode = event.keyCode
        val modifier = event.mods

        if (inGame) {
            return@handler
        }

        // F12 to toggle GPU acceleration
        if (event.action == GLFW.GLFW_PRESS && keyCode == GLFW.GLFW_KEY_F12) {
            val backend = BrowserBackendManager.backend ?: return@handler
            if (!backend.accelerationFlags.isSupported) {
                logger.warn("GPU acceleration is not supported by the current browser backend.")
                return@handler
            }

            val accelerated = GlobalBrowserSettings.accelerated ?: return@handler
            accelerated.set(!accelerated.get())
            logger.info("GPU acceleration is now ${if (accelerated.get()) "enabled" else "disabled"}.")
        }
    }

    private fun handleCurrentScreen(screen: Screen?): Boolean {
        // We check against mc.screen, not screen, because somehow this works.
        if (mc.screen is TaskProgressScreen) {
            return false
        }

        if (HideAppearance.isHidingNow || ClientInteropServer.isSkipping) {
            return if (screen is CustomSharedMinecraftScreen) {
                val original = screen.originalScreen
                if (original is CustomSharedMinecraftScreen) {
                    return false
                }

                mc.setScreen(original)
                true
            } else {
                closeScreen()
                false
            }
        }

        if (screen is CustomSharedMinecraftScreen) {
            return false
        }

        // Are we currently playing the game?
        if (mc.level != null && screen == null) {
            closeScreen()
            return false
        }

        return handleCurrentMinecraftScreen(screen ?: TitleScreen())
    }

    /**
     * @return should cancel the minecraft screen
     */
    private fun handleCurrentMinecraftScreen(minecraftScreen: Screen): Boolean {
        val customScreenType = CustomScreenType.recognize(minecraftScreen)
        if (customScreenType == null) {
            closeScreen()
            return false
        }

        val name = customScreenType.routeName
        val route = runCatching {
            ThemeManager.getScreenLocation(customScreenType, false)
        }.getOrNull()

        if (route == null) {
            closeScreen()
            return false
        }

        val theme = route.theme

        return when {
            // When we want to fully replace a screen.
            theme.isScreenSupported(name) -> {
                mc.setScreen(CustomSharedMinecraftScreen(customScreenType, theme, originalScreen = minecraftScreen))
                true
            }
            // When we just want to overlay it.
            theme.isOverlaySupported(name) -> {
                openScreen(theme, customScreenType)
                false
            }
            // When there is nothing to show.
            else -> {
                closeScreen()
                false
            }
        }
    }

    /**
     * Checks if the given screen is an active client screen.
     */
    @JvmStatic
    fun isClientScreen(screen: Screen?) = screen is CustomSharedMinecraftScreen
        || screen is CustomStandaloneMinecraftScreen
        || screen is InternetExplorerScreen

}
