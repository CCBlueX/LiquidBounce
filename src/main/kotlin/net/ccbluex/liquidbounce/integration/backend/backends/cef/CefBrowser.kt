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
package net.ccbluex.liquidbounce.integration.backend.backends.cef

import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.integration.backend.BrowserTexture
import net.ccbluex.liquidbounce.integration.backend.browser.Browser
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserRenderer
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserSettings
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserState
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserViewport
import net.ccbluex.liquidbounce.integration.backend.browser.GlobalBrowserSettings
import net.ccbluex.liquidbounce.integration.backend.input.InputAcceptor
import net.ccbluex.liquidbounce.integration.backend.input.InputHandler
import net.ccbluex.liquidbounce.integration.backend.input.InputListener
import net.ccbluex.liquidbounce.mcef.MCEF
import net.ccbluex.liquidbounce.mcef.cef.MCEFBrowser
import net.ccbluex.liquidbounce.mcef.cef.MCEFBrowserSettings
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@Suppress("TooManyFunctions")
class CefBrowser(
    private val backend: CefBrowserBackend,
    url: String,
    viewport: BrowserViewport,
    val settings: BrowserSettings,
    override var priority: Short = 0,
    inputAcceptor: InputAcceptor? = null
) : Browser, InputHandler, MinecraftShortcuts {

    internal val browserApi: MCEFBrowser
    private val logger: Logger

    init {
        require(url.isNotEmpty()) { "URL cannot be empty." }
        val quality = GlobalBrowserSettings.quality
        val (width, height) = viewport.getScaledDimensions(quality)
        browserApi = MCEF.INSTANCE.createBrowser(
            url,
            true,
            width,
            height,
            MCEFBrowserSettings(
                settings.currentFps,
                GlobalBrowserSettings.accelerated?.get() == true
            )
        ).apply {
            addOnPaintListener {
                comparePaintWithViewpoint(it.width, it.height)
            }
            addOnAcceleratedPaintListener {
                comparePaintWithViewpoint(it.width, it.height)
            }
        }

        logger = LogManager.getLogger("$CLIENT_NAME/CefBrowser/${browserApi.hashCode()}")
        logger.info("Initializing Browser API (url='$url')")
    }

    override var isInitialized: Boolean = false
        internal set(value) {
            require(!field) { "Browser $this is already initialized." }
            require(value) { "Cannot uninitialize browser $this." }

            // https://magpcss.org/ceforum/viewtopic.php?f=17&t=17702
            browserApi.loadURL(url)

            val quality = GlobalBrowserSettings.quality
            browserApi.zoomLevel = viewport.getZoomLevel(quality)
            field = true

            logger.info("Initialized Browser API")
        }

    override var state: BrowserState = BrowserState.Idle
        internal set(value) {
            field = value

            when (value) {
                is BrowserState.Loading ->
                    logger.info("Started loading (url='${url}')")
                is BrowserState.Success ->
                    logger.info("Finished loading (url='${url}', httpStatusCode=${value.httpStatusCode})")
                is BrowserState.Failure ->
                    logger.warn("Failed to load " +
                        "(url='${value.failedUrl}', errorCode=${value.errorCode}, errorText=${value.errorText})")
                else -> { /* Idle state, do nothing */ }
            }
        }

    override var viewport: BrowserViewport = viewport
        set(value) {
            field = value

            val quality = GlobalBrowserSettings.quality
            val (scaledWidth, scaledHeight) = value.getScaledDimensions(quality)
            val zoomLevel = value.getZoomLevel(quality)

            val viewRect = browserApi.getViewRect(null)
            // Check if the browser dimensions have changed
            if (viewRect.width == scaledWidth && viewRect.height == scaledHeight) {
                return
            }

            // TODO: CEF is suffering from a bug where resizing the browser,
            //   does not call [wasResized] and thus does not update the renderer.
            //   See: https://github.com/chromiumembedded/cef/issues/3826
            browserApi.resize(scaledWidth, scaledHeight)
            browserApi.zoomLevel = zoomLevel

            // To ensure the texture is updated, we clear the renderer. This call invalidates the
            // current UI.
            browserApi.clear()

            logger.debug(
                "Browser {} viewport updated: {}, scaled to {} x {} at zoom level {}",
                this,
                value,
                scaledWidth,
                scaledHeight,
                zoomLevel
            )
        }
    override var visible = true

    private val renderer = BrowserRenderer(this)
    private val inputListener: InputListener? = inputAcceptor?.let { _ ->
        InputListener(this, this, inputAcceptor)
    }

    override var url: String
        get() = browserApi.url
        set(value) {
            if (!isInitialized) {
                logger.warn("Cannot set URL of uninitialized browser $this.")
                // We continue anyway, because the browser API might accept it anyway.
            }

            state = BrowserState.Idle
            browserApi.loadURL(value)
        }

    override val texture: BrowserTexture?
        get() {
            if (!browserApi.renderer.isTextureReady || browserApi.renderer.isUnpainted) {
                return null
            }

            return BrowserTexture(
                browserApi.renderer.textureSetup!!,
                viewport.height,
                viewport.width,
                browserApi.renderer.isBGRA,
            )
        }

    override fun forceReload() {
        browserApi.reloadIgnoreCache()
    }

    override fun reload() {
        browserApi.reload()
    }

    override fun goForward() {
        browserApi.goForward()
    }

    override fun goBack() {
        browserApi.goBack()
    }

    override fun close() {
        renderer.close()
        inputListener?.close()
        backend.removeBrowser(this)
        browserApi.close()
    }

    override fun update(width: Int, height: Int) {
        if (!viewport.fullScreen) {
            return
        }

        viewport = viewport.copy(width = width, height = height)
    }

    override fun invalidate() {
        browserApi.clear()
    }

    override fun toString() = "CefBrowser(" +
        "hash='${browserApi.hashCode()}', " +
        "id='${browserApi.identifier}', " +
        "url='$url', " +
        "visible=$visible, " +
        "priority=$priority" +
        ")"

    override fun mouseClicked(mouseX: Double, mouseY: Double, mouseButton: Int) {
        browserApi.setFocus(true)
        val (scaledX, scaledY) = viewport.transformMouse(mouseX, mouseY, GlobalBrowserSettings.quality)
        browserApi.sendMousePress(scaledX, scaledY, mouseButton)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, mouseButton: Int) {
        browserApi.setFocus(true)
        val (scaledX, scaledY) = viewport.transformMouse(mouseX, mouseY, GlobalBrowserSettings.quality)
        browserApi.sendMouseRelease(scaledX, scaledY, mouseButton)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        val (scaledX, scaledY) = viewport.transformMouse(mouseX, mouseY, GlobalBrowserSettings.quality)
        browserApi.sendMouseMove(scaledX, scaledY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, delta: Double) {
        val (scaledX, scaledY) = viewport.transformMouse(mouseX, mouseY, GlobalBrowserSettings.quality)
        browserApi.sendMouseWheel(scaledX, scaledY, delta)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int) {
        browserApi.setFocus(true)
        browserApi.sendKeyPress(keyCode, scanCode.toLong(), modifiers)
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int) {
        browserApi.setFocus(true)
        browserApi.sendKeyRelease(keyCode, scanCode.toLong(), modifiers)
    }

    override fun charTyped(char: Char, modifiers: Int) {
        browserApi.setFocus(true)
        browserApi.sendKeyTyped(char, modifiers)
    }

    private fun comparePaintWithViewpoint(width: Int, height: Int) {
        val (scaledWidth, scaledHeight) = viewport.getScaledDimensions(GlobalBrowserSettings.quality)

        if (scaledWidth != width || scaledHeight != height) {
            logger.warn("Browser $this viewport size mismatch: " +
                "expected $scaledWidth x $scaledHeight, but got $width x $height. ")
            invalidate()
        }
    }

}
