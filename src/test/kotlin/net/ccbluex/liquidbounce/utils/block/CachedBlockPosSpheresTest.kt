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

import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import net.ccbluex.liquidbounce.utils.block.CachedBlockPosSpheres.RADIUS
import net.minecraft.util.math.BlockPos
import kotlin.test.Test
import kotlin.test.assertEquals

class CachedBlockPosSpheresTest {

    @Suppress("NestedBlockDepth")
    @Test
    fun test() {
        val cachedPositions = LongOpenHashSet()
        val calculatedPositions = LongOpenHashSet()
        for (radius in 0..RADIUS) {
            cachedPositions.clear()
            calculatedPositions.clear()

            cachedPositions.addAll(CachedBlockPosSpheres.rangeLong(0, radius))
            for (x in -radius..radius) {
                for (y in -radius..radius) {
                    for (z in -radius..radius) {
                        val radiusSq = x * x + y * y + z * z
                        if (radiusSq > radius * radius) {
                            continue
                        }
                        calculatedPositions.add(BlockPos.asLong(x, y, z))
                    }
                }
            }
            assertEquals(cachedPositions, calculatedPositions)
        }
    }

}
