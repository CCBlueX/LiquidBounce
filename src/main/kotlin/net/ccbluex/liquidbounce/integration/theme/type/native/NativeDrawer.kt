package net.ccbluex.liquidbounce.integration.theme.type.native

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import org.lwjgl.glfw.GLFW

class NativeDrawer(
    var route: NativeDrawableRoute?,
    val takesInput: () -> Boolean = { false }
) : Listenable, AutoCloseable {

    private var hasDrawn = false

    @Suppress
    val gameTickHandler = handler<GameTickEvent> {
        hasDrawn = false
    }

    @Suppress("unused")
    val onScreenRender = handler<ScreenRenderEvent> {
        if (!hasDrawn) {
            hasDrawn = true
        }
        route?.render(it.context, it.partialTicks)
    }

    @Suppress("unused")
    val onOverlayRender = handler<OverlayRenderEvent>(priority = EventPriorityConvention.READ_FINAL_STATE) {
        if (!hasDrawn) {
            hasDrawn = true
        }
        route?.render(it.context, it.tickDelta)
    }

    private var mouseX: Double = 0.0
    private var mouseY: Double = 0.0

    @Suppress("unused")
    val mouseButtonHandler = handler<MouseButtonEvent> { event ->
        if (!takesInput()) return@handler

        if (event.action == GLFW.GLFW_PRESS) {
            route?.mouseClicked(mouseX, mouseY, event.button)
        } else if (event.action == GLFW.GLFW_RELEASE) {
            route?.mouseReleased(mouseX, mouseY, event.button)
        }
    }

    @Suppress("unused")
    val mouseScrollHandler = handler<MouseScrollEvent> {
        // TODO: Implement mouse scroll
    }

    @Suppress("unused")
    val mouseCursorHandler = handler<MouseCursorEvent> { event ->
        val factorW = mc.window.framebufferWidth.toDouble() / mc.window.width.toDouble()
        val factorV = mc.window.framebufferHeight.toDouble() / mc.window.height.toDouble()
        val mouseX = event.x * factorW
        val mouseY = event.y * factorV

        this.mouseX = mouseX
        this.mouseY = mouseY
    }

    @Suppress("unused")
    val keyboardKeyHandler = handler<KeyboardKeyEvent> { event ->
        if (!takesInput()) return@handler

        val action = event.action
        val key = event.keyCode
        val scancode = event.scanCode
        val modifiers = event.mods

        if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
            route?.keyPressed(key, scancode, modifiers)
        } else if (action == GLFW.GLFW_RELEASE) {
            route?.keyReleased(key, scancode, modifiers)
        }
    }

    @Suppress("unused")
    val keyboardCharHandler = handler<KeyboardCharEvent> { event ->
        if (!takesInput()) return@handler

        route?.charTyped(event.codePoint.toChar(), event.modifiers)
    }

    fun select(route: NativeDrawableRoute?) {
        this.route = route
    }

    override fun close() {
        EventManager.unregisterEventHandler(this)
    }

}
