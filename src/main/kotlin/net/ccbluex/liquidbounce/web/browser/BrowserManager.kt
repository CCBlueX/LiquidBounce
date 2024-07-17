/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.web.browser

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.BrowserReadyEvent
import net.ccbluex.liquidbounce.utils.client.ErrorHandler
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.web.browser.supports.IBrowser
import net.ccbluex.liquidbounce.web.browser.supports.JcefBrowser
import net.ccbluex.liquidbounce.web.browser.supports.UjrBrowser
import net.ccbluex.liquidbounce.web.persistant.PersistentLocalStorage

object BrowserManager : Configurable("browser") {

    private val DEFAULT_BROWSER_TYPE = BrowserType.ULTRALIGHT
    private val browserType by enumChoice("type", DEFAULT_BROWSER_TYPE)

    /**
     * A browser exception. Used to indicate that something went wrong while using the browser.
     */
    class BrowserException(message: String) : Exception(message)

    /**
     * The current browser instance.
     */
    var browser: IBrowser? = null
        private set

    @Suppress("unused")
    val browserDrawer = BrowserDrawer { browser }

    @Suppress("unused")
    private val browserInput = BrowserInput { browser }

    init {
        PersistentLocalStorage
    }

    /**
     * Initializes the browser.
     */
    fun initBrowser() {
        val browser = browserType.getBrowser().apply { browser = this }

        // Be aware, this will block the execution of the client until the browser dependencies are available.
        browser.makeDependenciesAvailable {
            runCatching {
                // Initialize the browser backend
                browser.initBrowserBackend()

                EventManager.callEvent(BrowserReadyEvent(browser))
            }.onFailure(ErrorHandler::fatal)
        }
    }

    /**
     * Shuts down the browser.
     */
    fun shutdownBrowser() = runCatching {
        browser?.shutdownBrowserBackend()
        browser = null
    }.onFailure {
        logger.error("Failed to shutdown browser.", it)
    }.onSuccess {
        logger.info("Successfully shutdown browser.")
    }

}

enum class BrowserType(override val choiceName: String, val getBrowser: () -> IBrowser) : NamedChoice {
    JCEF("jcef", {
        JcefBrowser()
    }),
    ULTRALIGHT("ultralight", {
        UjrBrowser()
    })
}
