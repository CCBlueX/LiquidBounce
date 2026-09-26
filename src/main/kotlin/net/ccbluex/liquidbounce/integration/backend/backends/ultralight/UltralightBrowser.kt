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
package net.ccbluex.liquidbounce.integration.backend.backends.ultralight

import com.mojang.blaze3d.systems.RenderSystem
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
import net.ccbluex.liquidbounce.utils.client.clientLogger
import net.ccbluex.liquidbounce.utils.client.mc
import net.janrupf.ujr.api.UltralightMouseEventBuilder
import net.janrupf.ujr.api.UltralightScrollEventBuilder
import net.janrupf.ujr.api.UltralightSession
import net.janrupf.ujr.api.UltralightView
import net.janrupf.ujr.api.UltralightViewConfigBuilder
import net.janrupf.ujr.api.bitmap.UltralightBitmapSurface
import net.janrupf.ujr.api.cursor.UlCursor
import net.janrupf.ujr.api.event.UlScrollEventType
import net.janrupf.ujr.api.listener.UlMessageLevel
import net.janrupf.ujr.api.listener.UlMessageSource
import net.janrupf.ujr.api.listener.UltralightLoadListener
import net.janrupf.ujr.api.listener.UltralightViewListener
import org.joml.component1
import org.joml.component2

/**
 * How far one step of the mouse wheel scrolls, in pixels.
 */
private const val SCROLL_STEP = 50

/**
 * Ultralight's WebKit is built without media elements, but pages still check for them, Svelte for example.
 */
private const val MEDIA_ELEMENTS_SCRIPT = """
for (const name of ['HTMLMediaElement', 'HTMLAudioElement', 'HTMLVideoElement']) {
    if (typeof window[name] === 'undefined') {
        window[name] = class extends HTMLElement {};
    }
}
"""

/**
 * A browser of the Ultralight backend.
 *
 * Ultralight is not thread-safe, so everything that reaches the view is moved to the render thread, which the
 * backend drives Ultralight on.
 */
@Suppress("TooManyFunctions")
class UltralightBrowser internal constructor(
    private val backend: UltralightBrowserBackend,
    url: String,
    viewport: BrowserViewport,
    val settings: BrowserSettings,
    override var priority: Short = 0,
    override val isIncognito: Boolean = false,
    inputAcceptor: InputAcceptor? = null
) : Browser, InputHandler {

    private val logger = clientLogger("UltralightBrowser/${System.identityHashCode(this)}")

    /**
     * An incognito browser gets a session which keeps nothing on disk.
     */
    private val session: UltralightSession? = if (isIncognito) {
        backend.renderer.createSession(false, "incognito-${System.identityHashCode(this)}")
    } else {
        null
    }

    private val view: UltralightView
    private val paintTarget = UltralightPaintTarget()

    @Volatile
    private var currentUrl = url

    /**
     * Set once a main frame load failed, so finishing it doesn't count as success.
     */
    private var failedLoad = false

    init {
        require(url.isNotEmpty()) { "URL cannot be empty." }

        val quality = GlobalBrowserSettings.quality
        val (width, height) = viewport.getScaledDimensions(quality)
        val config = UltralightViewConfigBuilder()
            .transparent(true)
            .initialDeviceScale(quality)
            .build()

        view = backend.renderer.createView(width, height, config, session)
        view.setLoadListener(LoadListener())
        view.setViewListener(ViewListener())
        view.loadURL(url)

        logger.info("Initialized browser (url='$url')")
    }

    override val isInitialized = true

    override var state: BrowserState = BrowserState.Idle
        private set(value) {
            field = value

            when (value) {
                is BrowserState.Loading -> logger.info("Started loading (url='$currentUrl')")
                is BrowserState.Success -> logger.info("Finished loading (url='$currentUrl')")
                is BrowserState.Failure -> logger.warn(
                    "Failed to load (url='${value.failedUrl}', errorCode=${value.errorCode}, " +
                        "errorText=${value.errorText})"
                )
                else -> { /* Idle state, do nothing */ }
            }
        }

    override var viewport: BrowserViewport = viewport
        set(value) {
            field = value

            onRenderThread {
                val quality = GlobalBrowserSettings.quality
                val (width, height) = value.getScaledDimensions(quality)

                if (view.width() != width.toLong() || view.height() != height.toLong()) {
                    view.resize(width.toLong(), height.toLong())
                }
                view.setDeviceScale(quality.toDouble())
            }
        }

    override var visible = true

    private val renderer = BrowserRenderer(this)
    private val inputListener: InputListener? = inputAcceptor?.let { InputListener(this, this, it) }

    override var url: String
        get() = currentUrl
        set(value) {
            currentUrl = value
            onRenderThread {
                state = BrowserState.Idle
                view.loadURL(value)
            }
        }

    override val texture: BrowserTexture?
        get() {
            if (!paintTarget.isTextureReady || paintTarget.isUnpainted) {
                return null
            }

            return BrowserTexture(paintTarget.textureSetup!!, viewport.width, viewport.height, true)
        }

    /**
     * Uploads what Ultralight painted since the last frame, called after the backend rendered.
     */
    fun paint() {
        paintTarget.paint(view.surface() as UltralightBitmapSurface)
    }

    override fun forceReload() = onRenderThread { view.reload() }

    override fun reload() = onRenderThread { view.reload() }

    override fun goForward() = onRenderThread { view.goForward() }

    override fun goBack() = onRenderThread { view.goBack() }

    override fun close() {
        renderer.close()
        inputListener?.close()
        backend.removeBrowser(this)

        onRenderThread {
            // The view is freed once it is garbage collected, so stop the page right away
            view.stop()
            view.loadURL("about:blank")
            paintTarget.close()
        }
    }

    override fun update(width: Int, height: Int) {
        if (!viewport.fullScreen) {
            return
        }

        viewport = viewport.copy(width = width, height = height)
    }

    override fun invalidate() = onRenderThread { view.setNeedsPaint(true) }

    override fun toString() = "UltralightBrowser(" +
        "url='$currentUrl', " +
        "incognito=$isIncognito, " +
        "visible=$visible, " +
        "priority=$priority" +
        ")"

    override fun mouseClicked(mouseX: Double, mouseY: Double, mouseButton: Int) {
        val button = UltralightInput.mouseButton(mouseButton) ?: return
        val (x, y) = viewport.transformMouse(mouseX, mouseY, GlobalBrowserSettings.quality)

        view.focus()
        view.fireMouseEvent(UltralightMouseEventBuilder.down(button).x(x).y(y).build())
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, mouseButton: Int) {
        val button = UltralightInput.mouseButton(mouseButton) ?: return
        val (x, y) = viewport.transformMouse(mouseX, mouseY, GlobalBrowserSettings.quality)

        view.fireMouseEvent(UltralightMouseEventBuilder.up(button).x(x).y(y).build())
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        val (x, y) = viewport.transformMouse(mouseX, mouseY, GlobalBrowserSettings.quality)

        view.fireMouseEvent(UltralightMouseEventBuilder.moved().x(x).y(y).build())
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, delta: Double) {
        view.fireScrollEvent(
            UltralightScrollEventBuilder(UlScrollEventType.BY_PIXEL)
                .deltaY((delta * SCROLL_STEP).toInt())
                .build()
        )
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int) {
        view.focus()
        UltralightInput.keyEvents(true, keyCode, scanCode, modifiers).forEach(view::fireKeyEvent)
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int) {
        UltralightInput.keyEvents(false, keyCode, scanCode, modifiers).forEach(view::fireKeyEvent)
    }

    override fun charTyped(codepoint: Int) {
        view.focus()
        view.fireKeyEvent(UltralightInput.characterEvent(codepoint))
    }

    private fun onRenderThread(action: () -> Unit) {
        if (RenderSystem.isOnRenderThread()) {
            action()
        } else {
            mc.execute(action)
        }
    }

    private inner class LoadListener : UltralightLoadListener {

        override fun onBeginLoading(view: UltralightView, frameId: Long, isMainFrame: Boolean, url: String) {
            if (isMainFrame) {
                failedLoad = false
                state = BrowserState.Loading
            }
        }

        override fun onWindowObjectReady(view: UltralightView, frameId: Long, isMainFrame: Boolean, url: String) {
            // Runs before any script of the page
            if (isMainFrame) {
                view.evaluateScript(MEDIA_ELEMENTS_SCRIPT)
            }
        }

        override fun onFinishLoading(view: UltralightView, frameId: Long, isMainFrame: Boolean, url: String) {
            // Ultralight doesn't tell the status code, the page loaded all the same
            if (isMainFrame && !failedLoad) {
                state = BrowserState.Success(200)
            }
        }

        @Suppress("LongParameterList")
        override fun onFailLoading(
            view: UltralightView,
            frameId: Long,
            isMainFrame: Boolean,
            url: String,
            description: String,
            errorDomain: String,
            errorCode: Int
        ) {
            if (isMainFrame) {
                failedLoad = true
                state = BrowserState.Failure(errorCode, description, url)
            }
        }

    }

    private inner class ViewListener : UltralightViewListener {

        override fun onChangeURL(view: UltralightView, url: String) {
            currentUrl = url
        }

        @Suppress("LongParameterList")
        override fun onAddConsoleMessage(
            view: UltralightView,
            source: UlMessageSource,
            level: UlMessageLevel,
            message: String,
            lineNumber: Long,
            columnNumber: Long,
            sourceId: String
        ) {
            val text = "[console] $message ($sourceId:$lineNumber:$columnNumber)"
            if (level == UlMessageLevel.ERROR || level == UlMessageLevel.WARNING) {
                logger.warn(text)
            } else {
                logger.debug(text)
            }
        }

        override fun onChangeCursor(view: UltralightView, cursor: UlCursor) {
            if (visible) {
                mc.window.selectCursor(UltralightInput.cursor(cursor))
            }
        }

    }

}
