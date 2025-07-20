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
package net.ccbluex.liquidbounce.features.module.modules.movement.speed

import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.entity.set
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.block.BlockState
import net.minecraft.entity.EntityPose
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

/**
 * Prevents you from bumping into corners when chasing.
 */
object SpeedAntiCornerBump : MinecraftShortcuts {
    /**
     * Called when the speed mode might jump. Decides if the jump should be delayed.
     */
    fun shouldDelayJump(): Boolean {
        val input = SimulatedPlayer.SimulatedPlayerInput.fromClientPlayer(DirectionalInput(player.input))

        input.set(
            jump = true
        )

        val simulatedPlayer = SimulatedPlayer.fromClientPlayer(input)

        return getSuggestedJumpDelay(simulatedPlayer) != null
    }

    /**
     * Is called while the player stands on the ground to decide if he should jump now or later.
     * Does the following steps n times:
     * 1. Jumps
     * 2. Wait until the player hits an edge. If we don't hit an edge before being on ground.
     *    We don't suggest a jump delay
     *
     * When we hit a wall on the second jump, we suggest delaying the jump so that the second jump will be able to
     * jump on the block.
     *
     * A delay is not suggested...
     * - if we can’t jump on the block because there is not enough space
     *
     * @param n number of jumps to simulate
     *
     * @return suggested delay in ticks, if we don't suggest a delay, null
     */
    private fun getSuggestedJumpDelay(
        simulatedPlayer: SimulatedPlayer,
        n: Int = 2,
    ): Int? {
        // Times the player jumped. Starts at 1 because we already jump on the first tick() call.
        var jumpCount = 1
        // Holds the last position where the player was on ground. The player is on ground when the method starts.
        var lastGroundPos = simulatedPlayer.pos

        for (tickIdx in 0..65) {
            simulatedPlayer.tick()

            // Jump is already good. No need to change anything about it.
            if (simulatedPlayer.onGround) {
                if (jumpCount++ >= n) {
                    break
                }

                lastGroundPos = simulatedPlayer.pos
            }

            if (simulatedPlayer.horizontalCollision) {
                // If we hit the wall while going upwards, it doesn't make sense to delay, since we will hit the block
                // anyway.
                if (jumpCount == 1 && simulatedPlayer.velocity.y > 0.0) {
                    return null
                }

                // Check if it makes sense to try to jump on the block
                if (!canJumpOnBlock(simulatedPlayer.pos, lastGroundPos)) {
                    return null
                }

                return tickIdx
            }
        }

        return null
    }

    /**
     * @param lastGroundPos the last position where the player was on ground
     */
    fun canJumpOnBlock(
        collidingPos: Vec3d,
        lastGroundPos: Vec3d,
    ): Boolean {
        val playerDims = player.getDimensions(EntityPose.STANDING)
        val box: Box = playerDims.getBoxAt(collidingPos)
        val blockPos = BlockPos.ofFloored(box.minX - 1.0E-7, collidingPos.y, box.minZ - 1.0E-7)
        val blockPos2 = BlockPos.ofFloored(box.maxX + 1.0E-7, collidingPos.y, box.maxZ + 1.0E-7)

        if (!world.isRegionLoaded(blockPos, blockPos2)) {
            return false
        }

        val jumpOnPos = BlockPos.Mutable(0, blockPos.y, 0)
        for (x in blockPos.x..blockPos2.x) {
            for (z in blockPos.z..blockPos2.z) {
                jumpOnPos.x = x
                jumpOnPos.z = z
                val jumpOnState = jumpOnPos.getState()!!

                // Simple check that asserts that we can actually reach the block with a jump.
                if (jumpOnPos.y + 1 - lastGroundPos.y > 1.3) {
                    continue
                }

                if (!shouldJumpOnBlock(jumpOnPos, jumpOnState, box)) {
                    continue
                }
                val posOneAboveJumpOnBlock = jumpOnPos.up(1)
                val posTwoAboveJumpOnBlock = jumpOnPos.up(2)

                // The player box if we had hit that jump perfectly.
                val currentlyConsideredPlayerBox =
                    playerDims.getBoxAt(collidingPos.x, jumpOnPos.y + 1.0, collidingPos.z)

                val canEnterBlockAbove =
                    canPlayerEnterBlockPos(
                        posOneAboveJumpOnBlock,
                        posOneAboveJumpOnBlock.getState()!!,
                        currentlyConsideredPlayerBox,
                        tolerateLowBoundingBoxes = true,
                    )
                val canEnterBlockTwoAbove =
                    canPlayerEnterBlockPos(
                        posTwoAboveJumpOnBlock,
                        posTwoAboveJumpOnBlock.getState()!!,
                        currentlyConsideredPlayerBox,
                        tolerateLowBoundingBoxes = false,
                    )

                // We would not be able to stand on the block even if he had hit the jump perfectly.
                if (!canEnterBlockAbove || !canEnterBlockTwoAbove) {
                    continue
                }

                return true
            }
        }

        return false
    }

    /**
     * Would we come to a stop at the given block due to collision?
     *
     * @param playerBox the player box at the moment he collides with the given block
     */
    fun shouldJumpOnBlock(
        pos: BlockPos,
        blockState: BlockState,
        playerBox: Box,
    ): Boolean {
        val collisionShape = blockState.getCollisionShape(mc.world!!, pos)

        // The player is currently colliding with the wall, but not inside of it. So we need to expand the
        // player box a bit so that we can check if we collide with that box.
        val extendedPlayerBox = playerBox.expand(0.01, 0.01, 0.01)

        collisionShape.boundingBoxes.forEach {
            // Does the player collide with that box? If he collides, and we cannot step the block,
            // we would collide with that block, so we should jump on it.
            if (it.offset(pos).intersects(extendedPlayerBox) && it.maxY > 0.5) {
                return true
            }
        }

        return false
    }

    /**
     * Used for blocks that are above the block we try to jump on. Checks if the player can enter the block pos
     * or if he would collide with the block and fall down.
     *
     * @param playerBox the player box. it's minY should be the top of the block we want to jump on.
     * @param tolerateLowBoundingBoxes if true, bounding boxes whose height is 0.2 or lower are ignored. This should be
     * set if this block is directly above the block the player currently tries to jump on. Since in that case
     * the player can also jump on that block (because of the jump height), regardless if there is a block two blocks
     * above (because `1.8 (player height) + 0.2 = 2.0`).
     */
    fun canPlayerEnterBlockPos(
        pos: BlockPos,
        blockState: BlockState,
        playerBox: Box,
        tolerateLowBoundingBoxes: Boolean,
    ): Boolean {
        val collisionShape = blockState.getCollisionShape(mc.world!!, pos)

        // The player is currently colliding with the wall, but not inside of it. So we need to expand the
        // player box a bit so that we can check if we collide with that box.
        val extendedPlayerBox = playerBox.expand(0.01, 0.01, 0.01)

        collisionShape.boundingBoxes.forEach {
            if (tolerateLowBoundingBoxes && it.maxY <= 0.2) {
                return@forEach
            }
            // The player collides with that part of the bounding box. Thus, he would just slide down the block.
            if (it.offset(pos).intersects(extendedPlayerBox)) {
                return false
            }
        }

        return true
    }
}
