/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.web.browser

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.GameRenderEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.events.ScreenRenderEvent
import net.ccbluex.liquidbounce.event.events.WindowResizeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.sorting.compareByCondition
import net.ccbluex.liquidbounce.web.browser.supports.IBrowser
import net.ccbluex.liquidbounce.web.browser.supports.tab.ITab
import net.minecraft.client.render.GameRenderer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats

class BrowserDrawer(val browser: () -> IBrowser?) : Listenable {

    private val tabs
        get() = browser()?.getTabs() ?: emptyList()

    val preRenderHandler = handler<GameRenderEvent> {
        browser()?.drawGlobally()

        for (tab in tabs) {
            tab.drawn = false
        }
    }

    val windowResizeWHandler = handler<WindowResizeEvent> { ev ->
        for (tab in tabs) {
            tab.resize(ev.width, ev.height)
        }
    }

    val onScreenRender = handler<ScreenRenderEvent> {
        val (width, height) = mc.window.scaledWidth to mc.window.scaledHeight

        for (tab in tabs) {
            if (tab.drawn) {
                continue
            }

            renderTexture(width.toDouble(), height.toDouble(), tab.getTexture())
            tab.drawn = true
        }
    }

    val onOverlayRender = handler<OverlayRenderEvent> {
        val (width, height) = mc.window.scaledWidth to mc.window.scaledHeight

        for (tab in tabs.sortedWith(compareByCondition(ITab::preferOnTop))) {
            if (tab.drawn) {
                continue
            }

            renderTexture(width.toDouble(), height.toDouble(), tab.getTexture())
            tab.drawn = true
        }
    }

    val dominantTab: ITab?
        get() {
            if (tabs.isEmpty()) {
                return null
            }

            return tabs.firstOrNull { !it.drawn && !it.preferOnTop }
        }

    private fun renderTexture(width: Double, height: Double, texture: Int) {
        RenderSystem.disableDepthTest()
        RenderSystem.enableBlend()
        RenderSystem.blendFunc(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA)
        RenderSystem.setShader { GameRenderer.getPositionTexColorProgram() }
        RenderSystem.setShaderTexture(0, texture)
        val t = Tessellator.getInstance()
        val buffer = t.buffer
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR)
        buffer.vertex(0.0, height, 0.0)
            .texture(0.0f, 1.0f)
            .color(255, 255, 255, 255)
            .next()
        buffer.vertex(width, height, 0.0)
            .texture(1.0f, 1.0f)
            .color(255, 255, 255, 255)
            .next()
        buffer.vertex(width, 0.0, 0.0)
            .texture(1.0f, 0.0f)
            .color(255, 255, 255, 255)
            .next()
        buffer.vertex(0.0, 0.0, 0.0)
            .texture(0.0f, 0.0f)
            .color(255, 255, 255, 255)
            .next()
        t.draw()
        RenderSystem.setShaderTexture(0, 0)
        RenderSystem.enableDepthTest()
        RenderSystem.defaultBlendFunc()
        RenderSystem.enableBlend()
    }

}
