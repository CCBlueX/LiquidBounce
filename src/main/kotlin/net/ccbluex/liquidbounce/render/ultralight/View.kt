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

import com.labymedia.ultralight.UltralightView
import com.labymedia.ultralight.config.UltralightViewConfig
import com.labymedia.ultralight.input.UltralightKeyEvent
import com.labymedia.ultralight.input.UltralightMouseEvent
import com.labymedia.ultralight.input.UltralightScrollEvent
import net.ccbluex.liquidbounce.render.ultralight.js.UltralightJsContext
import net.ccbluex.liquidbounce.render.ultralight.listener.ViewListener
import net.ccbluex.liquidbounce.render.ultralight.listener.ViewLoadListener
import net.ccbluex.liquidbounce.render.ultralight.renderer.ViewRenderer
import net.ccbluex.liquidbounce.render.ultralight.theme.Page
import net.ccbluex.liquidbounce.utils.client.ThreadLock
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.longedSize
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.util.math.MatrixStack

open class View(val layer: RenderLayer, private val viewRenderer: ViewRenderer) {

    val ultralightView = ThreadLock<UltralightView>()

    val context: UltralightJsContext

    var viewingPage: Page? = null

    private var jsGarbageCollected = 0L

    init {
        // Setup view
        val (width, height) = mc.window.longedSize
        val viewConfig = UltralightViewConfig()
            .isTransparent(true)
            .initialDeviceScale(1.0)

        // Make sure renderer setups config correctly
        viewRenderer.setupConfig(viewConfig)

        ultralightView.lock(UltralightEngine.renderer.get().createView(width, height, viewConfig))
        ultralightView.get().setViewListener(ViewListener())
        ultralightView.get().setLoadListener(ViewLoadListener(this))

        // Setup JS bindings
        context = UltralightJsContext(this, ultralightView)

        logger.debug("Successfully created new view")
    }

    /**
     * Loads the specified [page]
     */
    fun loadPage(page: Page) {
        // Unregister listeners
        context.events._unregisterEvents()

        if (viewingPage != page && viewingPage != null) {
            page.close()
        }

        ultralightView.get().loadURL(page.viewableFile)
        viewingPage = page
        logger.debug("Successfully loaded page ${page.name} from ${page.viewableFile}")
    }

    /**
     * Loads the specified [page]
     */
    fun loadUrl(url: String) {
        // Unregister listeners
        context.events._unregisterEvents()

        ultralightView.get().loadURL(url)
        logger.debug("Successfully loaded page $url")
    }

    /**
     * Update view
     */
    fun update() {
        // Check if page has new update
        val page = viewingPage

        if (page?.hasUpdate() == true) {
            loadPage(page)
        }

        // Collect JS garbage
        collectGarbage()
    }

    /**
     * Render view
     */
    open fun render(matrices: MatrixStack) {
        viewRenderer.render(ultralightView.get(), matrices)
    }

    /**
     * Resizes web view to [width] and [height]
     */
    fun resize(width: Long, height: Long) {
        ultralightView.get().resize(width, height)
        logger.debug("Successfully resized to $width:$height")
    }

    /**
     * Garbage collect JS engine
     */
    private fun collectGarbage() {
        if (jsGarbageCollected == 0L) {
            jsGarbageCollected = System.currentTimeMillis()
        } else if (System.currentTimeMillis() - jsGarbageCollected > 1000) {
            logger.debug("Garbage collecting Ultralight Javascript...")
            ultralightView.get().lockJavascriptContext().use { lock ->
                lock.context.garbageCollect()
            }
            jsGarbageCollected = System.currentTimeMillis()
        }
    }

    /**
     * Free view
     */
    fun free() {
        ultralightView.get().unfocus()
        ultralightView.get().stop()
        viewingPage?.close()
        viewRenderer.delete()
        context.events._unregisterEvents()
    }

    fun focus() {
        ultralightView.get().focus()
    }

    fun unfocus() {
        ultralightView.get().unfocus()
    }

    fun fireScrollEvent(event: UltralightScrollEvent) {
        ultralightView.get().fireScrollEvent(event)
    }

    fun fireMouseEvent(event: UltralightMouseEvent) {
        ultralightView.get().fireMouseEvent(event)
    }

    fun fireKeyEvent(event: UltralightKeyEvent) {
        ultralightView.get().fireKeyEvent(event)
    }

}

class ScreenView(viewRenderer: ViewRenderer, val screen: Screen, val adaptedScreen: Screen?, val parentScreen: Screen?) :
    View(RenderLayer.SCREEN_LAYER, viewRenderer)
