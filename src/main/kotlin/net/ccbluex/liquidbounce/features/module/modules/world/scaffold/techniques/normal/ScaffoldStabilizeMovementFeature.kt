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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldNormalTechnique
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.math.copy
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.movement.getDegreesRelativeToView
import net.ccbluex.liquidbounce.utils.movement.getDirectionalInputForDegrees

object ScaffoldStabilizeMovementFeature : ToggleableValueGroup(ScaffoldNormalTechnique, "StabilizeMovement",
    true) {
    private const val MAX_CENTER_DEVIATION: Double = 0.2
    private const val MAX_CENTER_DEVIATION_IF_MOVING_TOWARDS: Double = 0.075

    @Suppress("unused")
    val moveEvent = handler<MovementInputEvent>(priority = EventPriorityConvention.MODEL_STATE) { event ->
        // Prevents the stabilization from giving the player a boost before jumping that cannot be corrected midair.
        if (event.jump && player.onGround()) {
            return@handler
        }

        val optimalLine = ModuleScaffold.currentOptimalLine ?: return@handler
        val currentInput = event.directionalInput

        val nearestPointOnLine = optimalLine.getNearestPointTo(player.position())

        val vecToLine = nearestPointOnLine.subtract(player.position())
        val horizontalVelocity = player.deltaMovement.copy(y = 0.0)
        val isRunningTowardsLine = vecToLine.dot(horizontalVelocity) > 0.0

        val maxDeviation =
            if (isRunningTowardsLine) {
                MAX_CENTER_DEVIATION_IF_MOVING_TOWARDS
            } else {
                MAX_CENTER_DEVIATION
            }

        if (nearestPointOnLine.distanceToSqr(player.position()) < maxDeviation * maxDeviation) {
            return@handler
        }

        val dgs = getDegreesRelativeToView(nearestPointOnLine.subtract(player.position()), player.yRot)

        val newDirectionalInput = getDirectionalInputForDegrees(DirectionalInput.NONE, dgs, deadAngle = 0.0F)

        val frontalAxisBlocked = currentInput.forwards || currentInput.backwards
        val sagittalAxisBlocked = currentInput.right || currentInput.left

        event.directionalInput =
            DirectionalInput(
                if (frontalAxisBlocked) currentInput.forwards else newDirectionalInput.forwards,
                if (frontalAxisBlocked) currentInput.backwards else newDirectionalInput.backwards,
                if (sagittalAxisBlocked) currentInput.left else newDirectionalInput.left,
                if (sagittalAxisBlocked) currentInput.right else newDirectionalInput.right,
            )
    }
}
