/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.block.BlockState
import net.minecraft.block.SideShapeType
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d

fun Vec3d.toBlockPos() = BlockPos(this)

fun BlockPos.getState() = mc.world?.getBlockState(this)

fun BlockPos.getBlock() = getState()?.block

fun BlockPos.getCenterDistanceSquared() = mc.player!!.squaredDistanceTo(this.x + 0.5, this.y + 0.5, this.z + 0.5)

/**
 * Search blocks around the player in a specific [radius]
 */
inline fun searchBlocks(radius: Int, filter: (BlockPos, BlockState) -> Boolean): List<Pair<BlockPos, BlockState>> {
    val blocks = mutableListOf<Pair<BlockPos, BlockState>>()

    val thePlayer = mc.player ?: return blocks

    for (x in radius downTo -radius + 1) {
        for (y in radius downTo -radius + 1) {
            for (z in radius downTo -radius + 1) {
                val blockPos = BlockPos(thePlayer.x.toInt() + x, thePlayer.y.toInt() + y, thePlayer.z.toInt() + z)
                val state = blockPos.getState() ?: continue

                if (!filter(blockPos, state)) {
                    continue
                }

                blocks.add(Pair(blockPos, state))
            }
        }
    }

    return blocks
}

fun BlockPos.canStandOn(): Boolean {
    return this.getState()!!.isSideSolid(mc.world!!, this, Direction.UP, SideShapeType.CENTER)
}
