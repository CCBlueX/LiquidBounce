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

package net.ccbluex.liquidbounce.utils.block.targetfinding

import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.core.BlockPos

private fun commonOffsetToInvestigate(vararg xzOffsets: Int): List<BlockPos> = buildList(xzOffsets.size.sq() * 2) {
    for (x in xzOffsets) {
        for (z in xzOffsets) {
            add(BlockPos(x, 0, z))
            add(BlockPos(x, -1, z))
        }
    }
}

enum class BlockPosOffsets(val list: List<BlockPos>): List<BlockPos> by list {
    NO_OFFSET(listOf(BlockPos.ZERO)),
    NORMAL(commonOffsetToInvestigate(0, -1, 1)),
    DOWN(commonOffsetToInvestigate(0, -1, -1, -2, 2)),
    FULL(commonOffsetToInvestigate(0, -1, 1, -2, 2, -3, 3, -4, -4)),
    ;
}
