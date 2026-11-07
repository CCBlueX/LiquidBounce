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
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Height of the highest block that stops a glide in column [x] [z], or null while the column is not loaded.
 */
fun interface ColumnHeights {
    fun top(x: Int, z: Int): Int?
}

/**
 * Flies an elytra to a far [goal] with rockets: climbs to a height clear of everything on the line, holds it with
 * rockets while heading for the goal, and hands over to [GlideControl] for the touch-down once the goal is close.
 * Out of rockets on the way, it glides down to [fallback], else to the goal.
 *
 * The cruise height follows the columns of [heights] between the player and the goal, with more height while some
 * are not loaded yet.
 */
class ElytraCruise(
    private val goal: Vec3,
    private val heights: ColumnHeights,
    rockets: Int,
    private val rocketTicks: Int,
    private val ceiling: Int,
    private val fallback: (Vec3) -> Vec3? = { null },
) : GlideSteering {

    var rockets = rockets
        private set

    private var boostTicks = 0
    private var altitude = Double.NaN
    private var ticks = 0
    private var landing: GlideControl? = null

    /**
     * Where the cruise heads now: the goal, or the ground it falls back to.
     */
    var target: Vec3 = goal
        private set

    val isLanding: Boolean
        get() = landing != null

    override fun steer(position: Vec3, velocity: Vec3): GlideInput {
        landing?.let { return it.steer(position, velocity) }

        val dx = goal.x - position.x
        val dz = goal.z - position.z
        val distance = sqrt(dx * dx + dz * dz)
        if (distance <= HANDOVER || rockets == 0 && boostTicks == 0) {
            return land(position, velocity, distance)
        }

        if (ticks++ % REPLAN_TICKS == 0 || altitude.isNaN()) {
            altitude = cruiseAltitude(position, goal, heights).coerceAtMost(ceiling - CEILING_MARGIN.toDouble())
        }

        val yaw = Mth.wrapDegrees(Math.toDegrees(atan2(-dx, dz))).toFloat()
        val below = altitude - position.y
        val speed = velocity.horizontalDistance()
        val fire = boostTicks == 0 && rockets > 0 && (speed < MIN_SPEED || below > LOW)
        if (fire) {
            rockets--
            boostTicks = rocketTicks
        }

        val boosted = boostTicks > 0
        if (boosted) {
            boostTicks--
        }

        val pitch = (-Math.toDegrees(atan2(below, AIM_AHEAD))).toFloat().coerceIn(MIN_PITCH, MAX_PITCH)
        return GlideInput(yaw, pitch, fire, boosted)
    }

    private fun land(position: Vec3, velocity: Vec3, distance: Double): GlideInput {
        val to = if (distance <= HANDOVER) goal else fallback(position) ?: goal
        target = to
        val control = GlideControl(to, rockets, rocketTicks, boostTicks)
        landing = control
        return control.steer(position, velocity)
    }

    companion object {

        /**
         * Below this the elytra is too slow and a rocket is fired.
         */
        private const val MIN_SPEED = 1.2
        private const val LOW = 3.0
        private const val AIM_AHEAD = 24.0
        private const val MIN_PITCH = -40f
        private const val MAX_PITCH = 15f
        private const val HANDOVER = 64.0
        private const val REPLAN_TICKS = 20
        private const val CEILING_MARGIN = 4

        /**
         * Height above every block on the line the cruise keeps.
         */
        const val CLEARANCE = 12

        /**
         * More height kept while part of the line is not loaded yet.
         */
        const val UNKNOWN_CLEARANCE = 24

        private const val SAMPLE = 4.0
        private const val CRUISE_SPEED = 1.5
        private const val EFFICIENCY = 0.7
        private const val CLIMB_RATE = 0.6
        private const val SPARE_ROCKETS = 3

        /**
         * The height to cruise at from [from] to [goal]: [CLEARANCE] above the start, the goal and every loaded
         * column on the line between, and [UNKNOWN_CLEARANCE] more while some of them are not loaded.
         */
        fun cruiseAltitude(from: Vec3, goal: Vec3, heights: ColumnHeights): Double {
            var top = max(from.y, goal.y)
            var unknown = false
            val dx = goal.x - from.x
            val dz = goal.z - from.z
            val length = sqrt(dx * dx + dz * dz)
            val samples = ceil(length / SAMPLE).toInt()
            for (i in 0..samples) {
                val t = if (samples == 0) 0.0 else i.toDouble() / samples
                val column = heights.top(Mth.floor(from.x + dx * t), Mth.floor(from.z + dz * t))
                if (column == null) {
                    unknown = true
                } else {
                    top = max(top, column.toDouble())
                }
            }
            return top + CLEARANCE + if (unknown) UNKNOWN_CLEARANCE else 0
        }

        /**
         * Rockets pushing [rocketTicks] ticks each that a cruise over [distance] blocks needs after climbing
         * [climb] blocks, with some to spare for the landing.
         */
        fun rocketsNeeded(distance: Double, climb: Double, rocketTicks: Int): Int {
            if (rocketTicks <= 0) {
                return Int.MAX_VALUE
            }

            val perRocket = rocketTicks * CRUISE_SPEED * EFFICIENCY
            val climbPerRocket = rocketTicks * CRUISE_SPEED * CLIMB_RATE
            return ceil(distance / perRocket).toInt() + ceil(max(climb, 0.0) / climbPerRocket).toInt() + SPARE_ROCKETS
        }
    }

}
