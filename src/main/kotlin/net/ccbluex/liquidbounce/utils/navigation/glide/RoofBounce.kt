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

package net.ccbluex.liquidbounce.utils.navigation.glide

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
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.copy
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceTo
import net.ccbluex.liquidbounce.utils.math.yaw
import net.ccbluex.liquidbounce.utils.movement.getDirectionalInputTowards
import net.ccbluex.liquidbounce.utils.navigation.render.RouteView
import net.ccbluex.liquidbounce.utils.navigation.render.TargetProgress
import net.ccbluex.liquidbounce.utils.navigation.terrain.LiveWorld
import net.ccbluex.liquidbounce.utils.navigation.terrain.Terrain
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.min

/**
 * Bounces along a flat floor, such as the Nether roof, with an elytra and [ModuleElytraRecast], which reopens the
 * elytra each time the feet touch the floor: much faster than running. The rest of the way is left to walking.
 */
class RoofBounce(private val owner: EventListener, private val view: RouteView) : EventListener {

    private var bounce: Bounce? = null

    val isActive: Boolean
        get() = bounce != null

    override fun parent() = owner

    /** Bounces one tick towards [goal] while an elytra is worn and ElytraRecast runs; false to walk. */
    fun navigate(event: MovementInputEvent, goal: Vec3, abilities: Abilities?, frameYaw: Float): Boolean {
        val position = player.position()
        val active = bounce
        val bounce = when {
            abilities == null || !ModuleElytraRecast.running -> null
            active == null -> start(position, goal)
            else -> keep(active, position, goal)
        }
        this.bounce = bounce
        if (bounce == null) {
            return false
        }

        bounce.ticks++
        event.directionalInput = getDirectionalInputTowards(goal.subtract(position), frameYaw)
        event.sneak = false
        // Opening the elytra takes a fresh jump press in the air; after that ElytraRecast reopens it on every bounce.
        event.jump = player.isFallFlying || player.onGround() || bounce.ticks % 2 == 0

        view.breaks = emptyList()
        view.places = emptyList()
        view.active(listOf(position, bounce.lineEnd), progress)
        return true
    }

    fun halt() {
        bounce = null
    }

    private fun start(position: Vec3, goal: Vec3): Bounce? {
        if (!player.onGround() || position.horizontalDistanceTo(goal) < MIN_DISTANCE) {
            return null
        }

        val end = bounceLine(Terrain(LiveWorld(world)), position, goal, LOOKAHEAD) ?: return null
        return Bounce(goal, floor = position.y, end)
    }

    /** [bounce] with its line moved on, or null once the goal is near or the floor ends. */
    private fun keep(bounce: Bounce, position: Vec3, goal: Vec3): Bounce? {
        if (position.horizontalDistanceTo(goal) < STOP_DISTANCE || position.y < bounce.floor - FLOOR_TOLERANCE) {
            return null
        }

        val feet = position.copy(y = bounce.floor)
        bounce.lineEnd = bounceLine(Terrain(LiveWorld(world)), feet, goal, KEEP_LOOKAHEAD) ?: return null
        return bounce
    }

    @Suppress("unused")
    private val rotationHandler = handler<RotationUpdateEvent> {
        val bounce = bounce ?: return@handler
        RotationManager.setRotationTarget(
            RotationTarget(
                Rotation(bounce.goal.subtract(player.position()).yaw, PITCH),
                movementCorrection = MovementCorrection.STRICT,
            ),
            Priority.IMPORTANT_FOR_USAGE_2,
            owner,
        )
    }

    private val progress = TargetProgress({ isActive }) { bounce?.lineEnd }

    private class Bounce(val goal: Vec3, val floor: Double, var lineEnd: Vec3) {
        var ticks = 0
    }

    companion object {
        // The pitch that bounces fastest.
        private const val PITCH = 75f
        private const val MIN_DISTANCE = 32.0
        private const val STOP_DISTANCE = 12.0
        private const val LOOKAHEAD = 24.0
        private const val KEEP_LOOKAHEAD = 12.0
        private const val MIN_KNOWN = 8.0
        private const val STEP = 0.5
        private const val FLAT = 1.0E-3
        private const val FLOOR_TOLERANCE = 0.5
        private const val HEADROOM = 2
        private const val HALF_WIDTH = 0.4
        private val SIDES = doubleArrayOf(-HALF_WIDTH, 0.0, HALF_WIDTH)

        /** The point towards [goal], up to [lookahead] blocks on, that the flat open floor under [feet] runs to. */
        fun bounceLine(terrain: Terrain, feet: Vec3, goal: Vec3, lookahead: Double): Vec3? {
            val dx = goal.x - feet.x
            val dz = goal.z - feet.z
            val length = Mth.length(dx, dz)
            if (length < STEP) {
                return null
            }

            val ux = dx / length
            val uz = dz / length
            val reach = min(lookahead, length)
            val y = Mth.floor(feet.y + FLAT)
            var along = 0.0
            while (along <= reach) {
                for (side in SIDES) {
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
    }

}
