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

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.navigation.render.RouteView
import net.ccbluex.liquidbounce.utils.navigation.route.NearGoal
import net.ccbluex.liquidbounce.utils.navigation.route.RouteLimits
import net.ccbluex.liquidbounce.utils.navigation.route.RoutePlanner
import net.ccbluex.liquidbounce.utils.navigation.route.RouteResult
import net.ccbluex.liquidbounce.utils.navigation.route.StageGoal
import net.ccbluex.liquidbounce.utils.navigation.terrain.LiveWorld
import net.ccbluex.liquidbounce.utils.navigation.walk.FollowState
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3

/**
 * Flies to goals for a walking navigator while the player can fly, see [FlightAbilities.capture]: plans flight
 * routes, in stages for far goals, and follows them, showing them in [view].
 */
class FlightNavigator(private val owner: EventListener, private val view: RouteView) : EventListener {

    private val planner = RoutePlanner<FlightPath>(this)
    private val follower = FlightFollower()

    /**
     * Whether the last tick was flown.
     */
    var isActive = false
        private set

    /**
     * Where the player heads, and whether it wants to sprint there.
     */
    val target: Vec3?
        get() = follower.target

    val sprint: Boolean
        get() = follower.sprint

    private var ticks = 0
    private var lastRequest = Int.MIN_VALUE / 2
    private var retryAt = 0
    private var failed = false
    private var stuck = false
    private var pathGoal: Vec3? = null
    private var requestedGoal: Vec3? = null
    private var requestedStage: NearGoal? = null
    private var followsStage = false

    override fun parent() = owner

    /**
     * Flies one tick towards [goal], within [radius], searching routes up to [maxLength] long.
     */
    fun navigate(
        event: MovementInputEvent,
        goal: Vec3,
        radius: Double,
        maxLength: Double,
        abilities: FlightAbilities,
        frameYaw: Float,
    ) {
        ticks++
        isActive = true
        val position = player.position()
        collectResult(goal, position)

        val path = follower.path
        if (path != null && !follower.isFinished) {
            if (follower.tick(event, frameYaw, abilities) == FollowState.STUCK) {
                stuck = true
            }
            view.active(path.points, follower)
        } else {
            follower.clear()
            event.directionalInput = DirectionalInput.NONE
            event.jump = false
            event.sneak = false
            when {
                failed -> view.invalid()
                !planner.isIdle -> view.pending(position, goal, planner.preview)
                else -> view.done()
            }
        }

        planIfNeeded(goal, radius, maxLength, position, abilities)
    }

    private fun collectResult(goal: Vec3, position: Vec3) {
        when (val result = planner.poll()) {
            RouteResult.Searching -> Unit

            RouteResult.AlreadyThere -> {
                // The search measures from block centres; the rest of the way is a straight line.
                planner.cancel()
                accept(FlightPath(position, listOf(position, goal), complete = true))
            }

            is RouteResult.Found -> {
                planner.cancel()
                accept(result.value)
            }

            is RouteResult.Failed -> {
                planner.cancel()
                failed = true
                retryAt = ticks + RETRY_TICKS
                if (follower.isFinished) {
                    follower.clear()
                }
            }
        }
    }

    private fun accept(path: FlightPath) {
        failed = false
        stuck = false
        followsStage = requestedStage.let { it != null && !it.isGoal(BlockPos.containing(path.end)) }
        pathGoal = requestedGoal
        follower.follow(path)
    }

    private fun planIfNeeded(goal: Vec3, radius: Double, maxLength: Double, position: Vec3,
                             abilities: FlightAbilities) {
        val path = follower.path
        val offPath = follower.isOffPath()
        val goalMoved = pathGoal.let { it == null || it.distanceTo(goal) > GOAL_MOVED }
        val finished = path != null && follower.isFinished
        val nearStage = path != null && followsStage && position.distanceTo(path.end) < STAGE_LEAD
        val urgent = stuck || offPath

        val needed = path == null || goalMoved || finished || nearStage || urgent
        if (!needed || ticks < retryAt) {
            return
        }
        if (!urgent && (!planner.isIdle || ticks - lastRequest < REPLAN_INTERVAL)) {
            return
        }

        request(goal, radius, maxLength, position, abilities)
        stuck = false
    }

    private fun request(goal: Vec3, radius: Double, maxLength: Double, start: Vec3, abilities: FlightAbilities) {
        val nearGoal = NearGoal(goal, radius)
        val stageRange = maxLength / 2.0
        val staged = StageGoal.isNeeded(LiveWorld(world), start, goal, stageRange)
        val region = if (staged) {
            StageGoal.region(start, stageRange, VERTICAL_MARGIN)
        } else {
            nearGoal.region(start, maxLength, VERTICAL_MARGIN)
        }
        val limits = RouteLimits(maxLength, maxExpansions = MAX_EXPANSIONS)

        val key = SearchKey(BlockPos.containing(start), nearGoal, staged, abilities.control)
        planner.request(key, region) { terrain ->
            val searchGoal = if (staged) StageGoal(nearGoal, start, stageRange, terrain) else nearGoal
            FlightRouteSearch(terrain, start, searchGoal, abilities, limits)
        }

        if (requestedGoal.let { it == null || it.distanceTo(goal) > GOAL_MOVED }) {
            failed = false
        }
        requestedGoal = goal
        requestedStage = nearGoal.takeIf { staged }
        lastRequest = ticks
    }

    /**
     * Stops flying routes until the next [navigate].
     */
    fun halt() {
        planner.cancel()
        follower.clear()
        isActive = false
        failed = false
        stuck = false
        pathGoal = null
        requestedGoal = null
        requestedStage = null
        followsStage = false
    }

    private data class SearchKey(
        val start: BlockPos,
        val goal: NearGoal,
        val staged: Boolean,
        val control: FlightControl,
    )

    private companion object {
        const val VERTICAL_MARGIN = 32
        const val MAX_EXPANSIONS = 20_000
        const val GOAL_MOVED = 1.5
        const val REPLAN_INTERVAL = 5
        const val RETRY_TICKS = 20
        const val STAGE_LEAD = 12.0
    }

}
