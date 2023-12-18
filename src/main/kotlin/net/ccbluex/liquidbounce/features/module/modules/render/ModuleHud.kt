/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.web.browser.BrowserManager
import net.ccbluex.liquidbounce.web.browser.supports.tab.ITab
import net.ccbluex.liquidbounce.web.theme.ThemeManager.overlayUrl

/**
 * Module HUD
 *
 * The client in-game dashboard.
 */

object ModuleHud : Module("HUD", Category.RENDER, state = true, hide = true) {

    private var browserTab: ITab? = null

    override val translationBaseKey: String
        get() = "liquidbounce.module.hud"

    val screenHandler = handler<ScreenEvent>(ignoreCondition = true) {
        if (!enabled || mc.world == null) {
            browserTab?.closeTab()
            browserTab = null
        } else if (browserTab == null) {
            browserTab = BrowserManager.browser?.createTab(overlayUrl)
        }
    }

    override fun enable() {
        // Should not happen, but in-case there is already a tab open, close it
        browserTab?.closeTab()

        // Create a new tab and open it
        browserTab = BrowserManager.browser?.createTab(overlayUrl)
    }

    override fun disable() {
        // Closes tab entirely
        browserTab?.closeTab()
        browserTab = null
    }

}
