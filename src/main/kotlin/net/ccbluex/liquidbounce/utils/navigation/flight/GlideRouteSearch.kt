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

import net.ccbluex.liquidbounce.utils.math.horizontalDistanceTo
import net.ccbluex.liquidbounce.utils.math.withLength
import net.ccbluex.liquidbounce.utils.navigation.route.RouteFailure
import net.ccbluex.liquidbounce.utils.navigation.route.RouteResult
import net.ccbluex.liquidbounce.utils.navigation.route.RouteSearch
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationRegion
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * A glide from [start], the take-off included: where the feet pass, every few ticks, and where they land, flown with
 * [GlideControl] keeping high until [approach] blocks from the goal.
 */
class GlidePath(val start: Vec3, val points: List<Vec3>, val landing: Vec3, val rockets: Int, val approach: Double)

/**
 * Plans a glide from standing at [start] to [goal] by flying it tick by tick with [GlideControl]: a jump towards the
 * goal, the elytra opened in the air, and the glide until the feet touch down. The glide is found when it touches
 * down unhurt next to the goal without bumping into anything on the way. When the direct glide fails, it is flown
 * again keeping high until close to the goal.
 *
 * Not thread-safe: advance it from one thread at a time.
 */
class GlideRouteSearch(
    private val terrain: NavigationTerrain,
    private val start: Vec3,
    private val goal: Vec3,
    private val radius: Double,
    private val abilities: GlideAbilities,
) : RouteSearch<GlidePath> {

    private var approach = 0.0
    private var control = GlideControl(goal, abilities.rockets, abilities.rocketTicks)
    private val points = arrayListOf(start)
    private var position = start
    private var motion = Vec3.ZERO
    private var tick = 0

    var result: RouteResult<GlidePath> = RouteResult.Searching
        private set

    override fun advance(): RouteResult<GlidePath> {
        repeat(TICKS_PER_SLICE) {
            if (result != RouteResult.Searching) {
                return result
            }
            result = simulate()
            if (result is RouteResult.Failed && approach == 0.0) {
                restart(HIGH_APPROACH)
            }
        }
        return result
    }

    private fun restart(approach: Double) {
        this.approach = approach
        control = GlideControl(goal, abilities.rockets, abilities.rocketTicks, approach = approach)
        points.clear()
        points += start
        position = start
        motion = Vec3.ZERO
        tick = 0
        result = RouteResult.Searching
    }

    override fun preview(): List<Vec3> = ArrayList(points)

    private fun simulate(): RouteResult<GlidePath> {
        if (tick >= MAX_TICKS || tick >= abilities.glideTicks) {
            return RouteResult.Failed(RouteFailure.TOO_COMPLEX)
        }

        val next = if (tick < TAKE_OFF_TICKS) {
            jump()
        } else {
            val input = control.steer(position, motion)
            motion = GlidePhysics.step(motion, input.yaw, input.pitch, input.boosted)
            position.add(motion)
        }

        val height = if (tick < TAKE_OFF_TICKS) NavigationTerrain.PLAYER_HEIGHT else GLIDE_HEIGHT
        if (!terrain.isClear(box(next, height))) {
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

    /**
     * A sprint jump towards the goal and the ticks in the air before the elytra opens.
     */
    private fun jump(): Vec3 {
        if (tick == 0) {
            val toGoal = Vec3(goal.x - start.x, 0.0, goal.z - start.z).withLength(TAKE_OFF_SPEED)
            motion = Vec3(toGoal.x, JUMP_SPEED, toGoal.z)
        }
        val next = position.add(motion)
        motion = Vec3(motion.x * AIR_DRAG, (motion.y - GRAVITY) * VERTICAL_DRAG, motion.z * AIR_DRAG)
        return next
    }

    /**
     * Touches down where the glide moving to [next] meets the ground, or fails when it meets anything else, comes
     * down too fast or away from the goal. Brushing a step too slowly to get hurt slides down along it instead.
     */
    private fun land(next: Vec3): RouteResult<GlidePath> {
        val sideways = box(Vec3(next.x, position.y, next.z), GLIDE_HEIGHT)
        val bumped = !terrain.isClear(sideways)
        if (tick < TAKE_OFF_TICKS || motion.y < GlideControl.SAFE_SINK ||
            bumped && motion.horizontalDistance() > SAFE_BUMP) {
            return RouteResult.Failed(RouteFailure.NO_PATH)
        }

        val spot = if (bumped) position else next
        val ground = groundUnder(spot, if (bumped) position.y - BUMP_DROP else next.y)
        if (ground.isNaN()) {
            return RouteResult.Failed(RouteFailure.NO_PATH)
        }

        val landing = Vec3(spot.x, ground, spot.z)
        val reach = max(radius, LANDING_REACH)
        val near = Vec3(landing.x, goal.y, landing.z).distanceTo(goal) <= reach &&
            abs(landing.y - goal.y) <= LANDING_HEIGHT
        if (!near) {
            return RouteResult.Failed(RouteFailure.NO_PATH)
        }

        return RouteResult.Found(
            GlidePath(start, points + landing, landing, abilities.rockets - control.rockets, approach)
        )
    }

    /**
     * The highest ground from the current height down to [bottom] under any part of the feet at [spot], as the
     * game lands the player on the edge of a block, or NaN.
     */
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

    private fun box(feet: Vec3, height: Double) = AABB(
        feet.x - HALF_WIDTH, feet.y, feet.z - HALF_WIDTH,
        feet.x + HALF_WIDTH, feet.y + height, feet.z + HALF_WIDTH,
    )

    companion object {
        private const val TICKS_PER_SLICE = 200
        private const val MAX_TICKS = 2400
        private const val HIGH_APPROACH = 16.0

        /**
         * Elytra wall damage starts at a horizontal speed lost of 0.3 blocks a tick.
         */
        private const val SAFE_BUMP = 0.3
        private const val BUMP_DROP = 2.0
        private const val TAKE_OFF_TICKS = 2
        private const val POINT_TICKS = 4
        private const val GLIDE_HEIGHT = 0.6
        private const val HALF_WIDTH = NavigationTerrain.PLAYER_WIDTH / 2.0
        private const val TAKE_OFF_SPEED = 0.3
        private const val JUMP_SPEED = 0.42
        private const val GRAVITY = 0.08
        private const val AIR_DRAG = 0.91
        private const val VERTICAL_DRAG = 0.98
        private const val GROUND_TOLERANCE = 0.01
        private const val LANDING_REACH = 3.0
        private const val LANDING_HEIGHT = 1.0
        private const val VERTICAL_MARGIN = 24
        private const val HORIZONTAL_MARGIN = 48

        /**
         * Every block a glide from [start] to [goal] can pass: around both, as far again around the goal as it is
         * away to circle down, and above them for rocket climbs.
         */
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
