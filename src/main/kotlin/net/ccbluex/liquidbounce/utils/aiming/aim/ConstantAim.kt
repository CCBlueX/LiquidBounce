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
package net.ccbluex.liquidbounce.utils.aiming.aim

import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.data.RotationDelta
import net.ccbluex.liquidbounce.utils.kotlin.random
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

/**
 * Turns straight at the goal with a fixed speed, instantly by default.
 */
class ConstantAim(
    parent: ModeValueGroup<*>,
    horizontalTurnSpeed: ClosedFloatingPointRange<Float> = 180f..180f,
    verticalTurnSpeed: ClosedFloatingPointRange<Float> = 180f..180f,
) : Aim("Constant", parent, listOf("Linear")) {

    private val horizontalTurnSpeed by floatRange("HorizontalTurnSpeed", horizontalTurnSpeed, 0f..180f)
    private val verticalTurnSpeed by floatRange("VerticalTurnSpeed", verticalTurnSpeed, 0f..180f)

    override fun turn(view: Rotation, goal: AimGoal, fresh: Boolean): RotationDelta = view.rotationDeltaTo(
        view.towardsLinear(goal.rotation, horizontalTurnSpeed.random(), verticalTurnSpeed.random())
    )

    override fun ticksToReach(from: Rotation, to: Rotation): Int {
        val delta = from.rotationDeltaTo(to)
        val yawTicks = ticks(abs(delta.deltaYaw), horizontalTurnSpeed.start)
        val pitchTicks = ticks(abs(delta.deltaPitch), verticalTurnSpeed.start)
        return max(0, max(yawTicks, pitchTicks) - 1)
    }

    private fun ticks(degrees: Float, speed: Float) = if (degrees == 0f) 0 else ceil(degrees / speed).toInt()

}
