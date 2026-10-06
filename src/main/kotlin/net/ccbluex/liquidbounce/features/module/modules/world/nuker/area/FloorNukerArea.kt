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

import net.ccbluex.liquidbounce.utils.block.state
import net.ccbluex.liquidbounce.utils.kotlin.coerceIn
import net.ccbluex.liquidbounce.utils.kotlin.rangeAround
import net.ccbluex.liquidbounce.utils.kotlin.safeIntRangeOf
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
    override fun lookupTargets(radius: Float, limit: Int?): List<Pair<BlockPos, BlockState>> {
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
        if (box.distanceToSqr(eyesPos) > rangeSquared) {
            // Return empty list if not
            return emptyList()
        }

        val xRange = safeIntRangeOf(startX, endX).coerceIn(player.blockX.rangeAround(radius.toInt() + 1))
        val yRange = safeIntRangeOf(startY, endY).coerceIn(player.blockY.rangeAround(radius.toInt() + 1))
        val zRange = safeIntRangeOf(startZ, endZ).coerceIn(player.blockZ.rangeAround(radius.toInt() + 1))

        // Iterate through each Y range first, so we can as soon we find a block on the floor,
        // we can skip the rest
        // From top to bottom
        start.set(xRange.first, 0, zRange.first)
        end.set(xRange.last, 0, zRange.last)

        return buildList {
            // Check if [topToBottom] is enabled, if so reverse the range
            for (y in yRange.let { if (topToBottom) it.reversed() else it }) {
                start.y = y
                end.y = y

                for (pos in (start..end).iterate()) {
                    val state = pos.state ?: continue

                    if (isPositionAvailable(eyesPos, rangeSquared, pos, state)) {
                        add(pos.immutable() to state)
                        limit?.also { limit -> if (size >= limit) return@buildList }
                    }
                }
            }
        }
    }

}
