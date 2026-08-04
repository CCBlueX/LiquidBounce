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
) : TrainingSample {

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

    override val inputSize: Int
        get() = INPUT_SIZE

    override val outputSize: Int
        get() = OUTPUT_SIZE

    override fun fillAsInput(dest: FloatArray, fromIndex: Int): Int {
        require(fromIndex in 0..dest.size && inputSize <= dest.size - fromIndex) {
            "Destination does not have enough space for $inputSize input values"
        }

        val totalDelta = totalDelta
        val previousVelocityDelta = previousVelocityDelta
        var index = fromIndex
        // Total Delta
        dest[index++] = totalDelta.deltaYaw
        dest[index++] = totalDelta.deltaPitch
        // Velocity Delta
        dest[index++] = previousVelocityDelta.deltaYaw
        dest[index++] = previousVelocityDelta.deltaPitch
        // Speed
        dest[index++] = targetDiff.horizontalDistance().toFloat() + playerDiff.horizontalDistance().toFloat()
        // Distance
        dest[index++] = distance
        return index
    }

    override fun fillAsOutput(dest: FloatArray, fromIndex: Int): Int {
        require(fromIndex in 0..dest.size && outputSize <= dest.size - fromIndex) {
            "Destination does not have enough space for $outputSize output values"
        }

        var index = fromIndex
        dest[index++] = velocityDelta.x
        dest[index++] = velocityDelta.y
        return index
    }

    companion object {
        private const val INPUT_SIZE = 6
        private const val OUTPUT_SIZE = 2

        private const val CURRENT_DIRECTION_VECTOR = "a"
        private const val PREVIOUS_DIRECTION_VECTOR = "b"
        private const val TARGET_DIRECTION_VECTOR = "c"
        private const val DELTA_VECTOR = "d"
        private const val HURT_TIME = "e"
        private const val AGE = "f"
        private const val P_DIFF = "g"
        private const val T_DIFF = "h"
        private const val DISTANCE = "i"

        private fun parse(file: File): List<CombatSample> = when {
            file.isDirectory -> file.listFiles()?.flatMap(::parse).orEmpty()
            file.extension == "json" -> file.readJson<List<CombatSample>>()
            else -> emptyList()
        }

        fun parse(vararg files: File): List<CombatSample> = files.flatMap(::parse)

    }
}
