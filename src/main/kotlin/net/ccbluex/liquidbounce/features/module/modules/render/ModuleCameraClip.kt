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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MouseScrollEvent
import net.ccbluex.liquidbounce.event.events.MouseScrollInHotbarEvent
import net.ccbluex.liquidbounce.event.events.PerspectiveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.input.isPressed
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.client.option.Perspective
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW

/**
 * CameraClip module
 *
 * Allows you to see through walls in third person view.
 *
 * @author 1zun4, sqlerrorthing
 */
object ModuleCameraClip : ClientModule("CameraClip", Category.RENDER) {
    private val cameraDistance = float("CameraDistance", 4f, 1f..48f)

    init {
        tree(ScrollAdjust)
    }

    val distance
        get() = if (ScrollAdjust.running) {
            ScrollAdjust.scrolledDistance
        } else {
            cameraDistance.get()
        }

    private object ScrollAdjust : ToggleableConfigurable(ModuleCameraClip, "ScrollAdjust", true) {
        private val rememberScrolled by boolean("RememberScrolled", false)
        private val requireFreeLook by boolean("RequireFreeLook", false)
        private val sensitivity by float("Sensitivity", 0.3f, 0.1f..2f)
        private val modifierKey by key("Modifier", GLFW.GLFW_KEY_LEFT_CONTROL)

        var scrolledDistance = cameraDistance.get()
            private set(value) {
                @Suppress("UNCHECKED_CAST")
                field = value.coerceIn(cameraDistance.range as ClosedFloatingPointRange<Float>)
            }

        private val canPerformScroll get() =
            (modifierKey == InputUtil.UNKNOWN_KEY || modifierKey.isPressed)
                && (!requireFreeLook || ModuleFreeLook.running)
                && (mc.options.perspective != Perspective.FIRST_PERSON || ModuleFreeLook.running)

        @Suppress("unused")
        private val resetHandler = handler<PerspectiveEvent>(
            priority = EventPriorityConvention.READ_FINAL_STATE
        ) {
            if (it.perspective == Perspective.FIRST_PERSON) {
                reset()
            }
        }

        @Suppress("unused")
        private val scrollHandler = handler<MouseScrollEvent> {
            if (!canPerformScroll) {
                return@handler
            }

            scrolledDistance += (sensitivity * it.vertical).toFloat()
        }

        @Suppress("unused")
        private val hotbarScrollHandler = handler<MouseScrollInHotbarEvent> {
            if (canPerformScroll) {
                it.cancelEvent()
            }
        }

        fun reset() {
            if (rememberScrolled && scrolledDistance != cameraDistance.get()) {
                cameraDistance.set(scrolledDistance)
            } else {
                scrolledDistance = cameraDistance.get()
            }
        }

        override fun onEnabled() {
            reset()
        }
    }
}
