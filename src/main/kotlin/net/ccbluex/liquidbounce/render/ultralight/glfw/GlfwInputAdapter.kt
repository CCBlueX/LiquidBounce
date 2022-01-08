/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2022 CCBlueX
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
package net.ccbluex.liquidbounce.render.ultralight.glfw

import com.labymedia.ultralight.input.*
import net.ccbluex.liquidbounce.render.ultralight.UltralightEngine
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.system.MemoryStack
import java.nio.DoubleBuffer

class GlfwInputAdapter {

    /**
     * Called by GLFW when a key is pressed.
     *
     * @param window   The window that caused the event
     * @param key      The GLFW keycode
     * @param scancode The keyboard scancode
     * @param action   The GLFW action
     * @param mods     The key modifiers
     */
    fun keyCallback(window: Long, key: Int, scancode: Int, action: Int, mods: Int) {
        val translatedKey = glfwToUltralightKey(key)

        // Build the event
        val event = UltralightKeyEvent()
            .type(if (action == GLFW_PRESS || action == GLFW_REPEAT) UltralightKeyEventType.RAW_DOWN else UltralightKeyEventType.UP)
            .virtualKeyCode(translatedKey)
            .nativeKeyCode(scancode)
            .keyIdentifier(UltralightKeyEvent.getKeyIdentifierFromVirtualKeyCode(translatedKey))
            .modifiers(glfwToUltralightModifiers(mods))

        // Send the event
        UltralightEngine.activeView?.fireKeyEvent(event)
        if ((action == GLFW_PRESS || action == GLFW_REPEAT) && (key == GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_TAB)) {
            // These keys need to be translated specially
            val text = if (key == GLFW_KEY_ENTER) "\r" else "\t"
            val extraEvent = UltralightKeyEvent()
                .type(UltralightKeyEventType.CHAR)
                .text(text)
                .unmodifiedText(text)

            // Fire the event
            UltralightEngine.activeView?.fireKeyEvent(extraEvent)
        }
    }

    /**
     * Called by GLFW when a char is input.
     *
     * @param window    The window that caused the event
     * @param codepoint The unicode char that has been input
     */
    fun charCallback(window: Long, codepoint: Int) {
        // Convert the unicode code point to a UTF-16 string
        val text = String(Character.toChars(codepoint))

        // Create the event
        val event = UltralightKeyEvent()
            .type(UltralightKeyEventType.CHAR)
            .text(text)
            .unmodifiedText(text)

        // Fire the event
        UltralightEngine.activeView?.fireKeyEvent(event)
    }

    /**
     * Called by GLFW when the mouse moves.
     *
     * @param window The window that caused the event
     * @param x      The new x position of the cursor
     * @param y      The new y position of the cursor
     */
    fun cursorPosCallback(window: Long, x: Double, y: Double) {
        // Create the event
        val event = UltralightMouseEvent()
            .x((x * 1f).toInt())
            .y((y * 1f).toInt())
            .type(UltralightMouseEventType.MOVED)
            .button(
                when (GLFW_PRESS) {
                    GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) -> UltralightMouseEventButton.LEFT
                    GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_MIDDLE) -> UltralightMouseEventButton.MIDDLE
                    GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) -> UltralightMouseEventButton.RIGHT
                    else -> null
                }
            )

        // Fire the event
        UltralightEngine.activeView?.fireMouseEvent(event)
    }

    /**
     * Called by GLFW when a mouse button changes its state.
     *
     * @param window The window that caused the event
     * @param button the button that changed its state
     * @param action The new state of the button
     * @param mods   The mouse modifiers
     */
    fun mouseButtonCallback(window: Long, button: Int, action: Int, mods: Int) {
        var x: Double
        var y: Double
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.callocDouble(2)

            // Retrieve the current cursor pos
            glfwGetCursorPos(
                window,
                buffer.slice().position(0) as DoubleBuffer,
                buffer.slice().position(1) as DoubleBuffer
            )

            // Extract the x and y position
            x = buffer[0]
            y = buffer[1]
        }

        // Create the event
        val event = UltralightMouseEvent()
            .x((x * 1f).toInt())
            .y((y * 1f).toInt())
            .type(if (action == GLFW_PRESS) UltralightMouseEventType.DOWN else UltralightMouseEventType.UP)
        when (button) {
            GLFW.GLFW_MOUSE_BUTTON_LEFT -> event.button(UltralightMouseEventButton.LEFT)
            GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> event.button(UltralightMouseEventButton.MIDDLE)
            GLFW.GLFW_MOUSE_BUTTON_RIGHT -> event.button(UltralightMouseEventButton.RIGHT)
        }

        // Fire the event
        UltralightEngine.activeView?.fireMouseEvent(event)
    }

    /**
     * Called by GLFW when the user scrolls within the window.
     *
     * @param window The window that caused the event
     * @param xDelta The x scroll delta
     * @param yDelta The y scroll delta
     */
    fun scrollCallback(window: Long, xDelta: Double, yDelta: Double) {
        // Create the event
        val event = UltralightScrollEvent()
            .deltaX(xDelta.toInt() * 32)
            .deltaY(yDelta.toInt() * 32)
            .type(UltralightScrollEventType.BY_PIXEL)

        // Fire the event
        UltralightEngine.activeView?.fireScrollEvent(event)
    }

    /**
     * Called by GLFW when the window gains or looses focus.
     *
     * @param window The window that caused the event
     * @param focus  Whether the window gained focus
     */
    fun focusCallback(window: Long, focus: Boolean) {
        if (focus) {
            UltralightEngine.activeView?.focus()
        } else {
            UltralightEngine.activeView?.unfocus()
        }
    }

    /**
     * Translates GLFW key modifiers to Ultralight key modifiers.
     *
     * @param modifiers The GLFW key modifiers to translate
     * @return The translated Ultralight key modifiers
     */
    private fun glfwToUltralightModifiers(modifiers: Int): Int {
        var ultralightModifiers = 0
        if (modifiers and GLFW.GLFW_MOD_ALT != 0) {
            ultralightModifiers = ultralightModifiers or UltralightInputModifier.ALT_KEY
        }
        if (modifiers and GLFW.GLFW_MOD_CONTROL != 0) {
            ultralightModifiers = ultralightModifiers or UltralightInputModifier.CTRL_KEY
        }
        if (modifiers and GLFW.GLFW_MOD_SUPER != 0) {
            ultralightModifiers = ultralightModifiers or UltralightInputModifier.META_KEY
        }
        if (modifiers and GLFW.GLFW_MOD_SHIFT != 0) {
            ultralightModifiers = ultralightModifiers or UltralightInputModifier.SHIFT_KEY
        }
        return ultralightModifiers
    }

    /**
     * Translates a GLFW key code to an [UltralightKey].
     *
     * @param key The GLFW key code to translate
     * @return The translated Ultralight key, or [UltralightKey.UNKNOWN], if the key could not be translated
     */
    private fun glfwToUltralightKey(key: Int) = when (key) {
        GLFW.GLFW_KEY_SPACE -> UltralightKey.SPACE
        GLFW.GLFW_KEY_APOSTROPHE -> UltralightKey.OEM_7
        GLFW.GLFW_KEY_COMMA -> UltralightKey.OEM_COMMA
        GLFW.GLFW_KEY_MINUS -> UltralightKey.OEM_MINUS
        GLFW.GLFW_KEY_PERIOD -> UltralightKey.OEM_PERIOD
        GLFW.GLFW_KEY_SLASH -> UltralightKey.OEM_2
        GLFW.GLFW_KEY_0 -> UltralightKey.NUM_0
        GLFW.GLFW_KEY_1 -> UltralightKey.NUM_1
        GLFW.GLFW_KEY_2 -> UltralightKey.NUM_2
        GLFW.GLFW_KEY_3 -> UltralightKey.NUM_3
        GLFW.GLFW_KEY_4 -> UltralightKey.NUM_4
        GLFW.GLFW_KEY_5 -> UltralightKey.NUM_5
        GLFW.GLFW_KEY_6 -> UltralightKey.NUM_6
        GLFW.GLFW_KEY_7 -> UltralightKey.NUM_7
        GLFW.GLFW_KEY_8 -> UltralightKey.NUM_8
        GLFW.GLFW_KEY_9 -> UltralightKey.NUM_9
        GLFW.GLFW_KEY_SEMICOLON -> UltralightKey.OEM_1
        GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_KP_EQUAL -> UltralightKey.OEM_PLUS
        GLFW.GLFW_KEY_A -> UltralightKey.A
        GLFW.GLFW_KEY_B -> UltralightKey.B
        GLFW.GLFW_KEY_C -> UltralightKey.C
        GLFW.GLFW_KEY_D -> UltralightKey.D
        GLFW.GLFW_KEY_E -> UltralightKey.E
        GLFW.GLFW_KEY_F -> UltralightKey.F
        GLFW.GLFW_KEY_G -> UltralightKey.G
        GLFW.GLFW_KEY_H -> UltralightKey.H
        GLFW.GLFW_KEY_I -> UltralightKey.I
        GLFW.GLFW_KEY_J -> UltralightKey.J
        GLFW.GLFW_KEY_K -> UltralightKey.K
        GLFW.GLFW_KEY_L -> UltralightKey.L
        GLFW.GLFW_KEY_M -> UltralightKey.M
        GLFW.GLFW_KEY_N -> UltralightKey.N
        GLFW.GLFW_KEY_O -> UltralightKey.O
        GLFW.GLFW_KEY_P -> UltralightKey.P
        GLFW.GLFW_KEY_Q -> UltralightKey.Q
        GLFW.GLFW_KEY_R -> UltralightKey.R
        GLFW.GLFW_KEY_S -> UltralightKey.S
        GLFW.GLFW_KEY_T -> UltralightKey.T
        GLFW.GLFW_KEY_U -> UltralightKey.U
        GLFW.GLFW_KEY_V -> UltralightKey.V
        GLFW.GLFW_KEY_W -> UltralightKey.W
        GLFW.GLFW_KEY_X -> UltralightKey.X
        GLFW.GLFW_KEY_Y -> UltralightKey.Y
        GLFW.GLFW_KEY_Z -> UltralightKey.Z
        GLFW.GLFW_KEY_LEFT_BRACKET -> UltralightKey.OEM_4
        GLFW.GLFW_KEY_BACKSLASH -> UltralightKey.OEM_5
        GLFW.GLFW_KEY_RIGHT_BRACKET -> UltralightKey.OEM_6
        GLFW.GLFW_KEY_GRAVE_ACCENT -> UltralightKey.OEM_3
        GLFW.GLFW_KEY_ESCAPE -> UltralightKey.ESCAPE
        GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> UltralightKey.RETURN
        GLFW.GLFW_KEY_TAB -> UltralightKey.TAB
        GLFW.GLFW_KEY_BACKSPACE -> UltralightKey.BACK
        GLFW.GLFW_KEY_INSERT -> UltralightKey.INSERT
        GLFW.GLFW_KEY_DELETE -> UltralightKey.DELETE
        GLFW.GLFW_KEY_RIGHT -> UltralightKey.RIGHT
        GLFW.GLFW_KEY_LEFT -> UltralightKey.LEFT
        GLFW.GLFW_KEY_DOWN -> UltralightKey.DOWN
        GLFW.GLFW_KEY_UP -> UltralightKey.UP
        GLFW.GLFW_KEY_PAGE_UP -> UltralightKey.PRIOR
        GLFW.GLFW_KEY_PAGE_DOWN -> UltralightKey.NEXT
        GLFW.GLFW_KEY_HOME -> UltralightKey.HOME
        GLFW.GLFW_KEY_END -> UltralightKey.END
        GLFW.GLFW_KEY_CAPS_LOCK -> UltralightKey.CAPITAL
        GLFW.GLFW_KEY_SCROLL_LOCK -> UltralightKey.SCROLL
        GLFW.GLFW_KEY_NUM_LOCK -> UltralightKey.NUMLOCK
        GLFW.GLFW_KEY_PRINT_SCREEN -> UltralightKey.SNAPSHOT
        GLFW.GLFW_KEY_PAUSE -> UltralightKey.PAUSE
        GLFW.GLFW_KEY_F1 -> UltralightKey.F1
        GLFW.GLFW_KEY_F2 -> UltralightKey.F2
        GLFW.GLFW_KEY_F3 -> UltralightKey.F3
        GLFW.GLFW_KEY_F4 -> UltralightKey.F4
        GLFW.GLFW_KEY_F5 -> UltralightKey.F5
        GLFW.GLFW_KEY_F6 -> UltralightKey.F6
        GLFW.GLFW_KEY_F7 -> UltralightKey.F7
        GLFW.GLFW_KEY_F8 -> UltralightKey.F8
        GLFW.GLFW_KEY_F9 -> UltralightKey.F9
        GLFW.GLFW_KEY_F10 -> UltralightKey.F10
        GLFW.GLFW_KEY_F11 -> UltralightKey.F11
        GLFW.GLFW_KEY_F12 -> UltralightKey.F12
        GLFW.GLFW_KEY_F13 -> UltralightKey.F13
        GLFW.GLFW_KEY_F14 -> UltralightKey.F14
        GLFW.GLFW_KEY_F15 -> UltralightKey.F15
        GLFW.GLFW_KEY_F16 -> UltralightKey.F16
        GLFW.GLFW_KEY_F17 -> UltralightKey.F17
        GLFW.GLFW_KEY_F18 -> UltralightKey.F18
        GLFW.GLFW_KEY_F19 -> UltralightKey.F19
        GLFW.GLFW_KEY_F20 -> UltralightKey.F20
        GLFW.GLFW_KEY_F21 -> UltralightKey.F21
        GLFW.GLFW_KEY_F22 -> UltralightKey.F22
        GLFW.GLFW_KEY_F23 -> UltralightKey.F23
        GLFW.GLFW_KEY_F24 -> UltralightKey.F24
        GLFW.GLFW_KEY_KP_0 -> UltralightKey.NUMPAD0
        GLFW.GLFW_KEY_KP_1 -> UltralightKey.NUMPAD1
        GLFW.GLFW_KEY_KP_2 -> UltralightKey.NUMPAD2
        GLFW.GLFW_KEY_KP_3 -> UltralightKey.NUMPAD3
        GLFW.GLFW_KEY_KP_4 -> UltralightKey.NUMPAD4
        GLFW.GLFW_KEY_KP_5 -> UltralightKey.NUMPAD5
        GLFW.GLFW_KEY_KP_6 -> UltralightKey.NUMPAD6
        GLFW.GLFW_KEY_KP_7 -> UltralightKey.NUMPAD7
        GLFW.GLFW_KEY_KP_8 -> UltralightKey.NUMPAD8
        GLFW.GLFW_KEY_KP_9 -> UltralightKey.NUMPAD9
        GLFW.GLFW_KEY_KP_DECIMAL -> UltralightKey.DECIMAL
        GLFW.GLFW_KEY_KP_DIVIDE -> UltralightKey.DIVIDE
        GLFW.GLFW_KEY_KP_MULTIPLY -> UltralightKey.MULTIPLY
        GLFW.GLFW_KEY_KP_SUBTRACT -> UltralightKey.SUBTRACT
        GLFW.GLFW_KEY_KP_ADD -> UltralightKey.ADD
        GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> UltralightKey.SHIFT
        GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> UltralightKey.CONTROL
        GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> UltralightKey.MENU
        GLFW.GLFW_KEY_LEFT_SUPER -> UltralightKey.LWIN
        GLFW.GLFW_KEY_RIGHT_SUPER -> UltralightKey.RWIN
        else -> UltralightKey.UNKNOWN
    }

}
