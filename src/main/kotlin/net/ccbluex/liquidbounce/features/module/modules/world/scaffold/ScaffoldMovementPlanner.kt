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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold

import net.ccbluex.fastutil.objectHashSetOf
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugGeometry
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.fastCos
import net.ccbluex.liquidbounce.utils.client.fastSin
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.getMovementDirectionOfInput
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.times
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.round

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
                    RotationManager.currentRotation?.yRot ?: player.yRot,
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
        val optimalLine = Line(Vec3(lineBaseBlock.x + 0.5, player.position().y, lineBaseBlock.z + 0.5), direction)

        // Debug optimal line
        ModuleScaffold.debugGeometry("optimalLine") {
            ModuleDebug.DebuggedLine(optimalLine, if (lastBlocksLine == null) Color4b.RED else Color4b.GREEN)
        }

        return optimalLine
    }

    private fun divergesTooMuchFromDirection(lastBlocksLine: Line, direction: Vec3): Boolean {
        return lastBlocksLine.direction.dot(direction) < 0.5 // cos(60deg)
    }

    /**
     * Tries to fit a line that goes through the last placed blocks. Currently only considers the last two.
     */
    private fun fitLinesThroughLastPlacedBlocks(): Line? {
        // Take the last 2 blocks placed
        if (lastPlacedBlocks.size < 2) {
            return null
        }
        val last = lastPlacedBlocks.last()
        val secondToLast = lastPlacedBlocks[lastPlacedBlocks.size - 2]

        // Just debug stuff
        if (ModuleDebug.running) {
            debugLastPlacedBlocks(listOf(secondToLast, last))
        }

        val avgPos = secondToLast.offset(last).toVec3d() * 0.5
        val dir = last.subtract(secondToLast).toVec3d().normalize()

        // Calculate the average direction of the last placed blocks
        return Line(avgPos, dir)
    }

    private fun debugLastPlacedBlocks(lastPlacedBlocksToConsider: List<BlockPos>) {
        lastPlacedBlocksToConsider.forEachIndexed { idx, pos ->
            val alpha = ((1.0 - idx.toDouble() / lastPlacedBlocksToConsider.size.toDouble()) * 255.0).toInt()

            ModuleScaffold.debugGeometry("lastPlacedBlock$idx") {
                ModuleDebug.DebuggedBox(AABB(pos), Color4b(alpha, alpha, 255, 127))
            }
        }
    }

    private val offsetsToTry = doubleArrayOf(0.301, 0.0, -0.301)

    /**
     * Find the block the player stands on.
     * It considers all blocks which the player's hitbox collides with and chooses one. If the player stands on the last
     * block this function returned, this block is preferred.
     */
    private fun findBlockPlayerStandsOn(): BlockPos? {
        // Contains the blocks which the player is currently supported by
        val candidates = objectHashSetOf<BlockPos>()

        for (xOffset in offsetsToTry) {
            for (zOffset in offsetsToTry) {
                val playerPos = player.position().toBlockPos(xOffset, -1.0, zOffset)

                val isEmpty = playerPos.getState()?.getCollisionShape(world, playerPos)?.isEmpty ?: true

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
        return candidates.firstOrNull().also { lastPosition = it }
    }

    /**
     * The player can move in a lot of directions. But there are only 8 directions which make sense for scaffold to
     * follow (NORTH, NORTH_EAST, EAST, etc.). This function chooses such a direction based on the current angle.
     * i.e. if we were looking like 30° to the right, we would choose the direction NORTH_EAST (1.0, 0.0, 1.0).
     * And scaffold would move diagonally to the right.
     *
     * @return normalized direction vector without y value
     */
    private fun chooseDirection(currentAngle: Float): Vec3 {
        // Transform the angle ([-180; 180]) to [0; 8]
        val currentDirection = currentAngle / 180.0F * 4 + 4

        // Round the angle to the nearest integer, which represents the direction.
        val newDirectionNumber = round(currentDirection)
        // Do this transformation backwards, and we have an angle that follows one of the 8 directions.
        val newDirectionAngle = Mth.wrapDegrees((newDirectionNumber - 4) / 4.0F * 180.0F + 90.0F).toRadians()

        return Vec3(newDirectionAngle.fastCos().toDouble(), 0.0, newDirectionAngle.fastSin().toDouble())
    }

    /**
     * Remembers the last placed blocks and removes old ones.
     */
    fun trackPlacedBlock(target: BlockPos) {
        if (target == lastPlacedBlocks.lastOrNull()) return

        while (lastPlacedBlocks.size >= MAX_LAST_PLACE_BLOCKS) {
            lastPlacedBlocks.removeFirst()
        }

        lastPlacedBlocks.add(target)
    }

    fun reset() {
        lastPosition = null
        this.lastPlacedBlocks.clear()
    }
}
