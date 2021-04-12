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
import net.ccbluex.liquidbounce.render.ultralight.js.bindings.UltralightJsWrapper
import net.ccbluex.liquidbounce.render.ultralight.listener.ViewListener
import net.ccbluex.liquidbounce.render.ultralight.listener.ViewLoadListener
import net.ccbluex.liquidbounce.render.ultralight.renderer.ViewRenderer
import net.ccbluex.liquidbounce.render.ultralight.theme.Page
import net.ccbluex.liquidbounce.utils.extensions.longedSize
import net.ccbluex.liquidbounce.utils.logger
import net.ccbluex.liquidbounce.utils.mc
import net.minecraft.client.gui.screen.Screen

open class View(ultralightRenderer: UltralightRenderer, val viewRenderer: ViewRenderer) {

    private lateinit var view: UltralightView
    var currentPage: Page? = null

    lateinit var jsWrapper: UltralightJsWrapper
    lateinit var databind: Databind

    private var jsGarbageCollected = 0L

    init {
        // Setup view
        UltralightEngine.contextThread.scheduleBlocking {
            val (width, height) = mc.window.longedSize
            val viewConfig = UltralightViewConfig()
                .isTransparent(true)
                .initialDeviceScale(1.0)

            // Make sure renderer setups config correctly
            viewRenderer.setupConfig(viewConfig)

            view = ultralightRenderer.createView(width, height, viewConfig)
            view.setViewListener(ViewListener())
            view.setLoadListener(ViewLoadListener(this))

            // Setup JS bindings
            databind = Databind(
                DatabindConfiguration
                    .builder()
                    .contextProviderFactory(ViewContextProvider.Factory(view))
                    .build()
            )
            jsWrapper = UltralightJsWrapper(ViewContextProvider(view), this)
        }

        logger.debug("Created new view ${toString()}")
    }

    /**
     * Loads the specified [page]
     */
    fun loadPage(page: Page) {
        // Unregiste listeners
        jsWrapper.unregisterEvents()

        lockWebView {
            if (currentPage != page && currentPage != null) {
                page.close()
            }

            view.loadURL(page.viewableFile)
            currentPage = page
        }

        logger.debug("Loaded page on ${toString()}")
    }

    /**
     * Update view
     */
    fun update() {
        lockWebView {
            // Check if page has new update
            val page = currentPage

            if (page?.hasUpdate() == true) {
                loadPage(page)
            }

            // Collect JS garbage
            collectGarbage()
        }
    }

    /**
     * Render view
     */
    fun render() {
        viewRenderer.render(view)
    }

    /**
     * Resizes web view to [width] and [height]
     */
    fun resize(width: Long, height: Long) {
        lockWebView {
            view.resize(width, height)
        }

        logger.debug("Resized ${toString()} to (w: $width h: $height)")
    }

    /**
     * Free the view
     */
    fun free() {
        jsWrapper.unregisterEvents()

        lockWebView {
            view.unfocus()
            view.stop()
            currentPage?.close()
        }
    }

    /**
     * Lock [runnable] on context thread
     */
    fun <T> lockWebView(runnable: (UltralightView) -> T): T {
        return UltralightEngine.contextThread.scheduleBlocking { runnable(this.view) }
    }

    /**
     * Garbage collect JS engine
     */
    private fun collectGarbage() {
        if (jsGarbageCollected == 0L) {
            jsGarbageCollected = System.currentTimeMillis()
        } else if (System.currentTimeMillis() - jsGarbageCollected > 1000) {
            logger.debug("Garbage collecting Ultralight Javascript...")
            view.lockJavascriptContext().use { lock ->
                lock.context.garbageCollect()
            }
            jsGarbageCollected = System.currentTimeMillis()
        }
    }

    /**
     * Shows some detailed infos about view
     */
    override fun toString() = "View(page: $currentPage, url: ${view.url()}, w: ${view.width()}, h: ${view.height()})"

    fun focus() {
        lockWebView { it.focus() }
    }

    fun unfocus() {
        lockWebView { it.unfocus() }
    }

    fun fireScrollEvent(event: UltralightScrollEvent) {
        lockWebView { it.fireScrollEvent(event) }
    }

    fun fireMouseEvent(event: UltralightMouseEvent) {
        lockWebView { it.fireMouseEvent(event) }
    }

    fun fireKeyEvent(event: UltralightKeyEvent) {
        lockWebView { it.fireKeyEvent(event) }
    }

    fun remove() {

    }

}

class ScreenView(ultralightRenderer: UltralightRenderer, viewRenderer: ViewRenderer, val screen: Screen) : View(ultralightRenderer, viewRenderer) {
    val active: Boolean = true
}
