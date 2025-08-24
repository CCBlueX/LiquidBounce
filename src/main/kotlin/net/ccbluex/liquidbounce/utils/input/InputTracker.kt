/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

package net.ccbluex.liquidbounce.utils.input

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.MouseButtonEvent
import net.ccbluex.liquidbounce.event.handler
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW

/**
 * Singleton object that tracks the state of mouse buttons and key presses.
 * It listens for mouse button events and provides utility functions to check if
 * a key or mouse button is currently pressed or was recently pressed.
 */
object InputTracker : EventListener {

    /**
     * Tracks the state of each mouse button.
     *
     * [GLFW.GLFW_RELEASE], [GLFW.GLFW_PRESS] or [GLFW.GLFW_REPEAT]
     * @see GLFW
     */
    private val mouseStates = IntArray(32)

    /**
     * Tracks the last time each mouse button was pressed.
     * Array indices correspond to GLFW mouse button codes.
     */
    private val mouseLastPressed = LongArray(32)

    /**
     * Tracks the last time each keyboard key was pressed.
     * Map key is the GLFW key code, value is the timestamp.
     */
    private val keyLastPressed = mutableMapOf<Int, Long>()

    /**
     * Extension property that checks if a key binding is pressed on either the keyboard or mouse.
     *
     * @return True if the key binding is pressed on any input device, false otherwise.
     */
    val KeyBinding.isPressedOnAny: Boolean
        get() = pressedOnKeyboard || pressedOnMouse

    /**
     * Extension property that checks if a key binding is pressed on the keyboard.
     *
     * @return True if the key is pressed on the keyboard, false otherwise.
     */
    val KeyBinding.pressedOnKeyboard: Boolean
        get() = this.boundKey.category == InputUtil.Type.KEYSYM
            && boundKey.isPressed

    /**
     * Extension property that checks if a key binding is pressed on the mouse.
     *
     * @return True if the mouse button is pressed, false otherwise.
     */
    val KeyBinding.pressedOnMouse: Boolean
        get() = this.boundKey.category == InputUtil.Type.MOUSE && isMouseButtonPressed(this.boundKey.code)

    /**
     * Extension property that checks if a key binding was pressed recently.
     *
     * @param withinMs The time window in milliseconds to check within.
     * @return True if the key binding was pressed within the specified time, false otherwise.
     */
    fun KeyBinding.wasPressedRecently(withinMs: Long): Boolean {
        return when (this.boundKey.category) {
            InputUtil.Type.KEYSYM -> wasKeyPressedRecently(this.boundKey.code, withinMs)
            InputUtil.Type.MOUSE -> wasMouseButtonPressedRecently(this.boundKey.code, withinMs)
            else -> false
        }
    }

    /**
     * Extension property that gets the time elapsed since the key binding was last pressed.
     *
     * @return Milliseconds since last press, or Long.MAX_VALUE if never pressed.
     */
    val KeyBinding.timeSinceLastPress: Long
        get() {
            return when (this.boundKey.category) {
                InputUtil.Type.KEYSYM -> getTimeSinceKeyPress(this.boundKey.code)
                InputUtil.Type.MOUSE -> getTimeSinceMousePress(this.boundKey.code)
                else -> Long.MAX_VALUE
            }
        }

    /**
     * Event handler for mouse button actions. It updates the mouseStates map
     * and tracks timing when a mouse button is pressed or released.
     */
    @Suppress("unused")
    private val handleMouseAction = handler<MouseButtonEvent> { event ->
        mouseStates[event.button] = event.action

        // Track when the button was pressed
        if (event.action == GLFW.GLFW_PRESS) {
            mouseLastPressed[event.button] = System.currentTimeMillis()
        }
    }

    /**
     * Checks if the specified mouse button is currently pressed.
     *
     * @param button The GLFW code of the mouse button.
     * @return True if the mouse button is pressed, false otherwise.
     */
    fun isMouseButtonPressed(button: Int): Boolean = mouseStates[button] == GLFW.GLFW_PRESS

    /**
     * Checks if the specified mouse button was pressed recently.
     *
     * @param button The GLFW code of the mouse button.
     * @param withinMs The time window in milliseconds to check within.
     * @return True if the mouse button was pressed within the specified time, false otherwise.
     */
    fun wasMouseButtonPressedRecently(button: Int, withinMs: Long): Boolean {
        val lastPressed = mouseLastPressed[button]
        return lastPressed > 0 && (System.currentTimeMillis() - lastPressed) <= withinMs
    }

    /**
     * Gets the time elapsed since the specified mouse button was last pressed.
     *
     * @param button The GLFW code of the mouse button.
     * @return Milliseconds since last press, or Long.MAX_VALUE if never pressed.
     */
    fun getTimeSinceMousePress(button: Int): Long {
        val lastPressed = mouseLastPressed[button]
        return if (lastPressed > 0) {
            System.currentTimeMillis() - lastPressed
        } else {
            Long.MAX_VALUE
        }
    }

    /**
     * Checks if the specified keyboard key was pressed recently.
     * Note: This requires manual tracking via updateKeyPress() since we don't have a keyboard event handler.
     *
     * @param keyCode The GLFW key code.
     * @param withinMs The time window in milliseconds to check within.
     * @return True if the key was pressed within the specified time, false otherwise.
     */
    fun wasKeyPressedRecently(keyCode: Int, withinMs: Long): Boolean {
        val lastPressed = keyLastPressed[keyCode] ?: return false
        return (System.currentTimeMillis() - lastPressed) <= withinMs
    }

    /**
     * Gets the time elapsed since the specified keyboard key was last pressed.
     *
     * @param keyCode The GLFW key code.
     * @return Milliseconds since last press, or Long.MAX_VALUE if never pressed.
     */
    fun getTimeSinceKeyPress(keyCode: Int): Long {
        val lastPressed = keyLastPressed[keyCode]
        return if (lastPressed != null) {
            System.currentTimeMillis() - lastPressed
        } else {
            Long.MAX_VALUE
        }
    }

}
