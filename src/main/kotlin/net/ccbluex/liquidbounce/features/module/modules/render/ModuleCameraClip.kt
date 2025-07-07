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

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.event.events.MouseScrollEvent
import net.ccbluex.liquidbounce.event.events.MouseScrollInHotbarEvent
import net.ccbluex.liquidbounce.event.events.PerspectiveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.client.option.Perspective
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW

private val BASE_DISTANCE_RANGE = 1f..48f
private const val DEFAULT_DISTANCE = 4f

/**
 * CameraClip module
 *
 * Allows you to see through walls in third person view.
 *
 * @author 1zun4, sqlerrorthing
 */
object ModuleCameraClip : ClientModule("CameraClip", Category.RENDER), CameraDistance {
    internal val distanceMode = choices(
        "Distance",
        FixedCameraDistance,
        arrayOf(FixedCameraDistance, ScrollAdjustCameraDistance)
    )

    override val distance: Float
        get() = distanceMode.activeChoice.distance
}

internal object ScrollAdjustCameraDistance : CameraDistanceChoice("ScrollAdjust") {
    private var baseDistance by float("BaseDistance", DEFAULT_DISTANCE, BASE_DISTANCE_RANGE)
    private val rememberScrolled by boolean("RememberScrolled", false)
    private val sensitivity by float("Sensitivity", 0.3f, 0.1f..3f)
    private val modifierKey by key("Modifier", GLFW.GLFW_KEY_LEFT_CONTROL)

    private var scrolledDistance = DEFAULT_DISTANCE
        set(value) {
            field = value.coerceIn(BASE_DISTANCE_RANGE)
        }

    private val canPerformScroll get() =
        (modifierKey == InputUtil.UNKNOWN_KEY || InputUtil.isKeyPressed(mc.window.handle, modifierKey.code))
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

        scrolledDistance = scrolledDistance + (sensitivity * it.vertical).toFloat()
    }

    @Suppress("unused")
    private val hotbarScrollHandler = handler<MouseScrollInHotbarEvent> {
        if (canPerformScroll) {
            it.cancelEvent()
        }
    }

    fun reset() {
        if (rememberScrolled && scrolledDistance != baseDistance) {
            baseDistance = scrolledDistance
        } else {
            scrolledDistance = baseDistance
        }
    }

    override fun enable() {
        reset()
    }

    override val distance get() = scrolledDistance
}

internal object FixedCameraDistance : CameraDistanceChoice("Fixed") {
    override val distance by float("CameraDistance", DEFAULT_DISTANCE, BASE_DISTANCE_RANGE)
}

internal sealed class CameraDistanceChoice(name: String) : Choice(name), CameraDistance {
    override val parent
        get() = ModuleCameraClip.distanceMode
}

internal sealed interface CameraDistance {
    val distance: Float
}
