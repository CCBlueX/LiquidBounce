package net.ccbluex.liquidbounce.render.engine

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.event.EventManager.callEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.render.ui.ItemImageAtlas
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.SimpleFramebuffer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ChatScreen
import kotlin.math.sin


object UIRenderer {

    private var isDrawingHudFramebuffer: Boolean = false

    val overlayFramebuffer: SimpleFramebuffer by lazy {
        val fb = SimpleFramebuffer(
            mc.window.framebufferWidth,
            mc.window.framebufferHeight, true, MinecraftClient.IS_SYSTEM_MAC
        )

        fb.setClearColor(0.0f, 0.0f, 0.0f, 0.0f)

        fb
    }

    private val lastTimeScreenOpened = Chronometer()
    private var wasScreenOpen = false

    fun easeFunction(x: Float): Float {
        return sin((x * Math.PI) / 2.0).toFloat()
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

    fun getBlurRadius(): Float {
        return (this.getBlurRadiusFactor() * 20.0F).coerceIn(5.0F..20.0F)
    }

    fun startUIOverlayDrawing(context: DrawContext, tickDelta: Float) {
        ItemImageAtlas.updateAtlas(context)

        if (ModuleHud.isBlurable) {
            this.isDrawingHudFramebuffer = true

            this.overlayFramebuffer.clear(true)
            this.overlayFramebuffer.beginWrite(true)
        }

        callEvent(OverlayRenderEvent(context, tickDelta))
    }

    fun endUIOverlayDrawing() {
        if (!this.isDrawingHudFramebuffer) {
            return
        }

        this.isDrawingHudFramebuffer = false

        RenderSystem.enableBlend()
        RenderSystem.blendFunc(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA)

        this.overlayFramebuffer.endWrite()

        mc.framebuffer.beginWrite(true)

        // Remember the previous projection matrix because the draw method changes it AND NEVER FUCKING CHANGES IT
        // BACK IN ORDER TO INTRODUCE HARD TO FUCKING FIND BUGS. Thanks Mojang :+1:
        val projectionMatrix = RenderSystem.getProjectionMatrix()
        val vertexSorting = RenderSystem.getVertexSorting()

        this.overlayFramebuffer.draw(mc.window.framebufferWidth, mc.window.framebufferHeight, false)

        RenderSystem.setProjectionMatrix(projectionMatrix, vertexSorting)
        RenderSystem.defaultBlendFunc()
    }

    fun setupDimensions(width: Int, height: Int) {
        this.overlayFramebuffer.resize(width, height, true)
    }

}
