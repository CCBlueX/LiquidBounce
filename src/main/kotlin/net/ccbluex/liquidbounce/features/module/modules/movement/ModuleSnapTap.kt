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

package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.events.KeyboardKeyEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove.ModuleInventoryMove
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.client.input.KeyEvent
import org.lwjgl.glfw.GLFW

/**
 * Snaptap module
 *
 * Prioritize the last pressed movement key.
 */

object ModuleSnapTap : ClientModule("SnapTap", ModuleCategories.MOVEMENT, aliases = listOf("NullMove", "SOCD")) {

    private class AxisState {
        var holdingNegative = false // Left Key
        var holdingPositive = false // Right Key
        var lastNegativePressTime = 0L
        var lastPositivePressTime = 0L

        fun onPress(positive: Boolean) {
            if (positive) {
                holdingPositive = true
                lastPositivePressTime = System.nanoTime()
            } else {
                holdingNegative = true
                lastNegativePressTime = System.nanoTime()
            }
        }

        fun onRelease(positive: Boolean) {
            if (positive) holdingPositive = false else holdingNegative = false
        }
    }

    // Holding Left and Right key
    private val horizontal = AxisState()
    // Holding Forward and Backward key
    private val vertical = AxisState()

    @Suppress("unused")
    val onKeyState = handler<KeyboardKeyEvent> { event ->
        val keyboardLeft = mc.options.keyLeft
        val keyboardRight = mc.options.keyRight
        val keyboardForward = mc.options.keyUp
        val keyboardBack = mc.options.keyDown

        val keyEvent = KeyEvent(event.keyCode, event.scanCode, event.mods)
        val pressed = event.action == GLFW.GLFW_PRESS
        val released = event.action == GLFW.GLFW_RELEASE

        when {
            keyboardLeft.matches(keyEvent) -> {
                if (pressed) { horizontal.onPress(false) }
                else if (released) { horizontal.onRelease(false) }
            }
            keyboardRight.matches(keyEvent) -> {
                if (pressed) { horizontal.onPress(true) }
                else if (released) { horizontal.onRelease(true) }
            }
            keyboardBack.matches(keyEvent) -> {
                if (pressed) { vertical.onPress(false) }
                else if (released) { vertical.onRelease(false) }
            }
            keyboardForward.matches(keyEvent) -> {
                if (pressed) { vertical.onPress(true) }
                else if (released) { vertical.onRelease(true) }
            }
        }
    }

    @Suppress("unused")
    val onMovementInput = handler<MovementInputEvent> { event ->
        if (mc.gui.screen() != null && !ModuleInventoryMove.enabled) return@handler

        var isKeyLeftHeld = event.directionalInput.left
        var isKeyRightHeld = event.directionalInput.right
        var isKeyForwardHeld = event.directionalInput.forwards
        var isKeyBackwardHeld = event.directionalInput.backwards

        if (horizontal.holdingNegative && horizontal.holdingPositive) {
            val posIsLast = horizontal.lastPositivePressTime >= horizontal.lastNegativePressTime
            isKeyLeftHeld = !posIsLast
            isKeyRightHeld = posIsLast
        }

        if (vertical.holdingNegative && vertical.holdingPositive) {
            val posIsLast = vertical.lastPositivePressTime >= vertical.lastNegativePressTime
            isKeyBackwardHeld = !posIsLast
            isKeyForwardHeld = posIsLast
        }

        event.directionalInput = DirectionalInput(isKeyForwardHeld, isKeyBackwardHeld, isKeyLeftHeld, isKeyRightHeld)
    }
}
