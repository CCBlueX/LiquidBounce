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
package net.ccbluex.liquidbounce.utils.aiming.data

import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.math.fma
import net.ccbluex.liquidbounce.utils.raytracing.hasLineOfSight
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

/** How far short of a surface its line of sight is checked, so a block's own face is not in the way. */
private const val SIGHT_MARGIN = 1e-3

/**
 * What a view has to meet for its target to count as hit: a ray from the eyes into [box], entering through [face] if
 * one is given, with no block in the way unless [throughWalls].
 */
@AddonApi
class Surface @JvmOverloads constructor(
    val box: AABB,
    val face: Direction? = null,
    val throughWalls: Boolean = false,
) {

    /** Whether the ray of [rotation] from [eyes] meets this surface. */
    fun isHitBy(eyes: Vec3, rotation: Rotation): Boolean {
        if (box.contains(eyes)) {
            return face == null
        }

        val direction = rotation.directionVector
        val reach = eyes.distanceTo(box.center) + box.size
        val hit = AABB.clip(listOf(box), eyes, eyes.fma(reach, direction), BlockPos.ZERO) ?: return false
        if (face != null && hit.direction != face) {
            return false
        }
        return throughWalls || hasLineOfSight(eyes, hit.location.fma(-SIGHT_MARGIN, direction))
    }

    /** The corners of what has to be hit: [face] of [box], or all of it. */
    val corners: List<Vec3>
        get() = buildList {
            for (x in sides(Direction.Axis.X, box.minX, box.maxX)) {
                for (y in sides(Direction.Axis.Y, box.minY, box.maxY)) {
                    for (z in sides(Direction.Axis.Z, box.minZ, box.maxZ)) {
                        add(Vec3(x, y, z))
                    }
                }
            }
        }

    private fun sides(axis: Direction.Axis, min: Double, max: Double) = when {
        face?.axis != axis -> doubleArrayOf(min, max)
        face.axisDirection == Direction.AxisDirection.POSITIVE -> doubleArrayOf(max)
        else -> doubleArrayOf(min)
    }

    @AddonApi
    companion object {

        /** The box an attack on [entity] has to hit. */
        @JvmStatic
        fun of(entity: Entity) = Surface(entity.boundingBox.inflate(entity.pickRadius.toDouble()))

        /** The outline of the block at [pos], or its [face] of it; a full block where it has no outline. */
        @JvmStatic
        @JvmOverloads
        fun of(pos: BlockPos, face: Direction? = null, throughWalls: Boolean = false): Surface {
            val shape = world.getBlockState(pos).getShape(world, pos)
            val box = if (shape.isEmpty) AABB(pos) else shape.bounds().move(pos)
            return Surface(box, face, throughWalls)
        }

    }

}
