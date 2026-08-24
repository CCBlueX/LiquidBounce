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

import net.ccbluex.liquidbounce.config.types.list.Tagged
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
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockOffsetOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTargetFindingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPosOffsets
import net.ccbluex.liquidbounce.utils.block.targetfinding.CenterTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.FaceHandlingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.PlayerLocationOnPlacement
import net.ccbluex.liquidbounce.utils.block.targetfinding.findBestBlockPlacementTarget
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.entity.FallingPlayer
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.raytracing.traceFromPlayer
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Pose
import net.minecraft.world.phys.Vec3
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Clutch module
 *
 * Automatically places a block beneath you to save you when you are about to fall into the void,
 * including when you are knocked off an edge. Unlike a naive "block straight below me" check, this
 * simulates the player's fall trajectory (so horizontal knockback is accounted for) and searches a
 * widening area around the predicted landing spot for any reachable support to place against.
 */
object ModuleClutch : ClientModule("Clutch", ModuleCategories.WORLD) {

    private val minFallDistance by float("MinFallDistance", 1.0f, 0.0f..10.0f)

    /**
     * How many ticks of fall to simulate when deciding whether we are about to land. If no collision
     * is predicted within this horizon, we treat it as falling into the void and start clutching.
     * A larger value reacts earlier (good against strong knockback), at the cost of placing sooner.
     */
    private val lookaheadTicks by int("Lookahead", 20, 5..60, "ticks")

    /**
     * How wide to search around the predicted landing column for a placeable support.
     * NORMAL ≈ scaffold's normal reach, WIDE/FULL catch blocks further to the side when you are
     * knocked toward a wall or the rim of an island.
     */
    private val expand by enumChoice("Expand", Expand.WIDE)

    private val auraReach by float("AuraReach", 4.5f, 3.0f..6.0f)
    private val useReachModule by boolean("UseReachModule", true)
    private val silentSelection by boolean("SilentSelection", true)

    private val rotations = tree(RotationsValueGroup(this))

    private var currentTarget: BlockPlacementTarget? = null
    private var currentSlot: HotbarItemSlot? = null

    /** Short horizon used only to bias the placement anchor toward knockback drift. */
    private const val DRIFT_PREDICT_TICKS = 3

    @Suppress("unused")
    private enum class Expand(override val tag: String, val offsets: BlockPosOffsets) : Tagged {
        NORMAL("Normal", BlockPosOffsets.NORMAL),
        WIDE("Wide", BlockPosOffsets.DOWN),
        FULL("Full", BlockPosOffsets.FULL),
    }

    override fun onDisabled() {
        SilentHotbar.resetSlot(this)
        currentTarget = null
        currentSlot = null
        super.onDisabled()
    }

    private fun effectiveBlockReach(): Float {
        val extra = if (useReachModule && ModuleReach.running) {
            ModuleReach.blockRangeIncrease
        } else {
            0.0f
        }
        return auraReach + extra
    }

    /**
     * Returns true if the player is descending and no collision is predicted within [lookaheadTicks].
     * This covers both straight drops and being knocked sideways off a platform.
     */
    private fun isFallingIntoVoid(): Boolean {
        if (player.deltaMovement.y >= 0.0 || player.fallDistance < minFallDistance) {
            return false
        }

        return FallingPlayer.fromPlayer(player).findCollision(lookaheadTicks) == null
    }

    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent>(
        priority = EventPriorityConvention.SAFETY_FEATURE
    ) {
        currentTarget = null
        currentSlot = null

        val blockSlot = Slots.OffhandWithHotbar.find { isValidBlock(it.itemStack) } ?: return@handler

        if (!isFallingIntoVoid()) {
            return@handler
        }

        val target = findRescueTarget(blockSlot) ?: return@handler

        currentTarget = target
        currentSlot = blockSlot

        RotationManager.setRotationTarget(
            target.rotation,
            valueGroup = rotations,
            priority = Priority.IMPORTANT_FOR_PLAYER_LIFE,
            provider = this@ModuleClutch
        )
    }

    /**
     * Searches downward from the player for the closest level at which a block can actually be placed,
     * anchored at the player's *predicted* horizontal position so knockback drift is followed.
     */
    private fun findRescueTarget(blockSlot: HotbarItemSlot): BlockPlacementTarget? {
        val reach = effectiveBlockReach()
        val reachInt = ceil(reach).toInt()

        // Predict where we will be in a few ticks so we follow horizontal knockback drift, then
        // fall back to the current position.
        val predictedPos = predictHorizontalLandingPos()
        val playerFeetY = floor(player.y).toInt()

        for (dy in 1..reachInt) {
            val anchorY = playerFeetY - dy
            val anchor = BlockPos(floor(predictedPos.x).toInt(), anchorY, floor(predictedPos.z).toInt())

            // Skip levels we cannot reach from the eyes.
            if (player.eyePosition.distanceTo(Vec3.atCenterOf(anchor)) > reach) {
                continue
            }

            val options = BlockPlacementTargetFindingOptions(
                BlockOffsetOptions(
                    expand.offsets.offsets,
                    BlockPlacementTargetFindingOptions.leastBlockDistanceToPos(predictedPos),
                ),
                FaceHandlingOptions(CenterTargetPositionFactory),
                stackToPlaceWith = blockSlot.itemStack,
                PlayerLocationOnPlacement(position = player.position(), pose = Pose.STANDING),
            )

            val target = findBestBlockPlacementTarget(anchor, options)
            if (target != null && player.eyePosition.distanceTo(Vec3.atCenterOf(target.placedBlock)) <= reach) {
                return target
            }
        }

        return null
    }

    /**
     * Estimates where the player will be horizontally after a few ticks, so the placement anchor
     * tracks knockback drift instead of the stale current column. Horizontal velocity decays by the
     * vanilla air-drag factor (~0.91/tick) each tick; we sum a short geometric series of it.
     */
    private fun predictHorizontalLandingPos(): Vec3 {
        val drag = 0.91
        val ticks = DRIFT_PREDICT_TICKS
        // Sum of decaying horizontal displacement over [ticks]: v * (1 + d + d^2 + ... + d^(t-1)).
        var factor = 0.0
        var term = 1.0
        repeat(ticks) {
            factor += term
            term *= drag
        }

        val velocity = player.deltaMovement
        return Vec3(
            player.x + velocity.x * factor,
            player.y,
            player.z + velocity.z * factor,
        )
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        val target = currentTarget ?: return@handler
        val slot = currentSlot ?: return@handler

        val reach = effectiveBlockReach()
        val rayTraceResult = traceFromPlayer(range = reach.toDouble())

        if (rayTraceResult.blockPos != target.interactedBlockPos) {
            return@handler
        }

        // Offhand blocks have no hotbar index, so there is nothing to select: we place straight
        // through [useHand] (OFF_HAND) below. Only hotbar slots need a selection step.
        slot.hotbarIndex?.let { hotbarIndex ->
            if (silentSelection) {
                SilentHotbar.selectSlotSilently(this, slot, 1)
            } else {
                player.inventory.selectedSlot = hotbarIndex
            }
        }

        val onSuccess: () -> Boolean = {
            currentTarget = null
            currentSlot = null
            true
        }

        doPlacement(
            rayTraceResult,
            hand = slot.useHand,
            onItemUseSuccess = onSuccess,
            onPlacementSuccess = onSuccess,
        )
    }

}
