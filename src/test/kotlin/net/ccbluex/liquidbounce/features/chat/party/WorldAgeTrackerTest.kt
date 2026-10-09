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

package net.ccbluex.liquidbounce.features.chat.party

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WorldAgeTrackerTest {

    @Test
    fun `age is offered after two advancing updates`() {
        val tracker = WorldAgeTracker()
        tracker.update(1000, 0)
        assertNull(tracker.current(0))

        tracker.update(1020, 1000)
        assertEquals(1040, tracker.current(2000))
    }

    @Test
    fun `frozen time withdraws the age`() {
        val tracker = WorldAgeTracker()
        tracker.update(1000, 0)
        tracker.update(1020, 1000)
        tracker.update(1020, 2000)
        assertEquals(1040, tracker.current(3000))

        tracker.update(1020, 3000)
        assertNull(tracker.current(4000))
    }

    @Test
    fun `lag shows up as drift`() {
        val tracker = WorldAgeTracker()
        tracker.update(0, 0)
        tracker.update(20, 1000)

        // 10 TPS for 10 seconds: 100 ticks instead of 200
        tracker.update(120, 11_000)
        assertEquals(100, tracker.drift(20, 1000, 11_000))
        assertEquals(0, tracker.drift(120, 11_000, 11_000))
    }

    @Test
    fun `reset forgets the world`() {
        val tracker = WorldAgeTracker()
        tracker.update(0, 0)
        tracker.update(20, 1000)
        tracker.reset()
        assertNull(tracker.current(1000))
    }

}
