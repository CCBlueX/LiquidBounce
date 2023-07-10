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

import net.ccbluex.liquidbounce.base.ultralight.UltralightEngine
import net.ccbluex.liquidbounce.base.ultralight.theme.ThemeManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.screen.EmptyScreen
import org.lwjgl.glfw.GLFW

/**
 * ClickGUI module
 *
 * Shows you an easy-to-use menu to toggle and configure modules.
 */

object ModuleClickGui : Module("ClickGUI", Category.RENDER, bind = GLFW.GLFW_KEY_RIGHT_SHIFT, disableActivation = true) {

    // Specifies whether the search bar should always be visible or only after pressing Ctrl + F.
    val searchAlwaysOnTop by boolean("SearchAlwaysOnTop", true)

    override fun enable() {
        val page = ThemeManager.page("clickgui") ?: error("unable to find clickgui page in current theme")

        val emptyScreen = EmptyScreen()
        UltralightEngine.newScreenView(emptyScreen).apply {
            loadPage(page)
        }
        mc.setScreen(emptyScreen)
    }

}
