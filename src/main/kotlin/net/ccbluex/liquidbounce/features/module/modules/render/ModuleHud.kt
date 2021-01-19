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

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.LiquidBounceRenderEvent
import net.ccbluex.liquidbounce.event.RenderHudEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.renderer.engine.Color4b
import net.ccbluex.liquidbounce.renderer.engine.RenderEngine
import net.ccbluex.liquidbounce.renderer.engine.RenderTask
import net.ccbluex.liquidbounce.renderer.engine.font.FontRenderer
import java.awt.Font

object ModuleHud : Module("HUD", Category.RENDER, defaultState = true) {
    val liquidBounceFont: Array<RenderTask> = kotlin.run {
        val renderer = FontRenderer.createFontRendererWithStyles(Font("Roboto Bold", Font.PLAIN, 45))

        renderer.begin()

        renderer.draw("LiquidBounce", 2f, 0f, Color4b(0, 111, 255, 255), true)

        renderer.commit()
    }

    val renderHandler = handler<RenderHudEvent> {
//        mc.textRenderer.drawWithShadow(it.matrixStack, "LiquidBounce", 2F, 2F, 0xfffff)

        LiquidBounce.moduleManager.filter { module -> module.state }.forEachIndexed { index, module ->
            val width = mc.textRenderer.getWidth(module.name)
            mc.textRenderer.drawWithShadow(
                it.matrixStack,
                module.name,
                mc.window.scaledWidth - width - 2F,
                2F + (mc.textRenderer.fontHeight * index),
                0xfffff
            )
        }
    }


    val realRenderHandler = handler<LiquidBounceRenderEvent> {
        RenderEngine.enqueueForRendering(0, liquidBounceFont)
    }

}
