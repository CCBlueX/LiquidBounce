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

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager.callEvent
import net.ccbluex.liquidbounce.event.events.FramebufferResizeEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.interfaces.PostEffectProcessorAdditions
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.buffer.MinecraftFramebuffer
import net.ccbluex.liquidbounce.render.drawFullScreenPositionTexture
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.newRenderPass
import net.ccbluex.liquidbounce.render.setUniform
import net.ccbluex.liquidbounce.render.ui.ItemImageAtlas
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.fastSin
import net.ccbluex.liquidbounce.utils.kotlin.mixinInterfaceCast
import net.ccbluex.liquidbounce.utils.render.clearColor
import net.ccbluex.liquidbounce.utils.render.clearColorAndDepth
import net.ccbluex.liquidbounce.utils.render.saveToFile
import net.minecraft.client.gl.Framebuffer
import net.minecraft.client.gl.PostEffectProcessor
import net.minecraft.client.gl.SimpleFramebuffer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.client.render.DefaultFramebufferSet
import net.minecraft.util.math.MathHelper
import java.util.concurrent.CompletableFuture

object BlurEffectRenderer : MinecraftShortcuts, EventListener {
    private val OVERLAY_FRAMEBUFFER_ID = LiquidBounce.identifier("overlay")
    private val UI_BLUR_ID = LiquidBounce.identifier("ui_blur")

    private var isDrawingHudFramebuffer = false

    private val overlayFramebuffer = SimpleFramebuffer(
        "LiquidBounceOverlay",
        mc.window.framebufferWidth,
        mc.window.framebufferHeight,
        true
    )

    private val tempFramebuffer = SimpleFramebuffer(
        "LiquidBounceTemp",
        mc.window.framebufferWidth,
        mc.window.framebufferHeight,
        false
    )

    private val lastTimeScreenOpened = Chronometer()
    private var wasScreenOpen = false

    private fun easeFunction(x: Float): Float {
        return (x * MathHelper.HALF_PI).fastSin()
    }

    @Suppress("unused")
    private val resizeHandler = handler<FramebufferResizeEvent> {
        this.overlayFramebuffer.resize(it.width, it.height)
        this.tempFramebuffer.resize(it.width, it.height)
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
        ItemImageAtlas.updateAtlas(context)

        if (ModuleHud.isBlurEffectActive) {
            this.isDrawingHudFramebuffer = true

            val framebufferWrapper = MinecraftFramebuffer(this.overlayFramebuffer)

            framebufferWrapper.beginWrite(viewport = true, clear = false)
        }

        callEvent(OverlayRenderEvent(context, tickDelta))
    }

    private var future1: CompletableFuture<*>? = null
    private var future2: CompletableFuture<*>? = null

    fun endOverlayDrawing() {
        if (!this.isDrawingHudFramebuffer) {
            return
        }

        this.isDrawingHudFramebuffer = false

        val framebufferWrapper = MinecraftFramebuffer(this.overlayFramebuffer)

        framebufferWrapper.end()

        // Debug picture
//        if (future1 == null || future1!!.isDone) {
//            future1 = this.overlayFramebuffer.colorAttachment!!.saveToFile(ConfigSystem.rootFolder.resolve("overlay.png"))
//        }
//        if (future2 == null || future2!!.isDone) {
//            future2 = mc.framebuffer.colorAttachment!!.saveToFile(ConfigSystem.rootFolder.resolve("game.png"))
//        }
        // FIXME: why mc.framebuffer.colorAttachment is transparent?

        mc.framebuffer.drawBlit(this.tempFramebuffer.colorAttachment)
        mc.framebuffer.copyDepthFrom(this.overlayFramebuffer)

        newRenderPass(mc.framebuffer).use { pass ->
            pass.setPipeline(ClientRenderPipelines.Blur)
            pass.bindSampler("texture0", tempFramebuffer.colorAttachment)
            pass.bindSampler("overlay", overlayFramebuffer.colorAttachment)
            pass.setUniform("radius", getBlurRadius())
            pass.drawFullScreenPositionTexture()
        }

        tempFramebuffer.colorAttachment!!.clearColor(0)
        overlayFramebuffer.clearColorAndDepth(0, 1.0)

//        val postEffectProcessor = tryLoadBlurEffectProcessor()
//        val mixinInterfaceCast = mixinInterfaceCast<PostEffectProcessorAdditions>(postEffectProcessor)
//
//        mixinInterfaceCast.`liquid_bounce$renderWithAdditionalExternalTargets`(
//            mc.framebuffer, mc.gameRenderer.pool,
//            { pass ->
//                val alphaBlendRange = ModuleHud.Blur.alphaBlendRange
//
//                pass.setUniform("Radius", getBlurRadius())
//                pass.setUniform(
//                    "BlurRange",
//                    alphaBlendRange.start,
//                    alphaBlendRange.endInclusive,
//                )
//            },
//            mapOf(OVERLAY_FRAMEBUFFER_ID to this.overlayFramebuffer as Framebuffer)
//        )
    }

//    private fun tryLoadBlurEffectProcessor(): PostEffectProcessor {
//        val postEffect = mc.shaderLoader.loadPostEffect(
//            UI_BLUR_ID,
//            setOf(DefaultFramebufferSet.MAIN, OVERLAY_FRAMEBUFFER_ID)
//        )
//
//        if (postEffect == null) {
//            ModuleHud.disableBlur()
//
//            error("Failed to load ui blur shader. Blur shader will be disabled")
//        }
//
//        return postEffect
//    }

}
