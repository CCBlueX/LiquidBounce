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

import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.math.distanceToSegment
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.movement.getDirectionalInputTowards
import net.ccbluex.liquidbounce.utils.navigation.render.RouteProgress
import net.ccbluex.liquidbounce.utils.navigation.walk.FollowState
import net.minecraft.world.entity.LivingEntity.FLYING_AIR_DRAG
import net.minecraft.world.phys.Vec3
import kotlin.math.min

/**
 * Flies a [Path] with movement input: the keys towards a point ahead on the route, jump to rise and sneak to
 * sink. With momentum the keys aim where the player would coast to, so it slows down in time for sharp corners.
 */
class Follower : RouteProgress {

    private var route: Route? = null
    private var takeOffTicks = 0

    var target: Vec3? = null
        private set

    var sprint = false
        private set

    val path: Path?
        get() = route?.path

    val isFinished: Boolean
        get() = route?.isFinished == true

    override val isMoving: Boolean
        get() = route?.isFinished == false

    override val ahead: List<Vec3>
        get() = route?.run { points.subList(min(corner, points.size), points.size) }.orEmpty()

    override fun head(partialTicks: Float): Vec3 = player.interpolateCurrentPosition(partialTicks)

    fun follow(path: Path) {
        route = Route(path)
    }

    fun clear() {
        route = null
        takeOffTicks = 0
        target = null
        sprint = false
    }

    fun isOffPath(): Boolean {
        val route = route?.takeUnless { it.isFinished } ?: return false
        val points = route.points
        return player.position().distanceToSegment(points[route.corner - 1], points[route.corner]) > OFF_PATH
    }

    fun tick(event: MovementInputEvent, frameYaw: Float, abilities: Abilities): FollowState {
        target = null
        sprint = false

        val route = route?.also { advance(it, abilities) }
        if (route == null || route.isFinished) {
            release(event)
            return FollowState.FINISHED
        }

        if (abilities.control == Control.MOMENTUM && !player.abilities.flying) {
            // A double jump starts creative flight.
            release(event, jump = takeOffTicks++ % 2 == 0)
            return FollowState.WALKING
        }

        takeOffTicks = 0
        val aim = aim(route, abilities)
        target = aim
        fly(event, frameYaw, aim, abilities, isFinal = aim == route.path.end)
        return progress(route)
    }

    private fun release(event: MovementInputEvent, jump: Boolean = false) {
        event.directionalInput = DirectionalInput.NONE
        event.jump = jump
        event.sneak = false
    }

    private fun progress(route: Route): FollowState {
        val distance = player.position().distanceTo(route.points[route.corner])
        if (distance < route.bestDistance - MIN_PROGRESS) {
            route.bestDistance = distance
            route.idleTicks = 0
        } else if (++route.idleTicks >= STUCK_TICKS) {
            route.resetProgress()
            return FollowState.STUCK
        }
        return FollowState.WALKING
    }

    /** Presses the keys that bring the point the player would coast to with them released towards [aim]. */
    private fun fly(
        event: MovementInputEvent,
        frameYaw: Float,
        aim: Vec3,
        abilities: Abilities,
        isFinal: Boolean,
    ) {
        val momentum = abilities.control == Control.MOMENTUM
        val velocity = player.deltaMovement
        val horizontalCoast = if (momentum) 1.0 / (1.0 - FLYING_AIR_DRAG) else 1.0
        val verticalCoast = if (momentum) 1.0 / (1.0 - Abilities.VERTICAL_DRAG) else 1.0
        val rest = player.position().add(
            velocity.x * horizontalCoast, velocity.y * verticalCoast, velocity.z * horizontalCoast,
        )
        val error = aim.subtract(rest)

        val horizontalTolerance = if (momentum) MOMENTUM_TOLERANCE else abilities.horizontalSpeed / 2.0
        val riseTolerance = if (momentum) MOMENTUM_TOLERANCE else abilities.verticalSpeed / 2.0
        // Sinking stops short of the ground on the way, as touching it ends creative flight.
        val sinkTolerance = if (momentum && !isFinal) SINK_TOLERANCE else riseTolerance

        val moves = error.horizontalDistanceSqr() > horizontalTolerance * horizontalTolerance
        event.directionalInput = if (moves) getDirectionalInputTowards(error, frameYaw) else DirectionalInput.NONE
        event.jump = error.y > riseTolerance
        event.sneak = error.y < -sinkTolerance && !event.jump
        sprint = moves && error.horizontalDistance() > SPRINT_DISTANCE
    }

    /** [LOOKAHEAD] blocks ahead on the route with momentum, else the next corner; never past a sharp corner. */
    private fun aim(route: Route, abilities: Abilities): Vec3 {
        val points = route.points
        if (abilities.control != Control.MOMENTUM) {
            return points[route.corner]
        }

        var remaining = LOOKAHEAD
        var from = player.position()
        for (corner in route.corner until points.size) {
            val point = points[corner]
            val length = from.distanceTo(point)
            if (length >= remaining) {
                return from.lerp(point, remaining / length)
            }
            if (isSharp(points, corner)) {
                return point
            }
            remaining -= length
            from = point
        }
        return points.last()
    }

    private fun advance(route: Route, abilities: Abilities) {
        val points = route.points
        val position = player.position()
        val start = route.corner
        while (!route.isFinished) {
            val reach = when {
                route.corner == points.lastIndex -> FINAL_REACH
                abilities.control == Control.DIRECT -> maxOf(FINAL_REACH, abilities.horizontalSpeed)
                isSharp(points, route.corner) -> SHARP_REACH
                else -> PASS_REACH
            }
            if (!position.closerThan(points[route.corner], reach)) {
                break
            }
            route.corner++
        }

        if (route.corner != start) {
            route.resetProgress()
        }
    }

    /** A path being flown, the [corner] flown to next and the progress towards it. */
    private class Route(val path: Path) {
        val points = path.points
        var corner = 1
        var bestDistance = Double.MAX_VALUE
        var idleTicks = 0

        val isFinished: Boolean
            get() = corner >= points.size

        fun resetProgress() {
            bestDistance = Double.MAX_VALUE
            idleTicks = 0
        }
    }

    private companion object {
        const val LOOKAHEAD = 14.0
        const val MOMENTUM_TOLERANCE = 0.25
        const val SINK_TOLERANCE = 0.8
        const val SPRINT_DISTANCE = 3.0
        const val FINAL_REACH = 0.5
        const val SHARP_REACH = 1.0
        const val PASS_REACH = 2.0
        const val OFF_PATH = 2.5
        const val MIN_PROGRESS = 0.1
        const val STUCK_TICKS = 60
        const val SHARP_TURN_COS = 0.7

        /** Whether the player stops at [corner] instead of flying through it: the last one, or a sharp turn. */
        fun isSharp(points: List<Vec3>, corner: Int): Boolean {
            if (corner <= 0 || corner >= points.lastIndex) {
                return corner >= points.lastIndex
            }

            val inbound = points[corner].subtract(points[corner - 1])
            val outbound = points[corner + 1].subtract(points[corner])
            val lengths = inbound.length() * outbound.length()
            return lengths > 1.0E-9 && inbound.dot(outbound) / lengths < SHARP_TURN_COS
        }
    }

}
