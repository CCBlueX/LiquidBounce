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
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleElytraRecast
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationTarget
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.yaw
import net.ccbluex.liquidbounce.utils.navigation.render.RouteProgress
import net.ccbluex.liquidbounce.utils.navigation.render.RouteView
import net.ccbluex.liquidbounce.utils.navigation.terrain.LiveWorld
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.ccbluex.liquidbounce.utils.navigation.walk.steer
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Bounces along a flat floor, such as the Nether roof, with an elytra and [ModuleElytraRecast]: holding jump reopens
 * the elytra each time the feet touch the floor, which carries the player much faster than running. It bounces while
 * the floor ahead stays flat and open and the goal is far, and leaves the rest to walking.
 */
class RoofBounce(private val owner: EventListener, private val view: RouteView) : EventListener {

    private var goal: Vec3? = null
    private var floor = Double.NaN
    private var lineEnd: Vec3? = null
    private var ticks = 0

    val isActive: Boolean
        get() = goal != null

    override fun parent() = owner

    /**
     * Bounces one tick towards [goal] while an elytra is worn ([abilities]) and ElytraRecast runs; false to walk.
     */
    fun navigate(event: MovementInputEvent, goal: Vec3, abilities: GlideAbilities?, frameYaw: Float): Boolean {
        if (abilities == null || !ModuleElytraRecast.running) {
            halt()
            return false
        }

        val position = player.position()
        val distance = horizontalDistance(position, goal)
        val terrain = NavigationTerrain(LiveWorld(world))
        if (!isActive) {
            if (!player.onGround() || distance < MIN_DISTANCE) {
                return false
            }
            val end = bounceLine(terrain, position, goal, LOOKAHEAD) ?: return false
            this.goal = goal
            floor = position.y
            lineEnd = end
            ticks = 0
        } else {
            val feet = Vec3(position.x, floor, position.z)
            val end = bounceLine(terrain, feet, goal, KEEP_LOOKAHEAD)
            if (distance < STOP_DISTANCE || end == null || position.y < floor - FLOOR_TOLERANCE) {
                halt()
                return false
            }
            lineEnd = end
        }

        ticks++
        event.directionalInput = steer(goal.subtract(position), frameYaw)
        event.sneak = false
        // Opening the elytra takes a fresh jump press in the air; after that ElytraRecast reopens it on every bounce.
        event.jump = player.isFallFlying || player.onGround() || ticks % 2 == 0

        view.breaks = emptyList()
        view.places = emptyList()
        view.active(listOfNotNull(position, lineEnd), progress)
        return true
    }

    @Suppress("unused")
    private val rotationHandler = handler<RotationUpdateEvent> {
        val goal = goal ?: return@handler
        RotationManager.setRotationTarget(
            RotationTarget(
                rotation = Rotation(goal.subtract(player.position()).yaw, PITCH),
                ticksUntilReset = 1,
                resetThreshold = 1f,
                considerInventory = false,
                movementCorrection = MovementCorrection.STRICT,
            ),
            Priority.IMPORTANT_FOR_USAGE_2,
            owner,
        )
    }

    fun halt() {
        goal = null
        floor = Double.NaN
        lineEnd = null
        ticks = 0
    }

    private val progress = object : RouteProgress {
        override val isMoving: Boolean
            get() = isActive

        override fun head(partialTicks: Float): Vec3 = player.interpolateCurrentPosition(partialTicks)

        override val ahead: List<Vec3>
            get() = listOfNotNull(lineEnd)
    }

    companion object {

        /**
         * The pitch that bounces fastest.
         */
        const val PITCH = 75f

        private const val MIN_DISTANCE = 32.0
        private const val STOP_DISTANCE = 12.0
        private const val LOOKAHEAD = 24.0
        private const val KEEP_LOOKAHEAD = 12.0
        private const val MIN_KNOWN = 8.0
        private const val STEP = 0.5
        private const val HALF_WIDTH = 0.4
        private const val FLAT = 1.0E-3
        private const val FLOOR_TOLERANCE = 0.5
        private const val HEADROOM = 2

        /**
         * Where the floor under [feet] runs on flat and open towards [goal] for up to [lookahead] blocks, wide enough
         * for the player and with room to bounce; null when it does not reach [MIN_KNOWN] blocks or the goal.
         */
        fun bounceLine(terrain: NavigationTerrain, feet: Vec3, goal: Vec3, lookahead: Double): Vec3? {
            val dx = goal.x - feet.x
            val dz = goal.z - feet.z
            val length = sqrt(dx * dx + dz * dz)
            if (length < STEP) {
                return null
            }

            val ux = dx / length
            val uz = dz / length
            val reach = min(lookahead, length)
            val y = Mth.floor(feet.y + FLAT)
            var along = 0.0
            while (along <= reach) {
                for (side in doubleArrayOf(-HALF_WIDTH, 0.0, HALF_WIDTH)) {
                    val x = Mth.floor(feet.x + ux * along - uz * side)
                    val z = Mth.floor(feet.z + uz * along + ux * side)
                    if (!terrain.isKnown(x, y, z)) {
                        return if (along >= MIN_KNOWN) at(feet, ux, uz, along) else null
                    }
                    val stand = terrain.standHeight(x, y, z)
                    if (stand.isNaN() || abs(stand - feet.y) > FLAT || !terrain.isPassable(x, y + HEADROOM, z)) {
                        return null
                    }
                }
                along += STEP
            }
            return at(feet, ux, uz, reach)
        }

        private fun at(feet: Vec3, ux: Double, uz: Double, along: Double) =
            Vec3(feet.x + ux * along, feet.y, feet.z + uz * along)

        private fun horizontalDistance(a: Vec3, b: Vec3): Double {
            val dx = a.x - b.x
            val dz = a.z - b.z
            return sqrt(dx * dx + dz * dz)
        }
    }

}
