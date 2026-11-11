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
import net.ccbluex.liquidbounce.features.client.clientState
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.navigation.render.RouteRenderer
import net.ccbluex.liquidbounce.utils.navigation.render.RouteView
import net.ccbluex.liquidbounce.utils.navigation.route.BlockGoal
import net.ccbluex.liquidbounce.utils.navigation.route.RouteLimits
import net.ccbluex.liquidbounce.utils.navigation.route.RoutePlanner
import net.ccbluex.liquidbounce.utils.navigation.route.RouteResult
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3

/** A teleport of [owner] somewhere and back: the route search, its run through [TeleportDispatcher] and the [view]. */
class TeleportTrip(private val owner: EventListener, private val steps: TeleportStepSettings) {

    val view = RouteView().apply {
        ghost = { partialTicks -> TeleportDispatcher.serverPosition(owner, partialTicks) }
    }
    private val planner = RoutePlanner<TeleportPlan>(owner)
    val renderer = RouteRenderer(owner, view)

    /** The run that was dispatched last, also after it ended. */
    var run: TeleportRun? by clientState { null }
        private set

    val isAvailable: Boolean
        get() = TeleportDispatcher.isAvailable(owner)

    val isAway: Boolean
        get() = TeleportDispatcher.holds(owner)

    /** Where the server has the player. */
    val feet: Vec3
        get() = TeleportDispatcher.serverPosition?.takeIf { isAway } ?: player.position()

    val eyes: Vec3
        get() = eyesAt(feet)

    fun request(
        origin: Vec3,
        goals: List<BlockPos>,
        maxLength: Double,
        destination: Vec3 = Vec3.atBottomCenterOf(goals.first()),
    ) {
        val limits = RouteLimits(maxLength)
        val goal = BlockGoal(goals)
        val stepLength = steps.stepLength
        planner.request(goal, goal.region(origin, maxLength, maxLength.toInt())) { terrain ->
            TeleportRouteSearch(terrain, origin, goals, limits, stepLength)
        }
        view.pending(origin, destination)
    }

    /** Advances the search by a tick; it ends with the first result that is not [RouteResult.Searching]. */
    fun poll(): RouteResult<TeleportPlan> {
        val result = planner.poll()
        if (result == RouteResult.Searching) {
            showSearch()
        } else {
            planner.cancel()
        }
        return result
    }

    private fun showSearch() {
        val start = view.start ?: return
        val destination = view.destination ?: return
        view.pending(start, destination, planner.preview)
    }

    fun dispatch(plan: TeleportPlan, finish: RunFinish): TeleportRun? =
        TeleportDispatcher.dispatch(owner, plan, finish, steps)?.also {
            run = it
            view.active(it.route.path, it)
        }

    /** Brings the server back to the client; true if that takes a run, which is over once not [isAway]. */
    fun comeBack(): Boolean {
        planner.cancel()
        view.engaged = false
        if (!isAway) {
            return false
        }

        TeleportDispatcher.undo(owner)
        showReturn()
        return true
    }

    fun showReturn() {
        TeleportDispatcher.run?.takeIf { it.owner === owner && it.finish == RunFinish.RESYNC }?.let {
            view.active(it.route.path, it)
        }
    }

    fun reset() {
        planner.cancel()
        run = null
    }

}
