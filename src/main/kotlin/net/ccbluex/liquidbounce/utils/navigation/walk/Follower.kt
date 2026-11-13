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
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer.SimulatedPlayerInput
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.math.distanceToSegment
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceTo
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceToSqr
import net.ccbluex.liquidbounce.utils.math.yaw
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.movement.getDirectionalInputTowards
import net.ccbluex.liquidbounce.utils.navigation.FollowState
import net.ccbluex.liquidbounce.utils.navigation.ProgressWatch
import net.ccbluex.liquidbounce.utils.navigation.terrain.Terrain
import net.ccbluex.liquidbounce.utils.render.route.RouteProgress
import net.minecraft.core.BlockPos
import net.minecraft.tags.FluidTags
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.FarmlandBlock
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Walks a [Path] step by step through movement input, mining, placing and opening with [actions] on the way. While
 * [placesBridgeFloors], level bridges are run across at full speed and the floor is left to that feature.
 */
@Suppress("TooManyFunctions")
class Follower(
    private val actions: BlockActions,
    private val placesBridgeFloors: () -> Boolean,
) : RouteProgress {

    var path: Path? = null
        private set
    private var index = 0

    private val step: Step?
        get() = path?.steps?.getOrNull(index)

    val isFinished: Boolean
        get() = path != null && step == null

    /** The block the step given up last leads into. */
    var stuckAt: BlockPos? = null
        private set

    private var movement = Movement.NONE

    val target get() = movement.target
    val sprint get() = movement.sprint
    val dive get() = movement.dive
    val isPlainWalk get() = movement.isPlainWalk
    val safeWalk get() = movement.safeWalk

    /** Whether this step or the next mines or places blocks, which aims at them in between the walking. */
    val building: Boolean
        get() = step?.builds == true || path?.steps?.getOrNull(index + 1)?.builds == true

    /** The bridge steps in a row from this step or the next, 0 without. */
    val bridgeAhead: Int
        get() {
            val steps = path?.steps ?: return 0
            val start = if (steps.getOrNull(index)?.isBridge == true) index else index + 1
            return runEnd(steps, start) - start
        }

    /** The line the bridge being crossed is run along, null while not crossing one. */
    private var bridgeLine: BridgeLine? = null

    /** The yaw of the bridge being crossed, null while not crossing one. */
    val bridgeHeading: Float?
        get() = bridgeLine?.heading

    /** The far end of the bridge being crossed, which the view keeps to instead of the steps ahead. */
    val bridgeTarget: Vec3?
        get() = bridgeLine?.to

    var remainingBreaks = emptyList<BlockPos>()
        private set
    var remainingPlaces = emptyList<BlockPos>()
        private set

    private var progress = Progress()
    private var breathing = false

    /** Where the sprint hop in the air was aimed; the steps it passes over lie behind the player. */
    private var hopAim: Vec3? = null

    /** Times the aim lay behind the way the player moved fast, which shows overshooting the path. */
    var overshoots = 0
        private set
    private var reversing = false

    private val Player.isStable: Boolean
        get() = onGround() || isInWater || onClimbable()

    private val isStable: Boolean
        get() = player.isStable

    fun follow(path: Path, startIndex: Int) {
        this.path = path
        index = startIndex
        stepChanged()
    }

    fun clear() {
        path = null
        index = 0
        movement = Movement.NONE
        hopAim = null
        bridgeLine = null
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
        if (step.isBridge && placesBridgeFloors()) {
            return crossBridge(event, frameYaw)
        }
        bridgeLine = null

        // A server that keeps refusing the blocks will not hold a bridge here.
        if (step.places.isNotEmpty() && actions.rejectedInRow >= MAX_REJECTIONS) {
            actions.forgetRejections()
            actions.idle()
            standStill(event)
            return giveUp(step)
        }

        val pendingBreak = step.breaks.firstOrNull { !isCleared(it) }
        val pendingPlace = step.places.firstOrNull { !isPlaced(it) }
        val unconfirmedPlace = step.places.firstOrNull(actions::isUnconfirmed)
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

            // The server may still take the block back, and the player with it into the void.
            unconfirmedPlace != null -> {
                actions.idle()
                standStill(event)
                movement = Movement(safeWalk = true)
                act(unconfirmedPlace, step)
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

    private fun walk(event: MovementInputEvent, frameYaw: Float, step: Step): FollowState {
        actions.idle()
        progress.acting = null
        movement = move(event, frameYaw, step)

        // A jump moves away from its target while walking back to the run-up, so it is only timed.
        val stuck = if (step.jump != null) {
            ++progress.jumpTicks >= JUMP_TICKS || progress.leap.hopeless
        } else {
            progress.watch.stalls(player.position().distanceTo(step.feet)) ||
                progress.isDueToPredict && !getsAhead(event, frameYaw, step)
        }
        return if (stuck) giveUp(step) else FollowState.WALKING
    }

    /**
     * Whether walking on as in this tick gets closer to where it aims, moved as the game moves the player.
     * A walk that the terrain holds up is given up right away instead of after [STUCK_TICKS].
     */
    private fun getsAhead(event: MovementInputEvent, frameYaw: Float, step: Step): Boolean {
        val aim = movement.target?.takeUnless { movement.dive } ?: return true
        val sprint = movement.sprint
        val body = SimulatedPlayer.fromClientPlayer(
            SimulatedPlayerInput(event.directionalInput, event.jump, sprint, event.sneak).also {
                it.forceSafeWalk = movement.safeWalk
            }
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
            ).also { it.forceSafeWalk = movement.safeWalk }
        }
        return false
    }

    private fun place(event: MovementInputEvent, frameYaw: Float, step: Step, pos: BlockPos) {
        val position = player.position()
        val pillars = step.move == Move.PILLAR
        standStill(event)
        when {
            pillars -> {
                event.directionalInput = centerInput(position, step.feet, frameYaw)
                event.jump = player.onGround()
            }

            // Bridging: lean over the edge, so the side of the block below is in sight.
            pos.y < step.from.y -> {
                event.directionalInput = getDirectionalInputTowards(step.feet.subtract(position), frameYaw)
                movement = Movement(step.feet, safeWalk = true)
            }
        }
        actions.place(pos, ready = !pillars || position.y >= pos.y + 1.0)
    }

    private fun act(pos: BlockPos, step: Step) =
        if (progress.actsTooLong(pos)) giveUp(step) else FollowState.ACTING

    /**
     * Runs along one straight line, at any angle, from where the crossing began to the far end of the bridge,
     * sprinting, while another feature places the floor underfoot. Following the staircase of bridge steps instead
     * would turn the way at every step.
     */
    private fun crossBridge(event: MovementInputEvent, frameYaw: Float): FollowState {
        actions.idle()
        progress.acting = null
        val steps = path?.steps ?: return FollowState.FINISHED
        val end = runEnd(steps, index) - 1
        val target = steps[end].feet
        val position = player.position()
        val left = position.horizontalDistanceTo(target)

        // The floor follows the straight run rather than every block of a staircase, so passed steps count as done.
        val start = index
        while (index <= end && left <= steps[index].feet.horizontalDistanceTo(target) + REACH_HORIZONTAL) {
            index++
        }
        if (index != start) {
            stepChanged()
            if (index > end) {
                bridgeLine = null
                return tick(event, frameYaw)
            }
        }

        // A new search that ends the run about where it did keeps the line, so the heading does not jump.
        val line = bridgeLine?.takeIf { it.to.horizontalDistanceTo(target) <= LINE_KEEP }
            ?: BridgeLine(position, target).also { bridgeLine = it }
        event.directionalInput = getDirectionalInputTowards(line.aimFrom(position).subtract(position), frameYaw)
        event.jump = false
        event.sneak = false
        movement = Movement(line.to, sprint = true, safeWalk = true)
        return if (progress.watch.stalls(left)) giveUp(steps[index]) else FollowState.WALKING
    }

    /** The index after the bridge steps in a row from [start]. */
    private fun runEnd(steps: List<Step>, start: Int): Int {
        var end = start
        while (end < steps.size && steps[end].isBridge) {
            end++
        }
        return end
    }

    /** A straight bridge crossing; the walk aims [PURSUIT_AHEAD] along it, which pulls a drift back gently. */
    private class BridgeLine(val from: Vec3, val to: Vec3) {
        private val length = from.horizontalDistanceTo(to)
        private val dx = if (length > 0.0) (to.x - from.x) / length else 0.0
        private val dz = if (length > 0.0) (to.z - from.z) / length else 0.0

        val heading: Float
            get() = to.subtract(from).yaw

        fun aimFrom(position: Vec3): Vec3 {
            val along = (position.x - from.x) * dx + (position.z - from.z) * dz
            val ahead = (along + PURSUIT_AHEAD).coerceIn(0.0, length)
            return Vec3(from.x + dx * ahead, to.y, from.z + dz * ahead)
        }
    }

    private fun giveUp(step: Step): FollowState {
        stuckAt = step.to
        progress = Progress()
        return FollowState.STUCK
    }

    private fun move(event: MovementInputEvent, frameYaw: Float, step: Step): Movement {
        // Air is filled up before a dive, and during one once it runs low.
        if (breathes() || step.underwater && isShortOfAir()) {
            standStill(event)
            progress.watch.excuse()
            return Movement.NONE
        }
        if (step.underwater) {
            return swim(event, frameYaw, step)
        }
        if (step.move == Move.CLIMB_UP) {
            return climbUp(event, frameYaw, step)
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
        val aim = forwardAim(position, shortcut(step) ?: hopAim ?: step.feet, step)

        event.directionalInput = centerInput(position, aim, frameYaw)
        event.sneak = step.sneak
        event.jump = wantsJump(step, player) || startsHop(position, aim, step.feet)
        return Movement(aim, sprint = !step.sneak, isPlainWalk = step.isLevelWalk, safeWalk = step.edge)
    }

    /** Pushes against the side the ladder or vine hangs on, which climbs on every version; jumping only from 1.14. */
    private fun climbUp(event: MovementInputEvent, frameYaw: Float, step: Step): Movement {
        val position = player.position()
        val terrain = Terrain.live()
        val feet = BlockPos.containing(position)
        val cell = if (terrain.isClimbable(feet.x, feet.y, feet.z)) feet else step.from
        val side = terrain.climbSide(cell.x, cell.y, cell.z)
        val aim = if (side == null) step.feet else step.feet.add(side.stepX * GRIP_REACH, 0.0, side.stepZ * GRIP_REACH)

        event.directionalInput = centerInput(position, aim, frameYaw)
        event.jump = true
        event.sneak = false
        return Movement(aim)
    }

    private fun wantsJump(step: Step, body: Player): Boolean {
        val keepsLevel = step.to.y >= step.from.y
        return when (step.move) {
            Move.SWIM -> keepsLevel
            Move.CLIMB_UP -> true
            Move.CLIMB_DOWN, Move.FALL, Move.DOWNWARD -> false
            else -> {
                val position = body.position()
                val rises = step.feet.y - position.y > Terrain.STEP + RISE_MARGIN
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

        val terrain = Terrain.live()
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
    private fun swim(event: MovementInputEvent, frameYaw: Float, step: Step): Movement {
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
            getDirectionalInputTowards(feet.subtract(position), frameYaw)
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
    private fun skipAhead(path: Path) {
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

    /**
     * The point [distance] blocks ahead along the rest of the path from [position], measured horizontally and up or
     * down ladders and vines; the end if the path is shorter.
     */
    fun pointAhead(position: Vec3, distance: Double): Vec3? {
        val path = path ?: return null
        val points = path.points
        var from = position
        var left = distance
        for (i in index + 1 until points.size) {
            val to = points[i]
            val climbs = path.steps[i - 1].move.let { it == Move.CLIMB_UP || it == Move.CLIMB_DOWN }
            val length = if (climbs) from.distanceTo(to) else from.horizontalDistanceTo(to)
            if (length >= left) {
                return from.lerp(to, left / length)
            }
            left -= length
            from = to
        }
        return points.last()
    }

    /** The farthest of the next points a straight walk gets to, while every step up to it is a plain walk. */
    private fun shortcut(step: Step): Vec3? {
        val path = path ?: return null
        if (!step.isPlain || !player.onGround()) {
            return null
        }

        val steps = path.steps
        var end = index
        while (end + 1 < min(steps.size, index + SHORTCUT_STEPS) && steps[end + 1].isPlain) {
            end++
        }

        val terrain = Terrain.live()
        val position = player.position()
        for (i in end downTo index + 1) {
            val point = steps[i].feet
            if (isStraightWalkable(terrain, position, point, SHORTCUT_STOP, path.abilities.safeFall)) {
                return point
            }
        }
        return null
    }

    private fun isReached(step: Step, next: Step?): Boolean {
        val built = step.places.all(::isPlaced) && step.places.none(actions::isUnconfirmed)
        if (!step.breaks.all(::isCleared) || !built || !step.opens.all(::isOpen)) {
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

        // Fast or in the air, the feet point is rarely hit; a plain step passed along its way is done.
        if (step.isPlain && hasPassed(step, position)) {
            return true
        }

        // On straight runs, entering the block is enough; turns wait for the center so corners are not cut.
        return isStable && next != null && block == step.to && continuesStraight(step, next)
    }

    private fun continuesStraight(step: Step, next: Step) =
        next.to.x - next.from.x == step.to.x - step.from.x && next.to.z - next.from.z == step.to.z - step.from.z &&
            (step.to.x != step.from.x || step.to.z != step.from.z)

    /** Whether [position] lies beyond the end of [step], seen along its way, and close beside and level with it. */
    private fun hasPassed(step: Step, position: Vec3): Boolean {
        val from = Vec3.atBottomCenterOf(step.from)
        val dx = step.feet.x - from.x
        val dz = step.feet.z - from.z
        val length = sqrt(dx * dx + dz * dz)
        if (length == 0.0 || abs(position.y - step.feet.y) >= PASS_VERTICAL) {
            return false
        }
        val along = ((position.x - from.x) * dx + (position.z - from.z) * dz) / length
        val beside = abs((position.x - from.x) * dz - (position.z - from.z) * dx) / length
        return along >= length && beside <= PASS_BESIDE
    }

    /**
     * [aim], unless it lies behind the way the player moves fast: then the point ahead on the path the player
     * covers in [STEER_TICKS], and in the air no aim at all rather than one back, which only turns in an arc.
     */
    private fun forwardAim(position: Vec3, aim: Vec3, step: Step): Vec3 {
        val motion = player.deltaMovement
        val speed = motion.horizontalDistance()
        val ahead = (aim.x - position.x) * motion.x + (aim.z - position.z) * motion.z
        val reverses = speed > FAST_SPEED && ahead < 0.0
        if (reverses && !reversing) {
            overshoots++
        }
        reversing = reverses
        if (!reverses || !step.isPlain) {
            return aim
        }

        val point = pointAhead(position, max(speed * STEER_TICKS, MIN_STEER))
        val pointAhead = point != null &&
            (point.x - position.x) * motion.x + (point.z - position.z) * motion.z >= 0.0
        return when {
            pointAhead -> point
            player.onGround() -> aim
            else -> position
        }
    }

    /** The sharpest turn of the path within [distance] blocks ahead, in degrees; 0 without a path. */
    fun turnAhead(distance: Double): Float {
        val points = path?.points ?: return 0f
        var from = player.position()
        var left = distance
        var heading = Float.NaN
        var sharpest = 0f
        for (i in index + 1 until points.size) {
            val to = points[i]
            val length = from.horizontalDistanceTo(to)
            if (length > MIN_SEGMENT) {
                val yaw = to.subtract(from).yaw
                if (!heading.isNaN()) {
                    sharpest = max(sharpest, abs(Mth.wrapDegrees(yaw - heading)))
                }
                heading = yaw
            }
            left -= length
            if (left <= 0.0) {
                break
            }
            from = to
        }
        return sharpest
    }

    private fun stepChanged() {
        progress = Progress()
        val remaining = path?.steps.orEmpty().drop(index)
        remainingBreaks = remaining.flatMap(Step::breaks)
        remainingPlaces = remaining.flatMap(Step::places)
    }

    private fun isCleared(pos: BlockPos) = world.getBlockState(pos).getCollisionShape(world, pos).isEmpty

    private fun isPlaced(pos: BlockPos) = !world.getBlockState(pos).canBeReplaced()

    /** Anything else that took the place of the door, trapdoor or fence gate counts as open. */
    private fun isOpen(pos: BlockPos): Boolean =
        world.getBlockState(pos).getOptionalValue(BlockStateProperties.OPEN).orElse(true)

    private val Step.builds: Boolean
        get() = breaks.isNotEmpty() || places.isNotEmpty()

    private val Step.isLevelWalk: Boolean
        get() = (move == Move.TRAVERSE || move == Move.DIAGONAL) && !sneak && !edge

    private val Step.isPlain: Boolean
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

        /** Keeps to the edge of the ground, as sneaking does, without its slowdown. */
        val safeWalk: Boolean = false,
    ) {
        companion object {
            val NONE = Movement()
        }
    }

    /** A new one with every step. */
    private class Progress {
        val leap = Leap()
        var jumpTicks = 0
        val watch = ProgressWatch(PROGRESS, STUCK_TICKS)
        var acting: BlockPos? = null
        private var actingTicks = 0

        val isDueToPredict: Boolean
            get() = watch.idleTicks > 0 && watch.idleTicks % PREDICT_INTERVAL == 0

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

        /** How far past the middle of a ladder or vine the climb pushes, into the block it hangs on. */
        const val GRIP_REACH = 1.0
        const val OFF_PATH = 1.5

        /** A block down from the path counts as off it. */
        const val FALL_BELOW = 0.9
        const val PROGRESS = 0.1
        const val STUCK_TICKS = 40

        /** Without progress, the way on is predicted every so many ticks, this far ahead. */
        const val PREDICT_INTERVAL = 5
        const val PREDICT_TICKS = 20
        const val ACTION_TICKS = 60
        const val MAX_REJECTIONS = 3

        /** Walking back to the run-up included. */
        const val JUMP_TICKS = 120

        /** Above the feet block: the middle of the room a swimming player has in a gap one block high. */
        const val SWIM_LEVEL = 0.2
        const val SWIM_TOLERANCE = 0.15
        const val LOW_AIR = 0.3
        const val AIR_SEARCH = 4

        const val SHORTCUT_STEPS = 8

        /** How far along the bridge line the walk aims ahead of the player. */
        const val PURSUIT_AHEAD = 2.0

        /** How far the end of a newly searched bridge may lie from the old one for the line to stay. */
        const val LINE_KEEP = 1.5

        /** A plain step counts as passed this close beside its way and level with it. */
        const val PASS_BESIDE = 0.6
        const val PASS_VERTICAL = 1.25

        /** Blocks per tick from which an aim behind counts as overshooting, about Speed's pace. */
        const val FAST_SPEED = 0.3

        /** Ticks of travel the aim lies ahead on the path after an overshoot, and at least this far. */
        const val STEER_TICKS = 3.0
        const val MIN_STEER = 1.0
        const val MIN_SEGMENT = 0.3
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
