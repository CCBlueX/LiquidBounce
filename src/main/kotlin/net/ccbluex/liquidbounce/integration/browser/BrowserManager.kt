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
package net.ccbluex.liquidbounce.integration.browser

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.BrowserReadyEvent
import net.ccbluex.liquidbounce.integration.browser.impl.cef.JcefBrowser
import net.ccbluex.liquidbounce.integration.interop.persistant.PersistentLocalStorage
import net.ccbluex.liquidbounce.integration.task.TaskManager
import net.ccbluex.liquidbounce.utils.client.logger

object BrowserManager {

    private val BROWSER_TYPE = BrowserType.JCEF

    /**
     * A browser exception. Used to indicate that something went wrong while using the browser.
     */
    class BrowserException(message: String) : Exception(message)

    /**
     * The current browser instance.
     */
    var browser: Browser? = null
        private set

    lateinit var settings: BrowserRendererSettings
        private set

    @Suppress("unused")
    val browserDrawer = BrowserDrawer { browser }

    @Suppress("unused")
    private val browserInput = BrowserInput { browser }

    init {
        PersistentLocalStorage
    }

    /**
     * Makes the browser dependencies available and initializes the browser
     * when the dependencies are available.
     */
    fun makeDependenciesAvailable(taskManager: TaskManager) {
        val browser = BROWSER_TYPE.browser().apply { browser = this }
        browser.makeDependenciesAvailable(taskManager, ::startBrowser)
    }

    /**
     * Initializes the browser.
     */
    fun startBrowser() {
        // Ensure that the browser is available
        val browser = browser ?: throw BrowserException("Browser is not available.")
        logger.info("Initializing browser...")

        // Ensure that the browser is started on the render thread
        RenderSystem.assertOnRenderThread()

        browser.start()
        settings = BrowserRendererSettings()

        EventManager.callEvent(BrowserReadyEvent(browser))
        logger.info("Successfully initialized browser.")
    }

    /**
     * Shuts down the browser.
     */
    fun stopBrowser() = runCatching {
        browser?.stop()
        browser = null
    }.onFailure {
        logger.error("Failed to shutdown browser.", it)
    }.onSuccess {
        logger.info("Successfully shutdown browser.")
    }

}

enum class BrowserType(override val choiceName: String, val browser: () -> Browser) : NamedChoice {
    JCEF("jcef", ::JcefBrowser)
}
