/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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
package net.ccbluex.liquidbounce.integration.backend.browser

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.FramebufferResizeEvent
import net.ccbluex.liquidbounce.event.events.GameRenderEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.events.ResourceReloadEvent
import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.event.events.ScreenRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.integration.backend.BrowserTexture
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.drawTexQuad
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.MODEL_STATE
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.READ_FINAL_STATE
import net.minecraft.client.gui.GuiGraphics
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
    private val windowResizeHandler = handler<FramebufferResizeEvent> { event ->
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

        if (!browser.visible || rendered || browser.priority > 0 && mc.screen != null) {
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
    private fun render(context: GuiGraphics) {
        val texture = browser.texture ?: return
        val scaleFactor = mc.window.guiScale.toFloat()

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
        context: GuiGraphics,
        texture: BrowserTexture,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ) {
        val pipeline = if (texture.bgra) {
            ClientRenderPipelines.JCEF.BGRA_BLURRED_TEXTURE
        } else {
            ClientRenderPipelines.JCEF.BLURRED_TEXTURE
        }

        context.drawTexQuad(
            texture.textureSetup,
            x0 = x, y0 = y, x1 = x + width, y1 = y + height,
            pipeline = pipeline,
        )
    }

    override fun close() {
        EventManager.unregisterEventHandler(this)
    }

}
