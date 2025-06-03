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
package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection
import net.ccbluex.liquidbounce.utils.aiming.features.processors.RotationProcessor
import net.ccbluex.liquidbounce.utils.aiming.features.processors.ShortStopRotationProcessor
import net.ccbluex.liquidbounce.utils.client.RestrictedSingleUseAction
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.minecraft.entity.Entity

/**
 * An aim plan is a plan to aim at a certain rotation.
 * It is being used to calculate the next rotation to aim at.
 *
 * @param rotation The rotation we want to aim at.
 * @param angleSmooth The mode of the smoother.
 */
@Suppress("LongParameterList")
class RotationTarget(
    val rotation: Rotation,
    var entity: Entity? = null,
    /**
     * The rotation processors which are being used to calculate the next rotation.
     * This list should start with [net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.AngleSmooth]
     * and then continue with other processors like [ShortStopRotationProcessor] and [FailFocus].
     */
    val processors: List<RotationProcessor> = emptyList(),
    /**
     * The ticks until reset defines the amount of ticks until we are rotating back.
     */
    val ticksUntilReset: Int,
    /**
     * The reset threshold defines the threshold at which we are going to reset the aim plan.
     * The threshold is being calculated by the distance between the current rotation and the rotation we want to aim.
     *
     * TODO: Replace this with a fixed check that checks if our current mouse movement
     *   outperforms the threshold.
     */
    val resetThreshold: Float,
    /**
     * Consider if the inventory is open or not. If the inventory is open, we might not want to continue updating.
     */
    val considerInventory: Boolean,
    val movementCorrection: MovementCorrection,
    /**
     * What should be done if the target rotation has been reached. Can be `null`.
      */
    val whenReached: RestrictedSingleUseAction? = null
) {

    /**
     * Calculates the next rotation to aim at.
     * [currentRotation] is the current rotation or rather last rotation we aimed at. It is being used to calculate the
     * next rotation.
     *
     * We might even return null if we do not want to aim at anything yet.
     */
    fun towards(currentRotation: Rotation, isResetting: Boolean): Rotation {
        if (isResetting) {
            entity = null
            return process(currentRotation, player.rotation)
        }

        return process(currentRotation, rotation)
    }

    private fun process(currentRotation: Rotation, targetRotation: Rotation): Rotation {
        var targetRotation = targetRotation

        if (processors.isEmpty()) {
            return targetRotation
        }

        for (processor in processors) {
            // We process the rotation with the processor but only the [targetRotation] is being updated.
            targetRotation = processor.process(this, currentRotation, targetRotation)
        }
        return targetRotation
    }

}
