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

package net.ccbluex.liquidbounce.integration.screen.impl

import net.ccbluex.liquidbounce.LiquidBounce.logger
import net.ccbluex.liquidbounce.integration.backend.BrowserBackendManager
import net.ccbluex.liquidbounce.integration.backend.browser.Browser
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserSettings
import net.ccbluex.liquidbounce.integration.screen.CustomScreenType
import net.ccbluex.liquidbounce.integration.screen.ScreenManager
import net.ccbluex.liquidbounce.integration.theme.ThemeManager

class CustomOverlay(
    private val screenType: CustomScreenType,
    var browserSettings: BrowserSettings = ScreenManager.browserSettings
) {

    /**
     * This [browser] might be null.
     */
    var browser: Browser? = null
        private set

    var visible: Boolean
        set(value) {
            if (value && browser == null) {
                open()
            }

            browser?.visible = value
        }
        get() = browser?.visible ?: false

    fun open() {
        if (browser != null) {
            return
        }

        if (!BrowserBackendManager.isInitialized) {
            if (!BrowserBackendManager.isSkipping) {
                logger.error("Could not open custom overlay because the browser backend is not initialized.")
            }
            return
        }

        browser = ThemeManager.openImmediate(
            screenType,
            true,
            browserSettings
        )
    }

    fun close() {
        browser?.close()
        browser = null
    }

}
