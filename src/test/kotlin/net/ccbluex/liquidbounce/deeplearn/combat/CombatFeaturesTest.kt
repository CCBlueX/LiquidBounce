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

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CombatFeaturesTest {
    @Test
    fun `perception follows vanilla three step interpolation after the fighter's delay`() {
        val timeline = fight(ticks = 20, source = CombatSource.OBSERVED) { tick ->
            targetX = if (tick >= 5) 3.0 else 0.0
        }
        val immediate = CombatPerception(timeline, 0)
        assertEquals(listOf(0.0, 1.0, 2.0, 3.0, 3.0), (4..8).map { immediate.x[it] })
        val delayed = CombatPerception(timeline, 2)
        assertEquals(listOf(0.0, 1.0, 2.0, 3.0), (6..9).map { delayed.x[it] })
    }

    @Test
    fun `a lead shows a steadily moving opponent where it will be`() {
        val timeline = fight(ticks = 40, source = CombatSource.FIRST_PERSON) { tick -> targetX = tick * 0.25 }
        val ahead = CombatPerception(timeline, 0).lead(2)
        val later = CombatPerception(timeline, 0)
        // Interpolation settles on a steady lag behind the updates, after which the motion is exact
        for (tick in 25 until 35) {
            assertTrue(abs(ahead.x[tick] - later.x[tick + 2]) < 1e-3, "tick $tick")
        }
    }

    @Test
    fun `features never read future ticks`() {
        val random = Random(3)
        for (source in CombatSource.entries) {
            val timeline = fight(ticks = 80, source = source) { tick ->
                selfYaw = (tick * 7 % 50).toFloat()
                targetX = sin(tick / 6.0) * 2
                targetZ = 2.5 + cos(tick / 9.0)
                swing = tick % 11 == 0
                rotationUpdate = source == CombatSource.FIRST_PERSON || tick % 2 == 0
            }
            val tick = 40
            val before = features(timeline, tick)
            for (future in tick + 1 until timeline.ticks) {
                for (track in listOf(timeline.self, timeline.target)) {
                    track.x[future] = random.nextInt(-20000, 20000)
                    track.yaw[future] = random.nextInt(-400000, 400000)
                    track.events[future] = random.nextInt(0, Short.MAX_VALUE.toInt()).toShort()
                    track.state[future] = random.nextInt(0, Short.MAX_VALUE.toInt()).toShort()
                    track.clicks[future] = 1
                }
            }
            // An update tick keeps interpolated history causal, so only the values after it may differ.
            assertContentEquals(before, features(timeline, tick), "$source")
        }
    }

    @Test
    fun `live window preserves event ages beyond the rotation history`() {
        val end = 70
        val start = end - CombatFeatures.LIVE_WINDOW + 1
        for (age in listOf(11, 12, 19, 20, 30, 40, 41)) {
            fun recording(from: Int, size: Int) = fight(size, CombatSource.FIRST_PERSON) { tick ->
                swing = from + tick == end - age
                onGround = from + tick <= end - age
            }.also { timeline ->
                val at = end - age - from
                if (at in 0 until size) {
                    timeline.target.events[at] = CombatTrack.SWING.toShort()
                    timeline.self.events[at] = CombatTrack.ITEM_SWITCH.toShort()
                }
            }
            val full = recording(0, end + 1)
            val window = recording(start, CombatFeatures.LIVE_WINDOW)
            assertContentEquals(features(full, end), features(window, window.ticks - 1), "event age=$age")
        }
    }

    @Test
    fun `aim error and hitbox angle describe the opponent relative to the view`() {
        val timeline = fight(ticks = 20, source = CombatSource.FIRST_PERSON) { targetZ = 3.0 }
        val view = CombatView.recorded(timeline)
        val perception = CombatPerception.of(timeline)
        val onTarget = CombatFeatures.Geometry(timeline, perception, view, 15)
        assertTrue(abs(onTarget.aimYaw) < 1e-3f, "${onTarget.aimYaw}")
        assertEquals(0f, onTarget.hitboxAngle)
        view.yaw[15] = 20f
        val turned = CombatFeatures.Geometry(timeline, perception, view, 15)
        assertTrue(abs(turned.aimYaw + 20f) < 1e-3f, "${turned.aimYaw}")
        assertTrue(turned.hitboxAngle in 10f..20f, "${turned.hitboxAngle}")
        assertTrue(abs(turned.distance - 2.7) < 0.05, "${turned.distance}")
    }

    private fun features(timeline: CombatTimeline, tick: Int) = FloatArray(CombatFeatures.SIZE).also {
        CombatFeatures.write(timeline, CombatPerception.of(timeline), CombatView.recorded(timeline),
            CombatFeatures.recordedAttacks(timeline), tick, it)
    }

    private class TickBuilder {
        var selfX = 0.0
        var selfY = 0.0
        var selfZ = 0.0
        var selfYaw = 0f
        var knockback = false
        var targetX = 0.0
        var targetZ = 2.5
        var swing = false
        var onGround = true
        var sprint = false
        var rotationUpdate = true
    }

    private fun fight(ticks: Int, source: CombatSource, tick: TickBuilder.(Int) -> Unit): CombatTimeline {
        val self = CombatTrack(ticks)
        val target = CombatTrack(ticks)
        val builder = TickBuilder()
        for (index in 0 until ticks) {
            builder.tick(index)
            place(self, index, builder.selfX, builder.selfZ, builder)
            place(target, index, builder.targetX, builder.targetZ, builder)
            self.y[index] = (builder.selfY * CombatTrack.POSITION_UNITS).roundToInt()
            if (source == CombatSource.FIRST_PERSON) {
                self.events[index] = (self.events[index].toInt() and CombatTrack.POSITION_UPDATE.inv()).toShort()
            }
            if (builder.knockback) {
                self.events[index] = (self.events[index].toInt() or CombatTrack.KNOCKBACK).toShort()
            }
            self.yaw[index] = (builder.selfYaw * CombatTrack.ANGLE_UNITS).roundToInt()
            self.probes[index] = 0xFFFF.toShort()
            if (builder.swing) {
                self.events[index] = (self.events[index].toInt() or CombatTrack.SWING).toShort()
                self.clicks[index] = 1
            }
        }
        return CombatTimeline(1, source, CombatStyle.COOLDOWN, 774, 1, 2, 50, 50, self, target)
    }

    private fun place(track: CombatTrack, index: Int, x: Double, z: Double, builder: TickBuilder) {
        track.x[index] = (x * CombatTrack.POSITION_UNITS).roundToInt()
        track.z[index] = (z * CombatTrack.POSITION_UNITS).roundToInt()
        track.width[index] = 19
        track.height[index] = 58
        track.eyeHeight[index] = 52
        track.attackDelay[index] = 125
        track.nearestOther[index] = -1
        var state = CombatTrack.LINE_OF_SIGHT
        if (builder.onGround) state = state or CombatTrack.ON_GROUND
        if (builder.sprint) state = state or CombatTrack.SPRINTING
        track.state[index] = state.toShort()
        val rotation = if (builder.rotationUpdate) CombatTrack.ROTATION_UPDATE else 0
        track.events[index] = (CombatTrack.POSITION_UPDATE or rotation).toShort()
    }
}
