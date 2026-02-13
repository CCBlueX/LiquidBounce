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
import net.minecraft.util.Util
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.lwjgl.glfw.GLFW

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

    /**
     * Normalizes keyboard modifiers for macOS to ensure cross-platform compatibility with CEF.
     *
     * On macOS, the Command key (⌘) is mapped to GLFW_MOD_SUPER, while Windows/Linux use
     * GLFW_MOD_CONTROL for system shortcuts. This function ensures both modifiers are sent
     * to CEF when either is pressed, providing maximum compatibility with the browser engine's
     * internal shortcut handling.
     */
    private fun normalizeModifiersForPlatform(modifiers: Int): Int {
        if (Util.getPlatform() != Util.OS.OSX) {
            return modifiers
        }

        val hasSuper = (modifiers and GLFW.GLFW_MOD_SUPER) != 0
        val hasControl = (modifiers and GLFW.GLFW_MOD_CONTROL) != 0

        if (hasSuper || hasControl) {
            return modifiers or GLFW.GLFW_MOD_SUPER or GLFW.GLFW_MOD_CONTROL
        }

        return modifiers
    }

    /**
     * Executes clipboard copy operation via JavaScript.
     *
     * Uses the modern Clipboard API (navigator.clipboard.writeText) for browser-based copy operations.
     * Supports both text inputs/textareas and general page selections.
     */
    private fun executeCopyScript() {
        val script = """
            (function() {
                var text = '';
                var activeEl = document.activeElement;
                if (activeEl && (activeEl.tagName === 'INPUT' || activeEl.tagName === 'TEXTAREA')) {
                    var start = activeEl.selectionStart;
                    var end = activeEl.selectionEnd;
                    if (start !== end) {
                        text = activeEl.value.substring(start, end);
                    }
                } else {
                    text = window.getSelection().toString();
                }
                if (text && navigator.clipboard) {
                    navigator.clipboard.writeText(text);
                }
            })()
        """.trimIndent()
        browserApi.executeJavaScript(script, "", 1)
    }

    /**
     * Executes clipboard paste operation by injecting text from system clipboard into focused element.
     *
     * Reads from native system clipboard via GLFW and programmatically inserts into the active
     * input element. This approach is necessary because CEF's clipboard integration has
     * limitations on macOS with keyboard shortcut handling.
     */
    private fun executePasteScript(clipboard: String) {
        val escaped = clipboard
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")

        val script = """
            (function() {
                var text = "$escaped";
                var activeEl = document.activeElement;
                if (activeEl && (activeEl.tagName === 'INPUT' || activeEl.tagName === 'TEXTAREA')) {
                    var start = activeEl.selectionStart || 0;
                    var end = activeEl.selectionEnd || 0;
                    var value = activeEl.value || '';
                    activeEl.value = value.substring(0, start) + text + value.substring(end);
                    activeEl.selectionStart = activeEl.selectionEnd = start + text.length;
                    activeEl.dispatchEvent(new Event('input', { bubbles: true }));
                }
            })()
        """.trimIndent()
        browserApi.executeJavaScript(script, "", 1)
    }

    /**
     * Executes clipboard cut operation via JavaScript.
     *
     * Removes selected text from the active element and copies it to the clipboard using
     * the modern Clipboard API.
     */
    private fun executeCutScript() {
        val script = """
            (function() {
                var text = '';
                var activeEl = document.activeElement;
                if (activeEl && (activeEl.tagName === 'INPUT' || activeEl.tagName === 'TEXTAREA')) {
                    var start = activeEl.selectionStart;
                    var end = activeEl.selectionEnd;
                    if (start !== end) {
                        text = activeEl.value.substring(start, end);
                        activeEl.value = activeEl.value.substring(0, start) + activeEl.value.substring(end);
                        activeEl.selectionStart = activeEl.selectionEnd = start;
                        activeEl.dispatchEvent(new Event('input', { bubbles: true }));
                    }
                } else {
                    text = window.getSelection().toString();
                    if (text) { document.execCommand('delete'); }
                }
                if (text && navigator.clipboard) {
                    navigator.clipboard.writeText(text);
                }
            })()
        """.trimIndent()
        browserApi.executeJavaScript(script, "", 1)
    }

    /**
     * Executes select all operation for the focused element or document.
     *
     * Uses element-specific selection for inputs/textareas for better reliability.
     */
    private fun executeSelectAllScript() {
        val script = """
            (function() {
                var activeEl = document.activeElement;
                if (activeEl && (activeEl.tagName === 'INPUT' || activeEl.tagName === 'TEXTAREA')) {
                    activeEl.select();
                } else {
                    document.execCommand('selectAll');
                }
            })()
        """.trimIndent()
        browserApi.executeJavaScript(script, "", 1)
    }

    /**
     * Handles macOS-specific keyboard shortcuts for clipboard operations.
     *
     * On macOS, CEF/JCEF has known issues with properly handling system keyboard shortcuts
     * (Command+C/V/X/A) due to modifier key mapping differences between the OS and the browser.
     * This method intercepts these shortcuts and implements them via JavaScript and native
     * clipboard access, ensuring consistent behavior across platforms.
     *
     * This is a platform-specific optimization for macOS where the Command key (GLFW_MOD_SUPER)
     * is the standard modifier for system shortcuts, unlike Windows/Linux which use Control.
     */
    private fun handleMacShortcut(keyCode: Int, modifiers: Int): Boolean {
        if (Util.getPlatform() != Util.OS.OSX) {
            return false
        }

        val hasCommandOrControl = (modifiers and GLFW.GLFW_MOD_SUPER) != 0 ||
                                   (modifiers and GLFW.GLFW_MOD_CONTROL) != 0

        if (!hasCommandOrControl) {
            return false
        }

        return when (keyCode) {
            GLFW.GLFW_KEY_C -> {
                executeCopyScript()
                true
            }
            GLFW.GLFW_KEY_V -> {
                val clipboard = GLFW.glfwGetClipboardString(mc.window.handle()) ?: ""
                if (clipboard.isNotEmpty()) {
                    executePasteScript(clipboard)
                }
                true
            }
            GLFW.GLFW_KEY_X -> {
                executeCutScript()
                true
            }
            GLFW.GLFW_KEY_A -> {
                executeSelectAllScript()
                true
            }
            else -> false
        }
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int) {
        browserApi.setFocus(true)

        if (handleMacShortcut(keyCode, modifiers)) {
            return
        }

        val normalizedModifiers = normalizeModifiersForPlatform(modifiers)
        browserApi.sendKeyPress(keyCode, scanCode.toLong(), normalizedModifiers)
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int) {
        browserApi.setFocus(true)
        val normalizedModifiers = normalizeModifiersForPlatform(modifiers)
        browserApi.sendKeyRelease(keyCode, scanCode.toLong(), normalizedModifiers)
    }

    override fun charTyped(char: Char, modifiers: Int) {
        browserApi.setFocus(true)
        val normalizedModifiers = normalizeModifiersForPlatform(modifiers)
        browserApi.sendKeyTyped(char, normalizedModifiers)
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
