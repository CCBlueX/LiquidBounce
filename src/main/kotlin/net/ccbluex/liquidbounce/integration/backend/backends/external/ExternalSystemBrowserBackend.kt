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
package net.ccbluex.liquidbounce.integration.backend.backends.external

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.integration.backend.BrowserAccelerationFlags
import net.ccbluex.liquidbounce.integration.backend.BrowserBackend
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserSettings
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserViewport
import net.ccbluex.liquidbounce.integration.backend.input.InputAcceptor
import net.ccbluex.liquidbounce.integration.task.TaskManager
import net.ccbluex.liquidbounce.utils.kotlin.sortedInsert

/**
 * Opens an external browser window.
 *
 * Funnily, this is surprisingly usable, even though
 * it just opens an URL.
 *
 * TODO: Use a webdriver to control an external browser window.
 *
 * @author Izuna <izuna.seikatsu@ccbluex.net>
 */
@Suppress("TooManyFunctions")
class ExternalSystemBrowserBackend : BrowserBackend, EventListener {

    override val isInitialized = true
    override var browsers = mutableListOf<ExternalSystemBrowser>()
    override var accelerationFlags = BrowserAccelerationFlags.UNSUPPORTED

    @Suppress("ThrowingExceptionsWithoutMessageOrCause")
    override fun makeDependenciesAvailable(taskManager: TaskManager, whenAvailable: () -> Unit) {
        whenAvailable()
    }

    @Suppress("EmptyFunctionBlock")
    override fun start() { }

    @Suppress("EmptyFunctionBlock")
    override fun stop() { }

    @Suppress("EmptyFunctionBlock")
    override fun update() { }

    override fun createBrowser(
        url: String,
        position: BrowserViewport,
        settings: BrowserSettings,
        priority: Short,
        inputAcceptor: InputAcceptor?
    ) = ExternalSystemBrowser(this, url, position, settings, priority)
        .apply(::addBrowser)

    private fun addBrowser(browser: ExternalSystemBrowser) {
        browsers.sortedInsert(browser, ExternalSystemBrowser::priority)
    }

    internal fun removeBrowser(browser: ExternalSystemBrowser) {
        browsers.remove(browser)
    }

}
