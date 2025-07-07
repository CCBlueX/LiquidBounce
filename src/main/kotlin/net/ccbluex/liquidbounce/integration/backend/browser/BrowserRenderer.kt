package net.ccbluex.liquidbounce.integration.backend.browser

import net.ccbluex.liquidbounce.common.RenderLayerExtensions
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.FrameBufferResizeEvent
import net.ccbluex.liquidbounce.event.events.GameRenderEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.events.ResourceReloadEvent
import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.event.events.ScreenRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.integration.backend.BrowserTexture
import net.ccbluex.liquidbounce.integration.backend.backends.cef.CefBrowser
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.MODEL_STATE
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.READ_FINAL_STATE
import net.minecraft.client.gui.DrawContext
import java.lang.AutoCloseable

/**
 * Renders the browser tab on the screen.
 *
 * This [EventListener] needs to be unregistered when the browser is closed.
 */
class BrowserRenderer(val browser: Browser) : EventListener, AutoCloseable {

    private var rendered = false

    @Suppress("unused")
    private val gameRenderHandler = handler<GameRenderEvent>(priority = MODEL_STATE) {
        rendered = false
    }

    @Suppress("unused")
    private val windowResizeHandler = handler<FrameBufferResizeEvent> { event ->
        browser.update(event.width, event.height)
    }

    @Suppress("unused")
    private val screenHandler = handler<ScreenEvent>(priority = READ_FINAL_STATE) {
        browser.invalidate()
    }

    @Suppress("unused")
    private val overlayRenderHandler = handler<OverlayRenderEvent>(browser.priority) { event ->
        if (this.shouldReload) {
            browser.forceReload()
            this.shouldReload = false
        }

        if (!browser.visible || rendered || browser.priority > 0 && mc.currentScreen != null) {
            return@handler
        }

        render(event.context)
    }

    @Suppress("unused")
    private val screenRenderHandler = handler<ScreenRenderEvent>(browser.priority) { event ->
        if (!browser.visible || rendered) {
            return@handler
        }

        render(event.context)
    }

    private var shouldReload = false

    @Suppress("unused")
    private val resourceReloadHandler = handler<ResourceReloadEvent> {
        shouldReload = true
    }

    /**
     * Renders a browser tab with proper scaling
     */
    private fun render(context: DrawContext) {
        val texture = browser.texture ?: return
        val scaleFactor = mc.window.scaleFactor.toFloat()

        val viewport = browser.viewport
        val x = viewport.x.toFloat() / scaleFactor
        val y = viewport.y.toFloat() / scaleFactor
        val w = viewport.width.toFloat() / scaleFactor
        val h = viewport.height.toFloat() / scaleFactor

        renderTexture(context, texture, x, y, w, h)
        rendered = true
    }

    @Suppress("LongParameterList")
    private fun renderTexture(
        context: DrawContext,
        texture: BrowserTexture,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ) {
        val layer = if (texture.bgra) {
            RenderLayerExtensions::getBgraBlurredTextureLayer
        } else {
            RenderLayerExtensions::getBlurredTextureLayer
        }

        context.drawTexture(
            layer, texture.identifier, x.toInt(), y.toInt(),
            0f, 0f, width.toInt(),
            height.toInt(), width.toInt(), height.toInt()
        )
    }

    override fun close() {
        EventManager.unregisterEventHandler(this)
    }

}
