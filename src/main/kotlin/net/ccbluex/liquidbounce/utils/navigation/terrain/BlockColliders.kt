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

package net.ccbluex.liquidbounce.utils.navigation.terrain

import it.unimi.dsi.fastutil.floats.FloatArraySet
import it.unimi.dsi.fastutil.floats.FloatArrays
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes.EPSILON
import kotlin.math.abs

/**
 * Moves a box through the blocks of [terrain] as Entity.collide does, without allocating for every block. Boxes are
 * kept in flat arrays, six coordinates each: minimum x, y, z, then maximum x, y, z. Not thread-safe.
 */
internal class BlockColliders(private val terrain: Terrain) {

    private val blocks = Long2ObjectOpenHashMap<DoubleArray>()
    private var colliders = DoubleArray(BOX * INITIAL_COLLIDERS)
    private var count = 0
    private val moving = DoubleArray(BOX)

    fun intersects(x: Int, y: Int, z: Int, box: AABB): Boolean {
        val block = boxes(x, y, z)
        for (offset in block.indices step BOX) {
            val alongX = block[offset + X] < box.maxX && block[offset + AXES + X] > box.minX
            val alongY = block[offset + Y] < box.maxY && block[offset + AXES + Y] > box.minY
            val alongZ = block[offset + Z] < box.maxZ && block[offset + AXES + Z] > box.minZ
            if (alongX && alongY && alongZ) {
                return true
            }
        }
        return false
    }

    /** How far a box [halfWidth] around [x] [z] could still move, the shorter way, and stay over the block. */
    fun footing(blockX: Int, blockY: Int, blockZ: Int, x: Double, z: Double, halfWidth: Double): Double {
        val block = boxes(blockX, blockY, blockZ)
        var best = 0.0
        for (offset in block.indices step BOX) {
            val alongX = minOf(x + halfWidth - block[offset + X], block[offset + AXES + X] - (x - halfWidth))
            val alongZ = minOf(z + halfWidth - block[offset + Z], block[offset + AXES + Z] - (z - halfWidth))
            best = maxOf(best, minOf(alongX, alongZ))
        }
        return best
    }

    /** Takes the blocks in [region] as the ones [collide] and [stepHeights] look at. */
    fun collect(region: AABB) {
        count = 0
        for (x in Mth.floor(region.minX - EPSILON)..Mth.floor(region.maxX + EPSILON)) {
            for (z in Mth.floor(region.minZ - EPSILON)..Mth.floor(region.maxZ + EPSILON)) {
                // One layer below for blocks taller than a block, like fences.
                for (y in Mth.floor(region.minY - EPSILON) - 1..Mth.floor(region.maxY + EPSILON)) {
                    add(boxes(x, y, z))
                }
            }
        }
    }

    /** Shapes.collide: how far [box] gets of the movement, axis by axis in the order of Direction.axisStepOrder. */
    fun collide(dx: Double, dy: Double, dz: Double, box: AABB): Vec3 {
        if (count == 0) {
            return Vec3(dx, dy, dz)
        }

        moving[X] = box.minX
        moving[Y] = box.minY
        moving[Z] = box.minZ
        moving[AXES + X] = box.maxX
        moving[AXES + Y] = box.maxY
        moving[AXES + Z] = box.maxZ
        val movedY = clip(Y, dy)
        val movedX: Double
        val movedZ: Double
        if (abs(dx) < abs(dz)) {
            movedZ = clip(Z, dz)
            movedX = clip(X, dx)
        } else {
            movedX = clip(X, dx)
            movedZ = clip(Z, dz)
        }
        return Vec3(movedX, movedY, movedZ)
    }

    /** Entity.collectCandidateStepUpHeights: the heights of the faces up to [maxStep] above [box], but for [skip]. */
    fun stepHeights(box: AABB, maxStep: Float, skip: Float): FloatArray {
        val candidates = FloatArraySet(4)
        for (offset in 0 until count * BOX step BOX) {
            for (face in Y..AXES + Y step AXES) {
                val relative = (colliders[offset + face] - box.minY).toFloat()
                if (relative >= 0f && relative != skip && relative <= maxStep) {
                    candidates.add(relative)
                }
            }
        }

        val sorted = candidates.toFloatArray()
        FloatArrays.unstableSort(sorted)
        return sorted
    }

    /** VoxelShape.collide: moves the [moving] box along [axis] until a collider stops it; how far it got. */
    private fun clip(axis: Int, movement: Double): Double {
        if (abs(movement) < EPSILON) {
            return 0.0
        }

        var moved = movement
        for (offset in 0 until count * BOX step BOX) {
            moved = clipBy(offset, axis, moved)
            if (abs(moved) < EPSILON) {
                moved = 0.0
                break
            }
        }

        moving[axis] += moved
        moving[AXES + axis] += moved
        return moved
    }

    private fun clipBy(offset: Int, axis: Int, moved: Double): Double {
        if (!overlaps(offset, (axis + 1) % AXES) || !overlaps(offset, (axis + 2) % AXES)) {
            return moved
        }
        return if (moved > 0.0) {
            val gap = colliders[offset + axis] - moving[AXES + axis]
            if (gap > -EPSILON) minOf(moved, gap) else moved
        } else {
            val gap = colliders[offset + AXES + axis] - moving[axis]
            if (gap < EPSILON) maxOf(moved, gap) else moved
        }
    }

    private fun overlaps(offset: Int, axis: Int) =
        colliders[offset + AXES + axis] - EPSILON > moving[axis] &&
            colliders[offset + axis] + EPSILON < moving[AXES + axis]

    private fun add(block: DoubleArray) {
        if (block.isEmpty()) {
            return
        }
        val used = count * BOX
        if (used + block.size > colliders.size) {
            colliders = colliders.copyOf(maxOf(colliders.size * 2, used + block.size))
        }
        block.copyInto(colliders, used)
        count += block.size / BOX
    }

    /** The collision boxes of the block at [x] [y] [z] in world coordinates. */
    private fun boxes(x: Int, y: Int, z: Int): DoubleArray {
        val key = BlockPos.asLong(x, y, z)
        blocks.get(key)?.let { return it }

        val local = terrain.shape(x, y, z).toAabbs()
        val block = DoubleArray(local.size * BOX)
        local.forEachIndexed { index, box ->
            val offset = index * BOX
            block[offset + X] = box.minX + x
            block[offset + Y] = box.minY + y
            block[offset + Z] = box.minZ + z
            block[offset + AXES + X] = box.maxX + x
            block[offset + AXES + Y] = box.maxY + y
            block[offset + AXES + Z] = box.maxZ + z
        }
        blocks.put(key, block)
        return block
    }

    private companion object {
        const val X = 0
        const val Y = 1
        const val Z = 2
        const val AXES = 3
        const val BOX = 2 * AXES
        const val INITIAL_COLLIDERS = 32
    }

}
