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
package net.ccbluex.liquidbounce.utils.block.liquid

import com.google.common.base.Predicates
import net.ccbluex.liquidbounce.utils.block.state
import net.minecraft.core.BlockPos
import net.minecraft.world.level.material.Fluids
import java.util.ArrayDeque
import java.util.function.LongSupplier
import java.util.function.Predicate

/**
 * Tracks recently placed liquid positions and provides pickup queries by time window.
 *
 * @param capacity maximum number of tracked entries; oldest entries are discarded first
 * @param nowProvider clock source used for timestamping and elapsed checks
 */
internal class TimedPickupTracker @JvmOverloads constructor(
    private val capacity: Int,
    private val nowProvider: LongSupplier = LongSupplier(System::currentTimeMillis)
) {
    init {
        require(capacity > 0) { "capacity must be positive." }
    }

    @JvmRecord
    private data class TrackedPos(
        val pos: BlockPos,
        val timestamp: Long,
    )

    private val trackedPositions = ArrayDeque<TrackedPos>(capacity.coerceAtLeast(0))

    /**
     * Removes all tracked entries.
     */
    fun clear() {
        trackedPositions.clear()
    }

    /**
     * Records a position at current time.
     * If the tracker is full, the oldest entry is removed.
     */
    fun record(pos: BlockPos) {
        if (capacity <= 0) {
            return
        }

        trackedPositions.addLast(TrackedPos(pos.immutable(), nowProvider.asLong))

        while (trackedPositions.size > capacity) {
            trackedPositions.removeFirst()
        }
    }

    /**
     * Removes entries that are older than [maxDelayMs] or fail [isPickupable].
     */
    fun prune(maxDelayMs: Long, isPickupable: Predicate<BlockPos>) {
        val now = nowProvider.asLong
        trackedPositions.removeIf { trackedPos ->
            now - trackedPos.timestamp > maxDelayMs || !isPickupable.test(trackedPos.pos)
        }
    }

    /**
     * Returns the oldest tracked position whose age is strictly greater than [minDelayMs] and
     * satisfies [predicate], or `null`.
     */
    @JvmOverloads
    fun firstEligible(minDelayMs: Long, predicate: Predicate<BlockPos> = Predicates.alwaysTrue()): BlockPos? {
        val now = nowProvider.asLong
        return trackedPositions.firstOrNull { trackedPos ->
            now - trackedPos.timestamp > minDelayMs && predicate.test(trackedPos.pos)
        }?.pos
    }

    enum class PickupFilter : Predicate<BlockPos> {
        WATER {
            override fun test(pos: BlockPos): Boolean {
                val state = pos.state ?: return false
                return state.fluidState.isSourceOfType(Fluids.WATER)
            }
        },
        LAVA {
            override fun test(pos: BlockPos): Boolean {
                val state = pos.state ?: return false
                return state.fluidState.isSourceOfType(Fluids.LAVA)
            }
        },
    }
}
