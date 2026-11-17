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
package net.ccbluex.liquidbounce.utils.aiming.aim

import net.ccbluex.liquidbounce.utils.aiming.data.RotationDelta
import java.util.random.RandomGenerator
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * A hand on a mouse, as an intermittent controller. Every few ticks it looks at where the goal was a moment ago. When
 * the movement under way will not end on the goal, it adds a corrective submovement towards where the goal will be:
 * bell-shaped speed that peaks early, timed by Fitts' law, landing scattered in proportion to its distance. After a
 * correction of a moving goal the hand pauses briefly while the arm coasts along with it, so a moving goal is followed
 * in bursts. Noise grows with speed, along and across the movement, and inside the tolerance nothing is corrected,
 * so the hand rests exactly on a goal that holds still.
 *
 * Degrees in, degrees out: nothing here depends on the game, so a stream can be produced and judged offline.
 */
class Hand(private val random: RandomGenerator) {

    /** Divides every movement time. */
    var speed = 1f

    /** Ticks before a new or switched goal is acted on. */
    var reaction = 2..4

    // A hand is not equally quick in every direction; which one is quicker differs between people
    private val slowYaw = anisotropy()
    private val slowPitch = anisotropy()

    private val yaw = Axis()
    private val pitch = Axis()
    private val seenYaw = FloatArray(HISTORY)
    private val seenPitch = FloatArray(HISTORY)
    private var seen = 0
    private var reactIn = 0
    private var lookIn = 0
    private var pauseIn = 0
    private var following = false
    private var tilt = 0f

    /** Lets go of the mouse: the next goal is new and has to be reacted to. */
    fun reset() {
        yaw.reset()
        pitch.reset()
        seen = 0
        reactIn = nextReaction()
        lookIn = 0
        pauseIn = 0
        following = false
        tilt = random.nextGaussian().toFloat() * TILT
    }

    /**
     * Degrees to turn this tick, a whole number of [step]s on each axis. [width] and [height] are the angular size of
     * what has to be hit, 0 for a bare rotation, and [reached] whether the view hits it already.
     */
    @Suppress("LongParameterList")
    fun step(
        viewYaw: Float,
        viewPitch: Float,
        goalYaw: Float,
        goalPitch: Float,
        width: Float,
        height: Float,
        reached: Boolean,
        step: Float,
    ): RotationDelta {
        val nearYaw = viewYaw + wrap(goalYaw - viewYaw)
        // Near straight up or down a small move of the goal swings its yaw a long way, so a jump is judged by how
        // far the goal moves in sight, not in yaw
        val moved = hypot((nearYaw - seenYaw[0]) * cos(goalPitch * DEG_TO_RAD), goalPitch - seenPitch[0])
        if (seen > 0 && moved > max(JUMP, max(width, height) * 2f)) {
            // A new goal: what was seen of the old one says nothing about how this one moves
            seen = 0
            reactIn = max(reactIn, nextReaction())
        }
        remember(nearYaw, goalPitch)

        if (pauseIn > 0) pauseIn--
        when {
            reactIn > 0 -> reactIn--
            lookIn > 0 -> lookIn--
            else -> look(viewYaw, viewPitch, width, height, reached, step)
        }

        var moveYaw = yaw.next()
        var movePitch = pitch.next()
        if (moveYaw != 0f || movePitch != 0f) {
            // Motor noise grows with speed, and the hand moves along a line turned a little off the planned one
            val along = random.nextGaussian().toFloat() * MOTOR_NOISE
            val across = tilt + random.nextGaussian().toFloat() * CROSS_NOISE
            val noiseYaw = moveYaw * along - movePitch * across
            val noisePitch = movePitch * along + moveYaw * across
            moveYaw += noiseYaw
            movePitch += noisePitch
            if (following) {
                // An arm kept in motion behind a moving goal adds noise that does not scale with speed
                moveYaw += random.nextGaussian().toFloat() * BASE_NOISE
                movePitch += random.nextGaussian().toFloat() * BASE_NOISE
            }
        }
        return RotationDelta(yaw.counts(moveYaw, step), pitch.counts(movePitch, step))
    }

    /** Ticks a movement over [distance] degrees onto something [width] degrees wide takes, without the reaction. */
    fun ticksFor(distance: Float, width: Float): Int {
        val seconds = (FITTS_A + FITTS_B * log2(distance / max(width, MIN_WIDTH) + 1f)) / speed
        return max(1, (seconds / TICK).roundToInt())
    }

    @Suppress("LongParameterList", "CognitiveComplexMethod")
    private fun look(viewYaw: Float, viewPitch: Float, width: Float, height: Float, reached: Boolean, step: Float) {
        lookIn = random.nextInt(LOOK.first, LOOK.last + 1)

        val delay = min(PERCEPTION_DELAY, seen - 1)
        val span = min(VELOCITY_SPAN, seen - 1 - delay)
        var velocityYaw = goalSpeed(seenYaw, delay, span)
        // People follow vertical movement, such as a jump, far less than sideways movement
        var velocityPitch = goalSpeed(seenPitch, delay, span) * PITCH_GAIN
        following = hypot(velocityYaw, velocityPitch) >= MOVING
        if (!following) {
            velocityYaw = 0f
            velocityPitch = 0f
            yaw.pursue(0f)
            pitch.pursue(0f)
        }

        // A bare rotation is only reached within half a step, a box anywhere well inside it
        val toleranceYaw = max(width * HOLD_SHARE, step / 2)
        val tolerancePitch = max(height * HOLD_SHARE, step / 2)

        // Until the movement under way and the pause after it are over, only a clear miss is corrected
        val busy = max(yaw.busy(), pitch.busy())
        val wait = max(busy, pauseIn)
        if (wait > 0) {
            val missYaw = seenYaw[delay] + velocityYaw * (delay + wait - 1) - viewYaw - yaw.ahead(wait)
            val missPitch = seenPitch[delay] + velocityPitch * (delay + wait - 1) - viewPitch - pitch.ahead(wait)
            if (abs(missYaw) <= toleranceYaw * URGENT && abs(missPitch) <= tolerancePitch * URGENT) {
                return
            }
        }

        val nowYaw = seenYaw[delay] + velocityYaw * delay - viewYaw - yaw.ahead(busy)
        val nowPitch = seenPitch[delay] + velocityPitch * delay - viewPitch - pitch.ahead(busy)
        val distance = hypot(nowYaw, nowPitch)
        val slow = if (distance > 0f) 1f + (slowYaw * nowYaw + slowPitch * nowPitch) / distance else 1f
        // Fitts' time covers the whole movement, so the main submovement leaves room for a correction
        val fitts = ticksFor(distance, if (abs(nowYaw) >= abs(nowPitch)) width else height) * PRIMARY_SHARE
        val ticks = max(MIN_SUBMOVEMENT.toFloat(), fitts * slow) *
            exp(random.nextGaussian() * DURATION_SPREAD).toFloat()
        val whole = ceil(ticks).toInt()
        // A turn this tick is judged against the goal as it is now, so the hand's n-th tick meets the goal n - 1 ticks
        // ahead; a moving goal is aimed at where it will be while the hand rests after the movement
        val lead = if (following) whole + LEAD else whole
        // A correction also sets how fast the arm drifts along afterwards
        val gain = PURSUIT_GAIN * (1f + random.nextGaussian().toFloat() * PURSUIT_GAIN_SPREAD).coerceIn(0.5f, 1.5f)
        val driftYaw = velocityYaw * gain
        val driftPitch = velocityPitch * gain
        val errorYaw = seenYaw[delay] + velocityYaw * (delay + lead - 1) - viewYaw - yaw.ahead(lead, driftYaw)
        val errorPitch =
            seenPitch[delay] + velocityPitch * (delay + lead - 1) - viewPitch - pitch.ahead(lead, driftPitch)
        if (abs(errorYaw) <= toleranceYaw && abs(errorPitch) <= tolerancePitch && (reached || width == 0f)) {
            return
        }

        val along = 1f - (UNDERSHOOT + random.nextGaussian().toFloat() * UNDERSHOOT_SPREAD).coerceIn(-0.03f, 0.15f) +
            random.nextGaussian().toFloat() * SCATTER_ALONG
        // Even the shortest aimed movement at a box lands a little off, so it does not end on the goal to the count
        val needYaw = abs(errorYaw) > toleranceYaw
        val needPitch = abs(errorPitch) > tolerancePitch
        val floorYaw = if (needYaw && width > 0f) LANDING_FLOOR else 0f
        val floorPitch = if (needPitch && height > 0f) LANDING_FLOOR else 0f
        val landYaw = errorYaw * along + random.nextGaussian().toFloat() * floorYaw
        val landPitch = errorPitch * along + random.nextGaussian().toFloat() * floorPitch
        yaw.submove(landYaw, needYaw, ticks, step)
        pitch.submove(landPitch, needPitch, ticks, step)
        yaw.pursue(driftYaw)
        pitch.pursue(driftPitch)
        pauseIn = whole + if (following) random.nextInt(PAUSE.first, PAUSE.last + 1) else 0
        // Each movement is a little turned, more or less like the one before
        tilt = tilt * TILT_KEEP + random.nextGaussian().toFloat() * TILT * sqrt(1f - TILT_KEEP * TILT_KEEP)
    }

    /**
     * Degrees per tick the goal moved over [span] ticks, [delay] ticks ago. A goal swinging faster than a hand can
     * follow, as one near straight up or down does, is not pursued at its speed but caught up with.
     */
    private fun goalSpeed(seen: FloatArray, delay: Int, span: Int) = if (span > 0) {
        ((seen[delay] - seen[delay + span]) / span).coerceIn(-MAX_PURSUIT, MAX_PURSUIT)
    } else {
        0f
    }

    private fun remember(yaw: Float, pitch: Float) {
        seenYaw.copyInto(seenYaw, 1, 0, HISTORY - 1)
        seenPitch.copyInto(seenPitch, 1, 0, HISTORY - 1)
        seenYaw[0] = yaw
        seenPitch[0] = pitch
        seen = min(seen + 1, HISTORY)
    }

    private fun anisotropy() =
        (ANISOTROPY.start + random.nextFloat() * (ANISOTROPY.endInclusive - ANISOTROPY.start)) *
            if (random.nextBoolean()) 1f else -1f

    private fun nextReaction() = random.nextInt(reaction.first, reaction.last + 1)

    /**
     * One axis of the hand: a drift velocity that changes smoothly and then fades, plus overlapping submovements.
     */
    private class Axis {
        private var driftFrom = 0f
        private var driftTo = 0f
        private var driftTick = RAMP
        private val amplitude = FloatArray(SUBMOVEMENTS)
        private val duration = FloatArray(SUBMOVEMENTS)
        private val elapsed = IntArray(SUBMOVEMENTS)
        private var carry = 0f

        fun reset() {
            driftFrom = 0f
            driftTo = 0f
            driftTick = RAMP
            duration.fill(0f)
            elapsed.fill(0)
            carry = 0f
        }

        private fun drift(tick: Int) = drift(driftFrom, driftTo, tick)

        private fun drift(from: Float, to: Float, tick: Int) = if (tick >= RAMP) {
            to * COAST.pow(tick - RAMP)
        } else {
            from + (to - from) * minimumJerk(tick / RAMP.toFloat())
        }

        fun pursue(velocity: Float) {
            driftFrom = drift(driftTick)
            driftTo = velocity
            driftTick = 0
        }

        /** Ticks until every submovement under way has ended. */
        fun busy(): Int {
            var ticks = 0
            for (i in 0 until SUBMOVEMENTS) {
                ticks = max(ticks, ceil(duration[i] - elapsed[i]).toInt())
            }
            return ticks
        }

        /** Degrees the hand turns in the next [ticks] ticks as planned, or with its drift set to [drift] now. */
        fun ahead(ticks: Int, drift: Float = Float.NaN): Float {
            var degrees = 0f
            val from = drift(driftTick)
            for (i in 1..ticks) {
                degrees += if (drift.isNaN()) drift(driftTick + i) else drift(from, drift, i)
            }
            for (i in 0 until SUBMOVEMENTS) {
                if (elapsed[i] < duration[i]) {
                    val end = min((elapsed[i] + ticks) / duration[i], 1f)
                    degrees += amplitude[i] * (progress(end) - progress(elapsed[i] / duration[i]))
                }
            }
            return degrees
        }

        fun submove(degrees: Float, needed: Boolean, ticks: Float, step: Float) {
            if (!needed && abs(degrees) < step) {
                return
            }
            // The smallest movement a hand makes on purpose is one count
            val distance = if (abs(degrees) < step) step * sign(degrees) else degrees
            var slot = 0
            for (i in 1 until SUBMOVEMENTS) {
                if (duration[i] - elapsed[i] < duration[slot] - elapsed[slot]) slot = i
            }
            if (elapsed[slot] < duration[slot]) {
                // Every slot is busy: the rest of the one closest to its end goes into the new one
                amplitude[slot] *= 1f - progress(elapsed[slot] / duration[slot])
                amplitude[slot] += distance
            } else {
                amplitude[slot] = distance
            }
            duration[slot] = ticks
            elapsed[slot] = 0
        }

        /** Degrees planned for this tick, exactly 0 when the hand rests. */
        fun next(): Float {
            if (driftTick < MAX_DRIFT_TICKS) driftTick++
            var degrees = drift(driftTick)
            for (i in 0 until SUBMOVEMENTS) {
                if (elapsed[i] < duration[i]) {
                    val t = duration[i]
                    degrees += amplitude[i] * (progress(min((elapsed[i] + 1) / t, 1f)) - progress(elapsed[i] / t))
                    elapsed[i]++
                }
            }
            return degrees
        }

        fun counts(degrees: Float, step: Float): Float {
            if (degrees == 0f) {
                // The hand is at rest, and what it did not turn of the last movement is lost
                carry = 0f
                return 0f
            }
            val wanted = degrees + carry
            val counts = (wanted / step).roundToInt()
            carry = wanted - counts * step
            return counts * step
        }
    }

    private companion object {
        const val TICK = 0.05f
        const val DEG_TO_RAD = (PI / 180).toFloat()

        /** Fitts' law for a mouse: seconds, and seconds per bit. */
        const val FITTS_A = 0.06f
        const val FITTS_B = 0.09f
        const val MIN_WIDTH = 1f

        const val HISTORY = 8
        const val PERCEPTION_DELAY = 1
        const val VELOCITY_SPAN = 2

        /** Ticks between two looks at the goal, the visual feedback loop. */
        val LOOK = 2..3

        /** Ticks the hand rests between two corrections of a moving goal, the refractory period. */
        val PAUSE = 0..2

        /** Ticks past the end of a movement its aim is taken for a moving goal. */
        const val LEAD = 1

        /** How far past the tolerance a movement under way has to miss to be corrected before it ends. */
        const val URGENT = 1.5f

        /** The arm drifts along with a moving goal at this share of its speed; submovements make up the rest. */
        const val PURSUIT_GAIN = 0.5f
        const val PURSUIT_GAIN_SPREAD = 0.3f
        const val PITCH_GAIN = 0.5f

        /** Ticks the drift takes to change. */
        const val RAMP = 3

        /** Share of its drift the arm keeps per tick until the next correction sets it again. */
        const val COAST = 0.8f
        const val MAX_DRIFT_TICKS = 100

        /** A submovement takes at least this many ticks, however short. */
        const val MIN_SUBMOVEMENT = 3

        /** Share of Fitts' time the main submovement takes; the rest is left for a correction. */
        const val PRIMARY_SHARE = 0.8f

        /** Time warp of the speed profile; below 1 the peak comes before the middle. */
        const val SKEW = 0.7f
        const val SUBMOVEMENTS = 3

        /** Aimed movements fall a little short on average, and land scattered along their direction. */
        const val UNDERSHOOT = 0.04f
        const val UNDERSHOOT_SPREAD = 0.03f
        const val SCATTER_ALONG = 0.05f

        /** Degrees a movement onto a box lands off at least, however short it is. */
        const val LANDING_FLOOR = 0.25f

        /** Radians a movement's direction is off, and how much of that the next movement keeps. */
        const val TILT = 0.06f
        const val TILT_KEEP = 0.6f
        const val DURATION_SPREAD = 0.12

        /** Share by which movements in one direction take longer than in the opposite one. */
        val ANISOTROPY = 0.1f..0.25f

        /** Every tick of a movement is off by this share of its speed, along and across its direction. */
        const val MOTOR_NOISE = 0.2f
        const val CROSS_NOISE = 0.05f

        /** Degrees of noise per tick that do not scale with speed, while following a moving goal. */
        const val BASE_NOISE = 0.12f

        /** Share of the target's width the landing may be off before a correction is planned. */
        const val HOLD_SHARE = 0.3f

        /** A goal that moves this far in one tick is a new one. */
        const val JUMP = 8f

        /** Degrees per tick from which a goal counts as moving. */
        const val MOVING = 0.3f

        /** Degrees per tick, the fastest goal movement a hand keeps pace with. */
        const val MAX_PURSUIT = 6f

        /** Share of a minimum-jerk movement done at [t] of its time. */
        fun minimumJerk(t: Float) = t * t * t * (10f + t * (6f * t - 15f))

        /** Share of a submovement done at [t] of its time: aimed movements peak early and slow down to land. */
        fun progress(t: Float) = minimumJerk(t.pow(SKEW))

        fun wrap(degrees: Float): Float {
            val wrapped = degrees % 360f
            return when {
                wrapped >= 180f -> wrapped - 360f
                wrapped < -180f -> wrapped + 360f
                else -> wrapped
            }
        }
    }

}
