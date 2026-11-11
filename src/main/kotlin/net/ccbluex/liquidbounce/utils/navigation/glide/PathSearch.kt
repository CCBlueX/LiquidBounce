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

package net.ccbluex.liquidbounce.utils.navigation.glide

import net.ccbluex.liquidbounce.utils.entity.ElytraMotion
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceTo
import net.ccbluex.liquidbounce.utils.math.withLength
import net.ccbluex.liquidbounce.utils.navigation.route.RouteFailure
import net.ccbluex.liquidbounce.utils.navigation.route.RouteResult
import net.ccbluex.liquidbounce.utils.navigation.route.RouteSearch
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationRegion
import net.ccbluex.liquidbounce.utils.navigation.terrain.Terrain
import net.minecraft.util.Mth
import net.minecraft.world.entity.LivingEntity.BASE_HORIZONTAL_AIR_DRAG
import net.minecraft.world.entity.LivingEntity.BASE_JUMP_POWER
import net.minecraft.world.entity.LivingEntity.BASE_VERTICAL_AIR_DRAG
import net.minecraft.world.entity.LivingEntity.DEFAULT_BASE_GRAVITY
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** A glide from [start] with its take-off: where the feet pass and land, flown by a [Landing] with [approach]. */
class Path(val start: Vec3, val points: List<Vec3>, val landing: Vec3, val rockets: Int, val approach: Double)

/**
 * Plans a glide by flying it tick by tick with a [Landing]: a jump towards [goal], the elytra opened in the air
 * and the glide until the feet touch down unhurt next to the goal; first directly, then keeping high until close.
 */
class PathSearch(
    private val terrain: Terrain,
    private val start: Vec3,
    private val goal: Vec3,
    private val radius: Double,
    private val abilities: Abilities,
) : RouteSearch<Path> {

    private var attempt = Attempt(approach = 0.0)
    private var result: RouteResult<Path> = RouteResult.Searching

    override fun advance(): RouteResult<Path> {
        repeat(TICKS_PER_SLICE) {
            if (result != RouteResult.Searching) {
                return result
            }

            result = attempt.tick()
            if (result is RouteResult.Failed && attempt.approach == 0.0) {
                attempt = Attempt(HIGH_APPROACH)
                result = RouteResult.Searching
            }
        }
        return result
    }

    override fun preview(): List<Vec3> = ArrayList(attempt.points)

    /** One flight of the glide, keeping high until [approach] blocks from the goal. */
    private inner class Attempt(val approach: Double) {

        val points = arrayListOf(start)
        private val rockets = Rockets(abilities.rockets, abilities.rocketTicks)
        private val control = Landing(goal, rockets, approach)
        private var position = start
        private var motion = Vec3.ZERO
        private var tick = 0

        fun tick(): RouteResult<Path> {
            if (tick >= MAX_TICKS || tick >= abilities.glideTicks) {
                return RouteResult.Failed(RouteFailure.TOO_COMPLEX)
            }

            val takingOff = tick < TAKE_OFF_TICKS
            val next = if (takingOff) {
                jump()
            } else {
                val input = control.steer(position, motion)
                motion = ElytraMotion.step(motion, input.yaw, input.pitch, input.boosted)
                position.add(motion)
            }

            val height = if (takingOff) Terrain.PLAYER_HEIGHT else GLIDE_HEIGHT
            if (!terrain.isClear(terrain.box(next.x, next.y, next.z, height))) {
                return land(next)
            }
            if (next.y < terrain.minY) {
                return RouteResult.Failed(RouteFailure.NO_PATH)
            }

            position = next
            tick++
            if (tick % POINT_TICKS == 0) {
                points += position
            }
            return RouteResult.Searching
        }

        /** A sprint jump towards the goal and the ticks in the air before the elytra opens. */
        private fun jump(): Vec3 {
            if (tick == 0) {
                val toGoal = Vec3(goal.x - start.x, 0.0, goal.z - start.z).withLength(TAKE_OFF_SPEED)
                motion = Vec3(toGoal.x, BASE_JUMP_POWER.toDouble(), toGoal.z)
            }
            val next = position.add(motion)
            motion = Vec3(
                motion.x * BASE_HORIZONTAL_AIR_DRAG,
                (motion.y - DEFAULT_BASE_GRAVITY) * BASE_VERTICAL_AIR_DRAG,
                motion.z * BASE_HORIZONTAL_AIR_DRAG,
            )
            return next
        }

        /** Lands where the glide meets the ground; a step brushed too slowly to get hurt is slid down along instead. */
        private fun land(next: Vec3): RouteResult<Path> {
            val bumped = !terrain.isClear(terrain.box(next.x, position.y, next.z, GLIDE_HEIGHT))
            if (tick < TAKE_OFF_TICKS || motion.y < Landing.SAFE_SINK ||
                bumped && motion.horizontalDistance() > SAFE_BUMP) {
                return RouteResult.Failed(RouteFailure.NO_PATH)
            }

            val spot = if (bumped) position else next
            val ground = groundUnder(spot, if (bumped) position.y - BUMP_DROP else next.y)
            if (ground.isNaN()) {
                return RouteResult.Failed(RouteFailure.NO_PATH)
            }

            val landing = Vec3(spot.x, ground, spot.z)
            val near = landing.horizontalDistanceTo(goal) <= max(radius, LANDING_REACH) &&
                abs(landing.y - goal.y) <= LANDING_HEIGHT
            if (!near) {
                return RouteResult.Failed(RouteFailure.NO_PATH)
            }

            val used = abilities.rockets - rockets.left
            return RouteResult.Found(Path(start, points + landing, landing, used, approach))
        }

        /** The highest ground down to [bottom] under any part of the feet at [spot], which land on edges; or NaN. */
        private fun groundUnder(spot: Vec3, bottom: Double): Double {
            var ground = Double.NaN
            for (x in Mth.floor(spot.x - HALF_WIDTH)..Mth.floor(spot.x + HALF_WIDTH)) {
                for (z in Mth.floor(spot.z - HALF_WIDTH)..Mth.floor(spot.z + HALF_WIDTH)) {
                    for (y in Mth.floor(position.y) downTo Mth.floor(bottom)) {
                        val height = terrain.standHeight(x, y, z)
                        if (height <= position.y && height >= bottom - GROUND_TOLERANCE && !(height <= ground)) {
                            ground = height
                        }
                    }
                }
            }
            return ground
        }

    }

    companion object {
        private const val TICKS_PER_SLICE = 200
        private const val MAX_TICKS = 2400
        private const val HIGH_APPROACH = 16.0

        // Elytra wall damage starts at a horizontal speed lost of 0.3 blocks a tick.
        private const val SAFE_BUMP = 0.3
        private const val BUMP_DROP = 2.0
        private const val TAKE_OFF_TICKS = 2
        private const val POINT_TICKS = 4
        private const val GLIDE_HEIGHT = 0.6
        private const val HALF_WIDTH = Terrain.PLAYER_WIDTH / 2.0
        private const val TAKE_OFF_SPEED = 0.3
        private const val GROUND_TOLERANCE = 0.01
        private const val LANDING_REACH = 3.0
        private const val LANDING_HEIGHT = 1.0
        private const val VERTICAL_MARGIN = 24
        private const val HORIZONTAL_MARGIN = 48

        /** The blocks a glide can pass: around both ends, with room to circle down to the goal and to climb above. */
        fun region(start: Vec3, goal: Vec3): NavigationRegion {
            val margin = min(start.horizontalDistanceTo(goal), HORIZONTAL_MARGIN.toDouble()).toInt() + 2
            return NavigationRegion(
                Mth.floor(min(start.x, goal.x)) - margin,
                Mth.floor(min(start.y, goal.y)) - 2,
                Mth.floor(min(start.z, goal.z)) - margin,
                Mth.floor(max(start.x, goal.x)) + margin,
                Mth.floor(max(start.y, goal.y)) + VERTICAL_MARGIN,
                Mth.floor(max(start.z, goal.z)) + margin,
            )
        }
    }

}
