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
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.navigation.render.RouteProgress
import net.ccbluex.liquidbounce.utils.navigation.terrain.LiveWorld
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.minecraft.core.BlockPos
import net.minecraft.tags.FluidTags
import net.minecraft.util.Mth
import net.minecraft.world.level.block.FarmlandBlock
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
     * Whether the movement of this tick swims under water towards [target], which also lies higher or lower.
     */
    var dive = false
        private set

    /**
     * Whether the player is coming up for air.
     */
    private var breathing = false

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
    private var stepTicks = 0
    private val leap = WalkLeap()

    /**
     * Where the sprint hop in the air was aimed.
     */
    private var hopAim: Vec3? = null

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
        dive = false
        isPlainWalk = false
        hopAim = null
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

        // A jump may first walk back to its run-up.
        val runUp = step.jump?.runUp
        if (runUp != null && distanceToSegment(position, runUp, from) <= OFF_PATH) {
            return false
        }

        // Shortcuts cut the corners of plain walks ahead.
        val points = path.points
        for (i in index until min(points.size - 1, index + SHORTCUT_STEPS)) {
            if (distanceToSegment(position, points[i], points[i + 1]) <= OFF_PATH) {
                return false
            }
            if (!path.steps[i].isPlain) {
                break
            }
        }
        return true
    }

    fun tick(event: MovementInputEvent, frameYaw: Float): FollowState {
        target = null
        sprint = false
        dive = false
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

        // A jump moves away from its target while walking back to the run-up.
        if (step.jump != null) {
            return if (++stepTicks >= JUMP_TICKS || leap.hopeless) giveUp(step) else FollowState.WALKING
        }

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
        if (breathes()) {
            standStill(event)
            sprint = false
            idleTicks = 0
            return
        }
        if (step.underwater) {
            swim(event, frameYaw, step)
            return
        }

        val jump = step.jump
        val path = path
        if (jump != null && path != null) {
            leap.tick(event, frameYaw, step, jump, path.abilities)
            target = leap.target
            sprint = leap.sprint
            return
        }

        val position = player.position()
        val feet = step.feet
        val horizontal = horizontalDistance(position, feet)
        val onGround = player.onGround()
        val inWater = player.isInWater
        val climbing = player.onClimbable()
        val keepsLevel = step.to.y >= step.from.y
        val aim = aim(step, onGround)

        target = aim
        event.directionalInput = centerInput(position, aim, frameYaw)
        event.sneak = step.sneak
        sprint = !step.sneak

        event.jump = when (step.move) {
            WalkMove.SWIM -> keepsLevel
            WalkMove.CLIMB_UP -> true
            WalkMove.CLIMB_DOWN, WalkMove.FALL, WalkMove.DOWNWARD -> false
            else -> {
                val rises = feet.y - position.y > NavigationTerrain.STEP + RISE_MARGIN
                (rises && horizontal < JUMP_REACH && (onGround || inWater || climbing)) || (inWater && keepsLevel)
            }
        } || startsHop(position, aim, feet)

        isPlainWalk = (step.move == WalkMove.TRAVERSE || step.move == WalkMove.DIAGONAL) && !step.sneak
    }

    /**
     * Where to walk for [step]: a shortcut ahead or the step itself; in the air, the steps a sprint hop passes over
     * lie behind the player, so it keeps to where the hop was aimed.
     */
    private fun aim(step: WalkStep, onGround: Boolean): Vec3 {
        if (onGround) {
            hopAim = null
        }
        return shortcut(step) ?: hopAim ?: step.feet
    }

    private fun startsHop(position: Vec3, aim: Vec3, feet: Vec3): Boolean {
        if (aim === feet || !canHop(position, aim)) {
            return false
        }
        hopAim = aim
        return true
    }

    /**
     * Whether to jump while sprinting straight on to [aim], which is faster than running: far enough, with food to
     * spare, room for the head all along the jump and no farmland the landing would trample.
     */
    private fun canHop(position: Vec3, aim: Vec3): Boolean {
        val distance = horizontalDistance(position, aim)
        if (!player.onGround() || !player.isSprinting || distance < HOP_DISTANCE ||
            player.foodData.foodLevel <= HOP_FOOD) {
            return false
        }

        val terrain = NavigationTerrain(LiveWorld(world))
        val dx = (aim.x - position.x) / distance
        val dz = (aim.z - position.z) / distance
        var travelled = 0.0
        while (travelled <= HOP_LENGTH) {
            val x = position.x + dx * travelled
            val z = position.z + dz * travelled
            val floor = terrain.state(Mth.floor(x), Mth.floor(position.y - FLOOR_PROBE), Mth.floor(z))
            if (!terrain.isClear(terrain.box(x, position.y + HOP_HEIGHT, z)) || floor.block is FarmlandBlock) {
                return false
            }
            travelled += HOP_SAMPLE
        }
        return true
    }

    /**
     * Swims towards the step: sprinting, which under water turns into the swimming pose that also fits through gaps
     * one block high, jumping to rise and sneaking to sink. Before going under, it breathes until the air is full.
     */
    private fun swim(event: MovementInputEvent, frameYaw: Float, step: WalkStep) {
        if (!player.isEyeInFluid(FluidTags.WATER) && player.airSupply < player.maxAirSupply) {
            standStill(event)
            idleTicks = 0
            return
        }

        val position = player.position()
        val target = step.feet.add(0.0, SWIM_LEVEL, 0.0)
        val rise = target.y - position.y

        this.target = target
        dive = true
        sprint = true
        event.directionalInput = centerInput(position, target, frameYaw)
        event.jump = rise > SWIM_TOLERANCE
        event.sneak = rise < -SWIM_TOLERANCE
    }

    /**
     * Whether to come up for air and stay until the air is full, which a long swim close under the surface needs
     * whatever the route counted on.
     */
    private fun breathes(): Boolean {
        if (!player.isInWater || player.airSupply >= player.maxAirSupply) {
            breathing = false
            return false
        }
        if (player.airSupply < player.maxAirSupply * LOW_AIR) {
            breathing = isAirAbove()
        }
        return breathing
    }

    /**
     * Whether open air is a short swim straight up from the head.
     */
    private fun isAirAbove(): Boolean {
        val head = BlockPos.containing(player.eyePosition)
        for (dy in 0..AIR_SEARCH) {
            val pos = head.above(dy)
            val state = world.getBlockState(pos)
            if (state.fluidState.isEmpty) {
                return state.getCollisionShape(world, pos).isEmpty
            }
        }
        return false
    }

    private fun centerInput(position: Vec3, feet: Vec3, frameYaw: Float): DirectionalInput =
        if (horizontalDistance(position, feet) > CENTER_TOLERANCE) {
            steer(feet.subtract(position), frameYaw)
        } else {
            DirectionalInput.NONE
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
        skipAhead(path)

        if (index != start) {
            stepChanged()
        }
    }

    /**
     * Moves on past plain steps whose blocks a shortcut walked by: when the player stands in the block of a later
     * plain step, the ones before it are done.
     */
    private fun skipAhead(path: WalkPath) {
        if (!player.onGround()) {
            return
        }

        val feet = BlockPos.containing(player.position())
        val end = min(path.steps.size, index + SHORTCUT_STEPS)
        for (i in index until end) {
            val step = path.steps[i]
            if (!step.isPlain) {
                return
            }
            if (step.to == feet) {
                index = i + 1
                return
            }
        }
    }

    /**
     * The farthest of the next points a straight walk from the player gets to, while every step up to it is a plain
     * walk on one level; null when [step] is not one or the player is in the air.
     */
    private fun shortcut(step: WalkStep): Vec3? {
        val path = path ?: return null
        if (!step.isPlain || !player.onGround()) {
            return null
        }

        var end = index
        while (end + 1 < min(path.steps.size, index + SHORTCUT_STEPS) && path.steps[end + 1].isPlain) {
            end++
        }

        val terrain = NavigationTerrain(LiveWorld(world))
        val position = player.position()
        for (i in end downTo index + 1) {
            val point = path.steps[i].feet
            if (isStraightWalkable(terrain, position, point, SHORTCUT_STOP, path.abilities.safeFall)) {
                return point
            }
        }
        return null
    }

    private val WalkStep.isPlain: Boolean
        get() = (move == WalkMove.TRAVERSE || move == WalkMove.DIAGONAL) && !sneak && jump == null &&
            breaks.isEmpty() && places.isEmpty() && opens.isEmpty() && !underwater

    private fun isReached(step: WalkStep, next: WalkStep?): Boolean {
        val pending = step.breaks.any { !isCleared(it) } || step.places.any { !isPlaced(it) } ||
            step.opens.any { !isOpen(it) }
        if (pending) {
            return false
        }

        val position = player.position()
        // A jump is done once it stands on its target, wherever on it, or holds on to the ladder it jumped to; in the
        // air above it, the landing still needs steering.
        if (step.jump != null) {
            val support = player.mainSupportingBlockPos.orElse(null)
            return player.onClimbable() && BlockPos.containing(position) == step.to ||
                player.onGround() && (BlockPos.containing(position) == step.to || support == step.to ||
                    support == step.to.below())
        }

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
        stepTicks = 0
        leap.clear()
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

        /**
         * Height above the feet block a swimmer heads for, the middle of the room a swimming player has in a gap one
         * block high.
         */
        const val SWIM_LEVEL = 0.2
        const val LOW_AIR = 0.3
        const val AIR_SEARCH = 4
        const val SWIM_TOLERANCE = 0.15
        const val JUMP_REACH = 1.5
        const val OFF_PATH = 1.5

        /**
         * How far below the path the player may stand before the route is worked out again; a block down counts.
         */
        const val FALL_BELOW = 0.9
        const val PROGRESS = 0.1
        const val STUCK_TICKS = 40
        const val ACTION_TICKS = 60

        /**
         * Longest a jump may take, walking back to its run-up included.
         */
        const val JUMP_TICKS = 120

        /**
         * Plain steps ahead a shortcut may skip, and how close to its end a straight walk has to get.
         */
        const val SHORTCUT_STEPS = 8
        const val SHORTCUT_STOP = 0.25

        /**
         * Sprint hops: the least distance to the aim, the food kept, and how far and high the head is checked for room.
         */
        const val HOP_DISTANCE = 4.0
        const val HOP_FOOD = 10
        const val HOP_LENGTH = 3.5
        const val HOP_HEIGHT = 1.25
        const val HOP_SAMPLE = 0.5
        const val FLOOR_PROBE = 0.5

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
