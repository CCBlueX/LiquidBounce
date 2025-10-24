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

import com.mojang.blaze3d.opengl.GlConst
import com.mojang.blaze3d.opengl.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.event.EventManager.callEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.render.buffer.Framebuffer
import net.ccbluex.liquidbounce.render.defaultBlendFunc
import net.ccbluex.liquidbounce.render.shader.BlitShader
import net.ccbluex.liquidbounce.render.shader.UniformProvider
import net.ccbluex.liquidbounce.render.shader.shaders.BlitToScreenShader
import net.ccbluex.liquidbounce.render.ui.ItemImageAtlas
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.fastSin
import net.ccbluex.liquidbounce.utils.io.resourceToString
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.client.texture.GlTexture
import net.minecraft.util.math.MathHelper
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL20

object BlurEffectRenderer : MinecraftShortcuts {

    private object BlurShader : BlitShader(
        resourceToString("/resources/liquidbounce/shaders/sobel.vert"),
        resourceToString("/resources/liquidbounce/shaders/blur/ui_blur.frag"),
        arrayOf(
            UniformProvider("texture0") { pointer ->
                GlStateManager._activeTexture(GL13.GL_TEXTURE0)
                GlStateManager._bindTexture(tmpFramebuffer.colorAttachment)
                GL20.glUniform1i(pointer, 0)
            },
            UniformProvider("overlay") { pointer ->
                val active = GlStateManager._getActiveTexture()
                GlStateManager._activeTexture(GL13.GL_TEXTURE9)
                GlStateManager._bindTexture(overlayFramebuffer.colorAttachment)
                GL20.glUniform1i(pointer, 9)
                GlStateManager._activeTexture(active)
            },
            UniformProvider("radius") { pointer -> GL20.glUniform1f(pointer, getBlurRadius()) }
        ))

    private var isDrawingHudFramebuffer = false

    private val overlayFramebuffer = Framebuffer(
        mc.window.framebufferWidth,
        mc.window.framebufferHeight,
        true
    )

    private val tmpFramebuffer = Framebuffer(
        mc.window.framebufferWidth,
        mc.window.framebufferHeight,
        true
    )

    private val lastTimeScreenOpened = Chronometer()
    private var wasScreenOpen = false

    private fun easeFunction(x: Float): Float {
        return (x * MathHelper.HALF_PI).fastSin()
    }

    private fun getBlurRadiusFactor(): Float {
        val isScreenOpen = mc.currentScreen != null && mc.currentScreen !is ChatScreen

        if (isScreenOpen && !wasScreenOpen) {
            lastTimeScreenOpened.reset()
        }

        wasScreenOpen = isScreenOpen

        return if (isScreenOpen) {
            easeFunction((lastTimeScreenOpened.elapsed.toFloat() / 500.0F + 0.1F).coerceIn(0.0F..1.0F))
        } else {
            1.0F
        }
    }

    private fun getBlurRadius(): Float {
        return (this.getBlurRadiusFactor() * 20.0F).coerceIn(5.0F..20.0F)
    }

    fun startOverlayDrawing(context: DrawContext, tickDelta: Float) {
        // FIXME: 1. BlurEffectRenderer is broken
        ItemImageAtlas.updateAtlas(context)

//        if (ModuleHud.isBlurEffectActive) {
//            this.isDrawingHudFramebuffer = true
//
//            this.overlayFramebuffer.beginWrite(true)
//        }

        callEvent(OverlayRenderEvent(context, tickDelta))
    }

    fun endOverlayDrawing() {
        if (!this.isDrawingHudFramebuffer) {
            return
        }

        this.isDrawingHudFramebuffer = false

        this.overlayFramebuffer.end()

        // Remember the previous projection matrix because the draw method changes it AND NEVER FUCKING CHANGES IT
        // BACK IN ORDER TO INTRODUCE HARD TO FUCKING FIND BUGS. Thanks Mojang :+1:
        val projectionMatrix = RenderSystem.getProjectionMatrix()
        val vertexSorting = RenderSystem.getProjectionType()

        GlStateManager._disableBlend()
//        RenderSystem.disableDepthTest()
//        RenderSystem.resetTextureMatrix()

        // Draw Minecraft's framebuffer to the temporary one to avoid feedback loop
        this.tmpFramebuffer.beginWrite(false)

        BlitToScreenShader.blit((mc.framebuffer.colorAttachment as GlTexture).glId)

        this.tmpFramebuffer.end()

        BlurShader.blit()

        GlStateManager._enableBlend()
        GlStateManager._blendFuncSeparate(
            GlConst.GL_ONE,
            GlConst.GL_ONE_MINUS_SRC_ALPHA,
            GlConst.GL_ONE,
            GlConst.GL_ZERO
        )

        this.overlayFramebuffer.drawBlit()

        RenderSystem.setProjectionMatrix(projectionMatrix, vertexSorting)
        defaultBlendFunc()
    }

    fun setupDimensions(width: Int, height: Int) {
        this.overlayFramebuffer.resize(width, height)
        this.tmpFramebuffer.resize(width, height)
    }

}
