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
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleNoFall
import net.ccbluex.liquidbounce.utils.aiming.raycast
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.targetFinding.BlockPlacementTargetFindingOptions
import net.ccbluex.liquidbounce.utils.block.targetFinding.CenterTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetFinding.findBestBlockPlacementTarget
import net.ccbluex.liquidbounce.utils.client.clickBlockWithSlot
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.item.findHotbarSlot
import net.minecraft.block.Blocks
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Vec3i

/**
 * Ignite module
 *
 * Automatically sets targets around you on fire.
 */
object ModuleIgnite : Module("Ignite", Category.WORLD) {

    var delay by int("Delay", 20, 0..400)

    // Target
    private val targetTracker = tree(TargetTracker())

    val networkTickHandler = repeatable { event ->
        val player = mc.player ?: return@repeatable

        val slot = findHotbarSlot(Items.LAVA_BUCKET) ?: return@repeatable

        for (enemy in targetTracker.enemies()) {
            if (enemy.squaredBoxedDistanceTo(player) > 6.0 * 6.0) {
                continue
            }

            val pos = enemy.blockPos

            val state = pos.getState()

            if (state?.block == Blocks.LAVA) {
                continue
            }

            val options = BlockPlacementTargetFindingOptions(
                listOf(Vec3i(0, 0, 0)),
                player.inventory.getStack(slot),
                CenterTargetPositionFactory,
                BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE
            )

            val currentTarget = findBestBlockPlacementTarget(pos, options) ?: continue

            val rotation = currentTarget.rotation.fixedSensitivity()
            val rayTraceResult = raycast(4.5, rotation) ?: return@repeatable

            if (rayTraceResult.type != HitResult.Type.BLOCK) {
                continue
            }

            player.networkHandler.sendPacket(
                PlayerMoveC2SPacket.LookAndOnGround(
                    rotation.yaw, rotation.pitch, player.isOnGround
                )
            )

            clickBlockWithSlot(player, rayTraceResult, slot)

            break
        }

    }
}
