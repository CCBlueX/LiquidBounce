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
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleReach
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ScaffoldBlockItemSelection.isValidBlock
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.block.doPlacement
import net.ccbluex.liquidbounce.utils.block.liquid.planPlacementAtPos
import net.ccbluex.liquidbounce.utils.block.targetfinding.PlacementPlan
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.entity.doesNotCollideBelow
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.raytracing.traceFromPlayer
import net.minecraft.world.phys.Vec3
import kotlin.math.ceil

/**
 * Clutch module
 *
 * Automatically places a block beneath you when falling into the void.
 */
object ModuleClutch : ClientModule("Clutch", ModuleCategories.WORLD) {

    private val minFallDistance by float("MinFallDistance", 2.0f, 0.5f..10.0f)
    private val auraReach by float("AuraReach", 4.5f, 3.0f..6.0f)
    private val useReachModule by boolean("UseReachModule", true)
    private val silentSelection by boolean("SilentSelection", true)

    private val rotations = tree(RotationsValueGroup(this))

    private var currentTarget: PlacementPlan? = null

    override fun onDisabled() {
        SilentHotbar.resetSlot(this)
        currentTarget = null
        super.onDisabled()
    }

    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent>(
        priority = EventPriorityConvention.FIRST_PRIORITY
    ) {
        val blockSlot = Slots.OffhandWithHotbar.find { isValidBlock(it.itemStack) }
        if (blockSlot == null) {
            currentTarget = null
            return@handler
        }

        // Check if player is falling into the void
        val isFallingIntoVoid = player.deltaMovement.y < -0.1 &&
                player.fallDistance >= minFallDistance &&
                player.doesNotCollideBelow()

        if (!isFallingIntoVoid) {
            currentTarget = null
            return@handler
        }

        val blockReach = auraReach +
                (if (useReachModule && ModuleReach.running) ModuleReach.blockRangeIncrease else 0.0f)
        val blockReachInt = ceil(blockReach).toInt()

        var bestPlan: PlacementPlan? = null
        val playerPos = player.blockPosition()

        for (dy in 1..blockReachInt) {
            val checkPos = playerPos.below(dy)
            val plan = planPlacementAtPos(checkPos, blockSlot)
            if (plan != null) {
                if (player.eyePosition.distanceTo(Vec3.atCenterOf(checkPos)) <= blockReach) {
                    bestPlan = plan
                    break
                }
            }
        }

        currentTarget = bestPlan

        if (bestPlan != null) {
            RotationManager.setRotationTarget(
                bestPlan.placementTarget.rotation,
                valueGroup = rotations,
                priority = Priority.IMPORTANT_FOR_PLAYER_LIFE,
                provider = this@ModuleClutch
            )
        }
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        val target = currentTarget ?: return@handler

        val blockReach = auraReach +
                (if (useReachModule && ModuleReach.running) ModuleReach.blockRangeIncrease else 0.0f)
        val rayTraceResult = traceFromPlayer(range = blockReach.toDouble())

        if (!target.doesCorrespondTo(rayTraceResult)) {
            return@handler
        }

        if (silentSelection) {
            SilentHotbar.selectSlotSilently(this, target.hotbarItemSlot, 1)
        } else {
            player.inventory.selectedSlot = target.hotbarItemSlot.hotbarIndex ?: 0
        }

        val onSuccess: () -> Boolean = {
            true
        }

        doPlacement(
            rayTraceResult,
            hand = target.hotbarItemSlot.useHand,
            onItemUseSuccess = onSuccess,
            onPlacementSuccess = onSuccess,
        )

        currentTarget = null
    }

}
