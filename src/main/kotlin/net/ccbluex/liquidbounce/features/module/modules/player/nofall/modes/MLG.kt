/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.SimulatedTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.raycast
import net.ccbluex.liquidbounce.utils.block.doPlacement
import net.ccbluex.liquidbounce.utils.block.getBlock
import net.ccbluex.liquidbounce.utils.block.targetFinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.block.targetFinding.BlockPlacementTargetFindingOptions
import net.ccbluex.liquidbounce.utils.block.targetFinding.CenterTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetFinding.findBestBlockPlacementTarget
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.entity.FallingPlayer
import net.ccbluex.liquidbounce.utils.item.Hotbar
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.block.Blocks
import net.minecraft.item.Items
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Vec3i

internal object MLG : Choice("MLG") {
    override val parent: ChoiceConfigurable
        get() = ModuleNoFall.modes

    private val minFallDist by float("MinFallDistance", 5f, 2f..50f)

    private val rotationsConfigurable = tree(RotationsConfigurable())

    private var currentTarget: BlockPlacementTarget? = null

    private val itemForMLG
        get() = Hotbar.findClosestItem(
            arrayOf(
                Items.WATER_BUCKET, Items.COBWEB, Items.POWDER_SNOW_BUCKET, Items.HAY_BLOCK, Items.SLIME_BLOCK
            )
        )

    private val fallDamageBlockingBlocks = arrayOf(
        Blocks.WATER, Blocks.COBWEB, Blocks.POWDER_SNOW, Blocks.HAY_BLOCK, Blocks.SLIME_BLOCK
    )

    val tickMovementHandler = handler<SimulatedTickEvent> {
        if (player.fallDistance <= minFallDist || itemForMLG == null) {
            return@handler
        }

        val collision = FallingPlayer.fromPlayer(player).findCollision(20)?.pos ?: return@handler

        if (collision.getBlock() in fallDamageBlockingBlocks) {
            return@handler
        }

        val options = BlockPlacementTargetFindingOptions(
            listOf(Vec3i(0, 0, 0)),
            player.inventory.getStack(itemForMLG!!),
            CenterTargetPositionFactory,
            BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE,
            player.pos
        )

        currentTarget = findBestBlockPlacementTarget(collision.up(), options)

        val target = currentTarget ?: return@handler
        RotationManager.aimAt(
            target.rotation,
            configurable = rotationsConfigurable,
            priority = Priority.IMPORTANT_FOR_PLAYER_LIFE,
            provider = ModuleNoFall
        )
    }

    val tickHandler = repeatable {
        val target = currentTarget ?: return@repeatable
        val rotation = RotationManager.serverRotation

        val rayTraceResult = raycast(4.5, rotation) ?: return@repeatable

        if (rayTraceResult.type != HitResult.Type.BLOCK || rayTraceResult.blockPos != target.interactedBlockPos
            || rayTraceResult.side != target.direction
        ) {
            return@repeatable
        }

        val item = itemForMLG ?: return@repeatable
        SilentHotbar.selectSlotSilently(this, item, 1)

        doPlacement(rayTraceResult)

        currentTarget = null
    }

}
