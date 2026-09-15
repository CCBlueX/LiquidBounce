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
package net.ccbluex.liquidbounce.addon

import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.block.doBreak
import net.ccbluex.liquidbounce.utils.block.doPlacement
import net.ccbluex.liquidbounce.utils.block.getBlock
import net.ccbluex.liquidbounce.utils.block.isInteractable
import net.ccbluex.liquidbounce.utils.block.outlineBox
import net.ccbluex.liquidbounce.utils.block.state
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockOffsetOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTargetFindingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.CenterTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.FaceHandlingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.PlayerLocationOnPlacement
import net.ccbluex.liquidbounce.utils.block.targetfinding.findBestBlockPlacementTarget
import net.ccbluex.liquidbounce.utils.raytracing.traceFromPlayer
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3

/**
 * Blocks around the player. Placing and breaking go through the client's own rotation and interaction
 * code, so they behave like the client's modules do.
 */
object World {

    /** Null when the chunk is not loaded. */
    @JvmStatic
    fun state(pos: BlockPos): BlockState? = pos.state

    @JvmStatic
    fun block(pos: BlockPos): Block? = pos.getBlock()

    /** Whether right-clicking [state] opens or toggles something instead of placing. */
    @JvmStatic
    fun isInteractable(state: BlockState?): Boolean = state.isInteractable

    /** The block's shape relative to its position, for [Render.blockBox]. */
    @JvmStatic
    fun outlineBox(pos: BlockPos): AABB = pos.outlineBox

    /**
     * Places the item in [hand] at [pos] against a neighbouring block, rotating silently towards it.
     *
     * @return false when there is nothing to place against or the spot is out of reach
     */
    @JvmStatic
    @JvmOverloads
    fun place(pos: BlockPos, hand: InteractionHand = InteractionHand.MAIN_HAND): Boolean {
        val player = Game.player
        val options = BlockPlacementTargetFindingOptions(
            BlockOffsetOptions.Default,
            FaceHandlingOptions(CenterTargetPositionFactory),
            stackToPlaceWith = player.getItemInHand(hand),
            PlayerLocationOnPlacement(position = player.position()),
        )
        val target = findBestBlockPlacementTarget(pos, options) ?: return false
        val hit = traceFromPlayer(target.rotation)
        if (hit.type != HitResult.Type.BLOCK) {
            return false
        }
        doPlacement(hit, target.rotation, hand = hand)
        return true
    }

    /**
     * Starts breaking [pos], rotating silently towards it. [immediate] sends start and stop at once,
     * which only works in creative or on blocks that break instantly.
     *
     * @return false when [pos] is not in reach
     */
    @JvmStatic
    @JvmOverloads
    fun breakBlock(pos: BlockPos, immediate: Boolean = false): Boolean {
        val hit = traceFromPlayer(Rotation.lookingAt(Vec3.atCenterOf(pos), Game.player.eyePosition))
        if (hit.type != HitResult.Type.BLOCK || hit.blockPos != pos) {
            return false
        }
        doBreak(hit, immediate)
        return true
    }

}
