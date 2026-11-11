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
package net.ccbluex.liquidbounce.utils.navigation.teleport

import net.ccbluex.liquidbounce.utils.aiming.utils.canSeeBox
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.ceil

private const val MAX_REACH_POSITIONS = 8

/** Blocks below the box the feet may still stand at, as the eyes are above them. */
private const val EYE_DEPTH = 2

fun eyesAt(feet: Vec3): Vec3 = feet.add(0.0, player.eyeHeight.toDouble(), 0.0)

/** Standable feet positions whose eyes see [box] within [reach], closest to [from] first; [block] does not hide it. */
fun reachPositions(
    terrain: NavigationTerrain,
    box: AABB,
    reach: Double,
    from: Vec3,
    block: BlockPos? = null,
): List<BlockPos> {
    val center = BlockPos.containing(box.center)
    val radius = ceil(reach).toInt()
    val positions = ArrayList<BlockPos>()

    for (dx in -radius..radius) {
        for (dz in -radius..radius) {
            for (dy in -radius - EYE_DEPTH..radius) {
                val position = center.offset(dx, dy, dz)
                val feet = terrain.feet(position) ?: continue
                val eyes = eyesAt(feet)
                if (!terrain.box(feet).intersects(box) && box.distanceToSqr(eyes) <= reach * reach &&
                    canSeeBox(eyes, box, reach, 0.0, block)
                ) {
                    positions += position
                }
            }
        }
    }

    positions.sortBy { terrain.feet(it)?.distanceToSqr(from) ?: Double.MAX_VALUE }
    return positions.take(MAX_REACH_POSITIONS)
}
