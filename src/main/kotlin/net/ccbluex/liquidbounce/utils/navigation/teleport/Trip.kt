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
package net.ccbluex.liquidbounce.utils.navigation.teleport

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.getEyePositionAt
import net.ccbluex.liquidbounce.utils.navigation.route.BackgroundSearch
import net.ccbluex.liquidbounce.utils.navigation.route.BlockGoal
import net.ccbluex.liquidbounce.utils.navigation.route.RouteLimits
import net.ccbluex.liquidbounce.utils.navigation.route.RouteResult
import net.ccbluex.liquidbounce.utils.render.route.RouteRenderer
import net.ccbluex.liquidbounce.utils.render.route.RouteView
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3

/** A teleport of [owner] somewhere and back: the route search, its run through [TeleportManager] and the [view]. */
class Trip(private val owner: EventListener, private val steps: StepsValueGroup) {

    val view = RouteView().apply {
        ghost = { partialTicks -> TeleportManager.serverPosition(owner, partialTicks) }
    }
    private val search = BackgroundSearch<Route>(owner)
    val renderer = RouteRenderer(owner, view)

    /** The run that was dispatched last, also after it ended. */
    var run: Teleport? = null
        private set

    /** The search of the last request, to seed a new one from the same origin. */
    private var lastSearch: WaypointSearch? = null

    val isAvailable: Boolean
        get() = TeleportManager.isAvailable(owner)

    val isAway: Boolean
        get() = TeleportManager.holds(owner)

    /** Where the server has the player. */
    val feet: Vec3
        get() = TeleportManager.serverPosition?.takeIf { isAway } ?: player.position()

    val eyes: Vec3
        get() = player.getEyePositionAt(feet)

    fun request(
        origin: Vec3,
        goals: List<BlockPos>,
        maxLength: Double,
        destination: Vec3 = Vec3.atBottomCenterOf(goals.first()),
        reuseScores: Boolean = false,
        goalFeet: Map<BlockPos, Vec3> = emptyMap(),
    ) {
        val limits = RouteLimits(maxLength)
        val goal = BlockGoal(goals)
        val stepLength = steps.stepLength
        // A seed is only valid for the same origin over the same ground; the start check stands in for the terrain,
        // which [BackgroundSearch] keeps fresh. An invalid seed costs a wrong route at most, never a wrong arrival:
        // [Route.plan] sweeps every segment again before the steps go out.
        val seed = if (reuseScores) {
            lastSearch?.takeIf { it.start.distanceToSqr(origin) <= Route.SAME_POSITION_SQ }?.scores()
        } else {
            null
        }
        search.request(goal, goal.region(origin, maxLength, maxLength.toInt())) { terrain ->
            WaypointSearch(terrain, origin, goals, limits, stepLength, seed, goalFeet).also { lastSearch = it }
        }
        view.pending(origin, destination)
    }

    /** Points the running search at [goals] without losing its progress; false when none is running. */
    fun retarget(goals: List<BlockPos>, goalFeet: Map<BlockPos, Vec3> = emptyMap()): Boolean {
        val goal = BlockGoal(goals)
        return search.retarget(goal) { (it as? WaypointSearch)?.retarget(goals, goalFeet) == true }
    }

    /** Advances the search by a tick; it ends with the first result that is not [RouteResult.Searching]. */
    fun poll(): RouteResult<Route> {
        val result = search.poll()
        if (result == RouteResult.Searching) {
            showSearch()
        } else {
            search.cancel()
        }
        return result
    }

    private fun showSearch() {
        val start = view.start ?: return
        val destination = view.destination ?: return
        view.pending(start, destination, search.preview)
    }

    fun dispatch(
        route: Route,
        finish: Teleport.Finish,
        flight: Teleport.FlightMode = Teleport.FlightMode.PACED,
        maxDrift: Double = TeleportManager.MAX_DRIFT,
    ): Teleport? =
        TeleportManager.dispatch(owner, route, finish, steps, flight, maxDrift)?.also {
            run = it
            view.active(it.route.path, it)
        }

    /** Brings the server back to the client; true if that takes a run, which is over once not [isAway]. */
    fun comeBack(): Boolean {
        search.cancel()
        view.engaged = false
        if (!isAway) {
            return false
        }

        TeleportManager.undo(owner)
        showReturn()
        return true
    }

    fun showReturn() {
        TeleportManager.run?.takeIf { it.owner === owner && it.finish == Teleport.Finish.RESYNC }?.let {
            view.active(it.route.path, it)
        }
    }

    fun reset() {
        search.cancel()
        run = null
        lastSearch = null
    }

}
