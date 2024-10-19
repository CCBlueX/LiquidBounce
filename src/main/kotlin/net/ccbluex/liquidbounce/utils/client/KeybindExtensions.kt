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

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.client.util.InputUtil.Type.*
import org.lwjgl.glfw.GLFW

val KeyBinding.isPressedOnAny
    get() = pressedOnKeyboard || pressedOnMouse

val KeyBinding.pressedOnKeyboard
    get() = this.boundKey.category == KEYSYM
        && InputUtil.isKeyPressed(mc.window.handle, this.boundKey.code)

val KeyBinding.pressedOnMouse
    get() = this.boundKey.category == MOUSE && MouseStateTracker.isButtonPressed(this.boundKey.code)

object MouseStateTracker : Listenable {

    private val mouseStates = IntArray(32)

    @Suppress("unused")
    private val handleMouseAction = handler<MouseButtonEvent> {
        mouseStates[it.button] = it.action
    }

    fun isButtonPressed(button: Int) = mouseStates[button]  == GLFW.GLFW_PRESS

}

class KeyBindingTracker internal constructor(val keyBinding: KeyBinding) : Listenable {
    private val countByTick = IntArray(20)
    private var tickIndex = 0
    private var currentCount = 0

    val cps: Int
        get() = countByTick.sum()

    var pressed = false
        private set(value) {
            if (value) {
                currentCount++
            }
            field = value
        }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun setPressed(action: Int) {
        when (action) {
            GLFW.GLFW_RELEASE -> pressed = false
            GLFW.GLFW_PRESS -> pressed = true
        }
    }

    init {
        when (keyBinding.boundKey.category) {
            KEYSYM -> handler<KeyEvent> {
                if (keyBinding.boundKey.code == it.key.keyCode) {
                    setPressed(it.action)
                    EventManager.callEvent(KeyBindingEvent(keyBinding, it))
                }
            }
            MOUSE -> handler<MouseButtonEvent> {
                if (keyBinding.boundKey.code == it.button) {
                    setPressed(it.action)
                    EventManager.callEvent(KeyBindingEvent(keyBinding, it))
                }
            }
            else -> throw UnsupportedOperationException("Unknown key binding type: $keyBinding")
        }

        handler<PlayerTickEvent> {
            countByTick[tickIndex] = currentCount
            currentCount = 0
            tickIndex = (tickIndex + 1) % countByTick.size
            EventManager.callEvent(KeyBindingCPSEvent(keyBinding, cps))
        }
    }
}

object KeyBindingTrackers : Listenable {
    private val trackers = listOf(
        KeyBindingTracker(mc.options.forwardKey),
        KeyBindingTracker(mc.options.backKey),
        KeyBindingTracker(mc.options.leftKey),
        KeyBindingTracker(mc.options.rightKey),
        KeyBindingTracker(mc.options.jumpKey),
        KeyBindingTracker(mc.options.attackKey),
        KeyBindingTracker(mc.options.useKey),
    )

    override fun children() = trackers
}

