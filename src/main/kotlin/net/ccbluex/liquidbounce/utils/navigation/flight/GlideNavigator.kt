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
 * Glides to goals for a walking navigator while an elytra is worn: plans a glide to goals far enough away and below,
 * or within reach of rockets, and flies it from take-off to touch-down. Farther goals are cruised to with rockets.
 */
class GlideNavigator(private val owner: EventListener, private val view: RouteView) : EventListener {

    private val planner = RoutePlanner<GlidePath>(this)
    private var glide: Glide? = null
    private var ticks = 0
    private var retryAt = 0

    val isActive: Boolean
        get() = glide != null

    var sprint = false
        private set

    override fun parent() = owner

    /** Glides one tick towards [goal]; false to walk instead. Glides are planned on the ground unless [direct]. */
    fun navigate(
        event: MovementInputEvent,
        goal: Vec3,
        radius: Double,
        abilities: GlideAbilities?,
        direct: Boolean,
        frameYaw: Float,
    ): Boolean {
        ticks++
        val glide = glide ?: start(goal, radius, abilities, direct) ?: return false
        return fly(event, glide, frameYaw)
    }

    /** Stops planning and steering; a glide in the air goes on as the game moves it. */
    fun halt() {
        planner.cancel()
        glide = null
        sprint = false
    }

    private fun start(goal: Vec3, radius: Double, abilities: GlideAbilities?, direct: Boolean): Glide? {
        if (abilities == null) {
            planner.cancel()
            return null
        }

        glide = cruise(goal, abilities, direct) ?: planned(goal, abilities)
        if (glide == null) {
            planIfUseful(goal, radius, abilities, direct)
        }
        return glide
    }

    /** A rocket cruise to a [goal] too far for one glide or not loaded, under an open sky, if the rockets reach. */
    private fun cruise(goal: Vec3, abilities: GlideAbilities, direct: Boolean): Glide? {
        if (direct || ticks < retryAt || !player.onGround() || world.dimensionType().hasCeiling()) {
            return null
        }

        val start = player.position()
        val distance = start.horizontalDistanceTo(goal)
        val loaded = world.hasChunkAt(BlockPos.containing(goal))
        if (distance < MIN_DISTANCE || distance <= MAX_DISTANCE && loaded) {
            return null
        }

        val climb = ElytraCruise.cruiseAltitude(start, goal, liveHeights) - start.y
        if (abilities.rockets < ElytraCruise.rocketsNeeded(distance, climb, abilities.rocketTicks)) {
            return null
        }

        planner.cancel()
        val rockets = GlideRockets(abilities.rockets, abilities.rocketTicks)
        return Glide(goal, ElytraCruise(goal, liveHeights, rockets, world.maxY, ::groundNear))
    }

    private fun planned(goal: Vec3, abilities: GlideAbilities): Glide? = when (val result = planner.poll()) {
        RouteResult.Searching -> null

        is RouteResult.Found -> {
            planner.cancel()
            val path = result.value
            // A glide found for where the player stood a moment ago is searched again.
            if (player.onGround() && player.position().distanceTo(path.start) <= START_TOLERANCE) {
                val rockets = GlideRockets(path.rockets, abilities.rocketTicks)
                Glide(goal, GlideControl(goal, rockets, path.approach), path)
            } else {
                null
            }
        }

        RouteResult.AlreadyThere, is RouteResult.Failed -> {
            planner.cancel()
            retryAt = ticks + RETRY_TICKS
            null
        }
    }

    private fun planIfUseful(goal: Vec3, radius: Double, abilities: GlideAbilities, direct: Boolean) {
        if (!planner.isIdle || ticks < retryAt || direct || !player.onGround()) {
            return
        }

        val start = player.position()
        val distance = start.horizontalDistanceTo(goal)
        val drop = start.y - goal.y
        if (distance < MIN_DISTANCE || distance > MAX_DISTANCE || drop < MIN_DROP && abilities.rockets == 0) {
            return
        }

        planner.request(BlockPos.containing(start) to goal, GlideRouteSearch.region(start, goal)) { terrain ->
            GlideRouteSearch(terrain, start, goal, radius, abilities)
        }
    }

    /** Jumps and opens the elytra in the air, then leaves the steering to [rotationHandler] until touch-down. */
    private fun fly(event: MovementInputEvent, glide: Glide, frameYaw: Float): Boolean {
        glide.ticks++
        if (player.isFallFlying) {
            glide.hasOpened = true
        } else if (glide.hasOpened || glide.ticks > TAKE_OFF_TICKS) {
            // Touched down, or the elytra did not open.
            if (!glide.hasOpened) {
                retryAt = ticks + RETRY_TICKS
            }
            halt()
            return false
        }

        event.sneak = false
        if (glide.hasOpened) {
            event.directionalInput = DirectionalInput.NONE
            event.jump = false
        } else {
            // Jump, let go and jump again in the air, which opens the elytra.
            event.directionalInput = steer(glide.goal.subtract(player.position()), frameYaw)
            event.jump = glide.ticks % 2 == 1
        }
        sprint = !glide.hasOpened

        view.breaks = emptyList()
        view.places = emptyList()
        view.active(glide.points, progress)
        return true
    }

    @Suppress("unused")
    private val rotationHandler = handler<RotationUpdateEvent> {
        val glide = glide ?: return@handler

        val rotation = if (player.isFallFlying) {
            val input = glide.steering.steer(player.position(), player.deltaMovement)
            if (input.fireRocket) {
                Slots.OffhandWithHotbar.findSlot(Items.FIREWORK_ROCKET)?.let { useHotbarSlotOrOffhand(it) }
            }
            Rotation(input.yaw, input.pitch)
        } else {
            Rotation(glide.goal.subtract(player.position()).yaw, 0f)
        }

        RotationManager.setRotationTarget(
            RotationTarget(rotation, movementCorrection = MovementCorrection.STRICT),
            Priority.IMPORTANT_FOR_USAGE_2,
            owner,
        )
    }

    private val liveHeights = ColumnHeights { x, z ->
        if (world.hasChunkAt(x, z)) world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) else null
    }

    /** The loaded ground closest to [position] to glide down to, if any. */
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

    private val progress = object : RouteProgress {
        override val isMoving: Boolean
            get() = isActive

        override val ahead: List<Vec3>
            get() = listOfNotNull(glide?.landing)

        override fun head(partialTicks: Float): Vec3 = player.interpolateCurrentPosition(partialTicks)
    }

    /** A glide from take-off to touch-down: planned along [path], or a cruise without one. */
    private class Glide(val goal: Vec3, val steering: GlideSteering, private val path: GlidePath? = null) {
        var ticks = 0
        var hasOpened = false

        val points: List<Vec3>
            get() = path?.points ?: listOf(player.position(), steering.target)

        val landing: Vec3
            get() = path?.landing ?: steering.target
    }

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
