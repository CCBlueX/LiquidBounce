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

package net.ccbluex.liquidbounce.utils.navigation.route

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.navigation.terrain.LiveWorld
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationWorld
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3

/**
 * Searches routes for a navigator whose goal is given anew every tick. Goals beyond half the route length or in
 * chunks not loaded are searched in stages.
 */
internal class GoalRouter<T>(
    owner: EventListener,
    private val verticalMargin: Int,
    newTerrain: (NavigationWorld) -> NavigationTerrain = ::NavigationTerrain,
) {

    private val planner = RoutePlanner<T>(owner, newTerrain)

    var ticks = 0
        private set
    private var lastRequest = Int.MIN_VALUE / 2
    private var retryAt = 0

    private var requested: Request? = null

    private var routed: Request? = null

    var failed = false
        private set

    /** Whether the route in use ends at a stage on the way to the goal. */
    var endsAtStage = false
        private set

    val isSearching: Boolean
        get() = !planner.isIdle

    val preview: List<Vec3>?
        get() = planner.preview

    fun tick() {
        ticks++
    }

    /** The result of the search, once. After a route was found, tell where it ends with [routeEnds] or [forget] it. */
    fun poll(): RouteResult<T> {
        val result = planner.poll()
        when (result) {
            RouteResult.Searching -> return result

            is RouteResult.Failed -> {
                failed = true
                retryAt = ticks + RETRY_TICKS
            }

            else -> {
                failed = false
                endsAtStage = false
                routed = requested
            }
        }

        planner.cancel()
        return result
    }

    fun routeEnds(end: BlockPos) {
        endsAtStage = routed?.stage?.isGoal(end) == false
    }

    fun forget() {
        routed = null
    }

    fun hasGoalMoved(goal: Vec3) = !routed.isFor(goal)

    fun isNewGoal(goal: Vec3) = !requested.isFor(goal)

    /** A new search waits [REPLAN_INTERVAL] ticks after the last one unless [urgent], [RETRY_TICKS] after a failure. */
    fun isDue(urgent: Boolean) =
        ticks >= retryAt && (urgent || !isSearching && ticks - lastRequest >= REPLAN_INTERVAL)

    /**
     * Searches routes of at most [maxLength] from [start] to [goal], or to a stage on the way. A running search for
     * the same start block, goal and [variant] goes on.
     */
    fun request(
        start: Vec3,
        goal: NearGoal,
        maxLength: Double,
        variant: Any? = null,
        search: (terrain: NavigationTerrain, goal: NavigationGoal) -> RouteSearch<T>,
    ) {
        // A stage with its detours stays within the route length.
        val stageRange = maxLength / 2.0
        val staged = StageGoal.isNeeded(LiveWorld(world), start, goal.center, stageRange)
        val region = if (staged) {
            StageGoal.region(start, stageRange, verticalMargin)
        } else {
            goal.region(start, maxLength, verticalMargin)
        }

        planner.request(SearchKey(BlockPos.containing(start), goal, staged, variant), region) { terrain ->
            search(terrain, if (staged) StageGoal(goal, start, stageRange, terrain) else goal)
        }

        if (isNewGoal(goal.center)) {
            failed = false
        }
        requested = Request(goal.center, goal.takeIf { staged })
        lastRequest = ticks
    }

    fun reset() {
        planner.cancel()
        requested = null
        routed = null
        failed = false
        endsAtStage = false
    }

    private fun Request?.isFor(goal: Vec3) =
        this != null && this.goal.distanceToSqr(goal) <= GOAL_MOVED * GOAL_MOVED

    private class Request(val goal: Vec3, val stage: NearGoal?)

    private data class SearchKey(val start: BlockPos, val goal: NearGoal, val staged: Boolean, val variant: Any?)

    companion object {
        /** Blocks before the end of a stage at which the next one is searched, so the navigator does not stop there. */
        const val STAGE_LEAD = 12.0

        private const val GOAL_MOVED = 1.5
        private const val REPLAN_INTERVAL = 5
        private const val RETRY_TICKS = 20
    }

}
