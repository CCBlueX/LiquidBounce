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
package net.ccbluex.liquidbounce.config.types.nesting

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.MouseScrollEvent
import net.ccbluex.liquidbounce.event.events.MouseScrollInHotbarEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.input.isPressed
import net.minecraft.client.util.InputUtil

/**
 * A configurable for scroll-adjusting values.
 */

data class ScrollAdjustOptions(
    val modifierKeyDefault: Int = InputUtil.GLFW_KEY_LEFT_ALT,
    val sensitivityDefault: Float = 0.5f,
    val sensitivityRange: ClosedFloatingPointRange<Float> = 0.1f..1.0f
)

open class ScrollAdjustConfigurable(
    parent: EventListener?,
    name: String,
    default: Boolean,
    private val adjustFunction: (Float) -> Unit,
    options: ScrollAdjustOptions = ScrollAdjustOptions()
) : ToggleableConfigurable(parent, name, default) {

    val modifierKey by key("Modifier", options.modifierKeyDefault)
    val sensitivity by float("Sensitivity", options.sensitivityDefault, options.sensitivityRange)

    open fun canPerformScroll(): Boolean = modifierKey == InputUtil.UNKNOWN_KEY || modifierKey.isPressed

    init {
        handler<MouseScrollEvent> { event ->
            if (!running) return@handler
            if (!canPerformScroll()) return@handler
            val delta = event.vertical.toFloat() * sensitivity
            adjustFunction(delta)
        }

        handler<MouseScrollInHotbarEvent> {
            if (running && canPerformScroll()) {
                it.cancelEvent()
            }
        }
    }
}
