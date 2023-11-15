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

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.engine.Color4b
import org.lwjgl.glfw.GLFW

/**
 * ClickGUI module
 *
 * Shows you an easy-to-use menu to toggle and configure modules.
 */

object ModuleClickGui : Module("ClickGUI", Category.RENDER, bind = GLFW.GLFW_KEY_RIGHT_SHIFT, disableActivation = true) {

    // Specifies whether the search bar should always be visible or only after pressing Ctrl + F.
    val searchAlwaysOnTop by boolean("SearchAlwaysOnTop", true)
    val searchAutoFocus by boolean("SearchAutoFocus", true)
    val moduleColor by color("ModuleColor", Color4b(0, 0, 0, 127)) // rgba(0, 0, 0, 0.5)
    val headerColor by color("HeaderColor", Color4b(0, 0, 0, 173)) // rgba(0, 0, 0, 0.68)
    val accentColor by color("AccentColor", Color4b(70, 119, 255, 255)) // #4677ff
    val textColor by color("TextColor", Color4b(255, 255, 255, 255)) // White
    val dimmedTextColor by color("DimmedTextColor", Color4b(211, 211, 211, 255)) // lightgrey

    fun getColorsAsStyle() = """
        --module: ${moduleColor.toHex(true)}
        --header: ${headerColor.toHex(true)}
        --accent: ${accentColor.toHex(true)}
        --text: ${textColor.toHex(true)}
        --text-dimmed: ${dimmedTextColor.toHex(true)}
        """.trimIndent()

    override fun enable() {
    }

}
