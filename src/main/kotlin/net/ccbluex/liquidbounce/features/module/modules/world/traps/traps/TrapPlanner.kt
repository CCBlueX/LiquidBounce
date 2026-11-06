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
package net.ccbluex.liquidbounce.features.module.modules.world.traps.traps

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.features.module.modules.world.traps.BlockChangeIntent
import net.ccbluex.liquidbounce.features.module.modules.world.traps.BlockIntentProvider
import net.ccbluex.liquidbounce.utils.block.collidingRegion
import net.ccbluex.liquidbounce.utils.block.immutable
import net.ccbluex.liquidbounce.utils.block.state
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTargetFindingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPosOffsets
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.ccbluex.liquidbounce.utils.math.iterate
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.overlapVolume
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.EntityDimensions
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

abstract class TrapPlanner<T>(
    parent: EventListener,
    name: String,
    enabled: Boolean
): ToggleableValueGroup(parent, name, enabled), BlockIntentProvider<T> {
    protected abstract val trapItems: Set<Item>
    protected abstract val trapWorthyBlocks: Set<Block>

    protected fun findSlotForTrap(): HotbarItemSlot? {
        return Slots.OffhandWithHotbar.findClosestSlot(trapItems)
    }

    /**
     * Called during simulated tick event
     */
    abstract fun plan(enemies: List<LivingEntity>): BlockChangeIntent<T>?

    protected fun findOffsetsForTarget(
        pos: Vec3,
        dims: EntityDimensions,
        velocity: Vec3,
        mustBeOnGround: Boolean
    ): List<BlockPos> {
        val ticksToLookAhead = 5
        val blockPos = pos.toBlockPos()
        val normalizedStartBB = dims.makeBoundingBox(pos) - blockPos
        val lookAhead = velocity.multiply(ticksToLookAhead.toDouble(), 0.0, ticksToLookAhead.toDouble())

        // A target that teleported, or whose last position is still stale, would make the searched region explode.
        if (lookAhead.horizontalDistance() > 30.0) {
            return BlockPosOffsets.NO_OFFSET.offsets
        }

        val normalizedEndBB = normalizedStartBB.move(lookAhead.x, 0.0, lookAhead.z)

        return findOffsetsBetween(normalizedStartBB, normalizedEndBB, blockPos, mustBeOnGround)
    }

    protected fun targetOverlapComparator(
        origin: BlockPos,
        orderedOffsets: List<BlockPos>,
        eyePos: Vec3,
    ): Comparator<BlockPos> {
        val priorityByPos = Object2IntOpenHashMap<BlockPos>(orderedOffsets.size)
        priorityByPos.defaultReturnValue(Int.MAX_VALUE)
        orderedOffsets.forEachIndexed { index, offset ->
            priorityByPos.putIfAbsent(origin.offset(offset), index)
        }

        val eyeDistanceComparator = BlockPlacementTargetFindingOptions.leastBlockDistanceToPos(eyePos)
        return Comparator.comparingInt(priorityByPos).reversed().thenComparing(eyeDistanceComparator)
    }

    private fun findOffsetsBetween(
        startBox: AABB,
        endBox: AABB,
        offsetPos: BlockPos,
        mustBeOnGround: Boolean
    ): List<BlockPos> {
        class ScoredOffset(val offset: BlockPos, val overlap: Double)

        val offsets = ArrayList<ScoredOffset>()

        // The target's body now and after the look-ahead; blocks it only passes over in between are dropped below.
        startBox.minmax(endBox).collidingRegion.iterate().forEach { offset ->
            val bp = offsetPos.offset(offset)

            val bb = AABB(offset)

            if (!startBox.intersects(bb) && !endBox.intersects(bb)) {
                return@forEach
            }

            val currentState = bp.state ?: return@forEach

            if (currentState.block in trapWorthyBlocks || !currentState.canBeReplaced()) {
                return@forEach
            }

            if (mustBeOnGround && (bp.below().state?.isAir != false)) {
                return@forEach
            }

            val overlap = startBox.overlapVolume(bb) + endBox.overlapVolume(bb) * 0.5

            offsets.add(ScoredOffset(offset.immutable, overlap))
        }

        offsets.sortByDescending { it.overlap }

        return offsets.map { it.offset }
    }
}
