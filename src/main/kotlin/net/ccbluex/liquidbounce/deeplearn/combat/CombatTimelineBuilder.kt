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

import net.ccbluex.liquidbounce.features.addon.UnstableAddonApi
import net.minecraft.util.Mth
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import kotlin.math.roundToInt

@UnstableAddonApi
class CombatSelfContext(
    val lineOfSight: Boolean,
    val probes: Int,
    val nearestOther: Int,
    val othersSwingNear: Boolean,
    val input: Int = 0,
    val clicks: Int = 0,
) {
    fun withClick() =
        CombatSelfContext(lineOfSight, probes, nearestOther, othersSwingNear, input, clicks.coerceAtLeast(1))

    companion object {
        fun capture(
            level: Level, self: CombatFrame, target: CombatFrame, others: Collection<CombatFrame>,
            input: Int = 0, clicks: Int = 0,
        ) = CombatSelfContext(
            CombatSampler.lineOfSight(level, self, target),
            CombatSampler.probes(level, self),
            CombatSampler.nearestOther(self, target, others),
            CombatSampler.othersSwingNear(self, target, others),
            input, clicks,
        )
    }
}

@UnstableAddonApi
class CombatTimelineBuilder(
    private val source: CombatSource,
    private val style: CombatStyle,
    private val protocol: Int,
    private val selfKey: Int,
    private val targetKey: Int,
    private val selfPing: Int,
    private val targetPing: Int,
) {
    private val self = ArrayList<CombatFrame>()
    private val target = ArrayList<CombatFrame>()
    private val contexts = ArrayList<CombatSelfContext>()

    val size get() = self.size

    fun add(self: CombatFrame, target: CombatFrame, context: CombatSelfContext) {
        this.self += self
        this.target += target
        contexts += context
    }

    fun build(id: Long, ticks: Int = size): CombatTimeline {
        require(ticks in 1..size)
        val origin = self.first().position
        val selfTrack = CombatTrack(ticks)
        val targetTrack = CombatTrack(ticks)
        fill(selfTrack, self, target, origin, ticks, contexts)
        fill(targetTrack, target, self, origin, ticks, null)
        return CombatTimeline(id, source, style, protocol, selfKey, targetKey, selfPing, targetPing,
            selfTrack, targetTrack)
    }

    /**
     * 1.8 servers, also when they translate for newer clients, only show that someone was hurt, never by whom,
     * so a hurt right after the opponent swung within reach is theirs.
     */
    private fun hurtBySwing(frames: List<CombatFrame>, opponents: List<CombatFrame>, tick: Int): Boolean {
        val frame = frames[tick]
        if (frame.damageCause >= 0 || frame.events and CombatTrack.HURT == 0) {
            return false
        }
        return (maxOf(0, tick - SWING_TICKS)..tick).any {
            opponents[it].events and CombatTrack.SWING != 0 &&
                opponents[it].position.distanceTo(frames[it].position) <= SWING_REACH
        }
    }

    private fun fill(
        track: CombatTrack, frames: List<CombatFrame>, opponents: List<CombatFrame>, origin: Vec3, ticks: Int,
        contexts: List<CombatSelfContext>?,
    ) {
        var yaw = 0
        var previousYaw = 0f
        for (tick in 0 until ticks) {
            val frame = frames[tick]
            val relative = frame.position.subtract(origin)
            track.x[tick] = (relative.x * CombatTrack.POSITION_UNITS).roundToInt()
            track.y[tick] = (relative.y * CombatTrack.POSITION_UNITS).roundToInt()
            track.z[tick] = (relative.z * CombatTrack.POSITION_UNITS).roundToInt()
            val delta = if (tick == 0) Mth.wrapDegrees(frame.yaw) else Mth.wrapDegrees(frame.yaw - previousYaw)
            yaw += (delta * CombatTrack.ANGLE_UNITS).roundToInt()
            previousYaw = frame.yaw
            track.yaw[tick] = yaw
            track.pitch[tick] = (frame.pitch * CombatTrack.ANGLE_UNITS).roundToInt()
            var events = frame.events
            if (frame.damageCause == opponents[tick].entityId || hurtBySwing(frames, opponents, tick)) {
                events = events or CombatTrack.HURT_BY_OPPONENT
            }
            track.events[tick] = events.toShort()
            track.hurtTime[tick] = frame.hurtTime.toByte()
            track.health[tick] = frame.health.toByte()
            track.attackDelay[tick] = frame.attackDelay.toByte()
            track.attackStrength[tick] = frame.attackStrength.toByte()
            track.item[tick] = frame.item.ordinal.toByte()
            track.width[tick] = size(frame.width)
            track.height[tick] = size(frame.height)
            track.eyeHeight[tick] = size(frame.eyeHeight)
            var state = frame.state
            val context = contexts?.get(tick)
            if (context != null) {
                if (context.lineOfSight) {
                    state = state or CombatTrack.LINE_OF_SIGHT
                }
                if (context.othersSwingNear) {
                    state = state or CombatTrack.OTHERS_SWING_NEAR
                }
                track.probes[tick] = context.probes.toShort()
                track.nearestOther[tick] = context.nearestOther.toByte()
                track.input[tick] = context.input.toByte()
                track.clicks[tick] = context.clicks.toByte()
            } else {
                track.nearestOther[tick] = CombatTrack.UNKNOWN.toByte()
            }
            track.state[tick] = state.toShort()
        }
    }

    private fun size(blocks: Float) = (blocks * CombatTrack.SIZE_UNITS).roundToInt().coerceIn(0, 254).toByte()

    private companion object {
        const val SWING_TICKS = 2
        const val SWING_REACH = 4.5
    }
}
