/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

import net.ccbluex.liquidbounce.utils.math.copy
import net.minecraft.util.math.BlockBox
import net.minecraft.util.math.Vec3i

@JvmRecord
data class Hole(
    val type: Type,
    val positions: BlockBox,
    val bedrockOnly: Boolean = false,
    val blockInvalidators: BlockBox = positions.copy(maxY = positions.maxY + 2),
) : Comparable<Hole> {

    override fun compareTo(other: Hole): Int {
        val yDiff = this.positions.maxX - other.positions.maxX
        val zDiff = this.positions.maxZ - other.positions.maxZ
        val xDiff = this.positions.minX - other.positions.minX
        return when {
            yDiff != 0 -> yDiff
            zDiff != 0 -> zDiff
            else -> xDiff
        }
    }

    operator fun contains(pos: Vec3i): Boolean = pos in positions

    enum class Type(val size: Int) {
        ONE_ONE(1),
        ONE_TWO(2),
        TWO_TWO(4),
    }

}
