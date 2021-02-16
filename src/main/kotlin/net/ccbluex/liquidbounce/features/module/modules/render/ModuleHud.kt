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

import net.ccbluex.liquidbounce.event.LiquidBounceRenderEvent
import net.ccbluex.liquidbounce.event.RenderHudEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.renderer.Fonts
import net.ccbluex.liquidbounce.renderer.engine.Color4b
import net.ccbluex.liquidbounce.renderer.engine.RenderEngine
import net.ccbluex.liquidbounce.renderer.engine.RenderTask
import net.ccbluex.liquidbounce.renderer.ultralight.WebView
import net.ccbluex.liquidbounce.renderer.ultralight.theme.ThemeManager

object ModuleHud : Module("HUD", Category.RENDER, state = true, hide = true) {

    private val webView = WebView(width = { mc.window.width }, height = { mc.window.height })

    init {
        webView.loadPage(ThemeManager.defaultTheme.page("hud"))
    }

    val renderHandler = handler<RenderHudEvent> {
//        webView.update()
//        webView.render()
    }

    // Engine testing

    val liquidBounceFont: Array<RenderTask> = run {
        Fonts.fontBold180.begin()
        Fonts.fontBold180.draw("LiquidBounce", 2f, 0f, Color4b(0, 111, 255, 255), true)
        Fonts.fontBold180.commit()
    }

    val engineRenderHandler = handler<LiquidBounceRenderEvent> {
        RenderEngine.enqueueForRendering(RenderEngine.HUD_LAYER, liquidBounceFont)
    }

}
