/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
package net.ccbluex.liquidbounce.render.engine

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.vertex.VertexFormat
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.FramebufferResizeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.createRenderPass
import net.ccbluex.liquidbounce.render.drawFullScreenPositionTexture
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.math.Easing
import net.ccbluex.liquidbounce.utils.render.clearColorAndDepth
import net.ccbluex.liquidbounce.utils.render.createUbo
import net.ccbluex.liquidbounce.utils.render.writeStd140
import net.minecraft.client.gl.SimpleFramebuffer
import net.minecraft.client.gui.screen.ChatScreen

object BlurEffectRenderer : MinecraftShortcuts, EventListener {

    var isDrawingHudFramebuffer = false
        set(value) {
            if (value) {
                clearOverlay()
            }
            field = value
        }

    val overlayFramebuffer = SimpleFramebuffer(
        "${LiquidBounce.CLIENT_NAME} BlurOverlay",
        mc.window.framebufferWidth,
        mc.window.framebufferHeight,
        true
    ).apply {
        this.setFilter(FilterMode.NEAREST)
    }

    private fun clearOverlay() {
        overlayFramebuffer.clearColorAndDepth()
    }

    private val lastTimeScreenOpened = Chronometer()
    private var wasScreenOpen = false

    @Suppress("unused")
    private val resizeHandler = handler<FramebufferResizeEvent> {
        if ((it.width != this.overlayFramebuffer.textureWidth || it.height != this.overlayFramebuffer.textureHeight)) {
            if (it.width == 0 || it.height == 0) {
                clearOverlay()
            } else {
                this.overlayFramebuffer.resize(it.width, it.height)
            }
        }
    }

    private val GUI_BLUR_UNIFORM_BUFFER = gpuDevice.createUbo(
        labelGetter = { "GUI blur UBO" },
        std140Size = { float + float + float },
    ).slice()

    fun shouldDrawBlur(): Boolean = inGame && mc.currentScreen == null &&
        ModuleHud.running && ModuleHud.isBlurEffectActive

    fun blitBlurOverlay() {
        if (!isDrawingHudFramebuffer) {
            return
        }
        isDrawingHudFramebuffer = false

        // Draw blur areas
        GUI_BLUR_UNIFORM_BUFFER.writeStd140 {
            putFloat(getBlurRadius())
            putFloat(ModuleHud.Blur.alphaBlendRange.start)
            putFloat(ModuleHud.Blur.alphaBlendRange.endInclusive)
        }

        mc.framebuffer.createRenderPass().use { pass ->
            pass.setPipeline(ClientRenderPipelines.GuiBlur)
            pass.bindSampler("texture0", mc.framebuffer.colorAttachmentView)
            pass.bindSampler("overlay", overlayFramebuffer.colorAttachmentView)
            pass.setUniform("BlurData", GUI_BLUR_UNIFORM_BUFFER)
            pass.drawFullScreenPositionTexture()
        }

        // overlayFramebuffer ---blit--> mc.framebuffer
        drawOverlayBlit()
    }

    /**
     * Draws a blit using a custom JCEF-compatible blending pipeline.
     * Replaces the call to `overlayFramebuffer.drawBlit(mc.framebuffer.colorAttachment)`.
     */
    private fun drawOverlayBlit() {
        val shapeIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS)
        val indexBuffer = shapeIndexBuffer.getIndexBuffer(6)
        val vertexBuffer = RenderSystem.getQuadVertexBuffer()

        mc.framebuffer.colorAttachmentView!!.createRenderPass(
            { "GUI blur overlay blit pass" },
        ).use { renderPass ->
            renderPass.setPipeline(ClientRenderPipelines.JCEF.Blit)
            RenderSystem.bindDefaultUniforms(renderPass)
            renderPass.setVertexBuffer(0, vertexBuffer)
            renderPass.setIndexBuffer(indexBuffer, shapeIndexBuffer.indexType)
            renderPass.bindSampler("InSampler", overlayFramebuffer.colorAttachmentView)
            renderPass.drawIndexed(0, 0, 6, 1)
        }
    }

    fun getBlurRadiusFactor(): Float {
        val isScreenOpen = mc.currentScreen != null && mc.currentScreen !is ChatScreen

        if (isScreenOpen && !wasScreenOpen) {
            lastTimeScreenOpened.reset()
        }
        wasScreenOpen = isScreenOpen

        return if (isScreenOpen) {
            val x = (lastTimeScreenOpened.elapsed.toFloat() / 333.0F + 0.1F).coerceIn(0.0F..1.0F)
            Easing.QUAD_OUT.transform(x)
        } else {
            1.0F
        }
    }

    private fun getBlurRadius(): Float {
        return (this.getBlurRadiusFactor() * 20.0F).coerceIn(5.0F..20.0F)
    }

}
