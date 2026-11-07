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

package net.ccbluex.liquidbounce.utils.navigation.walk

import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.movement.getDegreesRelativeToView
import net.ccbluex.liquidbounce.utils.movement.getDirectionalInputForDegrees
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sqrt

private const val SAMPLE_SPACING = 0.5
private const val DEAD_ANGLE = 20f

/**
 * Whether walking from [from] straight towards [to] gets within [stopDistance] of it on one level: every sample
 * on the way has ground within a step of the level, room for the body and no hazard.
 */
internal fun isStraightWalkable(terrain: NavigationTerrain, from: Vec3, to: Vec3, stopDistance: Double): Boolean {
    val dx = to.x - from.x
    val dz = to.z - from.z
    val length = sqrt(dx * dx + dz * dz)
    val samples = ceil(length / SAMPLE_SPACING).toInt()

    var level = from.y
    for (sample in 1..samples) {
        val t = min(sample * SAMPLE_SPACING, length) / length
        val x = from.x + dx * t
        val z = from.z + dz * t
        level = groundLevel(terrain, x, z, level) ?: return false
        if (to.distanceToSqr(x, level, z) <= stopDistance * stopDistance) {
            return true
        }
    }

    return false
}

/**
 * Feet height when standing at [x] [z] within a step of [level], or null.
 */
private fun groundLevel(terrain: NavigationTerrain, x: Double, z: Double, level: Double): Double? {
    val blockX = Mth.floor(x)
    val blockZ = Mth.floor(z)
    val step = NavigationTerrain.STEP

    for (y in Mth.floor(level + step) downTo Mth.floor(level - step)) {
        val height = terrain.standHeight(blockX, y, blockZ)
        if (!height.isNaN() && abs(height - level) <= step) {
            return height.takeIf { terrain.isClear(terrain.box(x, height, z)) }
        }
    }

    return null
}

/**
 * Keys that move towards [delta] when the input is applied relative to [frameYaw].
 */
internal fun steer(delta: Vec3, frameYaw: Float): DirectionalInput {
    if (delta.horizontalDistanceSqr() < 1.0E-6) {
        return DirectionalInput.NONE
    }

    return getDirectionalInputForDegrees(
        DirectionalInput.NONE,
        getDegreesRelativeToView(delta, frameYaw),
        deadAngle = DEAD_ANGLE,
    )
}
