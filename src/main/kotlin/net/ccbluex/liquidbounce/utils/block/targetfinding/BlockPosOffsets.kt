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

import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.utils.math.lengthSqr
import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i
import java.util.function.ToIntFunction
import java.util.function.ToLongFunction

private val COMPARATOR: Comparator<in Vec3i> =
    Comparator.comparingLong(ToLongFunction(Vec3i::lengthSqr))
        .thenComparingInt(ToIntFunction(Vec3i::getY))
        .thenComparingInt(ToIntFunction(Vec3i::getX))
        .thenComparingInt(ToIntFunction(Vec3i::getZ))

private fun generateScaffoldOffsets(vararg xzValues: Int): List<BlockPos> {
    val longs = LongOpenHashSet(xzValues.size * xzValues.size * 2)
    for (x in xzValues) {
        for (z in xzValues) {
            longs.add(BlockPos.asLong(x, 0, z))
            longs.add(BlockPos.asLong(x, -1, z))
        }
    }

    val result = longs.mapToArray(BlockPos::of)
    result.sortWith(COMPARATOR)

    return result.asList()
}

enum class BlockPosOffsets(val offsets: List<BlockPos>) {
    NO_OFFSET(listOf(BlockPos.ZERO)),
    NORMAL(generateScaffoldOffsets(0, -1, 1)),
    DOWN(generateScaffoldOffsets(0, -1, 1, -2, 2)),
    FULL(generateScaffoldOffsets(0, -1, 1, -2, 2, -3, 3, -4, 4)),
    ;

    fun containsOffset(x: Int, y: Int, z: Int): Boolean = offsets.contains(BlockPos(x, y, z))

}
