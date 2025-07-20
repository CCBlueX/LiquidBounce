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

import net.ccbluex.liquidbounce.utils.block.Region
import net.minecraft.util.math.BlockPos

@JvmRecord
data class Hole(
    val type: Type,
    val positions: Region,
    val bedrockOnly: Boolean = false,
    val blockInvalidators: Region = Region(positions.from, positions.to.up(2)),
) : Comparable<Hole> {

    override fun compareTo(other: Hole): Int = this.positions.from.compareTo(other.positions.from)

    operator fun contains(pos: BlockPos): Boolean = pos in positions

    enum class Type(val size: Int) {
        ONE_ONE(1),
        ONE_TWO(2),
        TWO_TWO(4),
    }

}
