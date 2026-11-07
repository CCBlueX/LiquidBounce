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
private val SIDE_X = intArrayOf(1, -1, 0, 0)
private val SIDE_Z = intArrayOf(0, 0, 1, -1)

/**
 * Whether walking from [from] straight towards [to] gets within [stopDistance] of it on one level: every sample
 * on the way has ground within a step of the level, room for the body and no hazard. A straight walk does not sneak,
 * so no block on the way may have a side that drops deeper than [safeFall] blocks.
 */
internal fun isStraightWalkable(
    terrain: NavigationTerrain,
    from: Vec3,
    to: Vec3,
    stopDistance: Double,
    safeFall: Int,
): Boolean {
    val dx = to.x - from.x
    val dz = to.z - from.z
    val length = sqrt(dx * dx + dz * dz)
    val samples = ceil(length / SAMPLE_SPACING).toInt()

    var level = from.y
    if (isNearDrop(terrain, Mth.floor(from.x), level, Mth.floor(from.z), safeFall)) {
        return false
    }

    for (sample in 1..samples) {
        val t = min(sample * SAMPLE_SPACING, length) / length
        val x = from.x + dx * t
        val z = from.z + dz * t
        level = groundLevel(terrain, x, z, level) ?: return false
        if (isNearDrop(terrain, Mth.floor(x), level, Mth.floor(z), safeFall)) {
            return false
        }
        if (to.distanceToSqr(x, level, z) <= stopDistance * stopDistance) {
            return true
        }
    }

    return false
}

/**
 * Whether a player standing in column [x] [z] with the feet at [level] can walk off a side into a drop deeper than
 * [safeFall] blocks, a hazard or the void.
 */
private fun isNearDrop(terrain: NavigationTerrain, x: Int, level: Double, z: Int, safeFall: Int): Boolean {
    val y = Mth.floor(level)
    for (i in SIDE_X.indices) {
        val sideX = x + SIDE_X[i]
        val sideZ = z + SIDE_Z[i]
        val open = terrain.isPassable(sideX, y, sideZ) && terrain.isPassable(sideX, y + 1, sideZ) &&
            !terrain.isWater(sideX, y, sideZ)
        if (open && !isSafeDrop(terrain, sideX, y, sideZ, level, safeFall)) {
            return true
        }
    }
    return false
}

/**
 * Whether falling down column [x] [z] from block [y] with the feet at [level] ends in water or on ground at most
 * [safeFall] blocks lower.
 */
private fun isSafeDrop(terrain: NavigationTerrain, x: Int, y: Int, z: Int, level: Double, safeFall: Int): Boolean {
    for (dropY in y downTo y - safeFall - 1) {
        val stand = terrain.standHeight(x, dropY, z)
        when {
            terrain.isWater(x, dropY, z) -> return true
            !stand.isNaN() -> return level - stand <= safeFall
            !terrain.isPassable(x, dropY, z) -> return false
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
