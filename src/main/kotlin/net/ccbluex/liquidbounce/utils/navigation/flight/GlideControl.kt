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

package net.ccbluex.liquidbounce.utils.navigation.flight

import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Where to look while gliding, whether to fire a rocket now and whether a rocket pushes this tick.
 */
class GlideInput(val yaw: Float, val pitch: Float, val fireRocket: Boolean, val boosted: Boolean)

/**
 * Steers a glide one tick at a time.
 */
fun interface GlideSteering {
    fun steer(position: Vec3, velocity: Vec3): GlideInput
}

/**
 * Elytra flight as the game moves it, one tick at a time.
 */
internal object GlidePhysics {

    private const val GRAVITY = 0.08
    private const val LIFT = 0.75
    private const val DIVE = 0.1
    private const val CLIMB = 0.04
    private const val CLIMB_LIFT = 3.2
    private const val TURN = 0.1
    private const val HORIZONTAL_DRAG = 0.99
    private const val VERTICAL_DRAG = 0.98
    private const val ROCKET_PUSH = 0.1
    private const val ROCKET_SPEED = 1.5
    private const val ROCKET_PULL = 0.5

    /**
     * The movement of the tick after one with [velocity], looking at [yaw] and [pitch], pushed by a rocket when
     * [boosted].
     */
    fun step(velocity: Vec3, yaw: Float, pitch: Float, boosted: Boolean): Vec3 {
        val look = Vec3.directionFromRotation(pitch, yaw)
        val lean = Math.toRadians(pitch.toDouble())
        val lookHorizontal = sqrt(look.x * look.x + look.z * look.z)
        val moveHorizontal = velocity.horizontalDistance()
        val lift = cos(lean) * cos(lean)

        var x = velocity.x
        var y = velocity.y + GRAVITY * (-1.0 + lift * LIFT)
        var z = velocity.z
        if (lookHorizontal > 0.0) {
            if (y < 0.0) {
                val convert = y * -DIVE * lift
                x += look.x * convert / lookHorizontal
                y += convert
                z += look.z * convert / lookHorizontal
            }
            if (lean < 0.0) {
                val convert = moveHorizontal * -sin(lean) * CLIMB
                x -= look.x * convert / lookHorizontal
                y += convert * CLIMB_LIFT
                z -= look.z * convert / lookHorizontal
            }
            x += (look.x / lookHorizontal * moveHorizontal - x) * TURN
            z += (look.z / lookHorizontal * moveHorizontal - z) * TURN
        }

        x *= HORIZONTAL_DRAG
        y *= VERTICAL_DRAG
        z *= HORIZONTAL_DRAG
        if (boosted) {
            x += look.x * ROCKET_PUSH + (look.x * ROCKET_SPEED - x) * ROCKET_PULL
            y += look.y * ROCKET_PUSH + (look.y * ROCKET_SPEED - y) * ROCKET_PULL
            z += look.z * ROCKET_PUSH + (look.z * ROCKET_SPEED - z) * ROCKET_PULL
        }
        return Vec3(x, y, z)
    }

}

/**
 * Glides to [goal], feet on the ground there, with [rockets] rockets that each push for [rocketTicks] ticks, the last
 * one fired still pushing for [boostTicks].
 *
 * Every tick it tries a range of pitches held until touch-down and takes the one that lands just short of the goal
 * with a vertical speed that does no harm. Too high, it circles the goal while sinking slowly, diving while far
 * above it; too low or too far, it fires a rocket and climbs towards a point above the goal while it pushes. With an
 * [approach], it keeps as high as it can until that close to the goal, over ridges the direct glide would clip.
 */
class GlideControl(
    private val goal: Vec3,
    rockets: Int,
    private val rocketTicks: Int,
    boostTicks: Int = 0,
    private val approach: Double = 0.0,
) : GlideSteering {

    /**
     * Rockets left to fire.
     */
    var rockets = rockets
        private set

    /**
     * Ticks the last rocket still pushes.
     */
    var boostTicks = boostTicks
        private set

    /**
     * Decides the next tick at [position] moving with [velocity].
     */
    override fun steer(position: Vec3, velocity: Vec3): GlideInput {
        val dx = goal.x - position.x
        val dz = goal.z - position.z
        val distance = sqrt(dx * dx + dz * dz)
        val bearing = Mth.wrapDegrees(Math.toDegrees(atan2(-dx, dz))).toFloat()
        sweepPitches(position, velocity, bearing, distance, dx, dz)

        if (approach > 0.0 && distance > approach && boostTicks == 0 && sweep.farthestAlong >= 0.0) {
            return GlideInput(bearing, sweep.farthestPitch, fireRocket = false, boosted = false)
        }

        var yaw = bearing
        var pitch = sweep.bestPitch
        // No pitch comes down within the horizon: far too high, which no rocket helps with.
        val overhead = sweep.bestError == Double.POSITIVE_INFINITY && position.y > goal.y
        val missed = sweep.bestError > TOLERANCE
        val fire = missed && !overhead && sweep.bestAlong < 0.0 && boostTicks == 0 && rockets > 0
        if (fire) {
            rockets--
            boostTicks = rocketTicks
        }

        val boosted = boostTicks > 0
        if (boosted) {
            boostTicks--
            val aim = goal.y + CLIMB_ABOVE + distance * CLIMB_SLOPE - position.y
            pitch = (-Math.toDegrees(atan2(aim, distance))).toFloat().coerceIn(MIN_CLIMB_PITCH, MAX_CLIMB_PITCH)
        } else if (overhead) {
            yaw = Mth.wrapDegrees(bearing + ORBIT_TURN)
            pitch = SPIRAL
        } else if (missed && sweep.bestAlong > 0.0) {
            yaw = Mth.wrapDegrees(bearing + ORBIT_TURN)
            pitch = PARACHUTE
        }
        return GlideInput(yaw, pitch, fire, boosted)
    }

    private val sweep = PitchSweep()

    /**
     * Finds the pitch, held towards [bearing], that touches down just short of the goal unhurt, and the one that
     * touches down farthest along the way to it.
     */
    private fun sweepPitches(position: Vec3, velocity: Vec3, bearing: Float, distance: Double, dx: Double, dz: Double) {
        val unitX = if (distance > MIN_DISTANCE) dx / distance else 0.0
        val unitZ = if (distance > MIN_DISTANCE) dz / distance else 1.0
        sweep.reset()
        for (pitch in PITCHES) {
            val touchdown = predict(position, velocity, bearing, pitch) ?: continue
            val along = (touchdown.x - goal.x) * unitX + (touchdown.z - goal.z) * unitZ
            var error = abs(along + SHORT_OF_GOAL)
            if (touchdown.y < SAFE_SINK) {
                error += UNSAFE
            }
            if (error < sweep.bestError) {
                sweep.bestError = error
                sweep.bestPitch = pitch
                sweep.bestAlong = along
            }
            if (along > sweep.farthestAlong) {
                sweep.farthestAlong = along
                sweep.farthestPitch = pitch
            }
        }
    }

    private class PitchSweep {
        var bestError = Double.POSITIVE_INFINITY
        var bestPitch = 0f
        var bestAlong = Double.NEGATIVE_INFINITY
        var farthestPitch = 0f
        var farthestAlong = Double.NEGATIVE_INFINITY

        fun reset() {
            bestError = Double.POSITIVE_INFINITY
            bestPitch = 0f
            bestAlong = Double.NEGATIVE_INFINITY
            farthestPitch = 0f
            farthestAlong = Double.NEGATIVE_INFINITY
        }
    }

    /**
     * Where the feet come down to the height of the goal, holding [yaw] and [pitch]: x and z of the spot and, as y,
     * the vertical speed then; null when they do not within [HORIZON] ticks.
     */
    private fun predict(position: Vec3, velocity: Vec3, yaw: Float, pitch: Float): Vec3? {
        var x = position.x
        var y = position.y
        var z = position.z
        var motion = velocity
        for (tick in 0 until HORIZON) {
            motion = GlidePhysics.step(motion, yaw, pitch, tick < boostTicks)
            x += motion.x
            y += motion.y
            z += motion.z
            if (y <= goal.y && motion.y < 0.0) {
                return Vec3(x, motion.y, z)
            }
        }
        return null
    }

    companion object {
        /**
         * Below this vertical speed a touch-down hurts.
         */
        const val SAFE_SINK = -0.45

        private val PITCHES = floatArrayOf(-45f, -35f, -25f, -15f, -10f, -5f, 0f, 5f, 10f, 20f)
        private const val HORIZON = 400
        private const val MIN_DISTANCE = 1.0E-6
        private const val SHORT_OF_GOAL = 1.0
        private const val UNSAFE = 100.0
        private const val TOLERANCE = 3.0
        private const val ORBIT_TURN = 90f
        private const val PARACHUTE = -45f

        /**
         * Looking down while circling far above the goal, to come down fast.
         */
        private const val SPIRAL = 30f
        private const val CLIMB_ABOVE = 4.0
        private const val CLIMB_SLOPE = 0.15
        private const val MIN_CLIMB_PITCH = -40f
        private const val MAX_CLIMB_PITCH = 10f
    }

}
