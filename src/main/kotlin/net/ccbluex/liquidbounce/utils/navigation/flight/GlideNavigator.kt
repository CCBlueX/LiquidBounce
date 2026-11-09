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

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationTarget
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.useHotbarSlotOrOffhand
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceTo
import net.ccbluex.liquidbounce.utils.math.yaw
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.navigation.render.RouteProgress
import net.ccbluex.liquidbounce.utils.navigation.render.RouteView
import net.ccbluex.liquidbounce.utils.navigation.route.RoutePlanner
import net.ccbluex.liquidbounce.utils.navigation.route.RouteResult
import net.ccbluex.liquidbounce.utils.navigation.walk.steer
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.item.Items
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.Vec3

/**
 * Glides to goals for a walking navigator while an elytra is worn: plans a glide whenever the goal lies far enough
 * away and below, or rockets can make up for the height, and flies it from the take-off to the touch-down, after
 * which walking takes over again. Goals too far for one glide, or not loaded yet, are cruised to with rockets when
 * there are enough of them.
 */
class GlideNavigator(private val owner: EventListener, private val view: RouteView) : EventListener {

    private val planner = RoutePlanner<GlidePath>(this)

    private var path: GlidePath? = null
    private var cruise: ElytraCruise? = null
    private var steering: GlideSteering? = null
    private var goal: Vec3? = null

    private var ticks = 0
    private var retryAt = 0
    private var flightTicks = 0
    private var hasGlided = false

    /**
     * Whether a glide is under way, from the take-off to the touch-down.
     */
    val isActive: Boolean
        get() = path != null || cruise != null

    /**
     * Whether the take-off wants to sprint.
     */
    var sprint = false
        private set

    override fun parent() = owner

    /**
     * Glides one tick towards [goal], within [radius], when a glide is under way or was just found; false to walk
     * this tick instead. Gliding is planned while the player stands, the goal is not [direct] and [abilities] allow.
     */
    fun navigate(
        event: MovementInputEvent,
        goal: Vec3,
        radius: Double,
        abilities: GlideAbilities?,
        direct: Boolean,
        frameYaw: Float,
    ): Boolean {
        ticks++
        if (!isActive) {
            if (abilities == null) {
                planner.cancel()
                return false
            }

            if (!startCruise(goal, abilities, direct)) {
                collectResult(goal, abilities)
            }
            if (!isActive) {
                planIfUseful(goal, radius, abilities, direct)
                return false
            }
        }

        return glide(event, frameYaw)
    }

    private fun collectResult(goal: Vec3, abilities: GlideAbilities) {
        when (val result = planner.poll()) {
            RouteResult.Searching -> Unit

            is RouteResult.Found -> {
                planner.cancel()
                val found = result.value
                // A glide found for where the player stood a moment ago is searched again.
                if (player.onGround() && player.position().distanceTo(found.start) <= START_TOLERANCE) {
                    path = found
                    steering = GlideControl(goal, found.rockets, abilities.rocketTicks, approach = found.approach)
                    this.goal = goal
                    flightTicks = 0
                    hasGlided = false
                }
            }

            RouteResult.AlreadyThere, is RouteResult.Failed -> {
                planner.cancel()
                retryAt = ticks + RETRY_TICKS
            }
        }
    }

    /**
     * Starts a rocket cruise to [goal] when it is too far for one glide or not loaded, the dimension is open above
     * and the rockets reach; true if it started.
     */
    private fun startCruise(goal: Vec3, abilities: GlideAbilities, direct: Boolean): Boolean {
        if (direct || ticks < retryAt || !player.onGround() || world.dimensionType().hasCeiling()) {
            return false
        }

        val start = player.position()
        val distance = start.horizontalDistanceTo(goal)
        val loaded = world.hasChunkAt(BlockPos.containing(goal))
        if (distance < MIN_DISTANCE || distance <= MAX_DISTANCE && loaded) {
            return false
        }

        val altitude = ElytraCruise.cruiseAltitude(start, goal, liveHeights)
        if (abilities.rockets < ElytraCruise.rocketsNeeded(distance, altitude - start.y, abilities.rocketTicks)) {
            return false
        }

        planner.cancel()
        val cruise = ElytraCruise(goal, liveHeights, abilities.rockets, abilities.rocketTicks, world.maxY, ::groundNear)
        this.cruise = cruise
        steering = cruise
        this.goal = goal
        flightTicks = 0
        hasGlided = false
        return true
    }

    private val liveHeights = ColumnHeights { x, z ->
        if (world.hasChunkAt(BlockPos(x, 0, z))) world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) else null
    }

    /**
     * The loaded ground closest to [position] to glide down to, or null when nothing near holds the player.
     */
    private fun groundNear(position: Vec3): Vec3? {
        var best: Vec3? = null
        var bestDistance = Double.MAX_VALUE
        val x0 = Mth.floor(position.x)
        val z0 = Mth.floor(position.z)
        for (dx in -GROUND_SCAN..GROUND_SCAN step GROUND_STEP) {
            for (dz in -GROUND_SCAN..GROUND_SCAN step GROUND_STEP) {
                val top = liveHeights.top(x0 + dx, z0 + dz) ?: continue
                val distance = (dx * dx + dz * dz).toDouble()
                if (top > world.minY && top < position.y && distance < bestDistance) {
                    best = Vec3(x0 + dx + 0.5, top.toDouble(), z0 + dz + 0.5)
                    bestDistance = distance
                }
            }
        }
        return best
    }

    private fun planIfUseful(goal: Vec3, radius: Double, abilities: GlideAbilities, direct: Boolean) {
        if (!planner.isIdle || ticks < retryAt || direct || !player.onGround()) {
            return
        }

        val start = player.position()
        val dx = goal.x - start.x
        val dz = goal.z - start.z
        val distance = Mth.length(dx, dz)
        val drop = start.y - goal.y
        if (distance < MIN_DISTANCE || distance > MAX_DISTANCE || drop < MIN_DROP && abilities.rockets == 0) {
            return
        }

        planner.request(SearchKey(BlockPos.containing(start), goal), GlideRouteSearch.region(start, goal)) { terrain ->
            GlideRouteSearch(terrain, start, goal, radius, abilities)
        }
    }

    /**
     * Jumps and opens the elytra in the air, then leaves the steering to [rotationHandler] until the feet touch
     * down again.
     */
    private fun glide(event: MovementInputEvent, frameYaw: Float): Boolean {
        val goal = goal ?: return false
        val points = path?.points ?: cruise?.let { listOf(player.position(), it.target) } ?: return false
        flightTicks++

        if (player.isFallFlying) {
            hasGlided = true
        } else if (hasGlided || flightTicks > TAKE_OFF_TICKS) {
            // Touched down, or the elytra did not open.
            if (!hasGlided) {
                retryAt = ticks + RETRY_TICKS
            }
            halt()
            return false
        }

        event.sneak = false
        if (hasGlided) {
            event.directionalInput = DirectionalInput.NONE
            event.jump = false
            sprint = false
        } else {
            // Jump, let go and jump again in the air, which opens the elytra.
            event.directionalInput = steer(goal.subtract(player.position()), frameYaw)
            event.jump = flightTicks % 2 == 1
            sprint = true
        }

        view.breaks = emptyList()
        view.places = emptyList()
        view.active(points, progress)
        return true
    }

    @Suppress("unused")
    private val rotationHandler = handler<RotationUpdateEvent> {
        val steering = steering ?: return@handler
        val goal = goal ?: return@handler

        val rotation = if (player.isFallFlying) {
            val input = steering.steer(player.position(), player.deltaMovement)
            if (input.fireRocket) {
                Slots.OffhandWithHotbar.findSlot(Items.FIREWORK_ROCKET)?.let { useHotbarSlotOrOffhand(it) }
            }
            Rotation(input.yaw, input.pitch)
        } else {
            Rotation(goal.subtract(player.position()).yaw, 0f)
        }

        RotationManager.setRotationTarget(
            RotationTarget(
                rotation = rotation,
                ticksUntilReset = 1,
                resetThreshold = 1f,
                considerInventory = false,
                movementCorrection = MovementCorrection.STRICT,
            ),
            Priority.IMPORTANT_FOR_USAGE_2,
            owner,
        )
    }

    /**
     * Stops gliding plans and steering; a glide in the air goes on as the game moves it.
     */
    fun halt() {
        planner.cancel()
        path = null
        cruise = null
        steering = null
        goal = null
        sprint = false
        flightTicks = 0
        hasGlided = false
    }

    private val progress = object : RouteProgress {
        override val isMoving: Boolean
            get() = isActive

        override fun head(partialTicks: Float): Vec3 = player.interpolateCurrentPosition(partialTicks)

        override val ahead: List<Vec3>
            get() = listOfNotNull(path?.landing ?: cruise?.target)
    }

    private data class SearchKey(val start: BlockPos, val goal: Vec3)

    private companion object {
        const val MIN_DISTANCE = 16.0
        const val MAX_DISTANCE = 192.0
        const val MIN_DROP = 6.0
        const val START_TOLERANCE = 1.0
        const val RETRY_TICKS = 40
        const val GROUND_SCAN = 96
        const val GROUND_STEP = 8
        const val TAKE_OFF_TICKS = 8
    }

}
