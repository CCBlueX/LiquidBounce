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
import net.ccbluex.liquidbounce.base.ultralight.ViewOverlay
import net.ccbluex.liquidbounce.base.ultralight.theme.ThemeManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

/**
 * Module HUD
 *
 * The client in-game dashboard.
 */

object ModuleHud : Module("HUD", Category.RENDER, state = true, hide = true) {
    override val translationBaseKey: String
        get() = "liquidbounce.module.hud"

    private var viewOverlay: ViewOverlay? = null

    /**
     * Create new HUD view
     */
    private fun makeView() {
        if (viewOverlay != null) {
            return
        }

        val page = ThemeManager.page("hud") ?: error("unable to find hud page in current theme")
        viewOverlay = UltralightEngine.newOverlayView().apply {
            loadPage(page)
        }
    }

    /**
     * Unload HUD view
     */
    private fun unloadView() {
        viewOverlay?.let { UltralightEngine.removeView(it) }
        viewOverlay = null
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
