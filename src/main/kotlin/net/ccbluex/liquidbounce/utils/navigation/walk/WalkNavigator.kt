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

import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.SprintEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.session.sessionState
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.getMovementDirectionOfInput
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.CRITICAL_MODIFICATION
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.fma
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceTo
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceToSqr
import net.ccbluex.liquidbounce.utils.math.yaw
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.navigation.flight.FlightNavigator
import net.ccbluex.liquidbounce.utils.navigation.flight.GlideNavigator
import net.ccbluex.liquidbounce.utils.navigation.flight.RoofBounce
import net.ccbluex.liquidbounce.utils.navigation.render.RouteProgress
import net.ccbluex.liquidbounce.utils.navigation.render.RouteRenderer
import net.ccbluex.liquidbounce.utils.navigation.render.RouteView
import net.ccbluex.liquidbounce.utils.navigation.ride.RideNavigator
import net.ccbluex.liquidbounce.utils.navigation.route.GoalRouter
import net.ccbluex.liquidbounce.utils.navigation.route.NearGoal
import net.ccbluex.liquidbounce.utils.navigation.route.RouteLimits
import net.ccbluex.liquidbounce.utils.navigation.route.RouteResult
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.min

/**
 * Walks the player to a goal given every tick: straight on where nothing is in the way, else along a searched path,
 * opening doors and, when allowed, breaking and placing blocks. A ridden vehicle is driven there instead, and the
 * player flies or glides where it can.
 */
@Suppress("TooManyFunctions")
class WalkNavigator(
    private val owner: EventListener,
    values: ValueGroup,
    rotations: RotationsValueGroup? = null,
    allowBreak: Boolean = false,
    allowPlace: Boolean = false,
    allowParkour: Boolean = true,
    showRoute: Boolean = true,
) : EventListener {

    private val settings = WalkSettings(values, allowBreak, allowPlace, allowParkour)
    private val rotations = rotations ?: values.tree(RotationsValueGroup(owner))
    private val view = RouteView()

    init {
        values.tree(RouteRenderer(owner, view, showRoute))
    }

    private val routes = GoalRouter<WalkPath>(this, VERTICAL_MARGIN) { NavigationTerrain(it, opensDoors = true) }
    private val actions = WalkActions(this, this.rotations)
    private val follower = WalkFollower(actions)
    private val flight = FlightNavigator(this, view)
    private val glide = GlideNavigator(this, view)
    private val bounce = RoofBounce(this, view)
    private val ride = RideNavigator(this, view) { settings.maxLength }
    private val stuckSpots = StuckSpots()

    /** Searches in a row that found nothing; each widens the next one. */
    private var failures by sessionState { 0 }
    private var stuck by sessionState { false }
    private var wasOffPath by sessionState { false }

    /** When a search last found nothing closer to walk to. */
    private var settledAt: Int? by sessionState { null }

    /** Whether the goal is reached by walking straight on, as of the last tick on the ground. */
    private var straight by sessionState { false }
    private var straightGoal: Vec3? by sessionState { null }
    private var steeringTarget: Vec3? by sessionState { null }
    private var sprintWanted by sessionState { false }

    private var lastLookYaw by sessionState { Float.NaN }

    val isActing: Boolean
        get() = actions.isActive

    /** Where the walk heads: the aim of a block action, else the way to the next point, else the movement input. */
    fun movementRotation(): Rotation {
        actions.rotation?.let { return it }

        val target = steeringTarget
        val yaw = target?.let(::lookYaw)
        if (target == null || yaw == null || yaw.isNaN()) {
            return Rotation(player.getMovementDirectionOfInput(), 0f)
        }
        return Rotation(yaw, if (follower.dive) divePitch(target) else 0f)
    }

    // Looking the way the walk goes lets the keys press forward under movement correction, so the player can sprint.
    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent> {
        actions.rotation?.let { aim ->
            lastLookYaw = aim.yaw
            return@handler
        }

        val target = steeringTarget
        val yaw = if (target == null) lastLookYaw else lookYaw(target)
        if (yaw.isNaN()) {
            return@handler
        }
        lastLookYaw = yaw
        RotationManager.setRotationTarget(
            Rotation(yaw, if (target != null && follower.dive) divePitch(target) else player.xRot),
            valueGroup = this.rotations,
            priority = Priority.NOT_IMPORTANT,
            provider = owner,
        )
    }

    /**
     * NaN with nothing to look at yet. A target the player is about to pass would turn the look around, so the path
     * beyond it is looked at or the last look held; while building the player keeps facing the way of the last aim.
     */
    private fun lookYaw(target: Vec3): Float {
        val position = player.position()
        val look = target.takeIf { it.horizontalDistanceToSqr(position) >= LOOK_DISTANCE * LOOK_DISTANCE }
            ?: follower.lookAhead(target, position, LOOK_DISTANCE)
            ?: return lastLookYaw
        val yaw = look.subtract(position).yaw
        val backwards = follower.building && lastLookYaw.isFinite() &&
            abs(Mth.wrapDegrees(yaw - lastLookYaw)) > QUARTER_TURN
        return if (backwards) Mth.wrapDegrees(yaw + 2 * QUARTER_TURN) else yaw
    }

    private fun divePitch(target: Vec3) = Rotation.fromRotationVec(target.subtract(player.position())).pitch

    @Suppress("unused")
    private val sprintHandler = handler<SprintEvent>(priority = CRITICAL_MODIFICATION) { event ->
        if (!sprintWanted || !event.directionalInput.isMoving) {
            return@handler
        }

        if (event.source == SprintEvent.Source.MOVEMENT_TICK || event.source == SprintEvent.Source.INPUT) {
            event.sprint = true
        }
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        reset()
    }

    override fun parent() = owner

    /**
     * Moves one tick towards [goal], within [radius]; true if the movement is a plain walk other features may add
     * to. Call from a [MovementInputEvent] handler at [CRITICAL_MODIFICATION].
     */
    fun navigate(event: MovementInputEvent, goal: Vec3, radius: Double): Boolean {
        routes.tick()
        stuckSpots.tick(goal)
        val position = player.position()

        if (player.rootVehicle.position().closerThan(goal, radius)) {
            stop(event)
            lastLookYaw = Float.NaN
            view.done()
            return false
        }

        if (player.onGround()) {
            // Goals beyond the path range are walked to in stages; a straight walk there is never checked in full.
            straight = position.closerThan(goal, settings.maxLength) &&
                isStraightWalkable(NavigationTerrain.live(), position, goal, radius, settings.safeFall)
        }

        if (rideVehicle(event, goal, radius) || fly(event, goal, radius) || glide(event, goal, radius) ||
            bounce(event, goal)
        ) {
            return false
        }

        if (straight) {
            walkStraight(event, goal)
            return true
        }

        straightGoal = null
        collectRoute(position)

        val path = follower.path
        val plainWalk = if (path != null && !follower.isFinished) {
            followPath(event, path)
        } else {
            fallback(event, goal, radius)
        }

        planIfNeeded(goal, radius, position)
        return plainWalk
    }

    /** For ticks without a goal. */
    fun halt() {
        ride.halt()
        haltFlying()
        stopWalking()
    }

    /** For when the owner is turned off. */
    fun reset() {
        halt()
        stuckSpots.clear()
    }

    private fun rideVehicle(event: MovementInputEvent, goal: Vec3, radius: Double): Boolean {
        if (!ride.navigate(event, goal, radius)) {
            return false
        }

        // The ride shows its own route.
        haltFlying()
        resetWalk()
        return true
    }

    private fun fly(event: MovementInputEvent, goal: Vec3, radius: Double): Boolean {
        // Goals a straight walk reaches are walked to unless the player flies already.
        val abilities = settings.captureFlight()?.takeUnless { straight && !it.isFlying }
        if (abilities == null) {
            flight.halt()
            return false
        }

        if (!flight.isActive) {
            glide.halt()
            bounce.halt()
            stopWalking()
        }

        flight.navigate(event, goal, radius, settings.maxLength, abilities, frameYaw())
        steeringTarget = flight.target
        sprintWanted = flight.sprint
        return true
    }

    private fun glide(event: MovementInputEvent, goal: Vec3, radius: Double): Boolean {
        val wasGliding = glide.isActive
        if (!glide.navigate(event, goal, radius, settings.captureGlide(), straight, frameYaw())) {
            return false
        }

        if (!wasGliding) {
            stopWalking()
        }
        steeringTarget = null
        sprintWanted = glide.sprint
        return true
    }

    private fun bounce(event: MovementInputEvent, goal: Vec3): Boolean {
        val wasBouncing = bounce.isActive
        if (!bounce.navigate(event, goal, settings.captureGlide(), frameYaw())) {
            return false
        }

        if (!wasBouncing) {
            stopWalking()
        }
        steeringTarget = null
        sprintWanted = true
        return true
    }

    private fun walkStraight(event: MovementInputEvent, goal: Vec3) {
        routes.reset()
        follower.clear()
        actions.idle()
        stuck = false

        straightGoal = goal
        walkTowards(event, goal, sprint = true)

        view.breaks = emptyList()
        view.places = emptyList()
        view.active(listOf(player.position(), goal), straightProgress)
    }

    private fun followPath(event: MovementInputEvent, path: WalkPath): Boolean {
        val state = follower.tick(event, frameYaw())
        if (state == FollowState.STUCK) {
            follower.stuckAt?.let(stuckSpots::add)
            stuck = true
        }

        steeringTarget = follower.target
        sprintWanted = follower.sprint && (!event.sneak || follower.dive)

        view.breaks = follower.remainingBreaks
        view.places = follower.remainingPlaces
        view.active(path.points, follower)
        return state == FollowState.WALKING && follower.isPlainWalk && !actions.isActive
    }

    /**
     * Without a path: the rest of the way within the feet block, else on over plain ground while the first path is
     * searched, else standing still.
     */
    private fun fallback(event: MovementInputEvent, goal: Vec3, radius: Double): Boolean {
        actions.idle()

        val searching = routes.isSearching
        val target = finalApproach(goal, radius) ?: goal.takeIf { searching && !routes.failed && hasHeadway(it) }
        if (target != null) {
            walkTowards(event, target, sprint = target === goal)
        } else {
            stop(event)
        }

        when {
            routes.failed -> view.invalid()
            searching -> view.pending(player.position(), goal, routes.preview)
            else -> view.done()
        }
        return false
    }

    private fun hasHeadway(goal: Vec3): Boolean {
        if (!player.onGround()) {
            return false
        }

        val position = player.position()
        val delta = Vec3(goal.x - position.x, 0.0, goal.z - position.z)
        val length = delta.length()
        if (length < MIN_HEADWAY) {
            return false
        }

        val ahead = position.fma(min(HEADWAY, length) / length, delta)
        val terrain = NavigationTerrain.live()
        return isStraightWalkable(terrain, position, ahead, HEADWAY_TOLERANCE, settings.safeFall)
    }

    /**
     * The point of the block the player stands in closest to [goal], if that block is close enough to it. Searches
     * measure from block centers, so they end in such a block before the player itself is close enough.
     */
    private fun finalApproach(goal: Vec3, radius: Double): Vec3? {
        val position = player.position()
        val feet = BlockPos.containing(position)
        if (!NearGoal(goal, radius).isGoal(feet)) {
            return null
        }

        val margin = player.bbWidth / 2.0
        return Vec3(
            goal.x.coerceIn(feet.x + margin, feet.x + 1.0 - margin),
            position.y,
            goal.z.coerceIn(feet.z + margin, feet.z + 1.0 - margin),
        )
    }

    private fun collectRoute(position: Vec3) {
        when (val result = routes.poll()) {
            RouteResult.Searching -> Unit

            RouteResult.AlreadyThere -> {
                follower.clear()
                settledAt = routes.ticks
            }

            is RouteResult.Found -> accept(result.value, position)

            is RouteResult.Failed -> {
                failures++
                if (follower.isFinished) {
                    follower.clear()
                }
            }
        }
    }

    private fun accept(path: WalkPath, position: Vec3) {
        failures = 0
        stuck = false
        if (path.steps.isEmpty()) {
            // Nothing closer to the goal can be reached from here.
            follower.clear()
            settledAt = routes.ticks
            return
        }

        // The player moved on while searching; the next search starts where the player is now.
        val startIndex = joinIndex(path, position) ?: return routes.forget()

        routes.routeEnds(path.steps.last().to)
        settledAt = null
        follower.follow(path, startIndex)
    }

    private fun joinIndex(path: WalkPath, position: Vec3): Int? {
        val points = path.points
        var best: Int? = null
        var bestDistance = Double.MAX_VALUE
        for (index in 0 until min(points.size, JOIN_POINTS)) {
            val point = points[index]
            val horizontal = position.horizontalDistanceTo(point)
            val near = horizontal <= JOIN_DISTANCE && abs(position.y - point.y) <= JOIN_DISTANCE
            if (near && horizontal < bestDistance) {
                best = index
                bestDistance = horizontal
            }
        }
        return best
    }

    private fun planIfNeeded(goal: Vec3, radius: Double, position: Vec3) {
        val path = follower.path
        val offPath = follower.isOffPath()
        val leftPath = offPath && !wasOffPath
        wasOffPath = offPath

        val finishedShort = path != null && follower.isFinished
        val nearStage = path != null && routes.endsAtStage &&
            position.horizontalDistanceTo(path.end) < GoalRouter.STAGE_LEAD
        val settled = settledAt.let { it != null && routes.ticks - it < SETTLE_TICKS }

        val needed = (path == null && !settled) || routes.hasGoalMoved(goal) || offPath || finishedShort || stuck ||
            nearStage
        if (needed && routes.isDue(urgent = stuck || leftPath)) {
            request(goal, radius, position)
            stuck = false
        }
    }

    private fun request(goal: Vec3, radius: Double, start: Vec3) {
        if (routes.isNewGoal(goal)) {
            failures = 0
        }
        // After searches that found nothing, the next ones look further around for a detour.
        val maxLength = settings.maxLength * (1 shl min(failures, MAX_WIDENING))
        val abilities = settings.capture(stuckSpots.blocks)
        // Every jump considered is simulated, which makes routes with them slower to work out.
        val limits = if (abilities.canParkour) {
            RouteLimits(maxLength, maxExpansions = PARKOUR_EXPANSIONS, maxComputeNanos = PARKOUR_COMPUTE_NANOS)
        } else {
            RouteLimits(maxLength)
        }

        routes.request(start, NearGoal(goal, radius), maxLength, stuckSpots.version) { terrain, searchGoal ->
            WalkRouteSearch(terrain, start, searchGoal, abilities, limits)
        }

        if (follower.path == null && !routes.failed) {
            view.pending(start, goal)
        }
    }

    /** The yaw the keys move relative to: the rotation under strict movement correction, else the view. */
    private fun frameYaw(): Float {
        val strict = RotationManager.activeRotationTarget?.movementCorrection == MovementCorrection.STRICT
        return if (strict && RotationManager.currentRotation != null) RotationManager.movementYaw else player.yRot
    }

    private fun walkTowards(event: MovementInputEvent, target: Vec3, sprint: Boolean) {
        steeringTarget = target
        sprintWanted = sprint
        event.directionalInput = steer(target.subtract(player.position()), frameYaw())
        event.jump = player.isInWater
        event.sneak = false
    }

    private fun stop(event: MovementInputEvent) {
        actions.idle()
        steeringTarget = null
        sprintWanted = false
        event.directionalInput = DirectionalInput.NONE
        event.jump = player.isInWater
        event.sneak = false
    }

    private fun haltFlying() {
        flight.halt()
        glide.halt()
        bounce.halt()
    }

    private fun stopWalking() {
        resetWalk()
        view.clear()
    }

    private fun resetWalk() {
        routes.reset()
        follower.clear()
        actions.idle()
        failures = 0
        stuck = false
        wasOffPath = false
        settledAt = null
        straight = false
        straightGoal = null
        steeringTarget = null
        sprintWanted = false
        lastLookYaw = Float.NaN
    }

    private val straightProgress = object : RouteProgress {
        override val isMoving: Boolean
            get() = straightGoal != null

        override fun head(partialTicks: Float): Vec3 = player.interpolateCurrentPosition(partialTicks)

        override val ahead: List<Vec3>
            get() = listOfNotNull(straightGoal)
    }

    private companion object {
        /** How far ahead a point has to be for the look to follow it; a step passed by a bit lies right behind. */
        const val LOOK_DISTANCE = 0.75
        const val QUARTER_TURN = 90f
        const val VERTICAL_MARGIN = 16
        const val SETTLE_TICKS = 100
        const val JOIN_POINTS = 8
        const val JOIN_DISTANCE = 1.0
        const val HEADWAY = 2.0
        const val MIN_HEADWAY = 0.5
        const val HEADWAY_TOLERANCE = 0.75
        const val PARKOUR_EXPANSIONS = 40_000
        const val MAX_WIDENING = 2
        val PARKOUR_COMPUTE_NANOS = TimeUnit.SECONDS.toNanos(2)
    }

}

/** Feet blocks the walk got stuck at, which searches avoid until [FORGET_TICKS] passed or the goal changed. */
private class StuckSpots {

    val blocks by sessionState { LongOpenHashSet() }

    /** Changes with [blocks], so a search for the set before is not kept. */
    var version by sessionState { 0 }
        private set

    private var goal: Vec3? by sessionState { null }
    private var age by sessionState { 0 }

    fun add(pos: BlockPos) {
        blocks.add(pos.asLong())
        version++
    }

    fun clear() = blocks.clear()

    fun tick(goal: Vec3) {
        val outdated = blocks.isNotEmpty() &&
            (++age > FORGET_TICKS || this.goal.let { it == null || !it.closerThan(goal, GOAL_CHANGE) })
        if (outdated) {
            blocks.clear()
            version++
        }

        if (blocks.isEmpty()) {
            this.goal = goal
            age = 0
        }
    }

    private companion object {
        const val GOAL_CHANGE = 8.0
        const val FORGET_TICKS = 600
    }

}
