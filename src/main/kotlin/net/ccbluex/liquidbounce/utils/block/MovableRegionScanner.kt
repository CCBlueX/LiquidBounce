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

import net.ccbluex.liquidbounce.utils.math.contains
import net.minecraft.util.math.BlockBox
import net.minecraft.util.math.BlockPos
import kotlin.math.max
import kotlin.math.min

private val ORIGIN = BlockBox(BlockPos.ORIGIN)

class MovableRegionScanner {
    var currentRegion = ORIGIN
        private set

    /**
     * Moves the current region; returns regions that have been newly covered
     */
    fun moveTo(region: BlockBox): List<BlockBox> {
        val lastRegion = this.currentRegion

        this.currentRegion = region

        return when {
            // No new blocks where covered
            lastRegion == region || lastRegion.contains(region) -> emptyList()
            // All blocks are new
            !lastRegion.intersects(region) -> listOf(region)
            // Some of the blocks are new, we have to check...
            else -> overlaps(region, lastRegion)
        }
    }

    private fun overlaps(region: BlockBox, lastRegion: BlockBox): List<BlockBox> {
        return listOf(
            BlockBox.create(
                BlockPos(min(region.maxX, lastRegion.maxX), region.minY, region.minZ),
                BlockPos(max(region.maxX, lastRegion.maxX), region.maxY, region.maxZ)
            ),
            BlockBox.create(
                BlockPos(min(region.minX, lastRegion.minX), region.minY, region.minZ),
                BlockPos(max(region.minX, lastRegion.minX), region.maxY, region.maxZ)
            ),
            BlockBox.create(
                BlockPos(region.minX, min(region.maxY, lastRegion.maxY), region.minZ),
                BlockPos(region.maxX, max(region.maxY, lastRegion.maxY), region.maxZ)
            ),
            BlockBox.create(
                BlockPos(region.minX, min(region.minY, lastRegion.minY), region.minZ),
                BlockPos(region.maxX, max(region.minY, lastRegion.minY), region.maxZ)
            ),
            BlockBox.create(
                BlockPos(region.minX, region.minY, min(region.maxZ, lastRegion.maxZ)),
                BlockPos(region.maxX, region.maxY, max(region.maxZ, lastRegion.maxZ))
            ),
            BlockBox.create(
                BlockPos(region.minX, region.minY, min(region.minZ, lastRegion.minZ)),
                BlockPos(region.maxX, region.maxY, max(region.minZ, lastRegion.minZ))
            )
        )
    }

    fun clearRegion() {
        this.currentRegion = ORIGIN
    }

}
