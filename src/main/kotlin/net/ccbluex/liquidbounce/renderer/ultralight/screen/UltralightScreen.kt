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
package net.ccbluex.liquidbounce.renderer.ultralight.screen

import com.labymedia.ultralight.input.*
import net.ccbluex.liquidbounce.renderer.ultralight.WebView
import net.ccbluex.liquidbounce.renderer.ultralight.theme.ThemeManager
import net.ccbluex.liquidbounce.utils.extensions.asText
import net.ccbluex.liquidbounce.utils.mc
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.util.math.MatrixStack
import org.lwjgl.glfw.GLFW.*


class UltralightScreen(name: String) : Screen(name.asText()) {

    private lateinit var webView: WebView

    private val theme = ThemeManager.defaultTheme
    val page = theme.page(name)

    override fun init() {
        webView = WebView(width = { mc.window.width }, height = { mc.window.height })
        webView.loadPage(page)
        super.init()
    }

    override fun render(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
        webView.update()
        webView.render()
        super.render(matrices, mouseX, mouseY, delta)
    }

    override fun onClose() {
        webView.close()
        super.onClose()
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        mouseButtonCallback(mc.mouse.x, mc.mouse.y, button, GLFW_PRESS)
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        mouseButtonCallback(mc.mouse.x, mc.mouse.y, button, GLFW_RELEASE)
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        // Create the event
        val event = UltralightMouseEvent()
            .x(mc.mouse.x.toInt())
            .y(mc.mouse.y.toInt())
            .type(UltralightMouseEventType.MOVED)

        // Translate the mouse state to the event
        when (GLFW_PRESS) {
            glfwGetMouseButton(
                mc.window.handle,
                GLFW_MOUSE_BUTTON_LEFT
            ) -> event.button(UltralightMouseEventButton.LEFT)
            glfwGetMouseButton(
                mc.window.handle,
                GLFW_MOUSE_BUTTON_MIDDLE
            ) -> event.button(UltralightMouseEventButton.MIDDLE)
            glfwGetMouseButton(
                mc.window.handle,
                GLFW_MOUSE_BUTTON_RIGHT
            ) -> event.button(UltralightMouseEventButton.RIGHT)
        }

        // Fire the event
        webView.lockWebView { it.fireMouseEvent(event) }

        super.mouseMoved(mouseX, mouseY)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        keyCallback(keyCode, scanCode, GLFW_PRESS, modifiers)
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        keyCallback(keyCode, scanCode, GLFW_RELEASE, modifiers)
        return super.keyReleased(keyCode, scanCode, modifiers)
    }

    override fun isPauseScreen(): Boolean {
        return false
    }

    override fun charTyped(chr: Char, keyCode: Int): Boolean {
        // Convert the unicode code point to a UTF-16 string
        val text = chr.toString()

        // Create the event
        val event = UltralightKeyEvent()
            .type(UltralightKeyEventType.CHAR)
            .text(text)
            .unmodifiedText(text)

        // Fire the event

        webView.lockWebView { it.fireKeyEvent(event) }

        return super.charTyped(chr, keyCode)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        // Create the event
        val event = UltralightScrollEvent()
            .deltaX(0)
            .deltaY(amount.toInt() * 32)
            .type(UltralightScrollEventType.BY_PIXEL)

        // Fire the event
        webView.lockWebView { it.fireScrollEvent(event) }

        return super.mouseScrolled(mouseX, mouseY, amount)
    }

    fun keyCallback(key: Int, scancode: Int, action: Int, mods: Int) {
        val translatedKey = glfwToUltralightKey(key)

        // Build the event
        val event = UltralightKeyEvent()
            .type(if (action == GLFW_PRESS || action == GLFW_REPEAT) UltralightKeyEventType.RAW_DOWN else UltralightKeyEventType.UP)
            .virtualKeyCode(translatedKey)
            .nativeKeyCode(scancode)
            .keyIdentifier(UltralightKeyEvent.getKeyIdentifierFromVirtualKeyCode(translatedKey))
            .modifiers(glfwToUltralightModifiers(mods))

        // Send the event
        webView.lockWebView { it.fireKeyEvent(event) }

        if ((action == GLFW_PRESS || action == GLFW_REPEAT) && (key == GLFW_KEY_ENTER || key == GLFW_KEY_TAB)) {
            // These keys need to be translated specially
            val text = if (key == GLFW_KEY_ENTER) "\r" else "\t"
            val extraEvent = UltralightKeyEvent()
                .type(UltralightKeyEventType.CHAR)
                .text(text)
                .unmodifiedText(text)

            // Fire the event
            webView.lockWebView { it.fireKeyEvent(extraEvent) }
        }
    }

    fun mouseButtonCallback(mouseX: Double, mouseY: Double, button: Int, action: Int) {
        // Create the event
        val event = UltralightMouseEvent()
            .x(mc.mouse.x.toInt())
            .y(mc.mouse.y.toInt())
            .type(if (action == GLFW_PRESS) UltralightMouseEventType.DOWN else UltralightMouseEventType.UP)
        when (button) {
            GLFW_MOUSE_BUTTON_LEFT -> event.button(UltralightMouseEventButton.LEFT)
            GLFW_MOUSE_BUTTON_MIDDLE -> event.button(UltralightMouseEventButton.MIDDLE)
            GLFW_MOUSE_BUTTON_RIGHT -> event.button(UltralightMouseEventButton.RIGHT)
        }

        // Fire the event
        webView.lockWebView { it.fireMouseEvent(event) }
    }

    private fun glfwToUltralightModifiers(modifiers: Int): Int {
        var ultralightModifiers = 0
        if (modifiers and GLFW_MOD_ALT != 0) {
            ultralightModifiers = ultralightModifiers or UltralightInputModifier.ALT_KEY
        }
        if (modifiers and GLFW_MOD_CONTROL != 0) {
            ultralightModifiers = ultralightModifiers or UltralightInputModifier.CTRL_KEY
        }
        if (modifiers and GLFW_MOD_SUPER != 0) {
            ultralightModifiers = ultralightModifiers or UltralightInputModifier.META_KEY
        }
        if (modifiers and GLFW_MOD_SHIFT != 0) {
            ultralightModifiers = ultralightModifiers or UltralightInputModifier.SHIFT_KEY
        }
        return ultralightModifiers
    }

    private fun glfwToUltralightKey(key: Int): UltralightKey {
        return when (key) {
            GLFW_KEY_SPACE -> UltralightKey.SPACE
            GLFW_KEY_APOSTROPHE -> UltralightKey.OEM_7
            GLFW_KEY_COMMA -> UltralightKey.OEM_COMMA
            GLFW_KEY_MINUS -> UltralightKey.OEM_MINUS
            GLFW_KEY_PERIOD -> UltralightKey.OEM_PERIOD
            GLFW_KEY_SLASH -> UltralightKey.OEM_2
            GLFW_KEY_0 -> UltralightKey.NUM_0
            GLFW_KEY_1 -> UltralightKey.NUM_1
            GLFW_KEY_2 -> UltralightKey.NUM_2
            GLFW_KEY_3 -> UltralightKey.NUM_3
            GLFW_KEY_4 -> UltralightKey.NUM_4
            GLFW_KEY_5 -> UltralightKey.NUM_5
            GLFW_KEY_6 -> UltralightKey.NUM_6
            GLFW_KEY_7 -> UltralightKey.NUM_7
            GLFW_KEY_8 -> UltralightKey.NUM_8
            GLFW_KEY_9 -> UltralightKey.NUM_9
            GLFW_KEY_SEMICOLON -> UltralightKey.OEM_1
            GLFW_KEY_EQUAL, GLFW_KEY_KP_EQUAL -> UltralightKey.OEM_PLUS
            GLFW_KEY_A -> UltralightKey.A
            GLFW_KEY_B -> UltralightKey.B
            GLFW_KEY_C -> UltralightKey.C
            GLFW_KEY_D -> UltralightKey.D
            GLFW_KEY_E -> UltralightKey.E
            GLFW_KEY_F -> UltralightKey.F
            GLFW_KEY_G -> UltralightKey.G
            GLFW_KEY_H -> UltralightKey.H
            GLFW_KEY_I -> UltralightKey.I
            GLFW_KEY_J -> UltralightKey.J
            GLFW_KEY_K -> UltralightKey.K
            GLFW_KEY_L -> UltralightKey.L
            GLFW_KEY_M -> UltralightKey.M
            GLFW_KEY_N -> UltralightKey.N
            GLFW_KEY_O -> UltralightKey.O
            GLFW_KEY_P -> UltralightKey.P
            GLFW_KEY_Q -> UltralightKey.Q
            GLFW_KEY_R -> UltralightKey.R
            GLFW_KEY_S -> UltralightKey.S
            GLFW_KEY_T -> UltralightKey.T
            GLFW_KEY_U -> UltralightKey.U
            GLFW_KEY_V -> UltralightKey.V
            GLFW_KEY_W -> UltralightKey.W
            GLFW_KEY_X -> UltralightKey.X
            GLFW_KEY_Y -> UltralightKey.Y
            GLFW_KEY_Z -> UltralightKey.Z
            GLFW_KEY_LEFT_BRACKET -> UltralightKey.OEM_4
            GLFW_KEY_BACKSLASH -> UltralightKey.OEM_5
            GLFW_KEY_RIGHT_BRACKET -> UltralightKey.OEM_6
            GLFW_KEY_GRAVE_ACCENT -> UltralightKey.OEM_3
            GLFW_KEY_ESCAPE -> UltralightKey.ESCAPE
            GLFW_KEY_ENTER, GLFW_KEY_KP_ENTER -> UltralightKey.RETURN
            GLFW_KEY_TAB -> UltralightKey.TAB
            GLFW_KEY_BACKSPACE -> UltralightKey.BACK
            GLFW_KEY_INSERT -> UltralightKey.INSERT
            GLFW_KEY_DELETE -> UltralightKey.DELETE
            GLFW_KEY_RIGHT -> UltralightKey.RIGHT
            GLFW_KEY_LEFT -> UltralightKey.LEFT
            GLFW_KEY_DOWN -> UltralightKey.DOWN
            GLFW_KEY_UP -> UltralightKey.UP
            GLFW_KEY_PAGE_UP -> UltralightKey.PRIOR
            GLFW_KEY_PAGE_DOWN -> UltralightKey.NEXT
            GLFW_KEY_HOME -> UltralightKey.HOME
            GLFW_KEY_END -> UltralightKey.END
            GLFW_KEY_CAPS_LOCK -> UltralightKey.CAPITAL
            GLFW_KEY_SCROLL_LOCK -> UltralightKey.SCROLL
            GLFW_KEY_NUM_LOCK -> UltralightKey.NUMLOCK
            GLFW_KEY_PRINT_SCREEN -> UltralightKey.SNAPSHOT
            GLFW_KEY_PAUSE -> UltralightKey.PAUSE
            GLFW_KEY_F1 -> UltralightKey.F1
            GLFW_KEY_F2 -> UltralightKey.F2
            GLFW_KEY_F3 -> UltralightKey.F3
            GLFW_KEY_F4 -> UltralightKey.F4
            GLFW_KEY_F5 -> UltralightKey.F5
            GLFW_KEY_F6 -> UltralightKey.F6
            GLFW_KEY_F7 -> UltralightKey.F7
            GLFW_KEY_F8 -> UltralightKey.F8
            GLFW_KEY_F9 -> UltralightKey.F9
            GLFW_KEY_F10 -> UltralightKey.F10
            GLFW_KEY_F11 -> UltralightKey.F11
            GLFW_KEY_F12 -> UltralightKey.F12
            GLFW_KEY_F13 -> UltralightKey.F13
            GLFW_KEY_F14 -> UltralightKey.F14
            GLFW_KEY_F15 -> UltralightKey.F15
            GLFW_KEY_F16 -> UltralightKey.F16
            GLFW_KEY_F17 -> UltralightKey.F17
            GLFW_KEY_F18 -> UltralightKey.F18
            GLFW_KEY_F19 -> UltralightKey.F19
            GLFW_KEY_F20 -> UltralightKey.F20
            GLFW_KEY_F21 -> UltralightKey.F21
            GLFW_KEY_F22 -> UltralightKey.F22
            GLFW_KEY_F23 -> UltralightKey.F23
            GLFW_KEY_F24 -> UltralightKey.F24
            GLFW_KEY_KP_0 -> UltralightKey.NUMPAD0
            GLFW_KEY_KP_1 -> UltralightKey.NUMPAD1
            GLFW_KEY_KP_2 -> UltralightKey.NUMPAD2
            GLFW_KEY_KP_3 -> UltralightKey.NUMPAD3
            GLFW_KEY_KP_4 -> UltralightKey.NUMPAD4
            GLFW_KEY_KP_5 -> UltralightKey.NUMPAD5
            GLFW_KEY_KP_6 -> UltralightKey.NUMPAD6
            GLFW_KEY_KP_7 -> UltralightKey.NUMPAD7
            GLFW_KEY_KP_8 -> UltralightKey.NUMPAD8
            GLFW_KEY_KP_9 -> UltralightKey.NUMPAD9
            GLFW_KEY_KP_DECIMAL -> UltralightKey.DECIMAL
            GLFW_KEY_KP_DIVIDE -> UltralightKey.DIVIDE
            GLFW_KEY_KP_MULTIPLY -> UltralightKey.MULTIPLY
            GLFW_KEY_KP_SUBTRACT -> UltralightKey.SUBTRACT
            GLFW_KEY_KP_ADD -> UltralightKey.ADD
            GLFW_KEY_LEFT_SHIFT, GLFW_KEY_RIGHT_SHIFT -> UltralightKey.SHIFT
            GLFW_KEY_LEFT_CONTROL, GLFW_KEY_RIGHT_CONTROL -> UltralightKey.CONTROL
            GLFW_KEY_LEFT_ALT, GLFW_KEY_RIGHT_ALT -> UltralightKey.MENU
            GLFW_KEY_LEFT_SUPER -> UltralightKey.LWIN
            GLFW_KEY_RIGHT_SUPER -> UltralightKey.RWIN
            else -> UltralightKey.UNKNOWN
        }
    }

}
