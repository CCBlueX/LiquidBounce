/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.BrowserReadyEvent
import net.ccbluex.liquidbounce.event.events.GameRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.integration.backend.browser.GlobalBrowserSettings
import net.ccbluex.liquidbounce.integration.backend.backends.cef.CefBrowserBackend
import net.ccbluex.liquidbounce.integration.interop.persistant.PersistentLocalStorage
import net.ccbluex.liquidbounce.integration.task.TaskManager
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY

object BrowserBackendManager : EventListener {

    val browserBackend: BrowserBackend = CefBrowserBackend()

    init {
        PersistentLocalStorage
    }

    /**
     * Makes the browser dependencies available and initializes the browser
     * when the dependencies are available.
     */
    fun makeDependenciesAvailable(taskManager: TaskManager) {
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

        browserBackend.start()

        GlobalBrowserSettings
        EventManager.callEvent(BrowserReadyEvent)
        logger.info("Successfully initialized browser.")
    }

    /**
     * Shuts down the browser.
     */
    fun stop() = runCatching {
        browserBackend.stop()
    }.onFailure {
        logger.error("Failed to shutdown browser.", it)
    }.onSuccess {
        logger.info("Successfully shutdown browser.")
    }

    /**
     * Causes an update of every browser by re-setting their viewport.
     */
    fun forceUpdate() = RenderSystem.recordRenderCall {
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
        if (!browserBackend.isInitialized) {
            return@handler
        }

        browserBackend.update()
    }

}
