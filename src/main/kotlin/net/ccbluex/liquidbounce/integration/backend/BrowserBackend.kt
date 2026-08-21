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

import net.ccbluex.liquidbounce.integration.backend.browser.Browser
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserSettings
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserViewport
import net.ccbluex.liquidbounce.integration.backend.input.InputAcceptor
import net.ccbluex.liquidbounce.integration.screen.ScreenManager
import net.ccbluex.liquidbounce.integration.task.TaskManager

/**
 * The browser interface which is used to create tabs and manage the browser backend.
 * Due to different possible browser backends, this interface is used to abstract the browser backend.
 */
interface BrowserBackend {

    val isInitialized: Boolean
    var accelerationFlags: BrowserAccelerationFlags
    val browsers: List<Browser>

    /**
     * Whether this backend can give a browser cookies and storage of its own. Backends that hand the
     * page to something they do not control cannot.
     */
    val supportsIncognito: Boolean

    fun makeDependenciesAvailable(taskManager: TaskManager, whenAvailable: () -> Unit)

    /**
     * Starts the browser backend and initializes it.
     */
    fun start()

    /**
     * Stops the browser backend and cleans up resources.
     */
    fun stop()

    /**
     * Usually does a global render update of the browser.
     */
    fun update()

    /**
     * @param incognito Whether the browser gets cookies and storage of its own that are kept in memory
     *                  only, instead of sharing the ones the client persists. Not every backend can
     *                  honour this - check [Browser.isIncognito] when it matters.
     */
    fun createBrowser(
        url: String,
        position: BrowserViewport = BrowserViewport.FULLSCREEN,
        settings: BrowserSettings = ScreenManager.browserSettings,
        priority: Short = 0,
        incognito: Boolean = false,
        inputAcceptor: InputAcceptor? = null
    ): Browser

}
