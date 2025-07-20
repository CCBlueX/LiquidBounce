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
package net.ccbluex.liquidbounce.script.bindings.api

import net.ccbluex.liquidbounce.utils.aiming.utils.raycast
import net.ccbluex.liquidbounce.utils.block.doPlacement
import net.ccbluex.liquidbounce.utils.block.targetfinding.*
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.combat.attack
import net.minecraft.entity.Entity
import net.minecraft.util.Hand
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3i

@Suppress("unused")
object ScriptInteractionUtil {

    @JvmName("attackEntity")
    fun attackEntity(entity: Entity, swing: Boolean, keepSprint: Boolean) {
        // Safety check
        if (entity == mc.player) {
            return
        }

        entity.attack(swing, keepSprint)
    }

    @JvmName("interactEntity")
    fun interactEntity(entity: Entity, hand: Hand) {
        // Safety check
        if (entity == mc.player) {
            return
        }

        mc.interactionManager?.interactEntity(mc.player, entity, hand)
    }

    @JvmName("useItem")
    fun useItem(hand: Hand) {
        mc.interactionManager?.interactItem(mc.player, hand)
    }

    /**
     * Places a block at the given [blockPos] using the given [hand].
     *
     * @return true if the block was placed, false otherwise
     */
    @JvmName("placeBlock")
    fun placeBlock(blockPos: BlockPos, hand: Hand): Boolean {
        val itemStack = player.getStackInHand(hand)
        val blockPlacementOptions = BlockPlacementTargetFindingOptions(
            BlockOffsetOptions(
                listOf(Vec3i.ZERO),
                BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE,
            ),
            FaceHandlingOptions(CenterTargetPositionFactory),
            stackToPlaceWith = itemStack,
            PlayerLocationOnPlacement(position = player.pos),
        )

        val bestPlacement = findBestBlockPlacementTarget(blockPos, blockPlacementOptions)
            ?: return false

        // Check if block is reachable to the player
        val rayTraceResult = raycast(bestPlacement.rotation)
            ?: return false

        // If the type we are aiming at is not a block, we can't place it
        if (rayTraceResult.type != HitResult.Type.BLOCK) {
            return false
        }

        doPlacement(rayTraceResult, hand = hand)
        return true
    }

}
