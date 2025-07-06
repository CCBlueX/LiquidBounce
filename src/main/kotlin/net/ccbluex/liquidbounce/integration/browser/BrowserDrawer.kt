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
package net.ccbluex.liquidbounce.integration.browser

import net.ccbluex.liquidbounce.common.RenderLayerExtensions
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.READ_FINAL_STATE
import net.minecraft.client.gui.DrawContext

/**
 * Draws any kind of [Browser] to the Minecraft screen.
 */
class BrowserDrawer(val browser: () -> Browser?) : EventListener {

    private val tabs
        get() = browser()?.tabs ?: emptyList()

    @Suppress("unused")
    private val gameRenderHandler = handler<GameRenderEvent> {
        val browser = browser() ?: return@handler
        if (!browser.isInitialized) {
            return@handler
        }

        browser.update()

        for (tab in tabs) {
            tab.drawn = false
        }
    }

    @Suppress("unused")
    private val windowResizeHandler = handler<FrameBufferResizeEvent> { event ->
        for (tab in tabs) {
            tab.resize(event.width, event.height)
        }
    }

    @Suppress("unused")
    private val screenRenderHandler = handler<ScreenRenderEvent>(priority = READ_FINAL_STATE) { event ->
        for (tab in tabs) {
            if (tab.drawn || !tab.visible) {
                continue
            }

            val scaleFactor = mc.window.scaleFactor.toFloat()
            val x = tab.position.x.toFloat() / scaleFactor
            val y = tab.position.y.toFloat() / scaleFactor
            val w = tab.position.width.toFloat() / scaleFactor
            val h = tab.position.height.toFloat() / scaleFactor

            renderTexture(event.context, tab.getTexture() ?: continue, x, y, w, h)
            tab.drawn = true
        }
    }

    private var shouldReload = false

    @Suppress("unused")
    private val resourceReloadHandler = handler<ResourceReloadEvent> {
        shouldReload = true
    }

    @Suppress("unused")
    private val overlayRenderHandler = handler<OverlayRenderEvent>(priority = READ_FINAL_STATE) { event ->
        if (this.shouldReload) {
            for (tab in tabs) {
                tab.forceReload()
            }

            this.shouldReload = false
        }

        for (tab in tabs) {
            if (tab.drawn || !tab.visible) {
                continue
            }

            if (tab.preferOnTop && mc.currentScreen != null) {
                continue
            }

            val scaleFactor = mc.window.scaleFactor.toFloat()
            val x = tab.position.x.toFloat() / scaleFactor
            val y = tab.position.y.toFloat() / scaleFactor
            val w = tab.position.width.toFloat() / scaleFactor
            val h = tab.position.height.toFloat() / scaleFactor

            renderTexture(event.context, tab.getTexture() ?: continue, x, y, w, h)
            tab.drawn = true
        }
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

}
