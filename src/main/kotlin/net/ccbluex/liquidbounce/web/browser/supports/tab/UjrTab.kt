package net.ccbluex.liquidbounce.web.browser.supports.tab

import net.ccbluex.liquidbounce.interfaces.IMixinGameRenderer
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.web.browser.supports.UjrBrowser
import net.janrupf.ujr.example.glfw.surface.GlfwSurface
import net.janrupf.ujr.example.glfw.web.WebWindow
import org.lwjgl.glfw.GLFW

@Suppress("TooManyFunctions")
class UjrTab(
    private val ujrBrowser: UjrBrowser,
    private val url: String,
    override val takesInput: () -> Boolean
) : ITab, InputAware {

    private val ujrWindow = WebWindow({ mc.window.framebufferWidth }, { mc.window.framebufferHeight })

    override var drawn = false
    override var preferOnTop = false

    override fun forceReload() {
        ujrWindow.view.reload()
    }

    init {
        loadUrl(url)
    }

    override fun loadUrl(url: String) {
        ujrWindow.view.loadURL(url)
    }

    override fun getUrl() = url
    override fun closeTab() {
        ujrWindow.view.stop()
        ujrBrowser.removeTab(this)
    }

    override fun getTexture() = (ujrWindow.view.surface() as GlfwSurface).texture

    override fun getShader() = (mc.gameRenderer as IMixinGameRenderer).bgraPositionTextureShader

    override fun resize(width: Int, height: Int) {
        if (width <= 100 || height <= 100) {
            return
        }

        ujrWindow.resizeIfNeeded()
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, mouseButton: Int) {
        ujrWindow.onMouseButton(mouseButton, GLFW.GLFW_PRESS)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, mouseButton: Int) {
        ujrWindow.onMouseButton(mouseButton, GLFW.GLFW_RELEASE)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        ujrWindow.onCursorPos(mouseX, mouseY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, delta: Double) {
        ujrWindow.onScroll(0.0, delta)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int) {
        ujrWindow.onKey(keyCode, scanCode, GLFW.GLFW_PRESS, modifiers)
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int) {
        ujrWindow.onKey(keyCode, scanCode, GLFW.GLFW_RELEASE, modifiers)
    }

    override fun charTyped(codePoint: Char, modifiers: Int) {
        if (codePoint == 0.toChar()) {
            return
        }

        ujrWindow.onChar(codePoint.code)
    }

}
