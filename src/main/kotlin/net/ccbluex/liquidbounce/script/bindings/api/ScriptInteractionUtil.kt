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
package net.ccbluex.liquidbounce.script.bindings.api

import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.block.doPlacement
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockOffsetOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTargetFindingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.CenterTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.FaceHandlingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.PlayerLocationOnPlacement
import net.ccbluex.liquidbounce.utils.block.targetfinding.findBestBlockPlacementTarget
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.combat.attackEntity
import net.ccbluex.liquidbounce.utils.raytracing.traceFromPlayer
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.HitResult

@Suppress("unused")
object ScriptInteractionUtil {

    @JvmName("attackEntity")
    fun attackEntityJs(entity: Entity, swing: Boolean, keepSprint: Boolean) {
        // Safety check
        if (entity == mc.player) {
            return
        }

        attackEntity(entity, if (swing) SwingMode.DO_NOT_HIDE else SwingMode.HIDE_BOTH, keepSprint)
    }

    @JvmName("interactEntity")
    fun interactEntity(entity: Entity, hand: InteractionHand) {
        // Safety check
        if (entity == mc.player) {
            return
        }

        mc.gameMode?.interact(mc.player!!, entity, hand)
    }

    @JvmName("useItem")
    fun useItem(hand: InteractionHand) {
        mc.gameMode?.useItem(mc.player!!, hand)
    }

    /**
     * Places a block at the given [blockPos] using the given [hand].
     *
     * @return true if the block was placed, false otherwise
     */
    @JvmName("placeBlock")
    fun placeBlock(blockPos: BlockPos, hand: InteractionHand): Boolean {
        val itemStack = player.getItemInHand(hand)
        val blockPlacementOptions = BlockPlacementTargetFindingOptions(
            BlockOffsetOptions.Default,
            FaceHandlingOptions(CenterTargetPositionFactory),
            stackToPlaceWith = itemStack,
            PlayerLocationOnPlacement(position = player.position()),
        )

        val bestPlacement = findBestBlockPlacementTarget(blockPos, blockPlacementOptions)
            ?: return false

        // Check if block is reachable to the player
        val rayTraceResult = traceFromPlayer(bestPlacement.rotation)

        // If the type we are aiming at is not a block, we can't place it
        if (rayTraceResult.type != HitResult.Type.BLOCK) {
            return false
        }

        doPlacement(rayTraceResult, hand = hand)
        return true
    }

}
