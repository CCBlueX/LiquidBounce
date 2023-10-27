/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
package net.ccbluex.liquidbounce.base.ultralight

import com.labymedia.ultralight.UltralightView
import com.labymedia.ultralight.config.UltralightViewConfig
import com.labymedia.ultralight.input.UltralightKeyEvent
import com.labymedia.ultralight.input.UltralightMouseEvent
import com.labymedia.ultralight.input.UltralightScrollEvent
import net.ccbluex.liquidbounce.base.ultralight.impl.listener.ViewListener
import net.ccbluex.liquidbounce.base.ultralight.impl.listener.ViewLoadListener
import net.ccbluex.liquidbounce.base.ultralight.impl.renderer.ViewRenderer
import net.ccbluex.liquidbounce.base.ultralight.js.UltralightJsContext
import net.ccbluex.liquidbounce.base.ultralight.theme.Page
import net.ccbluex.liquidbounce.utils.client.ThreadLock
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.sizeLong
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen

/**
 * A view overlay which is being rendered when the view state is [ViewOverlayState.VISIBLE] or [ViewOverlayState.TRANSITIONING].
 *
 * @param layer The layer to render the view on. This can be either [RenderLayer.OVERLAY_LAYER], [RenderLayer.SPLASH_LAYER] or [RenderLayer.SCREEN_LAYER]
 * @param viewRenderer The renderer to use for this view
 */
open class ViewOverlay(val layer: RenderLayer, private val viewRenderer: ViewRenderer) {

    var state = ViewOverlayState.VISIBLE

    val ultralightView = ThreadLock<UltralightView>()
    val context: UltralightJsContext

    private var ultralightPage: Page? = null
    private var garbageCollected = 0L

    private var onStateChange: ((ViewOverlayState) -> Unit)? = null

    init {
        // Setup view
        val (width, height) = mc.window.sizeLong
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

        if (ultralightPage != page && ultralightPage != null) {
            page.close()
        }

        ultralightView.get().loadURL(page.viewableFile)
        ultralightPage = page
        logger.debug("Successfully loaded page ${page.name} from ${page.viewableFile}")

        val (width, height) = mc.window.sizeLong

        // Fix black screen issue
        resize(width, height)
    }

    /**
     * Loads the specified [url]
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
        val page = ultralightPage

        if (page?.hasUpdate() == true) {
            loadPage(page)
        }

        // Collect JS garbage
        collectGarbage()
    }

    /**
     * Render view
     */
    open fun render(context: DrawContext) {
        viewRenderer.render(ultralightView.get(), context)
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
        if (garbageCollected == 0L) {
            garbageCollected = System.currentTimeMillis()
        } else if (System.currentTimeMillis() - garbageCollected > 1000) {
            logger.debug("Garbage collecting Ultralight Javascript...")
            ultralightView.get().lockJavascriptContext().use { lock ->
                lock.context.garbageCollect()
            }
            garbageCollected = System.currentTimeMillis()
        }
    }

    fun state(state: String) {
        this.state = ViewOverlayState.values().firstOrNull { it.jsName.equals(state, true) } ?: return
        onStateChange?.invoke(this.state)
    }

    fun setOnStageChange(stateChange: (ViewOverlayState) -> Unit): ViewOverlay {
        this.onStateChange = stateChange
        return this
    }

    fun free() {
        ultralightView.get().unfocus()
        ultralightView.get().stop()
        ultralightPage?.close()
        viewRenderer.delete()
        context.events._unregisterEvents()
    }

    fun focus() = ultralightView.get().focus()

    fun unfocus() = ultralightView.get().unfocus()

    fun fireScrollEvent(event: UltralightScrollEvent) = ultralightView.get().fireScrollEvent(event)
    fun fireMouseEvent(event: UltralightMouseEvent) = ultralightView.get().fireMouseEvent(event)
    fun fireKeyEvent(event: UltralightKeyEvent) = ultralightView.get().fireKeyEvent(event)

}

class ScreenViewOverlay(viewRenderer: ViewRenderer, val screen: Screen, val adaptedScreen: Screen?, val parentScreen: Screen?) :
    ViewOverlay(RenderLayer.SCREEN_LAYER, viewRenderer)

enum class ViewOverlayState(val jsName: String) {
    HIDDEN("hidden"),
    VISIBLE("visible"),
    TRANSITIONING("transitioning"),
    END("end")
}
