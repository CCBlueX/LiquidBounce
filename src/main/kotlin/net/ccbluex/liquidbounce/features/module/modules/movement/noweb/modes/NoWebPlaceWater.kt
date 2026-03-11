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
package net.ccbluex.liquidbounce.features.module.modules.movement.noweb.modes

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import net.ccbluex.fastutil.objectLinkedSetOf
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.noweb.ModuleNoWeb
import net.ccbluex.liquidbounce.features.module.modules.movement.noweb.NoWebMode
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.block.DIRECTIONS_EXCLUDING_DOWN
import net.ccbluex.liquidbounce.utils.block.DIRECTIONS_HORIZONTAL
import net.ccbluex.liquidbounce.utils.block.doPlacement
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.immutable
import net.ccbluex.liquidbounce.utils.block.liquid.TimedPickupTracker
import net.ccbluex.liquidbounce.utils.block.liquid.planPlacementAtPos
import net.ccbluex.liquidbounce.utils.block.targetBlockPos
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.centerOnSide
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.raytracing.traceFromPlayer
import net.ccbluex.liquidbounce.utils.world.waterEvaporates
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.WebBlock
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3

/**
 * TODO: fix water fluid not spread to break the cobweb
 */
@Suppress("TooManyFunctions")
object NoWebPlaceWater : NoWebMode("PlaceWater") {
    private const val MAX_TRACKED_WEBS = 8
    private const val PICKUP_TRACKER_CAPACITY = 16
    private const val SAME_WEB_RETRY_DELAY_MS = 250L

    private object Pickup : ToggleableValueGroup(this@NoWebPlaceWater, "Pickup", true) {
        // Keep a hard lower bound so water has enough time to spread at least one block.
        val pickupSpan by floatRange("PickupSpan", 0.8F..3.0F, 0.5F..20.0F, "s")
    }

    private val rotations = tree(RotationsValueGroup(this))
    private val pickupTracker = TimedPickupTracker(PICKUP_TRACKER_CAPACITY)
    private val trackedWebs = ObjectLinkedOpenHashSet<BlockPos>()

    private var currentAction: UseAction? = null
    private var lastSuccessfulWeb: BlockPos? = null
    private var lastSuccessfulAt = 0L

    init {
        tree(Pickup)
    }

    private data class UseAction(
        val slot: HotbarItemSlot,
        val rotation: Rotation,
        val resolveHitResult: (BlockHitResult) -> BlockHitResult?,
        val onSuccess: (BlockHitResult) -> Unit,
    )

    override fun disable() {
        resetState()
    }

    override fun handleEntityCollision(pos: BlockPos): Boolean {
        trackedWebs.add(pos.immutable)
        while (trackedWebs.size > MAX_TRACKED_WEBS) {
            trackedWebs.removeFirst()
        }

        // Do not cancel web slowdown in this mode.
        return false
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        resetState()
    }

    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent> {
        currentAction = null

        if (world.waterEvaporates) {
            return@handler
        }

        trackedWebs.removeIf { trackedPos -> trackedPos.getState()?.block !is WebBlock }

        val now = System.currentTimeMillis()
        val placeAction = Slots.OffhandWithHotbar.findClosestSlot(Items.WATER_BUCKET)?.let { waterSlot ->
            trackedWebs.firstNotNullOfOrNull { webPos ->
                if (lastSuccessfulWeb == webPos && now - lastSuccessfulAt <= SAME_WEB_RETRY_DELAY_MS) {
                    return@firstNotNullOfOrNull null
                }

                buildPlaceAction(webPos, waterSlot)
            }
        }

        currentAction = placeAction ?: buildPickupAction()

        val action = currentAction ?: return@handler
        RotationManager.setRotationTarget(
            action.rotation,
            valueGroup = rotations,
            priority = Priority.IMPORTANT_FOR_PLAYER_LIFE,
            provider = ModuleNoWeb,
        )
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        val action = currentAction ?: return@handler
        val resolvedHitResult = action.resolveHitResult(traceFromPlayer()) ?: return@handler

        SilentHotbar.selectSlotSilently(this, action.slot, 1)
        val onSuccess = {
            action.onSuccess(resolvedHitResult)
            true
        }

        doPlacement(
            resolvedHitResult,
            hand = action.slot.useHand,
            onItemUseSuccess = onSuccess,
            onPlacementSuccess = onSuccess,
        )

        currentAction = null
    }

    private fun buildPlaceAction(
        webPos: BlockPos,
        waterSlot: HotbarItemSlot,
    ): UseAction? {
        val webBox = AABB(webPos)
        val eyes = player.eyePosition

        return when {
            // Eye inside web => top placement is usually blocked by the web volume, so prefer side usage.
            webBox.contains(eyes) -> buildDirectionalPlaceAction(webPos, waterSlot, DIRECTIONS_EXCLUDING_DOWN)
            // Eye above web => standard "place on top" path is most reliable.
            eyes.y > webBox.maxY -> buildTopPlaceAction(webPos, waterSlot)
            // Otherwise keep side-only fallback to avoid clicking through the lower half.
            else -> buildDirectionalPlaceAction(webPos, waterSlot, DIRECTIONS_HORIZONTAL)
        }
    }

    private fun buildTopPlaceAction(
        webPos: BlockPos,
        waterSlot: HotbarItemSlot,
    ): UseAction? {
        val plan = planPlacementAtPos(webPos.above(), waterSlot) ?: return null

        return UseAction(
            slot = plan.hotbarItemSlot,
            rotation = plan.placementTarget.rotation,
            resolveHitResult = { rayTraceResult ->
                if (plan.doesCorrespondTo(rayTraceResult)) rayTraceResult else null
            },
            onSuccess = {
                markWebPlacementSuccess(webPos)
                pickupTracker.record(plan.targetPos)
            },
        )
    }

    private fun buildDirectionalPlaceAction(
        webPos: BlockPos,
        waterSlot: HotbarItemSlot,
        directions: Array<Direction>,
    ): UseAction? {
        val side = pickBestSide(webPos, directions) ?: return null
        val faceCenter = AABB(webPos).centerOnSide(side)
        val fallbackHitResult = BlockHitResult(faceCenter, side, webPos, false)

        return UseAction(
            slot = waterSlot,
            rotation = Rotation.lookingAt(point = faceCenter, from = player.eyePosition),
            resolveHitResult = { rayTraceResult ->
                resolveDirectionalPlacementHitResult(rayTraceResult, webPos, side, fallbackHitResult)
            },
            onSuccess = { placementHitResult ->
                markWebPlacementSuccess(webPos)
                recordDirectionalWaterCandidates(webPos, side, placementHitResult)
            },
        )
    }

    private fun buildPickupAction(): UseAction? {
        if (!Pickup.enabled) {
            return null
        }

        val pickupSpanStartMs = (Pickup.pickupSpan.start * 1000.0F).toLong()
        val pickupSpanEndMs = (Pickup.pickupSpan.endInclusive * 1000.0F).toLong()

        pickupTracker.prune(pickupSpanEndMs, TimedPickupTracker.PickupFilter.WATER)

        val maxRangeSq = player.blockInteractionRange().sq()
        val pickupPos = pickupTracker.firstEligible(pickupSpanStartMs) { pos ->
            AABB(pos).distanceToSqr(player.eyePosition) <= maxRangeSq
        } ?: return null

        val bucketSlot = Slots.OffhandWithHotbar.findClosestSlot(Items.BUCKET) ?: return null
        val pickupCenter = Vec3.atCenterOf(pickupPos)

        return UseAction(
            slot = bucketSlot,
            rotation = Rotation.lookingAt(point = pickupCenter, from = player.eyePosition),
            resolveHitResult = { rayTraceResult ->
                resolvePickupHitResult(rayTraceResult, pickupPos, pickupCenter)
            },
            onSuccess = {
                pickupTracker.prune(0L, TimedPickupTracker.PickupFilter.WATER)
            },
        )
    }

    private fun markWebPlacementSuccess(webPos: BlockPos) {
        lastSuccessfulWeb = webPos
        lastSuccessfulAt = System.currentTimeMillis()
        trackedWebs.remove(webPos)
    }

    private fun recordDirectionalWaterCandidates(
        webPos: BlockPos,
        side: Direction,
        placementHitResult: BlockHitResult,
    ) {
        val sidePos = webPos.relative(side)
        val inferredWaterPos = placementHitResult.targetBlockPos
        val candidates = objectLinkedSetOf(inferredWaterPos, webPos, sidePos)

        // Bucket placement near webs can resolve to adjacent cells depending on stance and hit face.
        // Track all six neighbors to keep pickup robust without branching per edge-case.
        for (direction in Direction.entries) {
            candidates += webPos.relative(direction)
        }

        candidates.forEach(pickupTracker::record)
    }

    private fun resolveDirectionalPlacementHitResult(
        rayTraceResult: BlockHitResult,
        webPos: BlockPos,
        side: Direction,
        fallbackHitResult: BlockHitResult,
    ): BlockHitResult {
        if (rayTraceResult.type != HitResult.Type.BLOCK) {
            return fallbackHitResult
        }

        val directWebFace = rayTraceResult.blockPos == webPos && rayTraceResult.direction == side
        val oppositeAdjacentFace =
            rayTraceResult.blockPos == webPos.relative(side) && rayTraceResult.direction == side.opposite

        return if (directWebFace || oppositeAdjacentFace) rayTraceResult else fallbackHitResult
    }

    private fun resolvePickupHitResult(
        rayTraceResult: BlockHitResult,
        pickupPos: BlockPos,
        pickupCenter: Vec3,
    ): BlockHitResult {
        val fluidTraceResult = traceFromPlayer(includeFluids = true)
        return when {
            // Prefer fluid-inclusive trace so we can hit source blocks hidden behind web geometry.
            fluidTraceResult.type == HitResult.Type.BLOCK && fluidTraceResult.blockPos == pickupPos -> {
                fluidTraceResult
            }

            rayTraceResult.type == HitResult.Type.BLOCK && rayTraceResult.blockPos == pickupPos -> {
                rayTraceResult
            }

            rayTraceResult.type == HitResult.Type.BLOCK && rayTraceResult.targetBlockPos == pickupPos -> {
                // We clicked the neighbor face but the target cell is the source block.
                BlockHitResult(pickupCenter, rayTraceResult.direction.opposite, pickupPos, false)
            }

            // Final fallback keeps vanilla-style right-click on the expected source cell.
            else -> BlockHitResult(pickupCenter, Direction.UP, pickupPos, false)
        }
    }

    private fun pickBestSide(
        webPos: BlockPos,
        directions: Array<Direction>,
    ): Direction? {
        return directions
            .filter { side ->
                val adjacentState = webPos.relative(side).getState() ?: return@filter false
                // Find a replaceable side to place water
                adjacentState.isAir ||
                    (adjacentState.fluidState.`is`(Fluids.LAVA) && adjacentState.fluidState.isSource)
            }
            .maxByOrNull { side ->
                player.lookAngle.dot(side.unitVec3)
            }
    }

    private fun resetState() {
        currentAction = null
        trackedWebs.clear()
        pickupTracker.clear()
    }
}
