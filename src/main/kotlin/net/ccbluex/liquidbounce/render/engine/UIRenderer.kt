package net.ccbluex.liquidbounce.render.engine

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.event.EventManager.callEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.SimpleFramebuffer
import net.minecraft.client.gui.DrawContext

object UIRenderer {
    val overlayFramebuffer: SimpleFramebuffer by lazy {
        val fb = SimpleFramebuffer(
            mc.window.framebufferWidth,
            mc.window.framebufferHeight, true, MinecraftClient.IS_SYSTEM_MAC
        )

        fb.setClearColor(0.0f, 0.0f, 0.0f, 0.0f)

        fb
    }

    fun drawUIOverlay(context: DrawContext, tickDelta: Float) {
        val a = mc.player
        val b = mc.world
        val c = mc.gameRenderer

        if (b != null) {
            this.overlayFramebuffer.clear(true)
            this.overlayFramebuffer.beginWrite(false)
        }

        callEvent(OverlayRenderEvent(context, tickDelta))

        RenderSystem.enableBlend()

        if (c != null) {
            this.overlayFramebuffer.endWrite()
        }

        mc.framebuffer.beginWrite(false)

        if (a != null) {
            this.overlayFramebuffer.draw(mc.window.framebufferWidth, mc.window.framebufferHeight, false)

            mc.framebuffer.beginWrite(true)


            context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)

            RenderSystem.defaultBlendFunc()
            RenderSystem.disableBlend()
            RenderSystem.depthMask(true)
            RenderSystem.enableDepthTest()
        }
    }

    fun setupDimensions(width: Int, height: Int) {
        this.overlayFramebuffer.resize(width, height, true)
    }

}
