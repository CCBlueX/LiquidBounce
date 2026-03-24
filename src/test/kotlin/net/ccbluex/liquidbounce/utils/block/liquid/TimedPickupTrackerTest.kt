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

import net.minecraft.core.BlockPos
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TimedPickupTrackerTest {

    private var now = 0L

    @Test
    fun `returns oldest eligible position`() {
        val tracker = TimedPickupTracker(capacity = 8, nowProvider = { now })
        val first = BlockPos(1, 64, 1)
        val second = BlockPos(2, 64, 2)

        tracker.record(first)
        now = 5L
        tracker.record(second)

        now = 10L
        assertEquals(first, tracker.firstEligible(8L))
    }

    @Test
    fun `pickup boundary is strict greater than min delay`() {
        val tracker = TimedPickupTracker(capacity = 8, nowProvider = { now })
        val pos = BlockPos(1, 64, 1)

        tracker.record(pos)
        now = 10L
        assertNull(tracker.firstEligible(10L))

        now = 11L
        assertEquals(pos, tracker.firstEligible(10L))
    }

    @Test
    fun `prune removes timed out and invalid positions`() {
        val tracker = TimedPickupTracker(capacity = 8, nowProvider = { now })
        val expired = BlockPos(1, 64, 1)
        val invalid = BlockPos(2, 64, 2)
        val valid = BlockPos(3, 64, 3)

        tracker.record(expired)
        now = 2L
        tracker.record(invalid)
        now = 4L
        tracker.record(valid)

        now = 6L
        tracker.prune(maxDelayMs = 5L) { it != invalid }

        assertEquals(valid, tracker.firstEligible(0L))
    }

    @Test
    fun `capacity one keeps only newest entry`() {
        val tracker = TimedPickupTracker(capacity = 1, nowProvider = { now })
        val first = BlockPos(1, 64, 1)
        val second = BlockPos(2, 64, 2)

        tracker.record(first)
        now = 1L
        tracker.record(second)
        now = 2L

        assertEquals(second, tracker.firstEligible(0L))
    }

    @Test
    fun `clear removes all tracked entries`() {
        val tracker = TimedPickupTracker(capacity = 8, nowProvider = { now })
        tracker.record(BlockPos(1, 64, 1))
        tracker.record(BlockPos(2, 64, 2))

        tracker.clear()
        now = 100L

        assertNull(tracker.firstEligible(0L))
    }

    @Test
    fun `firstEligible respects predicate`() {
        val tracker = TimedPickupTracker(capacity = 8, nowProvider = { now })
        val first = BlockPos(1, 64, 1)
        val second = BlockPos(2, 64, 2)
        tracker.record(first)
        now = 1L
        tracker.record(second)

        now = 10L
        assertEquals(second, tracker.firstEligible(0L) { it == second })
    }

    @Test
    fun `record snapshots mutable block positions`() {
        val tracker = TimedPickupTracker(capacity = 8, nowProvider = { now })
        val mutablePos = BlockPos.MutableBlockPos(1, 64, 1)
        tracker.record(mutablePos)

        mutablePos.set(9, 64, 9)
        now = 10L

        assertEquals(BlockPos(1, 64, 1), tracker.firstEligible(0L))
    }

    @Test
    fun `capacity must be positive`() {
        assertThrows<IllegalArgumentException> {
            TimedPickupTracker(capacity = 0)
        }
    }
}
