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
import net.minecraft.world.entity.LivingEntity.DEFAULT_BASE_GRAVITY
import net.minecraft.world.entity.LivingEntity.ELYTRA_HORIZONTAL_AIR_DRAG
import net.minecraft.world.entity.LivingEntity.ELYTRA_VERTICAL_AIR_DRAG
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Where to look while gliding, whether to fire a rocket now and whether a rocket pushes this tick. */
class GlideInput(val yaw: Float, val pitch: Float, val fireRocket: Boolean, val boosted: Boolean)

interface GlideSteering {

    /** Where the glide comes down. */
    val target: Vec3

    fun steer(position: Vec3, velocity: Vec3): GlideInput

}

/** The rockets [left] to fire on a glide, each pushing [burnTicks] ticks, the last one fired for [boostTicks] more. */
class GlideRockets(left: Int, private val burnTicks: Int) {

    var left = left
        private set

    var boostTicks = 0
        private set

    val canFire: Boolean
        get() = boostTicks == 0 && left > 0

    fun fire() {
        left--
        boostTicks = burnTicks
    }

    /** Whether a rocket pushes this tick. */
    fun tick(): Boolean {
        if (boostTicks <= 0) {
            return false
        }

        boostTicks--
        return true
    }

}

/** Elytra flight as the game moves it, one tick at a time. */
internal object GlidePhysics {

    private const val LIFT = 0.75
    private const val DIVE = 0.1
    private const val CLIMB = 0.04
    private const val CLIMB_LIFT = 3.2
    private const val TURN = 0.1
    private const val ROCKET_PUSH = 0.1
    private const val ROCKET_SPEED = 1.5
    private const val ROCKET_PULL = 0.5

    fun step(velocity: Vec3, yaw: Float, pitch: Float, boosted: Boolean): Vec3 {
        val look = Vec3.directionFromRotation(pitch, yaw)
        val lean = Math.toRadians(pitch.toDouble())
        val lookHorizontal = look.horizontalDistance()
        val moveHorizontal = velocity.horizontalDistance()
        val lift = cos(lean) * cos(lean)

        var x = velocity.x
        var y = velocity.y + DEFAULT_BASE_GRAVITY * (-1.0 + lift * LIFT)
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

        x *= ELYTRA_HORIZONTAL_AIR_DRAG
        y *= ELYTRA_VERTICAL_AIR_DRAG
        z *= ELYTRA_HORIZONTAL_AIR_DRAG
        if (boosted) {
            x += look.x * ROCKET_PUSH + (look.x * ROCKET_SPEED - x) * ROCKET_PULL
            y += look.y * ROCKET_PUSH + (look.y * ROCKET_SPEED - y) * ROCKET_PULL
            z += look.z * ROCKET_PUSH + (look.z * ROCKET_SPEED - z) * ROCKET_PULL
        }
        return Vec3(x, y, z)
    }

    fun yawTowards(dx: Double, dz: Double) = Mth.wrapDegrees(Math.toDegrees(atan2(-dx, dz))).toFloat()

    fun pitchTowards(up: Double, ahead: Double) = (-Math.toDegrees(atan2(up, ahead))).toFloat()

}

/**
 * Glides to [target]: every tick it takes the pitch that, held until touch-down, lands just short of it unhurt. Too
 * high, it circles down; too low or too far, it fires a rocket and climbs. With an [approach], it keeps as high as
 * it can until that close to the target, over ridges on the way.
 */
class GlideControl(
    override val target: Vec3,
    val rockets: GlideRockets,
    private val approach: Double = 0.0,
) : GlideSteering {

    private val sweep = PitchSweep()

    override fun steer(position: Vec3, velocity: Vec3): GlideInput {
        val dx = target.x - position.x
        val dz = target.z - position.z
        val distance = Mth.length(dx, dz)
        val bearing = GlidePhysics.yawTowards(dx, dz)
        sweepPitches(position, velocity, bearing, distance, dx, dz)

        if (approach > 0.0 && distance > approach && rockets.boostTicks == 0 && sweep.farthestAlong >= 0.0) {
            return GlideInput(bearing, sweep.farthestPitch, fireRocket = false, boosted = false)
        }

        // No pitch comes down within the horizon: far too high, which no rocket helps with.
        val overhead = sweep.bestError == Double.POSITIVE_INFINITY && position.y > target.y
        val missed = sweep.bestError > LANDING_TOLERANCE
        val fire = missed && !overhead && sweep.bestAlong < 0.0 && rockets.canFire
        if (fire) {
            rockets.fire()
        }

        val boosted = rockets.tick()
        val orbit = Mth.wrapDegrees(bearing + ORBIT_TURN)
        return when {
            boosted -> {
                val climb = target.y + CLIMB_ABOVE + distance * CLIMB_SLOPE - position.y
                val pitch = GlidePhysics.pitchTowards(climb, distance).coerceIn(MIN_CLIMB_PITCH, MAX_CLIMB_PITCH)
                GlideInput(bearing, pitch, fire, boosted = true)
            }

            overhead -> GlideInput(orbit, SPIRAL_PITCH, fire, boosted = false)
            missed && sweep.bestAlong > 0.0 -> GlideInput(orbit, PARACHUTE_PITCH, fire, boosted = false)
            else -> GlideInput(bearing, sweep.bestPitch, fire, boosted = false)
        }
    }

    /** Finds the pitch that lands just short of the target unhurt, and the one that lands farthest along the way. */
    private fun sweepPitches(position: Vec3, velocity: Vec3, bearing: Float, distance: Double, dx: Double, dz: Double) {
        val unitX = if (distance > MIN_DISTANCE) dx / distance else 0.0
        val unitZ = if (distance > MIN_DISTANCE) dz / distance else 1.0
        sweep.reset()
        for (pitch in PITCHES) {
            val touchdown = predict(position, velocity, bearing, pitch) ?: continue
            val along = (touchdown.x - target.x) * unitX + (touchdown.z - target.z) * unitZ
            var error = abs(along + SHORT_OF_GOAL)
            if (touchdown.y < SAFE_SINK) {
                error += UNSAFE_PENALTY
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

    /** Where holding [yaw] and [pitch] comes down to the height of the target: x and z, with the sink speed as y. */
    private fun predict(position: Vec3, velocity: Vec3, yaw: Float, pitch: Float): Vec3? {
        var x = position.x
        var y = position.y
        var z = position.z
        var motion = velocity
        for (tick in 0 until HORIZON_TICKS) {
            motion = GlidePhysics.step(motion, yaw, pitch, tick < rockets.boostTicks)
            x += motion.x
            y += motion.y
            z += motion.z
            if (y <= target.y && motion.y < 0.0) {
                return Vec3(x, motion.y, z)
            }
        }
        return null
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

    companion object {
        /** Below this vertical speed a touch-down hurts. */
        const val SAFE_SINK = -0.45

        private val PITCHES = floatArrayOf(-45f, -35f, -25f, -15f, -10f, -5f, 0f, 5f, 10f, 20f)
        private const val HORIZON_TICKS = 400
        private const val MIN_DISTANCE = 1.0E-6
        private const val SHORT_OF_GOAL = 1.0
        private const val UNSAFE_PENALTY = 100.0
        private const val LANDING_TOLERANCE = 3.0
        private const val ORBIT_TURN = 90f
        private const val PARACHUTE_PITCH = -45f
        private const val SPIRAL_PITCH = 30f
        private const val CLIMB_ABOVE = 4.0
        private const val CLIMB_SLOPE = 0.15
        private const val MIN_CLIMB_PITCH = -40f
        private const val MAX_CLIMB_PITCH = 10f
    }

}
