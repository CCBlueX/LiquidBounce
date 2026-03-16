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

package net.ccbluex.liquidbounce.utils.input

import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.KeyboardKeyEvent
import net.ccbluex.liquidbounce.event.events.MouseButtonEvent
import net.ccbluex.liquidbounce.event.handler
import net.minecraft.client.KeyMapping
import org.lwjgl.glfw.GLFW

/**
 * Tracking if an input constant is pressed, released or repeated,
 * and when it was last pressed. Also tracks how long an input has been pressed repeatedly.
 */
object InputTracker : EventListener {

    private const val COMBO_TIMEOUT_MS = 250L

    /**
     * Tracks the state of each mouse button.
     *
     * [GLFW.GLFW_RELEASE], [GLFW.GLFW_PRESS] or [GLFW.GLFW_REPEAT]
     * @see GLFW
     */
    private val inputStates = mutableMapOf<InputConstants.Key, Int>()

    /**
     * Tracks the last time an input was pressed.
     * Map key is the [InputConstants.Key] with value being the last time it was pressed in milliseconds.
     */
    private val inputLastPressed = mutableMapOf<InputConstants.Key, Long>()

    /**
     * Tracks how long we have been pressing a key continuously.
     */
    private val inputComboStartTime = mutableMapOf<InputConstants.Key, Long>()

    /**
     * Extension property that checks if a key binding is pressed on either the keyboard or mouse.
     *
     * @return True if the key binding is pressed on any input device, false otherwise.
     */
    val KeyMapping.isPressedOnAny: Boolean
        get() = pressedOnKeyboard || pressedOnMouse

    /**
     * Extension property that checks if a key binding is pressed on the keyboard.
     *
     * @return True if the key is pressed on the keyboard, false otherwise.
     */
    val KeyMapping.pressedOnKeyboard: Boolean
        get() = this.key.type == InputConstants.Type.KEYSYM
            && key.isPressed

    /**
     * Extension property that checks if a key binding is pressed on the mouse.
     *
     * @return True if the mouse button is pressed, false otherwise.
     */
    val KeyMapping.pressedOnMouse: Boolean
        get() = this.key.type == InputConstants.Type.MOUSE && isMouseButtonPressed(this.key)

    /**
     * Extension property that checks if a key binding was pressed recently.
     *
     * @param withinMs The time window in milliseconds to check within.
     * @return True if the key binding was pressed within the specified time, false otherwise.
     */
    fun KeyMapping.wasPressedRecently(withinMs: Long) = wasInputPressedRecently(this.key, withinMs)

    /**
     * Extension property that gets the time elapsed since the key binding was last pressed.
     *
     * @return Milliseconds since last press, or Long.MAX_VALUE if never pressed.
     */
    val KeyMapping.timeSinceLastPress: Long
        get() = getTimeSinceInputPress(this.key)

    /**
     * Extension property that gets the time elapsed since the key binding combo started.
     *
     * @return Milliseconds since combo start, or Long.MAX_VALUE if no combo is tracked.
     */
    val KeyMapping.timeSinceComboStart: Long
        get() = getTimeSinceComboStart(this.key)

    /**
     * Event handler for mouse button actions. It updates the mouseStates map
     * and tracks timing when a mouse button is pressed or released.
     */
    @Suppress("unused")
    private val handleMouseAction = handler<MouseButtonEvent> { event ->
        inputStates[event.key] = event.action

        // Track when the button was pressed
        if (event.action == GLFW.GLFW_PRESS) {
            updateInputPress(event.key)
        }
    }

    @Suppress("unused")
    private val handleKeyAction = handler<KeyboardKeyEvent> { event ->
        if (event.action == GLFW.GLFW_PRESS) {
            updateInputPress(event.key)
        }
    }

    @Suppress("unused")
    private val handleTick = handler<GameTickEvent> {
        val now = System.currentTimeMillis()
        inputComboStartTime.entries.removeIf { entry ->
            val lastPressed = inputLastPressed[entry.key]
            lastPressed == null || now - lastPressed > COMBO_TIMEOUT_MS
        }
    }

    /**
     * Checks if the specified mouse button is currently pressed.
     *
     * @param button The GLFW code of the mouse button.
     * @return True if the mouse button is pressed, false otherwise.
     */
    fun isMouseButtonPressed(button: InputConstants.Key): Boolean = inputStates[button] == GLFW.GLFW_PRESS

    /**
     * Checks if the specified mouse button was pressed recently.
     *
     * @param button The GLFW code of the mouse button.
     * @param withinMs The time window in milliseconds to check within.
     * @return True if the mouse button was pressed within the specified time, false otherwise.
     */
    fun wasInputPressedRecently(keyCode: InputConstants.Key, withinMs: Long): Boolean {
        val lastPressed = inputLastPressed[keyCode] ?: return false
        return lastPressed > 0 && (System.currentTimeMillis() - lastPressed) <= withinMs
    }

    /**
     * Gets the time elapsed since the specified input was last pressed.
     *
     * @param keyCode The GLFW key code.
     * @return Milliseconds since last press, or Long.MAX_VALUE if never pressed.
     */
    fun getTimeSinceInputPress(keyCode: InputConstants.Key): Long {
        val lastPressed = inputLastPressed[keyCode] ?: return Long.MAX_VALUE
        return if (lastPressed > 0) {
            System.currentTimeMillis() - lastPressed
        } else {
            Long.MAX_VALUE
        }
    }

    /**
     * Gets the time elapsed since the specified input combo started.
     *
     * @param keyCode The GLFW key code.
     * @return Milliseconds since combo start, or Long.MAX_VALUE if no combo is tracked.
     */
    fun getTimeSinceComboStart(keyCode: InputConstants.Key): Long {
        val comboStart = inputComboStartTime[keyCode] ?: return Long.MAX_VALUE
        return if (comboStart > 0) {
            System.currentTimeMillis() - comboStart
        } else {
            Long.MAX_VALUE
        }
    }

    /**
     * Updates the last pressed time for the specified input.
     */
    fun updateInputPress(keyCode: InputConstants.Key) {
        inputLastPressed[keyCode] = System.currentTimeMillis()
        if (!inputComboStartTime.containsKey(keyCode)) {
            inputComboStartTime[keyCode] = System.currentTimeMillis()
        }
    }

    fun resetCombo(keyCode: InputConstants.Key) {
        inputComboStartTime.remove(keyCode)
    }

}
