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
import net.ccbluex.liquidbounce.event.events.KeyboardKeyEvent
import net.ccbluex.liquidbounce.event.events.PerspectiveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeLook
import net.ccbluex.liquidbounce.utils.input.isPressed
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.client.CameraType
import org.lwjgl.glfw.GLFW

/**
 * CameraClip module
 *
 * Allows you to see through walls in third person view.
 *
 * @author 1zun4, sqlerrorthing
 */
object ModuleCameraClip : ClientModule("CameraClip", ModuleCategories.RENDER) {
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

    private object ScrollAdjust : ScrollAdjustValueGroup(
        ModuleCameraClip,
        "ScrollAdjust",
        true,
        { delta -> ScrollAdjust.scrolledDistance += delta },
        ScrollAdjustOptions(
            modifierKeyDefault = GLFW.GLFW_KEY_LEFT_CONTROL,
            sensitivityDefault = 0.3f,
            sensitivityRange = 0.1f..2f
        )
    ) {
        private val rememberScrolled by boolean("RememberScrolled", false)
        private val requireFreeLook by boolean("RequireFreeLook", false)

        var scrolledDistance = cameraDistance.get()
            private set(value) {
                @Suppress("UNCHECKED_CAST")
                field = value.coerceIn(cameraDistance.range as ClosedFloatingPointRange<Float>)
            }

        override fun canPerformScroll(): Boolean =
            (modifierKey == InputConstants.UNKNOWN || modifierKey.isPressed)
                && (!requireFreeLook || ModuleFreeLook.running)
                && (mc.options.cameraType != CameraType.FIRST_PERSON || ModuleFreeLook.running)

        @Suppress("unused")
        private val resetHandler = handler<PerspectiveEvent>(
            priority = EventPriorityConvention.READ_FINAL_STATE
        ) {
            if (it.perspective == CameraType.FIRST_PERSON) {
                reset()
            }
        }

        @Suppress("unused")
        private val releaseModifierHandler = handler<KeyboardKeyEvent> {
            if (it.key == modifierKey && it.action == GLFW.GLFW_RELEASE) {
                reset()
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
