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
import net.ccbluex.liquidbounce.features.client.clientState
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.block.toBlockPos
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.CRITICAL_MODIFICATION
import net.ccbluex.liquidbounce.utils.math.fma
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceToSqr
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
 * Walks straight ahead, the way you are facing, to a destination or after a player.
 */
object ModuleAutoWalk : ClientModule("AutoWalk", ModuleCategories.PLAYER) {

    private val modes = choices("Mode", Forward, arrayOf(Forward, Direction, Goto, Follow)).apply(::tagBy)
    private val navigator = WalkNavigator(this, this)

    fun goTo(position: Vec3) {
        val block = BlockPos.containing(position)
        // Configs read the setting as a Vec3i and would no longer load a BlockPos
        Goto.destination = Vec3i(block.x, block.y, block.z)
        modes.setByString(Goto.tag)
        enabled = true
    }

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

    private abstract class NavigationMode(name: String) : Mode(name) {
        override val parent: ModeValueGroup<*>
            get() = modes

        protected open val radius: Double
            get() = 1.0

        protected abstract fun goal(): Vec3?

        @Suppress("unused")
        private val moveInputHandler = handler<MovementInputEvent>(priority = CRITICAL_MODIFICATION) { event ->
            val goal = goal() ?: return@handler navigator.halt()
            navigator.navigate(event, goal, radius)
        }

        override fun disable() {
            navigator.reset()
        }
    }

    /**
     * Keeps a goal [DISTANCE] blocks the way the camera faces, set again once the view turned or half the way is
     * walked.
     */
    private object Direction : NavigationMode("Direction") {
        private var goal: Vec3? by clientState { null }
        private var goalYaw by clientState { 0f }

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
            if (goal != null && goal.horizontalDistanceToSqr(position) > REFRESH * REFRESH &&
                abs(Mth.wrapDegrees(yaw - goalYaw)) <= MAX_TURN
            ) {
                return goal
            }

            goalYaw = yaw
            return position.fma(DISTANCE, Rotation(yaw, 0f).directionVector).also { this.goal = it }
        }

        private const val DISTANCE = 1024.0
        private const val REFRESH = 512.0
        private const val RADIUS = 2.0
        private const val MAX_TURN = 20f
    }

    /** Turns the module off on arrival. */
    private object Goto : NavigationMode("Goto") {
        var destination by vec3i("Destination")

        private var goalOf: Vec3i? by clientState { null }
        private var goal by clientState { Vec3.ZERO }

        override fun goal(): Vec3? {
            val destination = destination
            if (destination != goalOf) {
                val pos = destination.toBlockPos()
                goal = Vec3.atBottomCenterOf(feetAt(pos))
                // Whether the destination is the ground to stand on is only known once its chunk is loaded.
                goalOf = destination.takeIf { world.hasChunkAt(pos.x, pos.z) }
            }

            if (!player.rootVehicle.position().closerThan(goal, radius)) {
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

        // The locate button picks the ground to stand on.
        private fun feetAt(pos: BlockPos): BlockPos {
            val terrain = NavigationTerrain.live()
            val above = pos.above()
            return if (!terrain.isStandable(pos) && terrain.isStandable(above)) above else pos
        }
    }

    private object Follow : NavigationMode("Follow") {
        var target by text("Player", "")
        private val distance by float("Distance", 3f, 1f..10f)

        /** Walked on to once [target] goes out of sight. */
        private var lastSeen: Vec3? by clientState { null }

        override val radius: Double
            get() = distance.toDouble()

        override fun disable() {
            super.disable()
            lastSeen = null
        }

        override fun goal(): Vec3? {
            world.players().find { it !== player && it.gameProfile.name.equals(target, true) }
                ?.let { lastSeen = it.position() }
            return lastSeen
        }
    }

}
