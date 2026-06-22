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
package net.ccbluex.liquidbounce.utils.block.hole

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Vec3i
import net.minecraft.world.level.levelgen.structure.BoundingBox

sealed interface Hole : Comparable<Hole> {

    val pos: BlockPos
    val bedrockOnly: Boolean

    val positions: BoundingBox

    val size: Int

    fun asList(): List<BlockPos>

    override fun compareTo(other: Hole): Int {
        return this.pos compareTo other.pos
    }

    operator fun contains(pos: Vec3i): Boolean = positions.isInside(pos)

    /**
     * Checks whether placing a block at [pos] would invalidate this hole.
     *
     * A block can invalidate the hole if its position falls within the hole's
     * area, extended upward by 2 blocks to account for the player's height.
     */
    fun isInvalidatedByFilling(pos: Vec3i): Boolean {
        return pos.x in this.positions.minX()..this.positions.maxX()
            && pos.y in this.positions.minY()..this.positions.maxY() + 2
            && pos.z in this.positions.minZ()..this.positions.maxZ()
    }


    /**
     * ```
     * ? x ?
     * x o x
     * ? x ?
     * ```
     */
    data class OneByOne(
        override val pos: BlockPos,
        override val bedrockOnly: Boolean = false,
    ) : Hole {
        override val positions: BoundingBox = BoundingBox(pos)

        override val size: Int get() = 1

        override fun asList(): List<BlockPos> = listOf(pos)
    }

    /**
     * ```
     * ? x x ?
     * x o o x
     * ? x x ?
     * ```
     */
    data class OneByTwo(
        override val pos: BlockPos,
        val axis: Direction.Axis,
        override val bedrockOnly: Boolean = false,
    ) : Hole {
        init {
            require(axis.isHorizontal) { "OneByTwo axis must be horizontal" }
        }

        private val other: BlockPos = pos.relative(
            if (axis == Direction.Axis.X) Direction.EAST else Direction.SOUTH
        )

        override val positions: BoundingBox = BoundingBox.fromCorners(pos, other)

        override val size: Int get() = 2

        override fun asList(): List<BlockPos> = listOf(pos, other)
    }

    /**
     * ```
     * ? x x ?
     * x o o x
     * x o o x
     * ? x x ?
     * ```
     */
    data class TwoByTwo(
        override val pos: BlockPos,
        override val bedrockOnly: Boolean = false,
    ) : Hole {
        override val positions: BoundingBox = BoundingBox(pos.x, pos.y, pos.z, pos.x + 1, pos.y, pos.z + 1)

        override val size: Int get() = 4

        override fun asList(): List<BlockPos> = listOf(pos, pos.east(), pos.south(), pos.east().south())
    }
}
