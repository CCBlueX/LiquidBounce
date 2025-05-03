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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.getMovementDirectionOfInput
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.*

object ScaffoldMovementPlanner {
    private const val MAX_LAST_PLACE_BLOCKS: Int = 4

    private val lastPlacedBlocks = ArrayDeque<BlockPos>(MAX_LAST_PLACE_BLOCKS)
    private var lastPosition: BlockPos? = null

    /**
     * When using scaffold the player wants to follow the line and the scaffold should support them in doing so.
     * This function calculates this ideal line that the player should move on.
     */
    fun getOptimalMovementLine(directionalInput: DirectionalInput): Line? {
        val direction =
            chooseDirection(
                getMovementDirectionOfInput(
                    player.yaw,
                    directionalInput,
                ),
            )

        // Is this a good way to find the block center?
        val blockUnderPlayer = findBlockPlayerStandsOn() ?: return null

        val lastBlocksLine = fitLinesThroughLastPlacedBlocks()

        // If it makes sense to follow the last placed blocks, we lay the movement line through them, otherwise, we
        // don't consider them because the user probably wants to do something new
        val lineBaseBlock = if (lastBlocksLine != null && !divergesTooMuchFromDirection(lastBlocksLine, direction)) {
            lastBlocksLine.position
        } else {
            blockUnderPlayer.toVec3d()
        }

        // We try to make the player run on this line
        val optimalLine = Line(Vec3d(lineBaseBlock.x + 0.5, player.pos.y, lineBaseBlock.z + 0.5), direction)

        // Debug optimal line
        ModuleDebug.debugGeometry(
            ModuleScaffold,
            "optimalLine",
            ModuleDebug.DebuggedLine(optimalLine, if (lastBlocksLine == null) Color4b.RED else Color4b.GREEN)
        )

        return optimalLine
    }

    private fun divergesTooMuchFromDirection(lastBlocksLine: Line, direction: Vec3d): Boolean {
        return acos(lastBlocksLine.direction.dotProduct(direction)).absoluteValue / Math.PI * 180 > 50.0
    }

    /**
     * Tries to fit a line that goes through the last placed blocks. Currently only considers the last two.
     */
    private fun fitLinesThroughLastPlacedBlocks(): Line? {
        // Take the last 2 blocks placed
        val lastPlacedBlocksToConsider =
            lastPlacedBlocks.subList(
                fromIndex = (lastPlacedBlocks.size - 2).coerceAtLeast(0),
                toIndex = (lastPlacedBlocks.size).coerceAtLeast(0),
            )

        if (lastPlacedBlocksToConsider.size < 2) {
            return null
        }

        // Just debug stuff
        debugLastPlacedBlocks(lastPlacedBlocksToConsider)

        val avgPos = Vec3d.of(lastPlacedBlocksToConsider[0].add(lastPlacedBlocksToConsider[1])).multiply(0.5)
        val dir = Vec3d.of(lastPlacedBlocksToConsider[1].subtract(lastPlacedBlocksToConsider[0])).normalize()

        // Calculate the average direction of the last placed blocks
        return Line(avgPos, dir)
    }

    private fun debugLastPlacedBlocks(lastPlacedBlocksToConsider: MutableList<BlockPos>) {
        lastPlacedBlocksToConsider.forEachIndexed { idx, pos ->
            val alpha = ((1.0 - idx.toDouble() / lastPlacedBlocksToConsider.size.toDouble()) * 255.0).toInt()

            ModuleDebug.debugGeometry(
                ModuleScaffold,
                "lastPlacedBlock$idx",
                ModuleDebug.DebuggedBox(Box.from(pos.toVec3d()), Color4b(alpha, alpha, 255, 127)),
            )
        }
    }

    /**
     * Find the block the player stands on.
     * It considers all blocks which the player's hitbox collides with and chooses one. If the player stands on the last
     * block this function returned, this block is preferred.
     */
    private fun findBlockPlayerStandsOn(): BlockPos? {
        val offsetsToTry = arrayOf(0.301, 0.0, -0.301)
        // Contains the blocks which the player is currently supported by
        val candidates = mutableListOf<BlockPos>()

        for (xOffset in offsetsToTry) {
            for (zOffset in offsetsToTry) {
                val playerPos = player.pos.add(xOffset, -1.0, zOffset).toBlockPos()

                val isEmpty = playerPos.getState()?.getCollisionShape(world, BlockPos.ORIGIN)?.isEmpty ?: true

                if (!isEmpty) {
                    candidates.add(playerPos)
                }
            }
        }

        // We want to keep the direction of the scaffold
        this.lastPlacedBlocks.lastOrNull()?.let { lastPlacedBlock ->
            if (lastPlacedBlock in candidates) {
                return lastPlacedBlock
            }
        }

        // Stabilize the heuristic
        if (lastPosition in candidates) {
            return lastPosition
        }

        // We have no reason to prefer a candidate so just pick any.
        val currPosition = candidates.firstOrNull()

        lastPosition = currPosition

        return currPosition
    }

    /**
     * The player can move in a lot of directions. But there are only 8 directions which make sense for scaffold to
     * follow (NORTH, NORTH_EAST, EAST, etc.). This function chooses such a direction based on the current angle.
     * i.e. if we were looking like 30° to the right, we would choose the direction NORTH_EAST (1.0, 0.0, 1.0).
     * And scaffold would move diagonally to the right.
     */
    private fun chooseDirection(currentAngle: Float): Vec3d {
        // Transform the angle ([-180; 180]) to [0; 8]
        val currentDirection = currentAngle / 180.0 * 4 + 4

        // Round the angle to the nearest integer, which represents the direction.
        val newDirectionNumber = round(currentDirection)
        // Do this transformation backwards, and we have an angle that follows one of the 8 directions.
        val newDirectionAngle = MathHelper.wrapDegrees((newDirectionNumber - 4) / 4.0 * 180.0 + 90.0) / 180.0 * PI

        return Vec3d(cos(newDirectionAngle), 0.0, sin(newDirectionAngle))
    }

    /**
     * Remembers the last placed blocks and removes old ones.
     */
    fun trackPlacedBlock(target: BlockPlacementTarget) {
        lastPlacedBlocks.add(target.placedBlock)

        while (lastPlacedBlocks.size > MAX_LAST_PLACE_BLOCKS)
            lastPlacedBlocks.removeFirst()
    }

    fun reset() {
        lastPosition = null
        this.lastPlacedBlocks.clear()
    }
}
