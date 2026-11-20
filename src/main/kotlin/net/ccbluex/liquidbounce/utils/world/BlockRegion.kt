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

package net.ccbluex.liquidbounce.utils.world

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import kotlin.math.max
import kotlin.math.min

data class BlockRegion(
    val minX: Int,
    val minY: Int,
    val minZ: Int,
    val maxX: Int,
    val maxY: Int,
    val maxZ: Int,
) {

    operator fun contains(pos: BlockPos) =
        pos.x in minX..maxX && pos.y in minY..maxY && pos.z in minZ..maxZ

    companion object {

        /**
         * Every block a route of at most [maxLength] from [start] to one of [ends] can pass, at most
         * [verticalMargin] blocks above or below them.
         */
        fun around(start: Vec3, ends: Collection<Vec3>, maxLength: Double, verticalMargin: Int): BlockRegion {
            var minX = start.x
            var minY = start.y
            var minZ = start.z
            var maxX = start.x
            var maxY = start.y
            var maxZ = start.z
            var closest = Double.MAX_VALUE

            for (end in ends) {
                minX = min(minX, end.x)
                minY = min(minY, end.y)
                minZ = min(minZ, end.z)
                maxX = max(maxX, end.x)
                maxY = max(maxY, end.y)
                maxZ = max(maxZ, end.z)
                closest = min(closest, start.distanceTo(end))
            }

            val detour = if (closest == Double.MAX_VALUE) maxLength else max(0.0, (maxLength - closest) / 2.0)
            val horizontal = detour + EDGE
            val vertical = min(detour, verticalMargin.toDouble()) + EDGE

            return BlockRegion(
                (minX - horizontal).toInt() - 1,
                (minY - vertical).toInt() - 1,
                (minZ - horizontal).toInt() - 1,
                (maxX + horizontal).toInt() + 1,
                (maxY + vertical).toInt() + 1,
                (maxZ + horizontal).toInt() + 1,
            )
        }

        private const val EDGE = 3.0
    }

}
