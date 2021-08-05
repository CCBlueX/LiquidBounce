/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.screen.EmptyScreen
import net.ccbluex.liquidbounce.render.ultralight.UltralightEngine
import net.ccbluex.liquidbounce.render.ultralight.theme.ThemeManager

/**
 * ClickGUI module
 *
 * Shows you an easy-to-use menu to toggle and configure modules.
 */

object ModuleClickGui : Module("ClickGUI", Category.RENDER, disableActivation = true) {

    override fun enable() {
        val page = ThemeManager.page("clickgui") ?: error("unable to find clickgui page in current theme")

        val emptyScreen = EmptyScreen()
        UltralightEngine.newScreenView(emptyScreen).apply {
            loadPage(page)
        }
        mc.setScreen(emptyScreen)
    }

}
