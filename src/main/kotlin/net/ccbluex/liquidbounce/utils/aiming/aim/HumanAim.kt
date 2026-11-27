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
import net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtil
import java.util.Random
import kotlin.math.abs

/**
 * Turns like a person moves a mouse, see [Hand]: it reacts late, flicks in a few ticks, lands close and corrects.
 */
class HumanAim(parent: ModeValueGroup<*>) : Aim(
    "Human",
    parent,
    listOf("Sigmoid", "Interpolation", "Acceleration", "AI", "Minarai"),
) {

    private val speed by float("Speed", 1f, 0.3f..3f, "x")
    private val reaction by intRange("Reaction", 2..4, 0..10, "ticks")

    private val hand = Hand(Random())

    override fun turn(view: Rotation, goal: AimGoal, isFresh: Boolean): RotationDelta {
        hand.speed = speed
        hand.reaction = reaction
        if (isFresh) {
            hand.reset(goal.kind)
        }

        return hand.step(
            view.yaw,
            view.pitch,
            goal.rotation.yaw,
            goal.rotation.pitch,
            goal.width,
            goal.height,
            goal.isReached,
            goal.kind,
            RotationUtil.gcd.toFloat(),
        )
    }

    override fun ticksToReach(from: Rotation, goal: AimGoal): Int {
        val delta = from.rotationDeltaTo(goal.rotation)
        val distance = delta.length()
        if (distance < RotationUtil.gcd / 2) {
            return 0
        }

        hand.speed = speed
        hand.reaction = reaction
        // The hand times a movement by how wide the goal is along it
        val width = if (abs(delta.deltaYaw) >= abs(delta.deltaPitch)) goal.width else goal.height
        return hand.reactionFor(goal.kind).first + hand.ticksFor(distance, width, goal.kind) - 1
    }

}
