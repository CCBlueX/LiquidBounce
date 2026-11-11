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
import net.ccbluex.liquidbounce.features.client.clientState
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer.SimulatedPlayerInput
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.math.distanceToSegment
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceTo
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceToSqr
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.navigation.render.RouteProgress
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.minecraft.core.BlockPos
import net.minecraft.tags.FluidTags
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.FarmlandBlock
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.min

enum class FollowState {
    WALKING,

    /** Breaking, placing or opening a block of the current step. */
    ACTING,
    FINISHED,

    /** The current step made no progress for too long. */
    STUCK,
}

/** Walks a [WalkPath] step by step through movement input, mining, placing and opening with [actions] on the way. */
@Suppress("TooManyFunctions")
class WalkFollower(private val actions: WalkActions) : RouteProgress {

    var path: WalkPath? by clientState { null }
        private set
    private var index by clientState { 0 }

    private val step: WalkStep?
        get() = path?.steps?.getOrNull(index)

    val isFinished: Boolean
        get() = path != null && step == null

    /** The block the step given up last leads into. */
    var stuckAt: BlockPos? by clientState { null }
        private set

    private var movement by clientState { Movement.NONE }

    val target get() = movement.target
    val sprint get() = movement.sprint
    val dive get() = movement.dive
    val isPlainWalk get() = movement.isPlainWalk

    /** Whether this step or the next mines or places blocks, which aims at them in between the walking. */
    val building: Boolean
        get() = step?.builds == true || path?.steps?.getOrNull(index + 1)?.builds == true

    var remainingBreaks by clientState { emptyList<BlockPos>() }
        private set
    var remainingPlaces by clientState { emptyList<BlockPos>() }
        private set

    private var progress by clientState { Progress() }
    private var breathing by clientState { false }

    /** Where the sprint hop in the air was aimed; the steps it passes over lie behind the player. */
    private var hopAim: Vec3? by clientState { null }

    private val Player.isStable: Boolean
        get() = onGround() || isInWater || onClimbable()

    private val isStable: Boolean
        get() = player.isStable

    fun follow(path: WalkPath, startIndex: Int) {
        this.path = path
        index = startIndex
        stepChanged()
    }

    fun clear() {
        path = null
        index = 0
        movement = Movement.NONE
        hopAim = null
        stepChanged()
    }

    /** Only checked while the player stands, swims or climbs, as jumps and drops leave the path on purpose. */
    fun isOffPath(): Boolean {
        val path = path ?: return false
        val step = step ?: return false
        if (!isStable) {
            return false
        }

        val position = player.position()
        val points = path.points
        val from = points[index]
        if (position.y < min(from.y, step.feet.y) - FALL_BELOW) {
            return true
        }

        // A jump may first walk back to its run-up.
        val runUp = step.jump?.runUp
        if (runUp != null && position.distanceToSegment(runUp, from) <= OFF_PATH) {
            return false
        }

        // Shortcuts cut the corners of plain walks ahead.
        for (i in index until min(points.size - 1, index + SHORTCUT_STEPS)) {
            if (position.distanceToSegment(points[i], points[i + 1]) <= OFF_PATH) {
                return false
            }
            if (!path.steps[i].isPlain) {
                break
            }
        }
        return true
    }

    fun tick(event: MovementInputEvent, frameYaw: Float): FollowState {
        movement = Movement.NONE
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
        progress.acting = null
        movement = move(event, frameYaw, step)

        // A jump moves away from its target while walking back to the run-up, so it is only timed.
        val stuck = if (step.jump != null) {
            ++progress.jumpTicks >= JUMP_TICKS || progress.leap.hopeless
        } else {
            progress.stalls(player.position().distanceTo(step.feet)) ||
                progress.isDueToPredict && !getsAhead(event, frameYaw, step)
        }
        return if (stuck) giveUp(step) else FollowState.WALKING
    }

    /**
     * Whether walking on as in this tick gets closer to where it aims, moved as the game moves the player.
     * A walk that the terrain holds up is given up right away instead of after [STUCK_TICKS].
     */
    private fun getsAhead(event: MovementInputEvent, frameYaw: Float, step: WalkStep): Boolean {
        val aim = movement.target?.takeUnless { movement.dive } ?: return true
        val sprint = movement.sprint
        val body = SimulatedPlayer.fromClientPlayer(
            SimulatedPlayerInput(event.directionalInput, event.jump, sprint, event.sneak)
        )
        body.yRot = frameYaw

        val closer = player.position().distanceTo(aim) - PROGRESS
        repeat(PREDICT_TICKS) {
            body.tick()
            val position = body.position()
            if (position.distanceTo(aim) < closer) {
                return true
            }
            body.input = SimulatedPlayerInput(
                centerInput(position, aim, frameYaw), wantsJump(step, body), sprint, event.sneak,
            )
        }
        return false
    }

    private fun place(event: MovementInputEvent, frameYaw: Float, step: WalkStep, pos: BlockPos) {
        val position = player.position()
        val pillars = step.move == WalkMove.PILLAR
        standStill(event)
        when {
            pillars -> {
                event.directionalInput = centerInput(position, step.feet, frameYaw)
                event.jump = player.onGround()
            }

            // Bridging: lean over the edge, so the side of the block below is in sight.
            pos.y < step.from.y -> {
                event.directionalInput = steer(step.feet.subtract(position), frameYaw)
                event.sneak = true
                movement = Movement(step.feet)
            }
        }
        actions.place(pos, ready = !pillars || position.y >= pos.y + 1.0)
    }

    private fun act(pos: BlockPos, step: WalkStep) =
        if (progress.actsTooLong(pos)) giveUp(step) else FollowState.ACTING

    private fun giveUp(step: WalkStep): FollowState {
        stuckAt = step.to
        progress = Progress()
        return FollowState.STUCK
    }

    private fun move(event: MovementInputEvent, frameYaw: Float, step: WalkStep): Movement {
        // Air is filled up before a dive, and during one once it runs low.
        if (breathes() || step.underwater && isShortOfAir()) {
            standStill(event)
            progress.idleTicks = 0
            return Movement.NONE
        }
        if (step.underwater) {
            return swim(event, frameYaw, step)
        }

        val jump = step.jump
        val abilities = path?.abilities
        if (jump != null && abilities != null) {
            val leap = progress.leap
            leap.tick(event, frameYaw, step, jump, abilities)
            return Movement(leap.target, leap.sprint)
        }

        val position = player.position()
        if (player.onGround()) {
            hopAim = null
        }
        val aim = shortcut(step) ?: hopAim ?: step.feet

        event.directionalInput = centerInput(position, aim, frameYaw)
        event.sneak = step.sneak
        event.jump = wantsJump(step, player) || startsHop(position, aim, step.feet)
        return Movement(aim, sprint = !step.sneak, isPlainWalk = step.isLevelWalk)
    }

    private fun wantsJump(step: WalkStep, body: Player): Boolean {
        val keepsLevel = step.to.y >= step.from.y
        return when (step.move) {
            WalkMove.SWIM -> keepsLevel
            WalkMove.CLIMB_UP -> true
            WalkMove.CLIMB_DOWN, WalkMove.FALL, WalkMove.DOWNWARD -> false
            else -> {
                val position = body.position()
                val rises = step.feet.y - position.y > NavigationTerrain.STEP + RISE_MARGIN
                val inReach = position.horizontalDistanceTo(step.feet) < JUMP_REACH
                (rises && inReach && body.isStable) || (body.isInWater && keepsLevel)
            }
        }
    }

    private fun startsHop(position: Vec3, aim: Vec3, feet: Vec3): Boolean {
        if (aim === feet || !canHop(position, aim)) {
            return false
        }
        hopAim = aim
        return true
    }

    /** Whether to jump while sprinting straight on to [aim], which is faster than running. */
    private fun canHop(position: Vec3, aim: Vec3): Boolean {
        val distance = position.horizontalDistanceTo(aim)
        val able = player.onGround() && player.isSprinting && player.foodData.foodLevel > HOP_FOOD
        if (!able || distance < HOP_DISTANCE) {
            return false
        }

        val terrain = NavigationTerrain.live()
        val dx = (aim.x - position.x) / distance
        val dz = (aim.z - position.z) / distance
        var travelled = 0.0
        while (travelled <= HOP_LENGTH) {
            val x = position.x + dx * travelled
            val z = position.z + dz * travelled
            // The landing would trample farmland.
            val floor = terrain.state(Mth.floor(x), Mth.floor(position.y - FLOOR_PROBE), Mth.floor(z))
            if (!terrain.isClear(terrain.box(x, position.y + HOP_HEIGHT, z)) || floor.block is FarmlandBlock) {
                return false
            }
            travelled += HOP_SAMPLE
        }
        return true
    }

    /** Sprinting under water swims in the pose that also fits through gaps one block high. */
    private fun swim(event: MovementInputEvent, frameYaw: Float, step: WalkStep): Movement {
        val position = player.position()
        val target = step.feet.add(0.0, SWIM_LEVEL, 0.0)
        val rise = target.y - position.y

        event.directionalInput = centerInput(position, target, frameYaw)
        event.jump = rise > SWIM_TOLERANCE
        event.sneak = rise < -SWIM_TOLERANCE
        return Movement(target, sprint = true, dive = true)
    }

    private fun isShortOfAir() = !player.isEyeInFluid(FluidTags.WATER) && player.airSupply < player.maxAirSupply

    /** Whether to come up for air and stay until it is full, which a long swim close under the surface needs. */
    private fun breathes(): Boolean {
        breathing = when {
            !player.isInWater || player.airSupply >= player.maxAirSupply -> false
            player.airSupply < player.maxAirSupply * LOW_AIR -> isAirAbove()
            else -> breathing
        }
        return breathing
    }

    private fun isAirAbove(): Boolean {
        val head = BlockPos.containing(player.eyePosition)
        val chunk = world.getChunk(head)
        for (dy in 0..AIR_SEARCH) {
            val pos = head.above(dy)
            val state = chunk.getBlockState(pos)
            if (state.fluidState.isEmpty) {
                return state.getCollisionShape(chunk, pos).isEmpty
            }
        }
        return false
    }

    private fun centerInput(position: Vec3, feet: Vec3, frameYaw: Float): DirectionalInput =
        if (position.horizontalDistanceTo(feet) > CENTER_TOLERANCE) {
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

    /** A shortcut walks by the blocks of plain steps: standing in the block of a later one, those before are done. */
    private fun skipAhead(path: WalkPath) {
        if (!player.onGround()) {
            return
        }

        val block = BlockPos.containing(player.position())
        for (i in index until min(path.steps.size, index + SHORTCUT_STEPS)) {
            val step = path.steps[i]
            if (!step.isPlain) {
                return
            }
            if (step.to == block) {
                index = i + 1
                return
            }
        }
    }

    /**
     * The first point of the path at least [distance] from [position], horizontally, within a few steps from [target]
     * if that is a point ahead, else from the step walked; null when the path stays closer.
     */
    fun lookAhead(target: Vec3, position: Vec3, distance: Double): Vec3? {
        val points = path?.points ?: return null
        val end = min(points.size, index + 1 + SHORTCUT_STEPS)
        val from = (index + 1 until end).firstOrNull { points[it] == target } ?: (index + 1)
        for (i in from until min(points.size, from + LOOK_STEPS)) {
            if (points[i].horizontalDistanceToSqr(position) >= distance * distance) {
                return points[i]
            }
        }
        return null
    }

    /** The farthest of the next points a straight walk gets to, while every step up to it is a plain walk. */
    private fun shortcut(step: WalkStep): Vec3? {
        val path = path ?: return null
        if (!step.isPlain || !player.onGround()) {
            return null
        }

        val steps = path.steps
        var end = index
        while (end + 1 < min(steps.size, index + SHORTCUT_STEPS) && steps[end + 1].isPlain) {
            end++
        }

        val terrain = NavigationTerrain.live()
        val position = player.position()
        for (i in end downTo index + 1) {
            val point = steps[i].feet
            if (isStraightWalkable(terrain, position, point, SHORTCUT_STOP, path.abilities.safeFall)) {
                return point
            }
        }
        return null
    }

    private fun isReached(step: WalkStep, next: WalkStep?): Boolean {
        if (!step.breaks.all(::isCleared) || !step.places.all(::isPlaced) || !step.opens.all(::isOpen)) {
            return false
        }

        val position = player.position()
        val block = BlockPos.containing(position)
        // In the air above its target, the landing of a jump still needs steering.
        if (step.jump != null) {
            val support = player.mainSupportingBlockPos.orElse(null)
            return player.onClimbable() && block == step.to ||
                player.onGround() && (block == step.to || support == step.to || support == step.to.below())
        }

        val close = position.horizontalDistanceTo(step.feet) < REACH_HORIZONTAL
        if (close && abs(position.y - step.feet.y) < REACH_VERTICAL) {
            return true
        }

        // On straight runs, entering the block is enough; turns wait for the center so corners are not cut.
        return isStable && next != null && block == step.to && continuesStraight(step, next)
    }

    private fun continuesStraight(step: WalkStep, next: WalkStep) =
        next.to.x - next.from.x == step.to.x - step.from.x && next.to.z - next.from.z == step.to.z - step.from.z &&
            (step.to.x != step.from.x || step.to.z != step.from.z)

    private fun stepChanged() {
        progress = Progress()
        val remaining = path?.steps.orEmpty().drop(index)
        remainingBreaks = remaining.flatMap(WalkStep::breaks)
        remainingPlaces = remaining.flatMap(WalkStep::places)
    }

    private fun isCleared(pos: BlockPos) = world.getBlockState(pos).getCollisionShape(world, pos).isEmpty

    private fun isPlaced(pos: BlockPos) = !world.getBlockState(pos).canBeReplaced()

    /** Anything else that took the place of the door, trapdoor or fence gate counts as open. */
    private fun isOpen(pos: BlockPos): Boolean =
        world.getBlockState(pos).getOptionalValue(BlockStateProperties.OPEN).orElse(true)

    private val WalkStep.builds: Boolean
        get() = breaks.isNotEmpty() || places.isNotEmpty()

    private val WalkStep.isLevelWalk: Boolean
        get() = (move == WalkMove.TRAVERSE || move == WalkMove.DIAGONAL) && !sneak

    private val WalkStep.isPlain: Boolean
        get() = isLevelWalk && jump == null && !builds && opens.isEmpty() && !underwater

    override val isMoving: Boolean
        get() = step != null

    override fun head(partialTicks: Float): Vec3 = player.interpolateCurrentPosition(partialTicks)

    override val ahead: List<Vec3>
        get() {
            val points = path?.points ?: return emptyList()
            return points.subList(min(index + 1, points.size), points.size)
        }

    private class Movement(
        val target: Vec3? = null,
        val sprint: Boolean = false,

        /** Swims under water towards [target], which also lies higher or lower. */
        val dive: Boolean = false,

        /** A plain walk other features may add to, such as an extra jump. */
        val isPlainWalk: Boolean = false,
    ) {
        companion object {
            val NONE = Movement()
        }
    }

    /** A new one with every step. */
    private class Progress {
        val leap = WalkLeap()
        var jumpTicks = 0
        var idleTicks = 0
        private var bestDistance = Double.MAX_VALUE
        var acting: BlockPos? = null
        private var actingTicks = 0

        fun stalls(distance: Double): Boolean {
            if (distance < bestDistance - PROGRESS) {
                bestDistance = distance
                idleTicks = 0
                return false
            }
            return ++idleTicks >= STUCK_TICKS
        }

        val isDueToPredict: Boolean
            get() = idleTicks > 0 && idleTicks % PREDICT_INTERVAL == 0

        fun actsTooLong(pos: BlockPos): Boolean {
            if (pos != acting) {
                acting = pos
                actingTicks = 0
                return false
            }
            return ++actingTicks >= ACTION_TICKS
        }
    }

    private companion object {
        const val REACH_HORIZONTAL = 0.35
        const val REACH_VERTICAL = 0.6
        const val CENTER_TOLERANCE = 0.1
        const val RISE_MARGIN = 0.01
        const val JUMP_REACH = 1.5
        const val OFF_PATH = 1.5

        /** A block down from the path counts as off it. */
        const val FALL_BELOW = 0.9
        const val PROGRESS = 0.1
        const val STUCK_TICKS = 40

        /** Without progress, the way on is predicted every so many ticks, this far ahead. */
        const val PREDICT_INTERVAL = 5
        const val PREDICT_TICKS = 20
        const val ACTION_TICKS = 60

        /** Walking back to the run-up included. */
        const val JUMP_TICKS = 120

        /** Above the feet block: the middle of the room a swimming player has in a gap one block high. */
        const val SWIM_LEVEL = 0.2
        const val SWIM_TOLERANCE = 0.15
        const val LOW_AIR = 0.3
        const val AIR_SEARCH = 4

        const val SHORTCUT_STEPS = 8
        const val SHORTCUT_STOP = 0.25
        const val LOOK_STEPS = 4

        const val HOP_DISTANCE = 4.0
        const val HOP_FOOD = 10
        const val HOP_LENGTH = 3.5
        const val HOP_HEIGHT = 1.25
        const val HOP_SAMPLE = 0.5
        const val FLOOR_PROBE = 0.5
    }

}
