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

package net.ccbluex.liquidbounce.utils.navigation.ride

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.math.distanceToSegment
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceTo
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceToSqr
import net.ccbluex.liquidbounce.utils.math.yaw
import net.ccbluex.liquidbounce.utils.navigation.render.RouteProgress
import net.ccbluex.liquidbounce.utils.navigation.render.RouteView
import net.ccbluex.liquidbounce.utils.navigation.route.NearGoal
import net.ccbluex.liquidbounce.utils.navigation.route.Replanner
import net.ccbluex.liquidbounce.utils.navigation.route.RouteLimits
import net.ccbluex.liquidbounce.utils.navigation.route.RouteResult
import net.ccbluex.liquidbounce.utils.navigation.terrain.Terrain
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import java.util.function.DoubleSupplier
import kotlin.math.abs
import kotlin.math.min

/**
 * Drives the vehicle the player rides to a goal: a boat with its own controls, a horse or other mount by looking the
 * way. Where the vehicle gets no closer, or the player cannot steer it, the player gets off to walk the rest.
 */
@Suppress("TooManyFunctions")
class Riding(
    private val owner: EventListener,
    private val view: RouteView,
    private val maxLength: DoubleSupplier,
) : EventListener {

    private val routes = Replanner<Path>(this, VERTICAL_MARGIN)
    private val steering = Steering()

    private var vehicle: Entity? = null
    private var path: Path? = null
    private var index = 0

    private var stuck = false
    private var stuckCount = 0
    private var bestDistance = Double.MAX_VALUE
    private var idleTicks = 0

    /** Whether the vehicle is in the goal node, so only the last bit is left to ride straight. */
    private var atGoal = false
    private var approachTicks = 0
    private var dismountTicks = 0

    /**
     * Moves the ridden vehicle one tick towards [goal], within [radius], or gets off it; false when the player rides
     * nothing.
     */
    fun navigate(event: MovementInputEvent, goal: Vec3, radius: Double): Boolean {
        if (!player.isPassenger) {
            if (vehicle != null || dismountTicks > 0) {
                halt()
            }
            return false
        }

        routes.tick()
        val vehicle = player.controlledVehicle
        val body = vehicle?.let { Body.of(player, it) }
        if (vehicle == null || body == null || dismountTicks > 0) {
            dismount(event, goal)
            return true
        }
        this.vehicle = vehicle

        val position = vehicle.position()
        collectRoute()

        val path = path
        when {
            path != null && index < path.points.size -> follow(event, vehicle, body, path)
            atGoal || path?.complete == true -> approach(event, vehicle, body, goal)
            stuckCount > MAX_STUCK || routes.failed || path?.exhausted == true -> dismount(event, goal)
            else -> {
                steering.stop(event)
                view.pending(position, goal, routes.preview)
            }
        }

        if (dismountTicks == 0) {
            planIfNeeded(goal, radius, position, body)
        }
        return true
    }

    /** Stops riding to the goal until the next [navigate]. */
    fun halt() {
        routes.reset()
        steering.reset()
        vehicle = null
        path = null
        index = 0
        stuck = false
        stuckCount = 0
        atGoal = false
        approachTicks = 0
        dismountTicks = 0
    }

    private fun follow(event: MovementInputEvent, vehicle: Entity, body: Body, path: Path) {
        val position = vehicle.position()
        advance(position, body, path)
        if (index >= path.points.size) {
            steering.stop(event)
            return
        }

        val point = path.points[index]
        trackProgress(position, point.position, path)

        val target = if (point.jump) point.position else straightAhead(position, body, path)
        when (body.kind) {
            VehicleKind.BOAT -> {
                val brake = target === path.end && position.horizontalDistanceTo(target) <
                    vehicle.deltaMovement.horizontalDistance() * BOAT_COAST_TICKS + 1.0
                steering.boat(event, vehicle, target, brake)
            }

            VehicleKind.MOUNT -> {
                val jumpTo = point.position.takeIf { point.jump }
                steering.mount(event, vehicle, target, jumpTo, body.width / 2.0 + JUMP_REACH)
            }
        }

        view.active(path.positions, progress)
    }

    /** The farthest of the next points the vehicle reaches in a straight line without a jump. */
    private fun straightAhead(position: Vec3, body: Body, path: Path): Vec3 {
        val moves = Moves(Terrain.live(), body)
        var target = path.points[index].position
        for (i in index + 1 until min(path.points.size, index + LOOKAHEAD)) {
            val next = path.points[i]
            if (next.jump || !moves.isStraight(position, next.position)) {
                break
            }
            target = next.position
        }
        return target
    }

    /** Moves on past the points of [path] the vehicle has got to. */
    private fun advance(position: Vec3, body: Body, path: Path) {
        val arrive = if (body.kind == VehicleKind.BOAT) BOAT_ARRIVE else MOUNT_ARRIVE
        var reached = -1
        for (i in index until min(path.points.size, index + JOIN_POINTS)) {
            val point = path.points[i]
            if (abs(position.y - point.position.y) > VERTICAL_ARRIVE) {
                continue
            }

            // A point the vehicle went past on the way to the next one counts as reached too.
            val next = path.points.getOrNull(i + 1)
            val distance = position.horizontalDistanceTo(point.position)
            val passed = next != null && !point.jump && !next.jump && distance <= arrive * 2.0 &&
                position.horizontalDistanceToSqr(next.position) < point.position.horizontalDistanceToSqr(next.position)
            if (passed || distance <= arrive) {
                reached = i
            }
        }

        if (reached >= 0) {
            index = reached + 1
            resetProgress()
        }
    }

    private fun trackProgress(position: Vec3, point: Vec3, path: Path) {
        val distance = position.horizontalDistanceTo(point)
        if (distance < bestDistance - MIN_PROGRESS) {
            bestDistance = distance
            idleTicks = 0
            return
        }
        if (++idleTicks < STUCK_TICKS) {
            return
        }

        resetProgress()
        // A boat pushed against the shore at the end of its route is there.
        if (index == path.points.lastIndex && distance < NEAR_END) {
            index = path.points.size
        } else {
            stuck = true
            stuckCount++
        }
    }

    private fun resetProgress() {
        bestDistance = Double.MAX_VALUE
        idleTicks = 0
    }

    /** Rides straight at [goal] from within its node, and gets off when that does not get the player there. */
    private fun approach(event: MovementInputEvent, vehicle: Entity, body: Body, goal: Vec3) {
        if (++approachTicks > APPROACH_TICKS) {
            dismount(event, goal)
            return
        }

        when (body.kind) {
            VehicleKind.BOAT -> steering.boat(event, vehicle, goal, brake = true)
            VehicleKind.MOUNT -> steering.mount(event, vehicle, goal, jumpTo = null, reach = 0.0)
        }
        view.done()
    }

    /** Gets off, facing [goal] so the player lands on its side. */
    private fun dismount(event: MovementInputEvent, goal: Vec3) {
        if (dismountTicks == 0) {
            routes.reset()
            path = null
        }

        dismountTicks++
        steering.face(goal.subtract(player.position()).yaw)
        steering.stop(event, sneak = dismountTicks > DISMOUNT_DELAY)
        view.done()
    }

    private fun collectRoute() {
        when (val result = routes.poll()) {
            RouteResult.Searching, is RouteResult.Failed -> Unit

            RouteResult.AlreadyThere -> {
                path = null
                atGoal = true
            }

            is RouteResult.Found -> {
                path = result.value
                index = 0
                stuck = false
                atGoal = false
                approachTicks = 0
                resetProgress()
                routes.routeEnds(result.value.points.last().node)
            }
        }
    }

    private fun planIfNeeded(goal: Vec3, radius: Double, position: Vec3, body: Body) {
        if (stuckCount > MAX_STUCK) {
            path = null
            return
        }

        val path = path
        val riding = path != null && index < path.points.size
        val offPath = path != null && riding && position.horizontal().distanceToSegment(
            path.positions[index].horizontal(), path.positions[index + 1].horizontal(),
        ) > OFF_PATH
        val finishedShort = path != null && !riding && !path.complete && !path.exhausted
        val nearStage = path != null && routes.endsAtStage &&
            position.horizontalDistanceToSqr(path.end) < Replanner.STAGE_LEAD * Replanner.STAGE_LEAD
        val urgent = stuck || offPath

        val needed = routes.hasGoalMoved(goal) || offPath || finishedShort || stuck || nearStage
        if (!needed || !routes.isDue(urgent)) {
            return
        }

        val maxLength = maxLength.asDouble
        val limits = RouteLimits(maxLength)
        routes.request(position, NearGoal(goal, radius), maxLength, body.kind) { terrain, searchGoal ->
            PathSearch(terrain, position, searchGoal, body, limits)
        }
        stuck = false
    }

    private val progress = object : RouteProgress {
        override val isMoving: Boolean
            get() = path.let { it != null && index < it.points.size }

        override fun head(partialTicks: Float): Vec3 =
            (vehicle ?: player).interpolateCurrentPosition(partialTicks)

        override val ahead: List<Vec3>
            get() = path?.let { it.positions.subList(min(index + 1, it.positions.size), it.positions.size) }.orEmpty()
    }

    override fun parent() = owner

    private companion object {
        const val VERTICAL_MARGIN = 16
        const val OFF_PATH = 3.0
        const val LOOKAHEAD = 6
        const val JOIN_POINTS = 4
        const val BOAT_ARRIVE = 1.0
        const val MOUNT_ARRIVE = 0.6
        const val VERTICAL_ARRIVE = 1.2
        const val JUMP_REACH = 1.0
        const val MIN_PROGRESS = 0.3
        const val STUCK_TICKS = 60
        const val MAX_STUCK = 4
        const val NEAR_END = 2.5
        const val APPROACH_TICKS = 60
        const val DISMOUNT_DELAY = 2

        /** Ticks a boat in water takes to coast to a stop. */
        const val BOAT_COAST_TICKS = 10.0
    }

}
