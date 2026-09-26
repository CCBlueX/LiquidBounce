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
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@UnstableAddonApi
object CombatFeatures {
    const val VERSION = 3
    const val HISTORY = 10
    const val EVENT_HISTORY = 40
    const val LIVE_WINDOW = EVENT_HISTORY + 1
    const val CURRENT = 36
    const val PER_TICK = 7
    const val SIZE = CURRENT + HISTORY * PER_TICK
    const val TURN_SCALE = 30f
    private const val DEFAULT_ATTACK_DELAY = 12.5f
    private const val REACH_LIMIT = 8.0

    /**
     * Writes the policy input for [tick]. Only ticks up to [tick] are read, from [timeline], the
     * [perception] of the opponent, the [view] and the own attack onsets in [attacks].
     */
    fun write(
        timeline: CombatTimeline, perception: CombatPerception, view: CombatView, attacks: BooleanArray,
        tick: Int, out: FloatArray, offset: Int = 0,
    ) {
        require(tick >= HISTORY)
        val geometry = Geometry(timeline, perception, view, tick)
        current(timeline, perception, view, attacks, tick, geometry).copyInto(out, offset)
        history(timeline, perception, view, tick, geometry, out, offset + CURRENT)
    }

    @Suppress("LongMethod")
    private fun current(
        timeline: CombatTimeline, perception: CombatPerception, view: CombatView, attacks: BooleanArray,
        tick: Int, geometry: Geometry,
    ): FloatArray {
        val self = timeline.self
        val target = timeline.target
        val (selfForward, selfLeft) = local(view.yaw[tick],
            self.positionX(tick) - self.positionX(tick - 1), self.positionZ(tick) - self.positionZ(tick - 1))
        val (targetForward, targetLeft) = local(view.yaw[tick],
            perception.x[tick] - perception.x[tick - 1], perception.z[tick] - perception.z[tick - 1])
        val ticksSinceAttack = ticksSince(tick) { attacks[it] }
        val sinceReset = min(ticksSinceAttack, ticksSince(tick) { self.hasEvent(it, CombatTrack.ITEM_SWITCH) })
        val delay = self.attackDelayTicks(tick)?.takeIf { it > 0f } ?: DEFAULT_ATTACK_DELAY
        val facing = wrap(perception.yaw[tick] - bearingYaw(
            self.positionX(tick) - perception.x[tick], self.positionZ(tick) - perception.z[tick]
        ))
        val direction = (view.yaw[tick] / 22.5f).roundToInt()

        return floatArrayOf(
            geometry.aimYaw / 180f,
            geometry.aimPitch / 90f,
            (geometry.hitboxAngle / TURN_SCALE).coerceIn(0f, 2f),
            (geometry.halfWidthAngle / TURN_SCALE).coerceIn(0f, 2f),
            (geometry.halfHeightAngle / 45f).coerceIn(0f, 2f),
            (geometry.distance / 6.0).toFloat().coerceIn(0f, 2f),
            ((perception.y[tick] - self.positionY(tick)) / 3.0).toFloat().coerceIn(-2f, 2f),
            (selfForward / 0.5).toFloat().coerceIn(-2f, 2f),
            (selfLeft / 0.5).toFloat().coerceIn(-2f, 2f),
            (self.positionY(tick) - self.positionY(tick - 1)).toFloat().coerceIn(-2f, 2f),
            (targetForward / 0.5).toFloat().coerceIn(-2f, 2f),
            (targetLeft / 0.5).toFloat().coerceIn(-2f, 2f),
            (perception.y[tick] - perception.y[tick - 1]).toFloat().coerceIn(-2f, 2f),
            flag(self.has(tick, CombatTrack.ON_GROUND)),
            flag(target.has(tick, CombatTrack.ON_GROUND)),
            flag(self.has(tick, CombatTrack.SPRINTING)),
            flag(target.has(tick, CombatTrack.SPRINTING)),
            self.hurtTime[tick] / 10f,
            target.hurtTime[tick] / 10f,
            (sinceReset / delay).coerceIn(0f, 1f),
            min(ticksSinceAttack, 20) / 20f,
            min(ticksSince(tick) { target.hasEvent(it, CombatTrack.SWING) }, 20) / 20f,
            abs(facing) / 180f,
            flag(self.has(tick, CombatTrack.LINE_OF_SIGHT)),
            (self.nearestOtherBlocks(tick) ?: 16f) / 16f,
            flag(self.has(tick, CombatTrack.OTHERS_SWING_NEAR)),
            flag(self.safeDirection(tick, direction)),
            flag(self.safeDirection(tick, direction + 8)),
            flag(self.safeDirection(tick, direction - 4)),
            flag(self.safeDirection(tick, direction + 4)),
            flag(self.has(tick, CombatTrack.USING_ITEM)),
            flag(target.has(tick, CombatTrack.BLOCKING)),
            flag(timeline.style == CombatStyle.LEGACY),
            min(ticksSince(tick) { self.has(it, CombatTrack.ON_GROUND) }, 20) / 20f,
            ((sinceReset - delay) / 10f).coerceIn(-1.5f, 1.5f),
            CombatSkill.input(timeline),
        ).also { check(it.size == CURRENT) }
    }

    private fun history(
        timeline: CombatTimeline, perception: CombatPerception, view: CombatView, tick: Int, geometry: Geometry,
        out: FloatArray, offset: Int,
    ) {
        var previousGeometry = Geometry(timeline, perception, view, tick - HISTORY)
        for (index in 0 until HISTORY) {
            val at = tick - HISTORY + 1 + index
            val step = if (at == tick) geometry else Geometry(timeline, perception, view, at)
            val base = offset + index * PER_TICK
            out[base] = (step.aimYaw / TURN_SCALE).coerceIn(-3f, 3f)
            out[base + 1] = (step.aimPitch / TURN_SCALE).coerceIn(-3f, 3f)
            out[base + 2] = (wrap(view.yaw[at] - view.yaw[at - 1]) / TURN_SCALE).coerceIn(-3f, 3f)
            out[base + 3] = ((view.pitch[at] - view.pitch[at - 1]) / TURN_SCALE).coerceIn(-3f, 3f)
            out[base + 4] = (wrap(step.bearingYaw - previousGeometry.bearingYaw) / TURN_SCALE).coerceIn(-3f, 3f)
            out[base + 5] = ((step.distance - previousGeometry.distance) / 0.5).toFloat().coerceIn(-2f, 2f)
            out[base + 6] = (step.hitboxAngle / TURN_SCALE).coerceIn(0f, 3f)
            previousGeometry = step
        }
    }

    fun recordedAttacks(timeline: CombatTimeline) = BooleanArray(timeline.ticks) { tick ->
        if (timeline.source == CombatSource.FIRST_PERSON) {
            timeline.self.clicks[tick] > 0
        } else {
            timeline.self.hasEvent(tick, CombatTrack.SWING) ||
                timeline.target.hasEvent(tick, CombatTrack.HURT_BY_OPPONENT)
        }
    }

    /** [dx] and [dz] as forward and left movement relative to [yaw]. */
    private fun local(yaw: Float, dx: Double, dz: Double): Pair<Double, Double> {
        val radians = Math.toRadians(yaw.toDouble())
        return (dx * -sin(radians) + dz * cos(radians)) to (dx * cos(radians) + dz * sin(radians))
    }

    private inline fun ticksSince(tick: Int, event: (Int) -> Boolean): Int {
        for (age in 0..EVENT_HISTORY) {
            val at = tick - age
            if (at < 0) {
                break
            }
            if (event(at)) {
                return age
            }
        }
        return EVENT_HISTORY
    }

    private fun flag(value: Boolean) = if (value) 1f else 0f

    fun wrap(degrees: Float): Float {
        var value = degrees % 360f
        if (value >= 180f) value -= 360f
        if (value < -180f) value += 360f
        return value
    }

    fun bearingYaw(dx: Double, dz: Double) = (Math.toDegrees(atan2(dz, dx)) - 90.0).toFloat()

    /** Aim geometry between the own eye and the perceived opponent at one tick. */
    class Geometry(timeline: CombatTimeline, perception: CombatPerception, view: CombatView, tick: Int) {
        val aimYaw: Float
        val aimPitch: Float
        val bearingYaw: Float
        val hitboxAngle: Float
        val halfWidthAngle: Float
        val halfHeightAngle: Float
        val distance: Double

        init {
            val self = timeline.self
            val target = timeline.target
            val eyeX = self.positionX(tick)
            val eyeY = self.positionY(tick) + self.eyeHeightBlocks(tick)
            val eyeZ = self.positionZ(tick)
            val halfWidth = target.widthBlocks(tick) / 2.0
            val height = target.heightBlocks(tick).toDouble()
            val minX = perception.x[tick] - halfWidth
            val minY = perception.y[tick]
            val minZ = perception.z[tick] - halfWidth
            val maxX = perception.x[tick] + halfWidth
            val maxY = perception.y[tick] + height
            val maxZ = perception.z[tick] + halfWidth
            val centerX = perception.x[tick] - eyeX
            val centerY = perception.y[tick] + height / 2 - eyeY
            val centerZ = perception.z[tick] - eyeZ
            val horizontal = sqrt(centerX * centerX + centerZ * centerZ).coerceAtLeast(1e-6)
            bearingYaw = bearingYaw(centerX, centerZ)
            val bearingPitch = (-Math.toDegrees(atan2(centerY, horizontal))).toFloat()
            aimYaw = wrap(bearingYaw - view.yaw[tick])
            aimPitch = bearingPitch - view.pitch[tick]
            halfWidthAngle = Math.toDegrees(atan(halfWidth / horizontal)).toFloat()
            val direct = sqrt(horizontal * horizontal + centerY * centerY)
            halfHeightAngle = Math.toDegrees(atan(height / 2 / direct)).toFloat()

            val nearestX = eyeX.coerceIn(minX, maxX) - eyeX
            val nearestY = eyeY.coerceIn(minY, maxY) - eyeY
            val nearestZ = eyeZ.coerceIn(minZ, maxZ) - eyeZ
            distance = sqrt(nearestX * nearestX + nearestY * nearestY + nearestZ * nearestZ)

            val yaw = Math.toRadians(view.yaw[tick].toDouble())
            val pitch = Math.toRadians(view.pitch[tick].toDouble())
            val dirX = -sin(yaw) * cos(pitch)
            val dirY = -sin(pitch)
            val dirZ = cos(yaw) * cos(pitch)
            hitboxAngle = if (intersects(eyeX, eyeY, eyeZ, dirX, dirY, dirZ, minX, minY, minZ, maxX, maxY, maxZ)) {
                0f
            } else {
                var along = (centerX * dirX + centerY * dirY + centerZ * dirZ).coerceIn(0.0, REACH_LIMIT)
                var angle = 180.0
                repeat(3) {
                    val px = (eyeX + dirX * along).coerceIn(minX, maxX) - eyeX
                    val py = (eyeY + dirY * along).coerceIn(minY, maxY) - eyeY
                    val pz = (eyeZ + dirZ * along).coerceIn(minZ, maxZ) - eyeZ
                    val length = sqrt(px * px + py * py + pz * pz).coerceAtLeast(1e-6)
                    val cosine = ((px * dirX + py * dirY + pz * dirZ) / length).coerceIn(-1.0, 1.0)
                    angle = min(angle, Math.toDegrees(acos(cosine)))
                    along = (px * dirX + py * dirY + pz * dirZ).coerceIn(0.0, REACH_LIMIT)
                }
                angle.toFloat()
            }
        }

        @Suppress("LongParameterList")
        private fun intersects(
            ox: Double, oy: Double, oz: Double, dx: Double, dy: Double, dz: Double,
            minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double,
        ): Boolean {
            var near = 0.0
            var far = REACH_LIMIT
            for ((origin, direction, low, high) in listOf(
                arrayOf(ox, dx, minX, maxX), arrayOf(oy, dy, minY, maxY), arrayOf(oz, dz, minZ, maxZ)
            )) {
                if (abs(direction) < 1e-9) {
                    if (origin < low || origin > high) {
                        return false
                    }
                    continue
                }
                val first = (low - origin) / direction
                val second = (high - origin) / direction
                near = max(near, min(first, second))
                far = min(far, max(first, second))
                if (near > far) {
                    return false
                }
            }
            return true
        }
    }
}

/**
 * The opponent as the fighter saw it: the server track delayed by the fighter's latency and
 * smoothed like vanilla's three-step entity interpolation.
 */
@UnstableAddonApi
class CombatPerception(timeline: CombatTimeline, delay: Int) {
    val x = DoubleArray(timeline.ticks)
    val y = DoubleArray(timeline.ticks)
    val z = DoubleArray(timeline.ticks)
    val yaw = FloatArray(timeline.ticks)

    init {
        val target = timeline.target
        var steps = 0
        var targetX = 0.0
        var targetY = 0.0
        var targetZ = 0.0
        var targetYaw = 0f
        var source = -1
        for (tick in 0 until timeline.ticks) {
            val raw = (tick - delay).coerceIn(0, timeline.ticks - 1)
            if (tick == 0) {
                x[0] = target.positionX(raw)
                y[0] = target.positionY(raw)
                z[0] = target.positionZ(raw)
                yaw[0] = target.yawDegrees(raw)
                targetX = x[0]
                targetY = y[0]
                targetZ = z[0]
                targetYaw = yaw[0]
                source = raw
                continue
            }
            if (raw != source) {
                source = raw
                val changed = target.x[raw] != target.x[raw - 1] || target.y[raw] != target.y[raw - 1] ||
                    target.z[raw] != target.z[raw - 1] || target.yaw[raw] != target.yaw[raw - 1]
                if (changed) {
                    targetX = target.positionX(raw)
                    targetY = target.positionY(raw)
                    targetZ = target.positionZ(raw)
                    targetYaw = target.yawDegrees(raw)
                    steps = 3
                }
            }
            x[tick] = x[tick - 1]
            y[tick] = y[tick - 1]
            z[tick] = z[tick - 1]
            yaw[tick] = yaw[tick - 1]
            if (steps > 0) {
                x[tick] += (targetX - x[tick]) / steps
                y[tick] += (targetY - y[tick]) / steps
                z[tick] += (targetZ - z[tick]) / steps
                yaw[tick] += CombatFeatures.wrap(targetYaw - yaw[tick]) / steps
                steps--
            }
        }
    }

    /** Moves the opponent [ticks] ahead along its current motion. */
    fun lead(ticks: Int): CombatPerception {
        for (tick in x.indices.reversed()) {
            if (tick == 0) break
            x[tick] += ticks * (x[tick] - x[tick - 1])
            y[tick] += ticks * (y[tick] - y[tick - 1])
            z[tick] += ticks * (z[tick] - z[tick - 1])
        }
        return this
    }

    companion object {
        /** Latency in ticks between the server and what the fighter saw. */
        fun delay(timeline: CombatTimeline) = when {
            timeline.source == CombatSource.FIRST_PERSON -> 0
            timeline.selfPing < 0 -> 2
            else -> (timeline.selfPing / 50f).roundToInt().coerceIn(0, 10)
        }

        fun of(timeline: CombatTimeline) = CombatPerception(timeline, delay(timeline))
    }
}

/**
 * The fighter's view per tick. Observed players only send rotations now and then, so the
 * recorded view is linearly interpolated between their updates.
 */
@UnstableAddonApi
class CombatView(val yaw: FloatArray, val pitch: FloatArray) {
    fun copy() = CombatView(yaw.copyOf(), pitch.copyOf())

    companion object {
        fun recorded(timeline: CombatTimeline): CombatView {
            val self = timeline.self
            val ticks = timeline.ticks
            val yaw = FloatArray(ticks) { self.yawDegrees(it) }
            val pitch = FloatArray(ticks) { self.pitchDegrees(it) }
            if (timeline.source == CombatSource.FIRST_PERSON) {
                return CombatView(yaw, pitch)
            }
            val updates = updateTicks(timeline)
            for (index in 0 until updates.size - 1) {
                val from = updates[index]
                val to = updates[index + 1]
                for (tick in from + 1 until to) {
                    val progress = (tick - from).toFloat() / (to - from)
                    yaw[tick] = yaw[from] + (yaw[to] - yaw[from]) * progress
                    pitch[tick] = pitch[from] + (pitch[to] - pitch[from]) * progress
                }
            }
            return CombatView(yaw, pitch)
        }

        fun updateTicks(timeline: CombatTimeline): IntArray {
            if (timeline.source == CombatSource.FIRST_PERSON) {
                return IntArray(timeline.ticks) { it }
            }
            val self = timeline.self
            return (0 until timeline.ticks).filter { tick ->
                tick == 0 || self.hasEvent(tick, CombatTrack.ROTATION_UPDATE) ||
                    self.yaw[tick] != self.yaw[tick - 1] || self.pitch[tick] != self.pitch[tick - 1]
            }.toIntArray()
        }
    }
}
