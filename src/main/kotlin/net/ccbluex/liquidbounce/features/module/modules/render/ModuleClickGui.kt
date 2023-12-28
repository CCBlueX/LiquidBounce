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

import com.google.gson.JsonObject
import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.web.integration.VrScreen
import org.lwjgl.glfw.GLFW

/**
 * ClickGUI module
 *
 * Shows you an easy-to-use menu to toggle and configure modules.
 */

object ModuleClickGui : Module("ClickGUI", Category.RENDER, bind = GLFW.GLFW_KEY_RIGHT_SHIFT, disableActivation = true) {

    // Specifies whether the search bar should always be visible or only after pressing Ctrl + F.
    private val searchAlwaysOnTop by boolean("SearchAlwaysOnTop", true)
    private val searchAutoFocus by boolean("SearchAutoFocus", true)
    private val shadow by boolean("Shadow", true)
    private val moduleColor by color("ModuleColor", Color4b(0, 0, 0, 127)) // rgba(0, 0, 0, 0.5)
    private val headerColor by color("HeaderColor", Color4b(0, 0, 0, 173)) // rgba(0, 0, 0, 0.68)
    private val accentColor by color("AccentColor", Color4b(70, 119, 255, 255)) // #4677ff
    private val textColor by color("TextColor", Color4b(255, 255, 255, 255)) // White
    private val dimmedTextColor by color("DimmedTextColor", Color4b(211, 211, 211, 255)) // lightgrey

    fun settingsAsJson() =
         JsonObject().apply {
            addProperty("modulesColor", moduleColor.toHex(true))
            addProperty("headerColor", headerColor.toHex(true))
            addProperty("accentColor", accentColor.toHex(true))
            addProperty("textColor", textColor.toHex(true))
            addProperty("textDimmed", dimmedTextColor.toHex(true))
            addProperty("searchAlwaysOnTop", searchAlwaysOnTop)
            addProperty("autoFocus", searchAutoFocus)
            addProperty("shadow", shadow)
        }

    override fun enable() {
        // Pretty sure we are not in a game, so we can't open the clickgui
        if (mc.player == null || mc.world == null) {
            return
        }

        RenderSystem.recordRenderCall {
            mc.setScreen(VrScreen("clickgui"))
        }
        super.enable()
    }

}
