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

package net.ccbluex.liquidbounce.features.module.modules.world.nuker.area

import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.math.component1
import net.ccbluex.liquidbounce.utils.math.component2
import net.ccbluex.liquidbounce.utils.math.component3
import net.ccbluex.liquidbounce.utils.math.iterate
import net.ccbluex.liquidbounce.utils.math.rangeTo
import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB

object FloorNukerArea : NukerArea("Floor") {

    private val relativeToPlayer by boolean("RelativeToPlayer", true)

    private val startPosition by vec3i("StartPosition", Vec3i.ZERO)
    private val endPosition by vec3i("EndPosition", Vec3i.ZERO)

    private val topToBottom by boolean("TopToBottom", true)

    @Suppress("detekt:CognitiveComplexMethod")
    override fun lookupTargets(radius: Float, count: Int?): List<Pair<BlockPos, BlockState>> {
        val (startX, startY, startZ) =
            if (relativeToPlayer) startPosition.offset(player.blockPosition()) else startPosition
        val (endX, endY, endZ) =
            if (relativeToPlayer) endPosition.offset(player.blockPosition()) else endPosition

        val start = BlockPos.MutableBlockPos(startX, startY, startZ)
        val end = BlockPos.MutableBlockPos(endX, endY, endZ)

        val box = AABB.encapsulatingFullBlocks(start, end)

        // Check if the box is within the radius
        val eyesPos = player.eyePosition
        val rangeSquared = (radius * radius).toDouble()
        if (box.squaredBoxedDistanceTo(eyesPos) > rangeSquared) {
            // Return empty list if not
            return emptyList()
        }

        // Create ranges from start position to end position, they might be flipped, so we need to use min/max
        val xRange = minOf(startX, endX)..maxOf(startX, endX)
        val yRange = minOf(startY, endY)..maxOf(startY, endY)
        val zRange = minOf(startZ, endZ)..maxOf(startZ, endZ)

        // Iterate through each Y range first, so we can as soon we find a block on the floor,
        // we can skip the rest
        // From top to bottom

        start.set(xRange.first, 0, zRange.first)
        end.set(xRange.last, 0, zRange.last)

        // Check if [topToBottom] is enabled, if so reverse the range
        for (y in yRange.let { if (topToBottom) it.reversed() else it }) {
            start.y = y
            end.y = y
            val m = (start..end).iterate().mapNotNull { pos ->
                val state = pos.getState() ?: return@mapNotNull null
                if (isPositionAvailable(eyesPos, rangeSquared, pos, state)) {
                    pos.immutable() to state
                } else {
                    null
                }
            }

            // Return when not empty
            if (m.isNotEmpty()) {
                return if (count != null) {
                    m.take(count)
                } else {
                    m
                }
            }
        }

        return emptyList()
    }

}
