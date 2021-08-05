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
import net.ccbluex.liquidbounce.render.ultralight.UltralightEngine
import net.ccbluex.liquidbounce.render.ultralight.View
import net.ccbluex.liquidbounce.render.ultralight.theme.ThemeManager

/**
 * Module HUD
 *
 * The client in-game dashboard.
 */

object ModuleHud : Module("HUD", Category.RENDER, state = true, hide = true) {
    override val translationBaseKey: String
        get() = "liquidbounce.module.hud"

    private var view: View? = null

    /**
     * Create new HUD view
     */
    private fun makeView() {
        if (view != null) {
            return
        }

        val page = ThemeManager.defaultTheme.page("hud") ?: error("unable to find hud page in current theme")
        view = UltralightEngine.newOverlayView().apply {
            loadPage(page)
        }
    }

    /**
     * Unload HUD view
     */
    private fun unloadView() {
        view?.let { UltralightEngine.removeView(it) }
        view = null
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
