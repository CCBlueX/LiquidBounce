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

import net.minecraft.core.Vec3i
import net.minecraft.world.level.levelgen.structure.BoundingBox

@JvmRecord
data class Hole(
    val type: Type,
    val positions: BoundingBox,
    val bedrockOnly: Boolean = false,
) : Comparable<Hole> {

    override fun compareTo(other: Hole): Int {
        val yDiff = this.positions.minY() - other.positions.minY()
        val zDiff = this.positions.minZ() - other.positions.minZ()
        val xDiff = this.positions.minX() - other.positions.minX()
        return when {
            yDiff != 0 -> yDiff
            zDiff != 0 -> zDiff
            else -> xDiff
        }
    }

    operator fun contains(pos: Vec3i): Boolean = positions.isInside(pos)

    /**
     * Checks whether placing a block at [pos] would invalidate this hole.
     *
     * A block can invalidate the hole if its position falls within the hole's
     * bounding box, extended upward by 2 blocks to account for the player's height.
     */
    fun isInvalidatedByFilling(pos: Vec3i): Boolean {
        return pos.x in this.positions.minX()..this.positions.maxX()
            && pos.y in this.positions.minY()..this.positions.maxY() + 2 // <- player height
            && pos.z in this.positions.minZ()..this.positions.maxZ()
    }

    enum class Type(val size: Int) {
        /**
         * ```
         * ? x ?
         * x o x
         * ? x ?
         * ```
         */
        ONE_ONE(1),

        /**
         * ```
         * ? x x ?
         * x o o x
         * ? x x ?
         * ```
         */
        ONE_TWO(2),

        /**
         * ```
         * ? x x ?
         * x o o x
         * x o o x
         * ? x x ?
         * ```
         */
        TWO_TWO(4),
    }

}
