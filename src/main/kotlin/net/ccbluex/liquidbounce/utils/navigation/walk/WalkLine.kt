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

private const val SAMPLE_SPACING = 0.5
private const val DEAD_ANGLE = 20f
private val SIDE_X = intArrayOf(1, -1, 0, 0)
private val SIDE_Z = intArrayOf(0, 0, 1, -1)

/** Whether walking straight from [from] gets within [stopDistance] of [to], past no drop deeper than [safeFall]. */
internal fun isStraightWalkable(
    terrain: NavigationTerrain,
    from: Vec3,
    to: Vec3,
    stopDistance: Double,
    safeFall: Int,
): Boolean {
    val dx = to.x - from.x
    val dz = to.z - from.z
    val length = Mth.length(dx, dz)
    val samples = ceil(length / SAMPLE_SPACING).toInt()

    var level = from.y
    if (terrain.isNearDrop(Mth.floor(from.x), level, Mth.floor(from.z), safeFall)) {
        return false
    }

    for (sample in 1..samples) {
        val t = min(sample * SAMPLE_SPACING, length) / length
        val x = from.x + dx * t
        val z = from.z + dz * t
        level = terrain.groundLevel(x, z, level)
        if (level.isNaN() || terrain.isNearDrop(Mth.floor(x), level, Mth.floor(z), safeFall)) {
            return false
        }
        if (to.distanceToSqr(x, level, z) <= stopDistance * stopDistance) {
            return true
        }
    }

    return false
}

/** Whether a side of column [x] [z] drops deeper than [safeFall] blocks below feet at [level], or into a hazard. */
private fun NavigationTerrain.isNearDrop(x: Int, level: Double, z: Int, safeFall: Int): Boolean {
    val y = Mth.floor(level)
    for (i in SIDE_X.indices) {
        val sideX = x + SIDE_X[i]
        val sideZ = z + SIDE_Z[i]
        val open = isOpen(sideX, y, sideZ) && !isWater(sideX, y, sideZ)
        if (open && !isSafeDrop(sideX, y, sideZ, level, safeFall)) {
            return true
        }
    }
    return false
}

private fun NavigationTerrain.isSafeDrop(x: Int, y: Int, z: Int, level: Double, safeFall: Int): Boolean {
    for (dropY in y downTo y - safeFall - 1) {
        val stand = standHeight(x, dropY, z)
        when {
            isWater(x, dropY, z) -> return true
            !stand.isNaN() -> return level - stand <= safeFall
            !isPassable(x, dropY, z) -> return false
        }
    }
    return false
}

/** Feet height when standing at [x] [z] within a step of [level], or NaN. */
private fun NavigationTerrain.groundLevel(x: Double, z: Double, level: Double): Double {
    val blockX = Mth.floor(x)
    val blockZ = Mth.floor(z)
    val step = NavigationTerrain.STEP

    for (y in Mth.floor(level + step) downTo Mth.floor(level - step)) {
        val height = standHeight(blockX, y, blockZ)
        if (!height.isNaN() && abs(height - level) <= step) {
            return if (isClear(box(x, height, z))) height else Double.NaN
        }
    }

    return Double.NaN
}

/** Keys that move towards [delta] when the input is applied relative to [frameYaw]. */
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
