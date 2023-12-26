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
package net.ccbluex.liquidbounce.script.bindings.api

import net.ccbluex.liquidbounce.utils.aiming.raycast
import net.ccbluex.liquidbounce.utils.block.doPlacement
import net.ccbluex.liquidbounce.utils.block.targetFinding.BlockPlacementTargetFindingOptions
import net.ccbluex.liquidbounce.utils.block.targetFinding.CenterTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetFinding.findBestBlockPlacementTarget
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.combat.attack
import net.minecraft.entity.Entity
import net.minecraft.network.packet.c2s.*
import net.minecraft.util.Hand
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3i

object JsInteractionUtil {

    @JvmName("attackEntity")
    fun attackEntity(entity: Entity, swing: Boolean, keepSprint: Boolean) {
        // Safety check
        if (entity == mc.player) {
            return
        }

        entity.attack(swing, keepSprint)
    }

    @JvmName("useEntity")
    fun useEntity(entity: Entity, hand: Hand) {
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

    @JvmName("placeBlock")
    fun placeBlock(blockPos: BlockPos): Boolean {
        val blockPlacementOptions = BlockPlacementTargetFindingOptions(
            listOf(Vec3i(0, 0, 0)),
            player.inventory.mainHandStack,
            CenterTargetPositionFactory,
            BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE
        )

        val bestPlacement = findBestBlockPlacementTarget(blockPos, blockPlacementOptions)
            ?: return false
        val rotation = bestPlacement.rotation.fixedSensitivity()
        val rayTraceResult = raycast(4.5, rotation) ?: return false

        if (rayTraceResult.type != HitResult.Type.BLOCK) {
            return false
        }

        doPlacement(rayTraceResult, hand = Hand.MAIN_HAND)
        return true
    }

}
