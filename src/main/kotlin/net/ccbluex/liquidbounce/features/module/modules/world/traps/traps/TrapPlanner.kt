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
package net.ccbluex.liquidbounce.features.module.modules.world.traps.traps

import it.unimi.dsi.fastutil.doubles.DoubleLongPair
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.features.module.modules.world.traps.BlockChangeIntent
import net.ccbluex.liquidbounce.features.module.modules.world.traps.BlockIntentProvider
import net.ccbluex.liquidbounce.utils.block.collidingRegion
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.ccbluex.liquidbounce.utils.math.iterate
import net.ccbluex.liquidbounce.utils.math.size
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.LivingEntity
import net.minecraft.item.Item
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

abstract class TrapPlanner<T>(
    parent: EventListener,
    name: String,
    enabled: Boolean
): ToggleableConfigurable(parent, name, enabled), BlockIntentProvider<T> {
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
        pos: Vec3d,
        dims: EntityDimensions,
        velocity: Vec3d,
        mustBeOnGround: Boolean
    ): List<BlockPos> {
        val ticksToLookAhead = 5
        val blockPos = pos.toBlockPos()
        val normalizedStartBB =
            dims.getBoxAt(pos).offset(-blockPos.x.toDouble(), -blockPos.y.toDouble(), -pos.z.toInt().toDouble())
        val normalizedEnddBB = normalizedStartBB.offset(
            velocity.x * ticksToLookAhead,
            0.0,
            velocity.z * ticksToLookAhead
        )

        val searchBB = normalizedEnddBB

        if (searchBB.size > 30) {
            return listOf(BlockPos.ORIGIN)
        }

        return findOffsetsBetween(normalizedStartBB, normalizedEnddBB, blockPos, mustBeOnGround)
    }

    private fun findOffsetsBetween(
        startBox: Box,
        endBox: Box,
        offsetPos: BlockPos,
        mustBeOnGround: Boolean
    ): List<BlockPos> {
        val offsets = mutableListOf<DoubleLongPair>()

        startBox.collidingRegion.iterate().forEach { offset ->
            val bp = offsetPos.add(offset)

            val bb = Box(offset)

            if (!startBox.intersects(bb) && !endBox.intersects(bb)) {
                return@forEach
            }

            val currentState = bp.getState()?.block

            if (currentState in trapWorthyBlocks || currentState != Blocks.AIR) {
                return@forEach
            }

            if (mustBeOnGround && (bp.down().getState()?.isAir != false)) {
                return@forEach
            }

            val intersect = startBox.intersection(bb).size + endBox.intersection(bb).size * 0.5

            offsets.add(DoubleLongPair.of(intersect, offset.asLong()))
        }

        offsets.sortByDescending { it.leftDouble() }

        return offsets.map { BlockPos.fromLong(it.rightLong()) }
    }
}
