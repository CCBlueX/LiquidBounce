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

package net.ccbluex.liquidbounce.integration.task

import net.ccbluex.liquidbounce.integration.task.type.ResourceTask
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class ResourceTaskTest {

    @Test
    fun `speed comes from the recent sample window, not from the whole lifetime`() {
        var now = 0L
        val task = ResourceTask("model.onnx") { Instant.ofEpochMilli(now) }

        now = 1_000
        task.update(1_000_000, 4_000_000, false)
        assertEquals(1_000_000L, task.speed)

        // 2 MB within 500 ms. The lifetime average would be 2 MB/s here.
        now = 1_500
        task.update(3_000_000, 4_000_000, false)
        assertEquals(4_000_000L, task.speed)
    }

    @Test
    fun `speed is cleared once the download is done`() {
        var now = 0L
        val task = ResourceTask("model.onnx") { Instant.ofEpochMilli(now) }

        now = 1_000
        task.update(1_000_000, 4_000_000, false)
        now = 1_500
        task.update(4_000_000, 4_000_000, true)

        assertEquals(0L, task.speed)
    }

    @Test
    fun `a restarted download resets the sample instead of reporting a bogus speed`() {
        var now = 0L
        val task = ResourceTask("model.onnx") { Instant.ofEpochMilli(now) }

        now = 1_000
        task.update(1_000_000, 4_000_000, false)
        now = 1_500
        task.update(1_500_000, 4_000_000, false)

        // Retry: the byte counter starts over
        now = 1_600
        task.update(100, 4_000_000, false)
        assertEquals(0L, task.speed)

        now = 2_100
        task.update(1_100, 4_000_000, false)
        assertEquals(2_000L, task.speed)
    }

}
