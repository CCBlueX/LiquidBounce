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
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.web.browser.supports.IBrowser
import net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram
import net.minecraft.client.render.GameRenderer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats

class BrowserDrawer(val browser: () -> IBrowser?) : Listenable {

    private val tabs
        get() = browser()?.getTabs() ?: emptyList()

    @Suppress("unused")
    val preRenderHandler = handler<GameRenderEvent> {
        browser()?.drawGlobally()

        for (tab in tabs) {
            tab.drawn = false
        }
    }

    @Suppress("unused")
    val windowResizeWHandler = handler<WindowResizeEvent> { ev ->
        for (tab in tabs) {
            tab.resize(ev.width, ev.height)
        }
    }

    @Suppress("unused")
    val onScreenRender = handler<ScreenRenderEvent> {
        val (width, height) = mc.window.scaledWidth to mc.window.scaledHeight

        for (tab in tabs) {
            if (tab.drawn) {
                continue
            }

            val scaleFactor = mc.window.scaleFactor.toFloat()
            val x = tab.dimension.x().toFloat() / scaleFactor
            val y = tab.dimension.y().toFloat() / scaleFactor
            val w = tab.dimension.width().toFloat() / scaleFactor
            val h = tab.dimension.height().toFloat() / scaleFactor

            renderTexture(x, y, w, h, tab.getTexture())
            tab.drawn = true
        }
    }

    private var shouldReload = false

    @Suppress("unused")
    val onReload = handler<ResourceReloadEvent> {
        shouldReload = true
    }

    @Suppress("unused")
    val onOverlayRender = handler<OverlayRenderEvent>(priority = EventPriorityConvention.READ_FINAL_STATE) {
        val (width, height) = mc.window.scaledWidth to mc.window.scaledHeight

        if (this.shouldReload) {
            for (tab in tabs) {
                tab.forceReload()
            }

            this.shouldReload = false
        }

        for (tab in tabs) {
            if (tab.drawn) {
                continue
            }

            if (tab.preferOnTop && mc.currentScreen != null) {
                continue
            }

            val scaleFactor = mc.window.scaleFactor.toFloat()
            val x = tab.dimension.x().toFloat() / scaleFactor
            val y = tab.dimension.y().toFloat() / scaleFactor
            val w = tab.dimension.width().toFloat() / scaleFactor
            val h = tab.dimension.height().toFloat() / scaleFactor

            renderTexture(x, y, w, h, tab.getTexture())
            tab.drawn = true
        }
    }

    private fun renderTexture(x: Float, y: Float, width: Float, height: Float, texture: Int) {
        RenderSystem.disableDepthTest()
        RenderSystem.enableBlend()
        RenderSystem.blendFunc(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA)
        RenderSystem.setShader { GameRenderer.getPositionTexColorProgram() }
        RenderSystem.setShaderTexture(0, texture)
        val tessellator = Tessellator.getInstance()
        val buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR)
        buffer.vertex(x, y + height, 0.0f)
            .texture(0.0f, 1.0f)
            .color(255, 255, 255, 255)
        buffer.vertex(x + width, y + height, 0.0f)
            .texture(1.0f, 1.0f)
            .color(255, 255, 255, 255)
        buffer.vertex(x + width, y, 0.0f)
            .texture(1.0f, 0.0f)
            .color(255, 255, 255, 255)
        buffer.vertex(x, y, 0.0f)
            .texture(0.0f, 0.0f)
            .color(255, 255, 255, 255)
        drawWithGlobalProgram(buffer.end())
        RenderSystem.setShaderTexture(0, 0)
        RenderSystem.enableDepthTest()
        RenderSystem.defaultBlendFunc()
        RenderSystem.enableBlend()
    }

}
