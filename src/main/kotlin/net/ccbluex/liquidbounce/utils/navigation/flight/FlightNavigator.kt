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
import net.ccbluex.liquidbounce.features.client.clientState
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.navigation.render.RouteView
import net.ccbluex.liquidbounce.utils.navigation.route.GoalRouter
import net.ccbluex.liquidbounce.utils.navigation.route.NearGoal
import net.ccbluex.liquidbounce.utils.navigation.route.RouteLimits
import net.ccbluex.liquidbounce.utils.navigation.route.RouteResult
import net.ccbluex.liquidbounce.utils.navigation.walk.FollowState
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3

/** Flies to goals for a walking navigator while the player can fly, see [FlightAbilities.capture]. */
class FlightNavigator(private val owner: EventListener, private val view: RouteView) : EventListener {

    private val routes = GoalRouter<FlightPath>(this, VERTICAL_MARGIN)
    private val follower = FlightFollower()
    private var stuck by clientState { false }

    /** Whether the last tick was flown. */
    var isActive by clientState { false }
        private set

    val target: Vec3?
        get() = follower.target

    val sprint: Boolean
        get() = follower.sprint

    override fun parent() = owner

    /** Flies one tick towards [goal], within [radius], searching routes up to [maxLength] long. */
    fun navigate(
        event: MovementInputEvent,
        goal: Vec3,
        radius: Double,
        maxLength: Double,
        abilities: FlightAbilities,
        frameYaw: Float,
    ) {
        routes.tick()
        isActive = true
        val position = player.position()
        collectRoute(goal, position)

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
                routes.failed -> view.invalid()
                routes.isSearching -> view.pending(position, goal, routes.preview)
                else -> view.done()
            }
        }

        planIfNeeded(goal, radius, maxLength, position, abilities)
    }

    /** Stops flying routes until the next [navigate]. */
    fun halt() {
        routes.reset()
        follower.clear()
        isActive = false
        stuck = false
    }

    private fun collectRoute(goal: Vec3, position: Vec3) {
        when (val result = routes.poll()) {
            RouteResult.Searching -> Unit

            // The search measures from block centers; the rest of the way is a straight line.
            RouteResult.AlreadyThere -> accept(FlightPath(position, listOf(position, goal), complete = true))

            is RouteResult.Found -> accept(result.value)

            is RouteResult.Failed -> if (follower.isFinished) {
                follower.clear()
            }
        }
    }

    private fun accept(path: FlightPath) {
        stuck = false
        routes.routeEnds(BlockPos.containing(path.end))
        follower.follow(path)
    }

    private fun planIfNeeded(
        goal: Vec3,
        radius: Double,
        maxLength: Double,
        position: Vec3,
        abilities: FlightAbilities,
    ) {
        val path = follower.path
        val finished = path != null && follower.isFinished
        val nearStage = path != null && routes.endsAtStage && position.closerThan(path.end, GoalRouter.STAGE_LEAD)
        val urgent = stuck || follower.isOffPath()

        val needed = path == null || routes.hasGoalMoved(goal) || finished || nearStage || urgent
        if (!needed || !routes.isDue(urgent)) {
            return
        }

        val limits = RouteLimits(maxLength, maxExpansions = MAX_EXPANSIONS)
        routes.request(position, NearGoal(goal, radius), maxLength, abilities.control) { terrain, searchGoal ->
            FlightRouteSearch(terrain, position, searchGoal, abilities, limits)
        }
        stuck = false
    }

    private companion object {
        const val VERTICAL_MARGIN = 32
        const val MAX_EXPANSIONS = 20_000
    }

}
