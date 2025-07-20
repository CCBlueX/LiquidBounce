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

import net.minecraft.util.math.BlockPos
import kotlin.math.max
import kotlin.math.min

class MovableRegionScanner {
    var currentRegion = Region.EMPTY
        private set

    /**
     * Moves the current region; returns regions that have been newly covered
     */
    fun moveTo(region: Region): Sequence<Region> {
        val lastRegion = this.currentRegion

        this.currentRegion = region

        return when {
            // No new blocks where covered
            lastRegion == region || region in lastRegion -> emptySequence()
            // All blocks are new
            !lastRegion.intersects(region) -> sequenceOf(region)
            // Some of the blocks are new, we have to check...
            else -> overlaps(region, lastRegion).filter { !it.isEmpty() }
        }
    }

    private fun overlaps(region: Region, lastRegion: Region): Sequence<Region> {
        return sequenceOf(
            Region(
                BlockPos(min(region.to.x, lastRegion.to.x), region.from.y, region.from.z),
                BlockPos(max(region.to.x, lastRegion.to.x), region.to.y, region.to.z)
            ),
            Region(
                BlockPos(min(region.from.x, lastRegion.from.x), region.from.y, region.from.z),
                BlockPos(max(region.from.x, lastRegion.from.x), region.to.y, region.to.z)
            ),
            Region(
                BlockPos(region.from.x, min(region.to.y, lastRegion.to.y), region.from.z),
                BlockPos(region.to.x, max(region.to.y, lastRegion.to.y), region.to.z)
            ),
            Region(
                BlockPos(region.from.x, min(region.from.y, lastRegion.from.y), region.from.z),
                BlockPos(region.to.x, max(region.from.y, lastRegion.from.y), region.to.z)
            ),
            Region(
                BlockPos(region.from.x, region.from.y, min(region.to.z, lastRegion.to.z)),
                BlockPos(region.to.x, region.to.y, max(region.to.z, lastRegion.to.z))
            ),
            Region(
                BlockPos(region.from.x, region.from.y, min(region.from.z, lastRegion.from.z)),
                BlockPos(region.to.x, region.to.y, max(region.from.z, lastRegion.from.z))
            )
        )
    }

    fun clearRegion() {
        this.currentRegion = Region.EMPTY
    }

}
