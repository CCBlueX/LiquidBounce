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

package net.ccbluex.liquidbounce.features.module.modules.world.nuker.area

import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker.wasTarget
import net.ccbluex.liquidbounce.utils.block.searchBlocksInCuboid
import net.ccbluex.liquidbounce.utils.entity.box
import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box

object SphereNukerArea : NukerArea("Sphere") {

    override fun lookupTargets(radius: Float, count: Int?): List<Pair<BlockPos, BlockState>> {
        val rangeSquared = (radius * radius).toDouble()
        val eyesPos = player.eyePos

        val positions = eyesPos.searchBlocksInCuboid(radius) { pos, state ->
            isPositionAvailable(eyesPos, rangeSquared, pos, state)
        }.toMutableList()

        positions.sortBy { (pos, _) ->
            // If there is a last target, sort by distance to it, otherwise go by hardness
            wasTarget?.let { pos.getSquaredDistance(it) } ?: pos.getSquaredDistance(player.blockPos)
        }

        val boundingBox = player.box.offset(0.0, -1.0, 0.0)
        val nonStandingPositions = positions.filter { (pos, _) ->
            !boundingBox.intersects(Box(pos))
        }

        // If there are more than one target, we should remove blocks that we are standing on
        val list = nonStandingPositions.ifEmpty { positions }

        return if (count != null) {
            list.take(count)
        } else {
            list
        }
    }

}
