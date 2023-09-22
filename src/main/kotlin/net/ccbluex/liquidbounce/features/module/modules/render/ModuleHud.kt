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
import net.ccbluex.liquidbounce.web.LayerDistribution
import net.ccbluex.liquidbounce.web.theme.ThemeManager
import net.janrupf.ujr.example.glfw.web.WebWindow

/**
 * Module HUD
 *
 * The client in-game dashboard.
 */

object ModuleHud : Module("HUD", Category.RENDER, state = true, hide = true) {

    private var hudWindow: WebWindow? = null

    override val translationBaseKey: String
        get() = "liquidbounce.module.hud"

    /**
     * Create new HUD view
     */
    private fun makeView() {
        hudWindow = ThemeManager.page("hud")?.let {
            LayerDistribution().newInGameLayer(it)
        }
    }

    /**
     * Unload HUD view
     */
    private fun unloadView() {
        hudWindow?.view?.stop()
        // todo: remove from windows
        hudWindow = null
    }

    override fun init() {
        makeView()
    }

    override fun enable() {
        makeView()
    }

    override fun disable() {
        unloadView()
    }

}
