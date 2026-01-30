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
package net.ccbluex.liquidbounce.features.module.modules.render.cameraclip

import com.mojang.blaze3d.platform.InputConstants
import it.unimi.dsi.fastutil.floats.FloatConsumer
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.MouseScrollEvent
import net.ccbluex.liquidbounce.event.events.MouseScrollInHotbarEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.input.isPressed
import org.lwjgl.glfw.GLFW

/**
 * A configurable for scroll-adjusting values.
 */
data class ScrollAdjustOptions(
    val modifierKeyDefault: Int = GLFW.GLFW_KEY_LEFT_ALT,
    val sensitivityDefault: Float = 0.5f,
    val sensitivityRange: ClosedFloatingPointRange<Float> = 0.1f..1.0f
)

open class ScrollAdjustValueGroup(
    parent: EventListener?,
    name: String,
    enabled: Boolean,
    private val adjustFunction: FloatConsumer,
    options: ScrollAdjustOptions = ScrollAdjustOptions()
) : ToggleableValueGroup(parent, name, enabled) {

    val modifierKey by key("Modifier", options.modifierKeyDefault)
    val sensitivity by float("Sensitivity", options.sensitivityDefault, options.sensitivityRange)

    open fun canPerformScroll(): Boolean = modifierKey == InputConstants.UNKNOWN || modifierKey.isPressed

    @Suppress("unused")
    private val mouseScrollHandler = handler<MouseScrollEvent> { event ->
        if (!running || !canPerformScroll()) {
            return@handler
        }

        val delta = event.vertical.toFloat() * sensitivity
        adjustFunction.accept(delta)
    }

    @Suppress("unused")
    private val scrollInHotbarHandler = handler<MouseScrollInHotbarEvent> { event ->
        if (running && canPerformScroll()) {
            event.cancelEvent()
        }
    }

}
