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

package net.ccbluex.liquidbounce.deeplearn.data

import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.minecraft.world.phys.Vec3

@JvmRecord
data class CombatSample(
    val currentVector: Vec3,
    val previousVector: Vec3,
    val targetVector: Vec3,
    val playerDiff: Vec3,
    val targetDiff: Vec3,
    val distance: Float,
) {

    val currentRotation
        get() = Rotation.fromRotationVec(currentVector)
    val targetRotation
        get() = Rotation.fromRotationVec(targetVector)
    val previousRotation
        get() = Rotation.fromRotationVec(previousVector)

    /**
     * Total delta should be in a positive direction,
     * going from the current rotation to the target rotation.
     */
    val totalDelta
        get() = currentRotation.rotationDeltaTo(targetRotation)

    /**
     * Velocity delta should be in a positive direction,
     * going from the previous rotation to the current rotation.
     */
    val previousVelocityDelta
        get() = previousRotation.rotationDeltaTo(currentRotation)

    val asInput: FloatArray
        get() {
            val totalDelta = totalDelta
            val previousVelocityDelta = previousVelocityDelta
            return floatArrayOf(
                totalDelta.deltaYaw,
                totalDelta.deltaPitch,
                previousVelocityDelta.deltaYaw,
                previousVelocityDelta.deltaPitch,
                // Speed
                targetDiff.horizontalDistance().toFloat() + playerDiff.horizontalDistance().toFloat(),
                distance,
            )
        }

}
