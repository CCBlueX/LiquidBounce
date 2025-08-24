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
package net.ccbluex.liquidbounce.utils.block

import it.unimi.dsi.fastutil.longs.LongArrayList
import it.unimi.dsi.fastutil.longs.LongImmutableList
import it.unimi.dsi.fastutil.longs.LongList
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.util.math.BlockPos

internal object CachedBlockPosSpheres {

    const val RADIUS = 10

    private val table: LongArray
    private val indices: IntArray

    init {
        val size = RADIUS * RADIUS + 1
        val temp = Array<LongList>(size) { LongArrayList() }
        for (x in -RADIUS..RADIUS) {
            for (y in -RADIUS..RADIUS) {
                for (z in -RADIUS..RADIUS) {
                    val radiusSq = x * x + y * y + z * z
                    if (radiusSq > RADIUS * RADIUS) {
                        continue
                    }
                    temp[radiusSq].add(BlockPos.asLong(x, y, z))
                }
            }
        }
        indices = IntArray(size) { i ->
            if (i == 0) 0 else temp[i - 1].size
        }
        table = LongArray(temp.sumOf { it.size })
        // Flat temp into table
        var index = 0
        for (list in temp) {
            with(list.longIterator()) {
                while (hasNext()) {
                    table[index++] = nextLong()
                }
            }
        }
    }

    /**
     * Gets all [BlockPos] (long value) within the specified radius range.
     * All positions are sorted by radius (ascending).
     *
     * @param fromRadius the minimum radius (inclusive, 0 to [RADIUS])
     * @param toRadius the maximum radius (inclusive, [fromRadius] to [RADIUS])
     * @return a sorted immutable [LongList] view containing the block positions of radius range.
     * @throws IndexOutOfBoundsException if [fromRadius] or [toRadius] is out of range.
     */
    fun rangeLong(fromRadius: Int = 0, toRadius: Int = RADIUS): LongList {
        if (fromRadius < 0 || toRadius < fromRadius || toRadius >= RADIUS) {
            throw IndexOutOfBoundsException("fromRadius=$fromRadius toRadius=$toRadius")
        }

        return LongImmutableList(
            table,
            indices[fromRadius.sq()],
            indices[toRadius.sq()] - indices[fromRadius.sq()],
        )
    }

}
