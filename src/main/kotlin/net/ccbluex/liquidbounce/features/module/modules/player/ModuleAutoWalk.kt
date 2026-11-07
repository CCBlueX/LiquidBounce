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
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.CRITICAL_MODIFICATION
import net.ccbluex.liquidbounce.utils.navigation.terrain.LiveWorld
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.ccbluex.liquidbounce.utils.navigation.walk.WalkNavigator
import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

/**
 * AutoWalk module
 *
 * Automatically makes you walk: straight ahead, pathfinding the way you are facing, to a destination or after a
 * player.
 */
object ModuleAutoWalk : ClientModule("AutoWalk", ModuleCategories.PLAYER) {

    private val modes = choices("Mode", Forward, arrayOf(Forward, Direction, Goto, Follow)).apply(::tagBy)
    private val navigator = WalkNavigator(this, this)

    /**
     * Walks to [position] and turns off on arrival.
     */
    fun goTo(position: Vec3) {
        val block = BlockPos.containing(position)
        // A BlockPos would no longer load from configs, which read the setting as a Vec3i
        Goto.destination = Vec3i(block.x, block.y, block.z)
        modes.setByString(Goto.tag)
        enabled = true
    }

    /**
     * Walks after the player named [name].
     */
    fun follow(name: String) {
        Follow.target = name
        modes.setByString(Follow.tag)
        enabled = true
    }

    private object Forward : Mode("Forward") {
        override val parent: ModeValueGroup<*>
            get() = modes

        @Suppress("unused")
        private val moveInputHandler = handler<MovementInputEvent>(priority = CRITICAL_MODIFICATION) { event ->
            event.directionalInput = event.directionalInput.copy(forwards = true)
        }
    }

    /**
     * Pathfinds to [goal] every tick.
     */
    private abstract class NavigationMode(name: String) : Mode(name) {
        override val parent: ModeValueGroup<*>
            get() = modes

        protected open val radius: Double
            get() = 1.0

        protected abstract fun goal(): Vec3?

        @Suppress("unused")
        private val moveInputHandler = handler<MovementInputEvent>(priority = CRITICAL_MODIFICATION) { event ->
            val goal = goal()
            if (goal == null) {
                navigator.halt()
            } else {
                navigator.navigate(event, goal, radius)
            }
        }

        override fun disable() {
            navigator.reset()
        }
    }

    /**
     * Keeps a goal on ground up to [LOOKAHEAD] blocks the way the camera faces, so the walk follows where you look
     * and goes around whatever is in the way. The goal moves on once [REFRESH] blocks are left to it or the view
     * turned, early enough that the next path is found before the current one runs out.
     */
    private object Direction : NavigationMode("Direction") {
        private var goal: Vec3? = null
        private var goalYaw = 0f

        override val radius: Double
            get() = RADIUS

        override fun disable() {
            super.disable()
            goal = null
        }

        override fun goal(): Vec3 {
            val position = player.position()
            val yaw = player.yRot
            val goal = goal
            if (goal != null && horizontalDistanceSqr(goal, position) > REFRESH * REFRESH &&
                abs(Mth.wrapDegrees(yaw - goalYaw)) <= MAX_TURN
            ) {
                return goal
            }

            goalYaw = yaw
            return ahead(position, yaw).also { this.goal = it }
        }

        /**
         * The farthest spot [NEAREST] to [LOOKAHEAD] blocks the way [yaw] faces to stand on near the height of
         * [position], else the point [LOOKAHEAD] blocks ahead.
         */
        private fun ahead(position: Vec3, yaw: Float): Vec3 {
            val terrain = NavigationTerrain(LiveWorld(world))
            val direction = Rotation(yaw, 0f).directionVector
            val y = Mth.floor(position.y)

            var distance = LOOKAHEAD
            while (distance >= NEAREST) {
                val point = position.add(direction.scale(distance))
                val x = Mth.floor(point.x)
                val z = Mth.floor(point.z)
                for (dy in HEIGHTS) {
                    val height = terrain.standHeight(x, y + dy, z)
                    if (!height.isNaN()) {
                        return Vec3(point.x, height, point.z)
                    }
                }
                distance -= STRIDE
            }

            return position.add(direction.scale(LOOKAHEAD))
        }

        private fun horizontalDistanceSqr(a: Vec3, b: Vec3): Double {
            val dx = a.x - b.x
            val dz = a.z - b.z
            return dx * dx + dz * dz
        }

        private const val LOOKAHEAD = 16.0
        private const val NEAREST = 10.0
        private const val STRIDE = 2.0
        private const val REFRESH = 6.0
        private const val RADIUS = 2.0
        private const val MAX_TURN = 20f
        private val HEIGHTS = intArrayOf(0, 1, -1, 2, -2, 3, -3, 4, -4)
    }

    /**
     * Walks to [destination], set here, with the locate button or with `.goto`, and turns off on arrival.
     */
    private object Goto : NavigationMode("Goto") {
        var destination by vec3i("Destination")

        private var goalOf: Vec3i? = null
        private var goal = Vec3.ZERO

        override fun goal(): Vec3? {
            val destination = destination
            if (destination != goalOf) {
                val pos = BlockPos(destination.x, destination.y, destination.z)
                goal = Vec3.atBottomCenterOf(feetAt(pos))
                // Whether the destination is the ground to stand on is only known once its chunk is loaded.
                goalOf = destination.takeIf { world.hasChunkAt(pos.x, pos.z) }
            }

            if (player.rootVehicle.position().distanceTo(goal) > radius) {
                return goal
            }

            notification(
                ModuleAutoWalk.name,
                message("arrived", destination.x, destination.y, destination.z),
                NotificationEvent.Severity.SUCCESS
            )
            ModuleAutoWalk.enabled = false
            return null
        }

        /**
         * [pos], or the block above it when [pos] is the ground to stand on, as the locate button picks it.
         */
        private fun feetAt(pos: BlockPos): BlockPos {
            val terrain = NavigationTerrain(LiveWorld(world))
            val above = pos.above()
            return if (!terrain.isStandable(pos) && terrain.isStandable(above)) above else pos
        }
    }

    private object Follow : NavigationMode("Follow") {
        var target by text("Player", "")
        private val distance by float("Distance", 3f, 1f..10f)

        /**
         * Where [target] was last seen, to walk on to once it goes out of sight.
         */
        private var lastSeen: Vec3? = null

        override val radius: Double
            get() = distance.toDouble()

        override fun disable() {
            super.disable()
            lastSeen = null
        }

        override fun goal(): Vec3? {
            val entity = world.players().find { it !== player && it.gameProfile.name.equals(target, true) }
            if (entity != null) {
                lastSeen = entity.position()
            }

            return lastSeen
        }
    }

}
