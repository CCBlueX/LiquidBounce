/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.SideShapeType
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.*
import kotlin.math.ceil

fun Vec3i.toBlockPos() = BlockPos(this)

fun BlockPos.getState() = mc.world?.getBlockState(this)

fun BlockPos.getBlock() = getState()?.block

fun BlockPos.getCenterDistanceSquared() = mc.player!!.squaredDistanceTo(this.x + 0.5, this.y + 0.5, this.z + 0.5)
fun BlockPos.isNeighborOfOrEquivalent(other: BlockPos) = this.getSquaredDistance(other) <= 2.0

/**
 * Search blocks around the player in a cuboid
 */
inline fun searchBlocksInCuboid(a: Int, filter: (BlockPos, BlockState) -> Boolean): List<Pair<BlockPos, BlockState>> {
    val blocks = mutableListOf<Pair<BlockPos, BlockState>>()

    val thePlayer = mc.player ?: return blocks

    for (x in a downTo -a + 1) {
        for (y in a downTo -a + 1) {
            for (z in a downTo -a + 1) {
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

/**
 * Search blocks around the player in a specific [radius]
 */
inline fun searchBlocksInRadius(
    radius: Float,
    filter: (BlockPos, BlockState) -> Boolean
): List<Pair<BlockPos, BlockState>> {
    val blocks = mutableListOf<Pair<BlockPos, BlockState>>()

    val thePlayer = mc.player ?: return blocks

    val playerPos = thePlayer.pos
    val radiusSquared = radius * radius
    val radiusInt = radius.toInt()

    for (x in radiusInt downTo -radiusInt) {
        for (y in radiusInt downTo -radiusInt) {
            for (z in radiusInt downTo -radiusInt) {
                val blockPos = BlockPos(thePlayer.x.toInt() + x, thePlayer.y.toInt() + y, thePlayer.z.toInt() + z)
                val state = blockPos.getState() ?: continue

                if (!filter(blockPos, state)) {
                    continue
                }
                if (Vec3d.of(blockPos).squaredDistanceTo(playerPos) > radiusSquared) {
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

/**
 * Check if [box] is reaching of specified blocks
 */
fun isBlockAtPosition(box: Box, isCorrectBlock: (Block?) -> Boolean): Boolean {
    for (x in MathHelper.floor(box.minX) until MathHelper.floor(box.maxX) + 1) {
        for (z in MathHelper.floor(box.minZ) until MathHelper.floor(box.maxZ) + 1) {
            val block = BlockPos.ofFloored(x.toDouble(), box.minY, z.toDouble()).getBlock()

            if (isCorrectBlock(block)) {
                return true
            }
        }
    }

    return false
}

/**
 * Check if [box] intersects with bounding box of specified blocks
 */
fun collideBlockIntersects(box: Box, isCorrectBlock: (Block?) -> Boolean): Boolean {
    for (x in MathHelper.floor(box.minX) until MathHelper.floor(box.maxX) + 1) {
        for (z in MathHelper.floor(box.minZ) until MathHelper.floor(box.maxZ) + 1) {
            val blockPos = BlockPos.ofFloored(x.toDouble(), box.minY, z.toDouble())
            val blockState = blockPos.getState() ?: continue
            val block = blockPos.getBlock() ?: continue

            if (isCorrectBlock(block)) {
                val shape = blockState.getCollisionShape(mc.world, blockPos)

                if (shape.isEmpty) {
                    continue
                }

                val boundingBox = shape.boundingBox

                if (box.intersects(boundingBox)) {
                    return true
                }
            }
        }
    }

    return false
}

fun Box.forEachCollidingBlock(function: (x: Int, y: Int, z: Int) -> Unit) {
    val from = BlockPos(this.minX.toInt(), this.minY.toInt(), this.minZ.toInt())
    val to = BlockPos(ceil(this.maxX).toInt(), ceil(this.maxY).toInt(), ceil(this.maxZ).toInt())

    for (x in from.x until to.x) {
        for (y in from.y until to.y) {
            for (z in from.z until to.z) {
                function(x, y, z)
            }
        }
    }
}

fun BlockState.canBeReplacedWith(pos: BlockPos, usedStack: ItemStack): Boolean {
    val placementContext = ItemPlacementContext(
        mc.player,
        Hand.MAIN_HAND,
        usedStack,
        BlockHitResult(Vec3d.of(pos), Direction.UP, pos, false)
    )

    return canReplace(
        placementContext
    )
}
