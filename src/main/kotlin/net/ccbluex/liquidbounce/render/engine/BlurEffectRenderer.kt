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

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager.callEvent
import net.ccbluex.liquidbounce.event.events.FramebufferResizeEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.buffer.MinecraftFramebuffer
import net.ccbluex.liquidbounce.render.drawFullScreenPositionTexture
import net.ccbluex.liquidbounce.render.newRenderPass
import net.ccbluex.liquidbounce.render.ui.ItemImageAtlas
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.fastSin
import net.ccbluex.liquidbounce.utils.render.clearColorAndDepth
import net.minecraft.client.gl.SimpleFramebuffer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.util.math.MathHelper

object BlurEffectRenderer : MinecraftShortcuts, EventListener {

    private var isDrawingHudFramebuffer = false

    private val overlayFramebuffer = SimpleFramebuffer(
        "BlurOverlay",
        mc.window.framebufferWidth,
        mc.window.framebufferHeight,
        true
    )

    private val lastTimeScreenOpened = Chronometer()
    private var wasScreenOpen = false

    private fun easeFunction(x: Float): Float {
        return (x * MathHelper.HALF_PI).fastSin()
    }

    @Suppress("unused")
    private val resizeHandler = handler<FramebufferResizeEvent> {
        this.overlayFramebuffer.resize(it.width, it.height)
    }

    fun getBlurRadiusFactor(): Float {
        val isScreenOpen = mc.currentScreen != null && mc.currentScreen !is ChatScreen

        if (isScreenOpen && !wasScreenOpen) {
            lastTimeScreenOpened.reset()
        }

        wasScreenOpen = isScreenOpen

        return if (isScreenOpen) {
            easeFunction((lastTimeScreenOpened.elapsed.toFloat() / 333.0F + 0.1F).coerceIn(0.0F..1.0F))
        } else {
            1.0F
        }
    }

    private fun getBlurRadius(): Float {
        return (this.getBlurRadiusFactor() * 20.0F).coerceIn(5.0F..20.0F)
    }

    fun startOverlayDrawing(context: DrawContext, tickDelta: Float) {
        if (ItemImageAtlas.updateAtlas(context)) {
            return
        }

        if (ModuleHud.isBlurEffectActive) {
            this.isDrawingHudFramebuffer = true

            overlayFramebuffer.clearColorAndDepth(0, 1.0)

            val framebufferWrapper = MinecraftFramebuffer(this.overlayFramebuffer)

            framebufferWrapper.beginWrite(viewport = true, clear = false)

            callEvent(OverlayRenderEvent(this.overlayFramebuffer, context, tickDelta))
        } else {
            callEvent(OverlayRenderEvent(mc.framebuffer, context, tickDelta))
        }
    }

    fun endOverlayDrawing() {
        if (!this.isDrawingHudFramebuffer) {
            return
        }

        this.isDrawingHudFramebuffer = false

        val framebufferWrapper = MinecraftFramebuffer(this.overlayFramebuffer)

        framebufferWrapper.end()

        newRenderPass(mc.framebuffer).use { pass ->
            pass.setPipeline(ClientRenderPipelines.GuiBlur)
            pass.bindSampler("texture0", mc.framebuffer.colorAttachment)
            pass.bindSampler("overlay", overlayFramebuffer.colorAttachment)
            pass.setUniform("radius", getBlurRadius())
            pass.setUniform("alphaBlendMin", ModuleHud.Blur.alphaBlendRange.start)
            pass.setUniform("alphaBlendMax", ModuleHud.Blur.alphaBlendRange.endInclusive)
            pass.drawFullScreenPositionTexture()
        }
        overlayFramebuffer.drawBlit(mc.framebuffer.colorAttachment)
    }

}
