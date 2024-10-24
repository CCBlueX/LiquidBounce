/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.MouseButtonEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW

/**
 * Singleton object that tracks the state of mouse buttons and key presses.
 * It listens for mouse button events and provides utility functions to check if
 * a key or mouse button is currently pressed.
 */
object InputTracker : Listenable {

    // Tracks the state of each mouse button (pressed or not).
    private val mouseStates = mutableMapOf<Int, Boolean>()

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
            && InputUtil.isKeyPressed(mc.window.handle, this.boundKey.code)

    /**
     * Extension property that checks if a key binding is pressed on the mouse.
     *
     * @return True if the mouse button is pressed, false otherwise.
     */
    val KeyBinding.pressedOnMouse: Boolean
        get() = this.boundKey.category == InputUtil.Type.MOUSE && isMouseButtonPressed(this.boundKey.code)

    /**
     * Event handler for mouse button actions. It updates the mouseStates map
     * when a mouse button is pressed or released.
     */
    @Suppress("unused")
    private val handleMouseAction = handler<MouseButtonEvent> {
        mouseStates[it.button] = it.action == GLFW.GLFW_PRESS
    }

    /**
     * Checks if the specified mouse button is currently pressed.
     *
     * @param button The GLFW code of the mouse button.
     * @return True if the mouse button is pressed, false otherwise.
     */
    fun isMouseButtonPressed(button: Int): Boolean = mouseStates.getOrDefault(button, false)
}
