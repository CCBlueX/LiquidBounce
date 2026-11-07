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

package net.ccbluex.liquidbounce.utils.navigation.walk

import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.navigation.render.RouteProgress
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

enum class FollowState {
    WALKING,

    /**
     * Breaking, placing or opening a block of the current step.
     */
    ACTING,

    /**
     * The last step is done.
     */
    FINISHED,

    /**
     * The current step made no progress for too long; see [WalkFollower.stuckAt].
     */
    STUCK,
}

/**
 * Walks a [WalkPath] step by step through movement input, mining, placing and opening with [actions] on the way.
 */
@Suppress("TooManyFunctions")
class WalkFollower(private val actions: WalkActions) : RouteProgress {

    var path: WalkPath? = null
        private set

    private var index = 0

    val step: WalkStep?
        get() = path?.steps?.getOrNull(index)

    val isFinished: Boolean
        get() = path.let { it != null && index >= it.steps.size }

    /**
     * The block the step given up last leads into.
     */
    var stuckAt: BlockPos? = null
        private set

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

    /**
     * Whether the movement of this tick is a plain walk other features may add to, such as an extra jump.
     */
    var isPlainWalk = false
        private set

    var remainingBreaks: List<BlockPos> = emptyList()
        private set

    var remainingPlaces: List<BlockPos> = emptyList()
        private set

    private var bestDistance = Double.MAX_VALUE
    private var idleTicks = 0
    private var actionTarget: BlockPos? = null
    private var actionTicks = 0

    fun follow(path: WalkPath, startIndex: Int) {
        this.path = path
        index = startIndex
        stepChanged()
    }

    fun clear() {
        path = null
        index = 0
        target = null
        sprint = false
        isPlainWalk = false
        remainingBreaks = emptyList()
        remainingPlaces = emptyList()
        resetProgress()
    }

    /**
     * Whether the player is away from the part of the path being walked. Only checked while the player stands,
     * swims or climbs, as jumps and drops leave the line on purpose.
     */
    fun isOffPath(): Boolean {
        val path = path ?: return false
        val step = step ?: return false
        if (!player.onGround() && !player.isInWater && !player.onClimbable()) {
            return false
        }

        val position = player.position()
        val from = path.points[index]
        if (position.y < min(from.y, step.feet.y) - FALL_BELOW) {
            return true
        }

        return distanceToSegment(position, from, step.feet) > OFF_PATH
    }

    fun tick(event: MovementInputEvent, frameYaw: Float): FollowState {
        target = null
        sprint = false
        isPlainWalk = false

        advance()
        val step = step ?: run {
            actions.idle()
            standStill(event)
            return FollowState.FINISHED
        }

        val pendingBreak = step.breaks.firstOrNull { !isCleared(it) }
        val pendingPlace = step.places.firstOrNull { !isPlaced(it) }
        val pendingOpen = step.opens.firstOrNull { !isOpen(it) }
        return when {
            pendingBreak != null -> {
                standStill(event)
                actions.mine(pendingBreak)
                act(pendingBreak, step)
            }

            pendingPlace != null -> {
                place(event, frameYaw, step, pendingPlace)
                act(pendingPlace, step)
            }

            pendingOpen != null -> {
                standStill(event)
                // Climbing on keeps the player at a hatch above.
                event.jump = player.isInWater || player.onClimbable()
                actions.open(pendingOpen)
                act(pendingOpen, step)
            }

            else -> walk(event, frameYaw, step)
        }
    }

    private fun walk(event: MovementInputEvent, frameYaw: Float, step: WalkStep): FollowState {
        actions.idle()
        actionTarget = null
        move(event, frameYaw, step)

        val distance = player.position().distanceTo(step.feet)
        if (distance < bestDistance - PROGRESS) {
            bestDistance = distance
            idleTicks = 0
        } else if (++idleTicks >= STUCK_TICKS) {
            return giveUp(step)
        }

        return FollowState.WALKING
    }

    private fun place(event: MovementInputEvent, frameYaw: Float, step: WalkStep, pos: BlockPos) {
        val position = player.position()
        when {
            step.move == WalkMove.PILLAR -> {
                standStill(event)
                event.directionalInput = centerInput(position, step.feet, frameYaw)
                event.jump = player.onGround()
                actions.place(pos, ready = position.y >= pos.y + 1.0)
            }

            // Bridging: lean over the edge, so the side of the block below is in sight.
            pos.y < step.from.y -> {
                standStill(event)
                event.directionalInput = steer(step.feet.subtract(position), frameYaw)
                event.sneak = true
                target = step.feet
                actions.place(pos, ready = true)
            }

            else -> {
                standStill(event)
                actions.place(pos, ready = true)
            }
        }
    }

    private fun act(pos: BlockPos, step: WalkStep): FollowState {
        if (pos != actionTarget) {
            actionTarget = pos
            actionTicks = 0
        } else if (++actionTicks >= ACTION_TICKS) {
            return giveUp(step)
        }

        return FollowState.ACTING
    }

    private fun giveUp(step: WalkStep): FollowState {
        stuckAt = step.to
        resetProgress()
        return FollowState.STUCK
    }

    private fun move(event: MovementInputEvent, frameYaw: Float, step: WalkStep) {
        val position = player.position()
        val feet = step.feet
        val horizontal = horizontalDistance(position, feet)
        val onGround = player.onGround()
        val inWater = player.isInWater
        val climbing = player.onClimbable()
        val keepsLevel = step.to.y >= step.from.y

        target = feet
        event.directionalInput = centerInput(position, feet, frameYaw)
        event.sneak = step.sneak
        sprint = !step.sneak

        event.jump = when (step.move) {
            WalkMove.PARKOUR -> onGround && wouldLeaveGround(event.directionalInput, frameYaw)

            WalkMove.SWIM -> keepsLevel
            WalkMove.CLIMB_UP -> true
            WalkMove.CLIMB_DOWN, WalkMove.FALL, WalkMove.DOWNWARD -> false
            else -> {
                val rises = feet.y - position.y > NavigationTerrain.STEP + RISE_MARGIN
                (rises && horizontal < JUMP_REACH && (onGround || inWater || climbing)) || (inWater && keepsLevel)
            }
        }

        isPlainWalk = (step.move == WalkMove.TRAVERSE || step.move == WalkMove.DIAGONAL) && !step.sneak
    }

    private fun centerInput(position: Vec3, feet: Vec3, frameYaw: Float): DirectionalInput =
        if (horizontalDistance(position, feet) > CENTER_TOLERANCE) {
            steer(feet.subtract(position), frameYaw)
        } else {
            DirectionalInput.NONE
        }

    /**
     * Whether walking on with [input] for one more tick leaves the ground, the moment a parkour jump has to start.
     */
    private fun wouldLeaveGround(input: DirectionalInput, frameYaw: Float): Boolean {
        val simulated = SimulatedPlayer.fromClientPlayer(
            SimulatedPlayer.SimulatedPlayerInput(input, jumping = false, sprinting = true, sneaking = false)
        )
        simulated.yRot = frameYaw
        simulated.tick()
        return !simulated.onGround
    }

    private fun standStill(event: MovementInputEvent) {
        event.directionalInput = DirectionalInput.NONE
        event.jump = player.isInWater
        event.sneak = false
    }

    private fun advance() {
        val path = path ?: return
        val start = index
        while (index < path.steps.size && isReached(path.steps[index], path.steps.getOrNull(index + 1))) {
            index++
        }

        if (index != start) {
            stepChanged()
        }
    }

    private fun isReached(step: WalkStep, next: WalkStep?): Boolean {
        val pending = step.breaks.any { !isCleared(it) } || step.places.any { !isPlaced(it) } ||
            step.opens.any { !isOpen(it) }
        if (pending) {
            return false
        }

        val position = player.position()
        val close = horizontalDistance(position, step.feet) < REACH_HORIZONTAL
        if (close && abs(position.y - step.feet.y) < REACH_VERTICAL) {
            return true
        }

        // On straight runs, entering the block is enough; turns wait for the center so corners are not cut.
        val stable = player.onGround() || player.isInWater || player.onClimbable()
        return stable && next != null && BlockPos.containing(position) == step.to && continuesStraight(step, next)
    }

    private fun continuesStraight(step: WalkStep, next: WalkStep) =
        next.to.x - next.from.x == step.to.x - step.from.x && next.to.z - next.from.z == step.to.z - step.from.z &&
            (step.to.x != step.from.x || step.to.z != step.from.z)

    private fun stepChanged() {
        resetProgress()
        val steps = path?.steps ?: return
        val remaining = steps.subList(min(index, steps.size), steps.size)
        remainingBreaks = remaining.flatMap(WalkStep::breaks)
        remainingPlaces = remaining.flatMap(WalkStep::places)
    }

    private fun resetProgress() {
        bestDistance = Double.MAX_VALUE
        idleTicks = 0
        actionTarget = null
        actionTicks = 0
    }

    private fun isCleared(pos: BlockPos) = world.getBlockState(pos).getCollisionShape(world, pos).isEmpty

    private fun isPlaced(pos: BlockPos) = !world.getBlockState(pos).canBeReplaced()

    /**
     * Whether the door, trapdoor or fence gate at [pos] is open; anything else that took its place counts as open.
     */
    private fun isOpen(pos: BlockPos) =
        world.getBlockState(pos).getOptionalValue(BlockStateProperties.OPEN).orElse(true)

    override val isMoving: Boolean
        get() = path != null && !isFinished

    override fun head(partialTicks: Float): Vec3 = player.interpolateCurrentPosition(partialTicks)

    override val ahead: List<Vec3>
        get() {
            val points = path?.points ?: return emptyList()
            return points.subList(min(index + 1, points.size), points.size)
        }

    private companion object {
        const val REACH_HORIZONTAL = 0.35
        const val REACH_VERTICAL = 0.6
        const val CENTER_TOLERANCE = 0.1
        const val RISE_MARGIN = 0.01
        const val JUMP_REACH = 1.5
        const val OFF_PATH = 1.5
        const val FALL_BELOW = 1.0
        const val PROGRESS = 0.1
        const val STUCK_TICKS = 40
        const val ACTION_TICKS = 60

        fun horizontalDistance(a: Vec3, b: Vec3): Double {
            val dx = a.x - b.x
            val dz = a.z - b.z
            return sqrt(dx * dx + dz * dz)
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
