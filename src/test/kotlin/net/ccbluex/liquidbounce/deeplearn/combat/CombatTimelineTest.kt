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
package net.ccbluex.liquidbounce.deeplearn.combat

import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPOutputStream
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class CombatTimelineTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `timelines round trip every column`() {
        val random = Random(7)
        val timelines = List(3) { timeline(random, ticks = 200 + it * 50, source = CombatSource.entries[it]) }
        val path = directory.resolve("fights.timeline")
        CombatTimelineFiles.write(path, timelines)
        val restored = CombatTimelineFiles.read(path)
        assertEquals(timelines.size, restored.size)
        timelines.zip(restored).forEach { (expected, actual) ->
            assertEquals(expected.id, actual.id)
            assertEquals(expected.source, actual.source)
            assertEquals(expected.style, actual.style)
            assertEquals(expected.protocol, actual.protocol)
            assertEquals(expected.selfKey, actual.selfKey)
            assertEquals(expected.targetKey, actual.targetKey)
            assertEquals(expected.selfPing, actual.selfPing)
            assertEquals(expected.targetPing, actual.targetPing)
            for ((a, b) in listOf(expected.self to actual.self, expected.target to actual.target)) {
                assertEquals(a.ticks, b.ticks)
                a.intColumns.zip(b.intColumns).forEach { (x, y) -> assertContentEquals(x, y) }
                a.shortColumns.zip(b.shortColumns).forEach { (x, y) -> assertContentEquals(x, y) }
                a.byteColumns.zip(b.byteColumns).forEach { (x, y) -> assertContentEquals(x, y) }
            }
        }
        Files.list(directory).use { entries -> assertTrue(entries.noneMatch { it.toString().endsWith(".tmp") }) }
    }

    @Test
    fun `observed fights stay small`() {
        val random = Random(11)
        val ticks = 20 * 60
        val path = directory.resolve("minute.timeline")
        CombatTimelineFiles.write(path, listOf(timeline(random, ticks, CombatSource.OBSERVED)))
        val bytesPerTick = Files.size(path).toDouble() / ticks
        assertTrue(bytesPerTick < 24.0, "$bytesPerTick bytes per tick")
    }

    @Test
    fun `packet rotations stay exact in angle units`() {
        val track = CombatTrack(1)
        track.yaw[0] = (-37 * 1.40625f * CombatTrack.ANGLE_UNITS).roundToInt()
        assertEquals(-37 * 1.40625f, track.yawDegrees(0))
    }

    @Test
    fun `foreign and truncated files are rejected`() {
        val foreign = directory.resolve("foreign.timeline")
        GZIPOutputStream(Files.newOutputStream(foreign)).use { it.write(byteArrayOf(0, 0, 0, 1, 0, 1)) }
        assertFails { CombatTimelineFiles.read(foreign) }
        val valid = directory.resolve("valid.timeline")
        CombatTimelineFiles.write(valid, listOf(timeline(Random(3), 100, CombatSource.FIRST_PERSON)))
        val bytes = Files.readAllBytes(valid)
        val truncated = directory.resolve("truncated.timeline")
        Files.write(truncated, bytes.copyOf(bytes.size / 2))
        assertFails { CombatTimelineFiles.read(truncated) }
    }

    private fun timeline(random: Random, ticks: Int, source: CombatSource) = CombatTimeline(
        random.nextLong(), source, CombatStyle.COOLDOWN, 767, random.nextInt(), random.nextInt(), 42, 87,
        track(random, ticks, observed = source != CombatSource.FIRST_PERSON),
        track(random, ticks, observed = true),
    )

    private fun track(random: Random, ticks: Int, observed: Boolean) = CombatTrack(ticks).apply {
        var yawDegrees = random.nextFloat() * 360f
        for (tick in 0 until ticks) {
            val update = !observed || tick % 2 == 0
            if (update) {
                yawDegrees += random.nextFloat() * 20f - 10f
            }
            val angle = if (observed) (yawDegrees / 1.40625f).roundToInt() * 1.40625f else yawDegrees
            yaw[tick] = (angle * CombatTrack.ANGLE_UNITS).roundToInt()
            pitch[tick] = ((random.nextFloat() * 10f) * CombatTrack.ANGLE_UNITS).roundToInt()
            move(this, tick)
            fight(this, tick, update, observed)
        }
    }

    private fun move(track: CombatTrack, tick: Int) {
        val airborne = tick % 40 < 6
        track.x[tick] = (sin(tick / 15.0) * 3 * CombatTrack.POSITION_UNITS).roundToInt()
        track.y[tick] = if (airborne) (tick % 40) * 1500 else 0
        track.z[tick] = (tick * 0.05 * CombatTrack.POSITION_UNITS).roundToInt()
        track.state[tick] = (if (airborne) 0 else CombatTrack.ON_GROUND or CombatTrack.SPRINTING).toShort()
        track.probes[tick] = 0xFFFF.toShort()
        track.width[tick] = 19
        track.height[tick] = 58
        track.eyeHeight[tick] = 52
        track.input[tick] = (tick % 16).toByte()
        track.nearestOther[tick] = -1
    }

    private fun fight(track: CombatTrack, tick: Int, update: Boolean, observed: Boolean) {
        val swing = tick % 11 == 0
        val updates = CombatTrack.ROTATION_UPDATE or CombatTrack.POSITION_UPDATE
        track.events[tick] = ((if (update) updates else 0) or (if (swing) CombatTrack.SWING else 0)).toShort()
        track.hurtTime[tick] = (10 - tick % 10).toByte()
        track.health[tick] = (254 - tick / 10).coerceAtLeast(0).toByte()
        track.attackDelay[tick] = 125
        track.attackStrength[tick] = if (observed) -1 else (tick % 13 * 8).toByte()
        track.item[tick] = CombatItem.SWORD.ordinal.toByte()
        track.clicks[tick] = if (!observed && swing) 1 else 0
    }
}
