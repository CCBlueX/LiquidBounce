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

import com.google.gson.annotations.SerializedName
import net.ccbluex.liquidbounce.config.gson.util.readJson
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import java.io.File

@JvmRecord
data class CombatSample(
    @SerializedName(CURRENT_DIRECTION_VECTOR)
    val currentVector: Vec3,
    @SerializedName(PREVIOUS_DIRECTION_VECTOR)
    val previousVector: Vec3,
    @SerializedName(TARGET_DIRECTION_VECTOR)
    val targetVector: Vec3,
    @SerializedName(DELTA_VECTOR)
    val velocityDelta: Vec2,

    @SerializedName(P_DIFF)
    val playerDiff: Vec3,
    @SerializedName(T_DIFF)
    val targetDiff: Vec3,

    @SerializedName(DISTANCE)
    val distance: Float,

    @SerializedName(HURT_TIME)
    val hurtTime: Int,
    /**
     * Age in this case is the Entity Age, however, we will use it later to determine
     * the time we have been tracking this entity.
     */
    @SerializedName(AGE)
    val age: Int
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
        get() = floatArrayOf(
            // Total Delta
            totalDelta.deltaYaw,
            totalDelta.deltaPitch,

            // Velocity Delta
            previousVelocityDelta.deltaYaw,
            previousVelocityDelta.deltaPitch,

            // Speed
            targetDiff.horizontalDistance().toFloat() + playerDiff.horizontalDistance().toFloat(),

            // Distance
            distance
        )

    val asOutput
        get() = floatArrayOf(
            velocityDelta.x,
            velocityDelta.y
        )

    companion object {
        const val CURRENT_DIRECTION_VECTOR = "a"
        const val PREVIOUS_DIRECTION_VECTOR = "b"
        const val TARGET_DIRECTION_VECTOR = "c"
        const val DELTA_VECTOR = "d"
        const val HURT_TIME = "e"
        const val AGE = "f"
        const val P_DIFF = "g"
        const val T_DIFF = "h"
        const val DISTANCE = "i"

        private fun parse(file: File): List<CombatSample> = when {
            file.isDirectory -> file.listFiles().flatMap(::parse)
            file.extension == "json" -> file.readJson<List<CombatSample>>()
            else -> emptyList()
        }

        fun parse(vararg files: File): List<CombatSample> = files.flatMap(::parse)

    }
}

