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
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceTo
import net.ccbluex.liquidbounce.utils.math.yaw
import net.ccbluex.liquidbounce.utils.navigation.render.RouteProgress
import net.ccbluex.liquidbounce.utils.navigation.render.RouteView
import net.ccbluex.liquidbounce.utils.navigation.route.NearGoal
import net.ccbluex.liquidbounce.utils.navigation.route.RouteLimits
import net.ccbluex.liquidbounce.utils.navigation.route.RoutePlanner
import net.ccbluex.liquidbounce.utils.navigation.route.RouteResult
import net.ccbluex.liquidbounce.utils.navigation.route.StageGoal
import net.ccbluex.liquidbounce.utils.navigation.terrain.LiveWorld
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import java.util.function.DoubleSupplier
import kotlin.math.abs
import kotlin.math.min

/**
 * Drives the vehicle the player rides to a goal: a boat over water and ice with its own controls, a horse or other
 * mount by looking the way. Where the vehicle gets no closer, or the player rides something it cannot steer, the
 * player gets off to walk the rest.
 *
 * Routes are at most [maxLength] blocks long; goals beyond half of that are ridden to in stages.
 */
@Suppress("TooManyFunctions")
class RideNavigator(
    private val owner: EventListener,
    private val view: RouteView,
    private val maxLength: DoubleSupplier,
) : EventListener {

    private val planner = RoutePlanner<RidePath>(this)
    private val steering = RideSteering()

    private var vehicle: Entity? = null
    private var path: RidePath? = null
    private var index = 0

    private var ticks = 0
    private var lastRequest = Int.MIN_VALUE / 2
    private var retryAt = 0
    private var failed = false
    private var stuck = false
    private var stuckCount = 0

    /**
     * Whether the vehicle is in the goal node, so only the last bit is left to ride straight.
     */
    private var atGoal = false
    private var approachTicks = 0
    private var dismountTicks = 0

    private var bestDistance = Double.MAX_VALUE
    private var progressAt = 0

    private var pathGoal: Vec3? = null
    private var requestedGoal: Vec3? = null
    private var requestedStage: NearGoal? = null
    private var followsStage = false

    /**
     * Moves the ridden vehicle one tick towards [goal], within [radius], or gets off it; false when the player rides
     * nothing, so the walk goes on.
     */
    fun navigate(event: MovementInputEvent, goal: Vec3, radius: Double): Boolean {
        if (!player.isPassenger) {
            if (vehicle != null || dismountTicks > 0) {
                halt()
            }
            return false
        }

        ticks++
        val vehicle = player.controlledVehicle
        val body = vehicle?.let { RideBody.of(player, it) }
        if (vehicle == null || body == null || dismountTicks > 0) {
            dismount(event, goal)
            return true
        }
        this.vehicle = vehicle

        val position = vehicle.position()
        collect()

        val path = path
        when {
            path != null && index < path.points.size -> follow(event, vehicle, body, path)
            atGoal || path?.complete == true -> approach(event, vehicle, body, goal)
            failed || path?.exhausted == true -> dismount(event, goal)
            else -> {
                steering.stop(event)
                view.pending(position, goal, planner.preview)
            }
        }

        if (dismountTicks == 0) {
            planIfNeeded(goal, radius, position, body)
        }
        return true
    }

    /**
     * Stops riding to the goal until the next [navigate].
     */
    fun halt() {
        planner.cancel()
        steering.reset()
        vehicle = null
        path = null
        index = 0
        failed = false
        stuck = false
        stuckCount = 0
        atGoal = false
        approachTicks = 0
        dismountTicks = 0
        pathGoal = null
        requestedGoal = null
        requestedStage = null
        followsStage = false
    }

    private fun follow(event: MovementInputEvent, vehicle: Entity, body: RideBody, path: RidePath) {
        val position = vehicle.position()
        advance(position, body, path)
        if (index >= path.points.size) {
            steering.stop(event)
            return
        }

        val point = path.points[index]
        trackProgress(position, point.position, path)

        val moves = RideMoves(NavigationTerrain(LiveWorld(world)), body)
        var target = point.position
        if (!point.jump) {
            for (i in index + 1 until min(path.points.size, index + LOOKAHEAD)) {
                val next = path.points[i]
                if (next.jump || !moves.isStraight(position, next.position)) {
                    break
                }
                target = next.position
            }
        }

        when (body.kind) {
            RideKind.BOAT -> {
                val brake = target === path.end && position.horizontalDistanceTo(target) <
                        vehicle.deltaMovement.horizontalDistance() * BOAT_COAST_TICKS + 1.0
                steering.boat(event, vehicle, target, brake)
            }

            RideKind.MOUNT -> {
                val jumpTo = point.position.takeIf { point.jump }
                steering.mount(event, vehicle, target, jumpTo, body.width / 2.0 + JUMP_REACH)
            }
        }

        view.active(path.positions, progress)
    }

    /**
     * Moves on past the points of [path] the vehicle has got to.
     */
    private fun advance(position: Vec3, body: RideBody, path: RidePath) {
        val arrive = if (body.kind == RideKind.BOAT) BOAT_ARRIVE else MOUNT_ARRIVE
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
                    position.horizontalDistanceTo(next.position) < point.position.horizontalDistanceTo(next.position)
            if (passed || distance <= arrive) {
                reached = i
            }
        }

        if (reached >= 0) {
            index = reached + 1
            bestDistance = Double.MAX_VALUE
            progressAt = ticks
        }
    }

    /**
     * Notes when the vehicle stops getting closer to [point]; a boat pushed against the shore at the end of its
     * route is there.
     */
    private fun trackProgress(position: Vec3, point: Vec3, path: RidePath) {
        val distance = position.horizontalDistanceTo(point)
        if (distance < bestDistance - MIN_PROGRESS) {
            bestDistance = distance
            progressAt = ticks
            return
        }
        if (ticks - progressAt < STUCK_TICKS) {
            return
        }

        progressAt = ticks
        bestDistance = Double.MAX_VALUE
        if (index == path.points.lastIndex && distance < NEAR_END) {
            index = path.points.size
        } else {
            stuck = true
            stuckCount++
        }
    }

    /**
     * Rides straight at [goal] from within its node, and gets off when that does not get the player there.
     */
    private fun approach(event: MovementInputEvent, vehicle: Entity, body: RideBody, goal: Vec3) {
        if (++approachTicks > APPROACH_TICKS) {
            dismount(event, goal)
            return
        }

        when (body.kind) {
            RideKind.BOAT -> steering.boat(event, vehicle, goal, brake = true)
            RideKind.MOUNT -> steering.mount(event, vehicle, goal, null, 0.0)
        }
        view.done()
    }

    /**
     * Gets off, facing [goal] so the player lands on its side.
     */
    private fun dismount(event: MovementInputEvent, goal: Vec3) {
        if (dismountTicks == 0) {
            planner.cancel()
            path = null
        }

        dismountTicks++
        steering.face(goal.subtract(player.position()).yaw)
        steering.stop(event, sneak = dismountTicks > DISMOUNT_DELAY)
        view.done()
    }

    private fun collect() {
        when (val result = planner.poll()) {
            RouteResult.Searching -> Unit

            RouteResult.AlreadyThere -> {
                planner.cancel()
                path = null
                atGoal = true
                failed = false
                pathGoal = requestedGoal
            }

            is RouteResult.Found -> {
                planner.cancel()
                accept(result.value)
            }

            is RouteResult.Failed -> {
                planner.cancel()
                failed = true
                retryAt = ticks + RETRY_TICKS
            }
        }
    }

    private fun accept(path: RidePath) {
        this.path = path
        index = 0
        failed = false
        stuck = false
        atGoal = false
        approachTicks = 0
        bestDistance = Double.MAX_VALUE
        progressAt = ticks
        followsStage = requestedStage.let { it != null && !it.isGoal(path.points.last().node) }
        pathGoal = requestedGoal
    }

    private fun planIfNeeded(goal: Vec3, radius: Double, position: Vec3, body: RideBody) {
        if (stuckCount > MAX_STUCK) {
            failed = true
            path = null
            return
        }

        val path = path
        val offPath = path != null && index < path.points.size && distanceToLeg(position, path) > OFF_PATH
        val goalMoved = pathGoal.let { it == null || it.distanceTo(goal) > GOAL_MOVED }
        val finishedShort = path != null && index >= path.points.size && !path.complete && !path.exhausted
        val nearStage = path != null && followsStage && position.horizontalDistanceTo(path.end) < STAGE_LEAD
        val urgent = stuck || offPath

        val needed = goalMoved || offPath || finishedShort || stuck || nearStage
        if (!needed || ticks < retryAt) {
            return
        }
        if (!urgent && (!planner.isIdle || ticks - lastRequest < REPLAN_INTERVAL)) {
            return
        }

        request(goal, radius, position, body)
        stuck = false
    }

    private fun request(goal: Vec3, radius: Double, start: Vec3, body: RideBody) {
        val nearGoal = NearGoal(goal, radius)
        val maxLength = maxLength.asDouble
        val stageRange = maxLength / 2.0
        val staged = StageGoal.isNeeded(LiveWorld(world), start, goal, stageRange)
        val region = if (staged) {
            StageGoal.region(start, stageRange, VERTICAL_MARGIN)
        } else {
            nearGoal.region(start, maxLength, VERTICAL_MARGIN)
        }
        val limits = RouteLimits(maxLength)

        val key = SearchKey(BlockPos.containing(start), nearGoal, staged, body.kind)
        planner.request(key, region) { terrain ->
            val searchGoal = if (staged) StageGoal(nearGoal, start, stageRange, terrain) else nearGoal
            RideRouteSearch(terrain, start, searchGoal, body, limits)
        }

        requestedGoal = goal
        requestedStage = nearGoal.takeIf { staged }
        lastRequest = ticks
    }

    /**
     * Horizontal distance from [position] to the leg of [path] being ridden.
     */
    private fun distanceToLeg(position: Vec3, path: RidePath): Double {
        val from = path.positions[index]
        val to = path.positions[index + 1]
        val dx = to.x - from.x
        val dz = to.z - from.z
        val lengthSqr = dx * dx + dz * dz
        val t = if (lengthSqr < 1.0E-6) {
            0.0
        } else {
            (((position.x - from.x) * dx + (position.z - from.z) * dz) / lengthSqr).coerceIn(0.0, 1.0)
        }
        return position.horizontalDistanceTo(Vec3(from.x + dx * t, position.y, from.z + dz * t))
    }

    private val progress = object : RouteProgress {
        override val isMoving: Boolean
            get() = path.let { it != null && index < it.points.size }

        override fun head(partialTicks: Float): Vec3 =
            (vehicle ?: player).interpolateCurrentPosition(partialTicks)

        override val ahead: List<Vec3>
            get() = path?.let { it.positions.subList(min(index + 1, it.positions.size), it.positions.size) }
                ?: emptyList()
    }

    override fun parent() = owner

    private data class SearchKey(val start: BlockPos, val goal: NearGoal, val staged: Boolean, val kind: RideKind)

    private companion object {
        const val VERTICAL_MARGIN = 16
        const val GOAL_MOVED = 1.5
        const val REPLAN_INTERVAL = 5
        const val RETRY_TICKS = 20
        const val STAGE_LEAD = 12.0
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

        /**
         * Ticks a boat in water takes to coast to a stop.
         */
        const val BOAT_COAST_TICKS = 10.0
    }

}
