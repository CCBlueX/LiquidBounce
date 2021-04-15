/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
package net.ccbluex.liquidbounce.render.ultralight

import com.labymedia.ultralight.UltralightRenderer
import com.labymedia.ultralight.UltralightView
import com.labymedia.ultralight.config.UltralightViewConfig
import com.labymedia.ultralight.databind.Databind
import com.labymedia.ultralight.databind.DatabindConfiguration
import com.labymedia.ultralight.input.UltralightKeyEvent
import com.labymedia.ultralight.input.UltralightMouseEvent
import com.labymedia.ultralight.input.UltralightScrollEvent
import net.ccbluex.liquidbounce.render.ultralight.js.ViewContextProvider
import net.ccbluex.liquidbounce.render.ultralight.js.bindings.UltralightJsEvents
import net.ccbluex.liquidbounce.render.ultralight.listener.ViewListener
import net.ccbluex.liquidbounce.render.ultralight.listener.ViewLoadListener
import net.ccbluex.liquidbounce.render.ultralight.renderer.ViewRenderer
import net.ccbluex.liquidbounce.render.ultralight.theme.Page
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.longedSize
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.screen.Screen

open class View(val layer: RenderLayer, ultralightRenderer: UltralightRenderer, private val viewRenderer: ViewRenderer) {

    internal val ultralightView: UltralightView
    val jsEvents: UltralightJsEvents
    val databind: Databind

    var viewPage: Page? = null

    private var jsGarbageCollected = 0L

    init {
        // Setup view
        val (width, height) = mc.window.longedSize
        val viewConfig = UltralightViewConfig()
            .isTransparent(true)
            .initialDeviceScale(1.0)

        // Make sure renderer setups config correctly
        viewRenderer.setupConfig(viewConfig)

        ultralightView = ultralightRenderer.createView(width, height, viewConfig)
        ultralightView.setViewListener(ViewListener())
        ultralightView.setLoadListener(ViewLoadListener(this))

        // Setup JS bindings
        databind = Databind(
            DatabindConfiguration
                .builder()
                .contextProviderFactory(ViewContextProvider.Factory(ultralightView))
                .build()
        )
        jsEvents = UltralightJsEvents(ViewContextProvider(ultralightView), this)

        logger.debug("Created new view ${toString()}")
    }

    /**
     * Loads the specified [page]
     */
    fun loadPage(page: Page) {
        // Unregiste listeners
        jsEvents._unregisterEvents()

        if (viewPage != page && viewPage != null) {
            page.close()
        }

        ultralightView.loadURL(page.viewableFile)
        viewPage = page

        logger.debug("Loaded page on ${toString()}")
    }

    /**
     * Update view
     */
    fun update() {
        // Check if page has new update
        val page = viewPage

        if (page?.hasUpdate() == true) {
            loadPage(page)
        }

        // Collect JS garbage
        collectGarbage()
    }

    /**
     * Render view
     */
    open fun render() {
        viewRenderer.render(ultralightView)
    }

    /**
     * Resizes web view to [width] and [height]
     */
    fun resize(width: Long, height: Long) {
        ultralightView.resize(width, height)
        logger.debug("Resized ${toString()} to (w: $width h: $height)")
    }

    /**
     * Garbage collect JS engine
     */
    private fun collectGarbage() {
        if (jsGarbageCollected == 0L) {
            jsGarbageCollected = System.currentTimeMillis()
        } else if (System.currentTimeMillis() - jsGarbageCollected > 1000) {
            logger.debug("Garbage collecting Ultralight Javascript...")
            ultralightView.lockJavascriptContext().use { lock ->
                lock.context.garbageCollect()
            }
            jsGarbageCollected = System.currentTimeMillis()
        }
    }

    /**
     * Free view
     */
    fun free() {
        // todo: figure out how to remove it from the ultralight renderer

        ultralightView.unfocus()
        ultralightView.stop()
        viewPage?.close()
        viewRenderer.delete()
        jsEvents._unregisterEvents()
    }

    fun focus() {
        ultralightView.focus()
    }

    fun unfocus() {
        ultralightView.unfocus()
    }

    fun fireScrollEvent(event: UltralightScrollEvent) {
        ultralightView.fireScrollEvent(event)
    }

    fun fireMouseEvent(event: UltralightMouseEvent) {
        ultralightView.fireMouseEvent(event)
    }

    fun fireKeyEvent(event: UltralightKeyEvent) {
        ultralightView.fireKeyEvent(event)
    }

    /**
     * Shows some detailed infos about view
     */
    final override fun toString() = "View(page: $viewPage, url: ${ultralightView.url()}, w: ${ultralightView.width()}, h: ${ultralightView.height()})"

}

class ScreenView(ultralightRenderer: UltralightRenderer, viewRenderer: ViewRenderer, val screen: Screen, val adaptedScreen: Screen?, val parentScreen: Screen?) :
    View(RenderLayer.SCREEN_LAYER, ultralightRenderer, viewRenderer)
