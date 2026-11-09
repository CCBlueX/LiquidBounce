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
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.getMovementDirectionOfInput
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.CRITICAL_MODIFICATION
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.fma
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceTo
import net.ccbluex.liquidbounce.utils.math.yaw
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.navigation.flight.FlightNavigator
import net.ccbluex.liquidbounce.utils.navigation.flight.GlideNavigator
import net.ccbluex.liquidbounce.utils.navigation.flight.RoofBounce
import net.ccbluex.liquidbounce.utils.navigation.render.RouteProgress
import net.ccbluex.liquidbounce.utils.navigation.render.RouteRenderer
import net.ccbluex.liquidbounce.utils.navigation.render.RouteView
import net.ccbluex.liquidbounce.utils.navigation.ride.RideNavigator
import net.ccbluex.liquidbounce.utils.navigation.route.NearGoal
import net.ccbluex.liquidbounce.utils.navigation.route.RouteLimits
import net.ccbluex.liquidbounce.utils.navigation.route.RoutePlanner
import net.ccbluex.liquidbounce.utils.navigation.route.RouteResult
import net.ccbluex.liquidbounce.utils.navigation.route.StageGoal
import net.ccbluex.liquidbounce.utils.navigation.terrain.LiveWorld
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.min

/**
 * Walks the player to a goal given every tick. Goals within reach of a straight walk are steered to directly,
 * everything else is pathfound, opening doors and gates on the way and breaking and placing blocks when allowed;
 * goals farther than half the path range or in chunks not loaded are walked to in stages, and unreachable goals are
 * walked as close as possible. A boat or mount the player rides is driven there instead, see [RideNavigator].
 *
 * Its settings go into [values]; it acts while [owner] runs. Blocks are aimed at with the [rotations] of the feature
 * it walks for, or with its own when the feature has none.
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

    private val walkSettings = WalkSettings(values, allowBreak, allowPlace, allowParkour)
    private val rotations = rotations ?: values.tree(RotationsValueGroup(owner))
    private val view = RouteView()

    init {
        values.tree(RouteRenderer(owner, view, showRoute))
    }

    private val planner = RoutePlanner<WalkPath>(this) { NavigationTerrain(it, opensDoors = true) }
    private val actions = WalkActions(this, this.rotations)
    private val follower = WalkFollower(actions)
    private val flight = FlightNavigator(this, view)
    private val glide = GlideNavigator(this, view)
    private val bounce = RoofBounce(this, view)
    private val ride = RideNavigator(this, view) { walkSettings.maxLength }

    private val avoid = LongOpenHashSet()
    private var avoidVersion = 0
    private var avoidSince = 0
    private var avoidGoal: Vec3? = null

    private var ticks = 0
    private var lastRequest = Int.MIN_VALUE / 2
    private var retryAt = 0
    private var failed = false

    /**
     * Searches in a row that found nothing for the same goal.
     */
    private var failures = 0
    private var stuck = false
    private var wasOffPath = false

    /**
     * When a search last found nothing closer to walk to.
     */
    private var settledAt: Int? = null

    /**
     * The goal the followed path was searched for, and the goal of the running search.
     */
    private var pathGoal: Vec3? = null
    private var requestedGoal: Vec3? = null

    /**
     * The goal of the running search when it looks for a stage on the way to a goal beyond its range, and whether
     * the followed path ends at such a stage.
     */
    private var requestedStage: NearGoal? = null
    private var followsStage = false

    private var straight = false
    private var direct = false
    private var directGoal: Vec3? = null
    private var steeringTarget: Vec3? = null
    private var sprintWanted = false

    /**
     * Whether a block is being mined, placed or opened for the path.
     */
    val isActing: Boolean
        get() = actions.isActive

    /**
     * Where the walk heads: the aim at a block being mined, placed or opened, else the way to the next point, pitched
     * towards it under water, else the direction of the movement input.
     */
    fun movementRotation(): Rotation {
        actions.rotation?.let { return it }

        val target = steeringTarget ?: return Rotation(player.getMovementDirectionOfInput(), 0f)
        val delta = target.subtract(player.position())
        if (delta.horizontalDistanceSqr() < MIN_STEER_DISTANCE_SQR) {
            return Rotation(player.getMovementDirectionOfInput(), 0f)
        }

        return if (follower.dive) Rotation.fromRotationVec(delta) else Rotation(delta.yaw, 0f)
    }

    /**
     * Turns the head the way the walk goes, so the keys press forward under movement correction and the player can
     * sprint; under water also up or down, so the swimming pose swims there. Mining and placing aim themselves, and
     * any other feature that rotates takes over.
     */
    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent> {
        val target = steeringTarget ?: return@handler
        if (actions.rotation != null) {
            return@handler
        }

        val delta = target.subtract(player.position())
        if (delta.horizontalDistanceSqr() < MIN_STEER_DISTANCE_SQR) {
            return@handler
        }

        RotationManager.setRotationTarget(
            if (follower.dive) Rotation.fromRotationVec(delta) else Rotation(delta.yaw, player.xRot),
            valueGroup = this.rotations,
            priority = Priority.NOT_IMPORTANT,
            provider = owner,
        )
    }

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
     * Stops walking and forgets where it got stuck; for when the owner is turned off.
     */
    fun reset() {
        halt()
        avoid.clear()
    }

    /**
     * Moves one tick towards [goal], within [radius]; true if the movement is a plain walk other features may add
     * to. Call from a [MovementInputEvent] handler at [CRITICAL_MODIFICATION].
     */
    fun navigate(event: MovementInputEvent, goal: Vec3, radius: Double): Boolean {
        ticks++
        val position = player.position()
        updateAvoid(goal)

        if (player.rootVehicle.position().distanceToSqr(goal) <= Mth.square(radius)) {
            stop(event)
            view.done()
            return false
        }

        if (player.onGround()) {
            // Goals beyond the path range are walked to in stages; a straight walk there is never checked in full.
            straight = position.distanceToSqr(goal) <= Mth.square(walkSettings.maxLength) &&
                isStraightWalkable(NavigationTerrain(LiveWorld(world)), position, goal, radius, walkSettings.safeFall)
            direct = straight
        }

        if (rideVehicle(event, goal, radius) || fly(event, goal, radius) || glide(event, goal, radius) ||
            bounce(event, goal)
        ) {
            return false
        }

        if (direct) {
            steerDirectly(event, goal)
            return true
        }

        directGoal = null
        collectResult(position)

        val path = follower.path
        val plainWalk = if (path != null && !follower.isFinished) {
            followPath(event)
        } else {
            fallback(event, goal, radius, path)
        }

        planIfNeeded(goal, radius, position)
        return plainWalk
    }

    /**
     * Drives the vehicle the player rides, or gets off it; false when the player rides nothing.
     */
    private fun rideVehicle(event: MovementInputEvent, goal: Vec3, radius: Double): Boolean {
        if (!ride.navigate(event, goal, radius)) {
            return false
        }

        standBy()
        return true
    }

    /**
     * Flies this tick when the player can; false to walk. Goals a plain walk reaches are walked to unless the player
     * flies already.
     */
    private fun fly(event: MovementInputEvent, goal: Vec3, radius: Double): Boolean {
        val abilities = walkSettings.captureFlight()?.takeUnless { direct && !it.isFlying }
        if (abilities == null) {
            flight.halt()
            return false
        }

        if (!flight.isActive) {
            glide.halt()
            bounce.halt()
            stopWalking()
        }

        flight.navigate(event, goal, radius, walkSettings.maxLength, abilities, frameYaw())
        steeringTarget = flight.target
        sprintWanted = flight.sprint
        return true
    }

    /**
     * Glides this tick when an elytra glide is under way or was just found; false to walk.
     */
    private fun glide(event: MovementInputEvent, goal: Vec3, radius: Double): Boolean {
        val wasGliding = glide.isActive
        if (!glide.navigate(event, goal, radius, walkSettings.captureGlide(), direct, frameYaw())) {
            return false
        }

        if (!wasGliding) {
            stopWalking()
        }
        steeringTarget = null
        sprintWanted = glide.sprint
        return true
    }

    /**
     * Bounces along a flat floor with an elytra and ElytraRecast while that is faster; false to walk.
     */
    private fun bounce(event: MovementInputEvent, goal: Vec3): Boolean {
        val wasBouncing = bounce.isActive
        if (!bounce.navigate(event, goal, walkSettings.captureGlide(), frameYaw())) {
            return false
        }

        if (!wasBouncing) {
            stopWalking()
        }
        steeringTarget = null
        sprintWanted = true
        return true
    }

    private fun steerDirectly(event: MovementInputEvent, goal: Vec3) {
        planner.cancel()
        follower.clear()
        actions.idle()
        pathGoal = null
        requestedGoal = null
        failed = false
        stuck = false

        directGoal = goal
        steeringTarget = goal
        sprintWanted = true
        event.directionalInput = steer(goal.subtract(player.position()), frameYaw())
        event.jump = player.isInWater
        event.sneak = false

        view.breaks = emptyList()
        view.places = emptyList()
        view.active(listOf(player.position(), goal), directProgress)
    }

    private fun followPath(event: MovementInputEvent): Boolean {
        val path = follower.path ?: return false
        val state = follower.tick(event, frameYaw())
        if (state == FollowState.STUCK) {
            follower.stuckAt?.let { avoid.add(it.asLong()) }
            avoidVersion++
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
     * Without a path to walk: straight towards the goal if nothing is in the way, else the rest of the way within
     * the block the player stands in, else on towards the goal while the first path is searched and the next blocks
     * are plain ground, else standing still.
     */
    private fun fallback(event: MovementInputEvent, goal: Vec3, radius: Double, path: WalkPath?): Boolean {
        actions.idle()

        val searching = !planner.isIdle
        val target = when {
            (failed || path == null) && straight -> goal
            else -> finalApproach(goal, radius) ?: goal.takeIf { searching && !failed && hasHeadway(it) }
        }
        if (target != null) {
            steeringTarget = target
            sprintWanted = target === goal
            event.directionalInput = steer(target.subtract(player.position()), frameYaw())
            event.jump = player.isInWater
            event.sneak = false
        } else {
            stop(event)
        }

        when {
            failed -> view.invalid()
            searching -> view.pending(player.position(), goal, planner.preview)
            else -> view.done()
        }
        return false
    }

    /**
     * Whether the next [HEADWAY] blocks towards [goal] can be walked straight on.
     */
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
        val terrain = NavigationTerrain(LiveWorld(world))
        return isStraightWalkable(terrain, position, ahead, HEADWAY_TOLERANCE, walkSettings.safeFall)
    }

    /**
     * The point of the block the player stands in closest to [goal], if that block is close enough to it. Searches
     * measure from block centres, so they end in such a block before the player itself is close enough.
     */
    private fun finalApproach(goal: Vec3, radius: Double): Vec3? {
        val position = player.position()
        val feet = BlockPos.containing(position)
        if (!NearGoal(goal, radius).isGoal(feet.x, feet.y, feet.z)) {
            return null
        }

        val margin = player.bbWidth / 2.0
        return Vec3(
            goal.x.coerceIn(feet.x + margin, feet.x + 1.0 - margin),
            position.y,
            goal.z.coerceIn(feet.z + margin, feet.z + 1.0 - margin),
        )
    }

    private fun collectResult(position: Vec3) {
        when (val result = planner.poll()) {
            RouteResult.Searching -> Unit

            RouteResult.AlreadyThere -> {
                planner.cancel()
                follower.clear()
                pathGoal = requestedGoal
                failed = false
                settle()
            }

            is RouteResult.Found -> {
                planner.cancel()
                accept(result.value, position)
            }

            is RouteResult.Failed -> {
                planner.cancel()
                failed = true
                failures++
                retryAt = ticks + RETRY_TICKS
                if (follower.isFinished) {
                    follower.clear()
                }
            }
        }
    }

    private fun accept(path: WalkPath, position: Vec3) {
        failed = false
        failures = 0
        stuck = false
        if (path.steps.isEmpty()) {
            // Nothing closer to the goal can be reached from here.
            follower.clear()
            pathGoal = requestedGoal
            settle()
            return
        }

        val startIndex = joinIndex(path, position)
        if (startIndex == null) {
            // The player moved on while searching; the next search starts where the player is now.
            pathGoal = null
            return
        }

        followsStage = requestedStage.let { it != null && !it.isGoal(path.steps.last().to) }
        pathGoal = requestedGoal
        settledAt = null
        follower.follow(path, startIndex)
    }

    private fun settle() {
        settledAt = ticks
    }

    /**
     * The step to begin [path] with: the one leading away from the path point the player is at.
     */
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

        val goalMoved = pathGoal.let { it == null || it.distanceToSqr(goal) > GOAL_MOVED * GOAL_MOVED }
        val finishedShort = path != null && follower.isFinished
        val nearStage = path != null && followsStage && position.horizontalDistanceTo(path.end) < STAGE_LEAD
        val urgent = stuck || leftPath

        val settled = settledAt.let { it != null && ticks - it < SETTLE_TICKS }
        val needed = (path == null && !settled) || goalMoved || offPath || finishedShort || stuck || nearStage
        if (!needed || ticks < retryAt) {
            return
        }

        val searching = !planner.isIdle
        if (!urgent && (searching || ticks - lastRequest < REPLAN_INTERVAL)) {
            return
        }

        request(goal, radius, position)
        stuck = false
    }

    /**
     * Searches a path to [goal], or, when it is out of range or not loaded, to the next stage on the way to it.
     */
    private fun request(goal: Vec3, radius: Double, start: Vec3) {
        val nearGoal = NearGoal(goal, radius)
        if (requestedGoal.let { it == null || it.distanceToSqr(goal) > GOAL_MOVED * GOAL_MOVED }) {
            failures = 0
        }
        // After searches that found nothing, the next ones look further around for a detour.
        val maxLength = walkSettings.maxLength * (1 shl min(failures, MAX_WIDENING))
        // A stage with its detours stays within the path range.
        val stageRange = maxLength / 2.0
        val staged = StageGoal.isNeeded(LiveWorld(world), start, goal, stageRange)
        val region = if (staged) {
            StageGoal.region(start, stageRange, VERTICAL_MARGIN)
        } else {
            nearGoal.region(start, maxLength, VERTICAL_MARGIN)
        }
        val abilities = walkSettings.capture(avoid)
        // Every jump considered is simulated, which makes routes with them slower to work out.
        val limits = if (abilities.canParkour) {
            RouteLimits(maxLength, maxExpansions = PARKOUR_EXPANSIONS, maxComputeNanos = PARKOUR_COMPUTE_NANOS)
        } else {
            RouteLimits(maxLength)
        }

        val key = SearchKey(BlockPos.containing(start), nearGoal, avoidVersion, staged)
        planner.request(key, region) { terrain ->
            val searchGoal = if (staged) StageGoal(nearGoal, start, stageRange, terrain) else nearGoal
            WalkRouteSearch(terrain, start, searchGoal, abilities, limits)
        }

        if (requestedGoal.let { it == null || it.distanceToSqr(goal) > GOAL_MOVED * GOAL_MOVED }) {
            failed = false
        }
        requestedGoal = goal
        requestedStage = nearGoal.takeIf { staged }
        lastRequest = ticks
        if (follower.path == null && !failed) {
            view.pending(start, goal)
        }
    }

    private fun updateAvoid(goal: Vec3) {
        if (avoid.isEmpty()) {
            avoidGoal = goal
            avoidSince = ticks
            return
        }

        val avoidGoal = avoidGoal
        val goalChanged = avoidGoal == null || avoidGoal.distanceToSqr(goal) > AVOID_GOAL_CHANGE * AVOID_GOAL_CHANGE
        if (goalChanged || ticks - avoidSince > AVOID_TICKS) {
            avoid.clear()
            avoidVersion++
            this.avoidGoal = goal
            avoidSince = ticks
        }
    }

    /**
     * The yaw the movement input is applied relative to: the rotation under strict movement correction, which
     * keeps the keys but moves along the rotation, else the view.
     */
    private fun frameYaw(): Float {
        val strict = RotationManager.activeRotationTarget?.movementCorrection == MovementCorrection.STRICT
        return if (strict && RotationManager.currentRotation != null) RotationManager.movementYaw else player.yRot
    }

    private fun stop(event: MovementInputEvent) {
        actions.idle()
        steeringTarget = null
        sprintWanted = false
        event.directionalInput = DirectionalInput.NONE
        event.jump = player.isInWater
        event.sneak = false
    }

    /**
     * Drops walking, flying and gliding while the player rides, keeping the route the ride shows.
     */
    private fun standBy() {
        flight.halt()
        glide.halt()
        bounce.halt()
        planner.cancel()
        follower.clear()
        actions.idle()
        pathGoal = null
        requestedGoal = null
        requestedStage = null
        followsStage = false
        directGoal = null
        steeringTarget = null
        sprintWanted = false
        straight = false
        direct = false
    }

    /**
     * Stops walking, riding, flying and gliding until the next [navigate]; for ticks without a goal.
     */
    fun halt() {
        ride.halt()
        flight.halt()
        glide.halt()
        bounce.halt()
        stopWalking()
    }

    private fun stopWalking() {
        planner.cancel()
        follower.clear()
        actions.idle()
        view.clear()
        pathGoal = null
        requestedGoal = null
        requestedStage = null
        followsStage = false
        directGoal = null
        steeringTarget = null
        sprintWanted = false
        straight = false
        direct = false
        failed = false
        failures = 0
        stuck = false
        wasOffPath = false
        settledAt = null
    }

    private val directProgress = object : RouteProgress {
        override val isMoving: Boolean
            get() = directGoal != null

        override fun head(partialTicks: Float): Vec3 = player.interpolateCurrentPosition(partialTicks)

        override val ahead: List<Vec3>
            get() = listOfNotNull(directGoal)
    }

    private data class SearchKey(val start: BlockPos, val goal: NearGoal, val avoidVersion: Int, val staged: Boolean)

    private companion object {
        const val MIN_STEER_DISTANCE_SQR = 1.0E-4
        const val VERTICAL_MARGIN = 16
        const val GOAL_MOVED = 1.5
        const val REPLAN_INTERVAL = 5
        const val RETRY_TICKS = 20
        const val SETTLE_TICKS = 100
        const val JOIN_POINTS = 8
        const val JOIN_DISTANCE = 1.0
        const val HEADWAY = 2.0
        const val MIN_HEADWAY = 0.5
        const val HEADWAY_TOLERANCE = 0.75
        const val AVOID_GOAL_CHANGE = 8.0
        const val AVOID_TICKS = 600

        /**
         * Blocks before the end of a stage at which the next stage is searched, so the walk goes on without a stop.
         */
        const val STAGE_LEAD = 12.0
        const val PARKOUR_EXPANSIONS = 40_000
        const val MAX_WIDENING = 2
        val PARKOUR_COMPUTE_NANOS = TimeUnit.SECONDS.toNanos(2)
    }

}
