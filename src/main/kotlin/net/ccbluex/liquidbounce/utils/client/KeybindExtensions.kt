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
package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.MouseButtonEvent
import net.ccbluex.liquidbounce.event.handler
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW

val KeyBinding.isPressedOnAny
    get() = pressedOnKeyboard || pressedOnMouse

val KeyBinding.pressedOnKeyboard
    get() = this.boundKey.category == InputUtil.Type.KEYSYM
        && InputUtil.isKeyPressed(mc.window.handle, this.boundKey.code)

val KeyBinding.pressedOnMouse
    get() = this.boundKey.category == InputUtil.Type.MOUSE && MouseStateTracker.isButtonPressed(this.boundKey.code)

object MouseStateTracker : Listenable {

    private val mouseStates = mutableMapOf<Int, Boolean>()

    @Suppress("unused")
    private val handleMouseAction = handler<MouseButtonEvent> {
        mouseStates[it.button] = it.action == GLFW.GLFW_PRESS
    }

    fun isButtonPressed(button: Int) = mouseStates.getOrDefault(button, false)

}


