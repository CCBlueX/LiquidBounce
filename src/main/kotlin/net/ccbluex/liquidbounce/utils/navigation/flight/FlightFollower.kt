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
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.navigation.render.RouteProgress
import net.ccbluex.liquidbounce.utils.navigation.walk.FollowState
import net.ccbluex.liquidbounce.utils.navigation.walk.steer
import net.minecraft.world.phys.Vec3
import kotlin.math.min

/**
 * Flies a [FlightPath] through movement input: the keys towards a point a little ahead on the route, jump to rise
 * and sneak to sink. With momentum, the keys aim where the player would come to rest, so it slows down in time for
 * sharp corners and the end; creative flight is started with a double jump.
 */
class FlightFollower : RouteProgress {

    var path: FlightPath? = null
        private set

    /**
     * The corner flown to next.
     */
    private var index = 1

    /**
     * Where the player heads, updated every tick.
     */
    var target: Vec3? = null
        private set

    /**
     * Whether the movement of this tick wants to sprint.
     */
    var sprint = false
        private set

    private var takeOffTicks = 0
    private var bestDistance = Double.MAX_VALUE
    private var idleTicks = 0

    val isFinished: Boolean
        get() = path.let { it != null && index >= it.points.size }

    fun follow(path: FlightPath) {
        this.path = path
        index = 1
        resetProgress()
    }

    fun clear() {
        path = null
        index = 1
        target = null
        sprint = false
        takeOffTicks = 0
        resetProgress()
    }

    /**
     * Whether the player drifted away from the line being flown.
     */
    fun isOffPath(): Boolean {
        val points = path?.points ?: return false
        if (index >= points.size) {
            return false
        }

        return distanceToSegment(player.position(), points[index - 1], points[index]) > OFF_PATH
    }

    fun tick(event: MovementInputEvent, frameYaw: Float, abilities: FlightAbilities): FollowState {
        target = null
        sprint = false

        val path = path
        path?.let { advance(it, abilities) }
        return when {
            path == null || index >= path.points.size -> {
                event.directionalInput = DirectionalInput.NONE
                event.jump = false
                event.sneak = false
                FollowState.FINISHED
            }

            abilities.control == FlightControl.MOMENTUM && !player.abilities.flying -> {
                takeOff(event)
                FollowState.WALKING
            }

            else -> {
                takeOffTicks = 0
                val aim = aim(path, abilities)
                target = aim
                fly(event, frameYaw, aim, abilities, isFinal = aim == path.end)
                progress(path)
            }
        }
    }

    private fun progress(path: FlightPath): FollowState {
        val distance = player.position().distanceTo(path.points[index])
        if (distance < bestDistance - PROGRESS) {
            bestDistance = distance
            idleTicks = 0
        } else if (++idleTicks >= STUCK_TICKS) {
            resetProgress()
            return FollowState.STUCK
        }
        return FollowState.WALKING
    }

    /**
     * Presses the keys that bring the point the player comes to rest at, with the keys released, towards [aim].
     */
    private fun fly(
        event: MovementInputEvent,
        frameYaw: Float,
        aim: Vec3,
        abilities: FlightAbilities,
        isFinal: Boolean,
    ) {
        val momentum = abilities.control == FlightControl.MOMENTUM
        val velocity = player.deltaMovement
        val horizontalCoast = if (momentum) 1.0 / (1.0 - FlightAbilities.HORIZONTAL_DRAG) else 1.0
        val verticalCoast = if (momentum) 1.0 / (1.0 - FlightAbilities.VERTICAL_DRAG) else 1.0
        val rest = player.position().add(velocity.x * horizontalCoast, velocity.y * verticalCoast,
            velocity.z * horizontalCoast)
        val error = aim.subtract(rest)

        val horizontalTolerance = if (momentum) MOMENTUM_TOLERANCE else abilities.horizontalSpeed / 2.0
        val riseTolerance = if (momentum) MOMENTUM_TOLERANCE else abilities.verticalSpeed / 2.0
        // Sinking stops short of the ground on the way, as touching it ends creative flight.
        val sinkTolerance = if (momentum && !isFinal) SINK_TOLERANCE else riseTolerance

        val moves = error.horizontalDistanceSqr() > horizontalTolerance * horizontalTolerance
        event.directionalInput = if (moves) steer(error, frameYaw) else DirectionalInput.NONE
        event.jump = error.y > riseTolerance
        event.sneak = error.y < -sinkTolerance && !event.jump
        sprint = moves && error.horizontalDistance() > SPRINT_DISTANCE
    }

    /**
     * Jump, let go and jump again, which starts creative flight.
     */
    private fun takeOff(event: MovementInputEvent) {
        event.directionalInput = DirectionalInput.NONE
        event.jump = takeOffTicks % 2 == 0
        event.sneak = false
        takeOffTicks++
    }

    /**
     * The point to head for: [LOOKAHEAD] blocks ahead along the route with momentum, else the next corner, never
     * past a corner the route turns sharply at.
     */
    private fun aim(path: FlightPath, abilities: FlightAbilities): Vec3 {
        val points = path.points
        if (abilities.control != FlightControl.MOMENTUM) {
            return points[index]
        }

        var remaining = LOOKAHEAD
        var from = player.position()
        for (corner in index until points.size) {
            val point = points[corner]
            val length = from.distanceTo(point)
            if (length >= remaining) {
                return from.add(point.subtract(from).scale(remaining / length))
            }
            if (isSharp(points, corner)) {
                return point
            }
            remaining -= length
            from = point
        }
        return points.last()
    }

    private fun advance(path: FlightPath, abilities: FlightAbilities) {
        val points = path.points
        val position = player.position()
        val start = index
        while (index < points.size) {
            val reach = when {
                index == points.lastIndex -> FINAL_REACH
                abilities.control == FlightControl.DIRECT -> maxOf(FINAL_REACH, abilities.horizontalSpeed)
                isSharp(points, index) -> SHARP_REACH
                else -> PASS_REACH
            }
            if (position.distanceTo(points[index]) >= reach) {
                break
            }
            index++
        }

        if (index != start) {
            resetProgress()
        }
    }

    private fun resetProgress() {
        bestDistance = Double.MAX_VALUE
        idleTicks = 0
    }

    override val isMoving: Boolean
        get() = path != null && !isFinished

    override fun head(partialTicks: Float): Vec3 = player.interpolateCurrentPosition(partialTicks)

    override val ahead: List<Vec3>
        get() {
            val points = path?.points ?: return emptyList()
            return points.subList(min(index, points.size), points.size)
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
        const val PROGRESS = 0.1
        const val STUCK_TICKS = 60

        /**
         * Cosine of the turn above which the player stops at a corner instead of flying through it.
         */
        const val SHARP_TURN = 0.7

        fun isSharp(points: List<Vec3>, corner: Int): Boolean {
            if (corner <= 0 || corner >= points.lastIndex) {
                return corner >= points.lastIndex
            }

            val inbound = points[corner].subtract(points[corner - 1])
            val outbound = points[corner + 1].subtract(points[corner])
            val lengths = inbound.length() * outbound.length()
            return lengths > 1.0E-9 && inbound.dot(outbound) / lengths < SHARP_TURN
        }

        fun distanceToSegment(point: Vec3, from: Vec3, to: Vec3): Double {
            val segment = to.subtract(from)
            val lengthSqr = segment.lengthSqr()
            if (lengthSqr < 1.0E-9) {
                return point.distanceTo(from)
            }

            val t = (point.subtract(from).dot(segment) / lengthSqr).coerceIn(0.0, 1.0)
            return point.distanceTo(from.add(segment.scale(t)))
        }
    }

}
