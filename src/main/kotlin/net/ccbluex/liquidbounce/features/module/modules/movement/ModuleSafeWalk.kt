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
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.NoneMode
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerSafeWalkEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugGeometry
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.ccbluex.liquidbounce.utils.entity.horizontalSpeed
import net.ccbluex.liquidbounce.utils.entity.isCloseToEdge
import net.ccbluex.liquidbounce.utils.entity.wouldBeCloseToFallOff
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.movement.getDegreesRelativeToView
import net.ccbluex.liquidbounce.utils.movement.getDirectionalInputForDegrees
import net.minecraft.world.phys.Vec3
import kotlin.math.min

/**
 * SafeWalk module
 *
 * Prevents you from falling down as if you were sneaking.
 */
object ModuleSafeWalk : ClientModule("SafeWalk", ModuleCategories.MOVEMENT) {

    @Suppress("UnusedPrivateProperty")
    private val modes = choices("Mode", 1, ::safeWalkChoices) // Default safe mode

    fun safeWalkChoices(mode: ModeValueGroup<Mode>): Array<Mode> {
        return arrayOf(
            NoneMode(mode),
            Safe(mode),
            OnEdge(mode)
        )
    }

    class Safe(override val parent: ModeValueGroup<Mode>) : Mode("Safe") {

        @Suppress("unused")
        val safeWalkHandler = handler<PlayerSafeWalkEvent> { event ->
            event.isSafeWalk = true
        }

    }

    class OnEdge(override val parent: ModeValueGroup<Mode>) : Mode("OnEdge") {

        private val edgeDistance by float("Distance", 0.1f, 0.1f..0.5f)
        private var center: Vec3? = null

        private enum class OnEdgeMode(override val tag: String) : Tagged {
            STOP("Stop"),
            INVERT("Invert"),
            CENTER("Center"),
        }

        /**
         * Defines how many ticks we should keep running the [mode]
         */
        private var keepTicks by intRange("Keep", 1..2, 1..20, suffix = "ticks")
        private var overwriteTicks = 0

        private var mode by enumChoice("Mode", OnEdgeMode.STOP)
        private var sneak by intRange("Sneak", 0..0, 0..20, suffix = "ticks")
        private var sneakTicks = 0
        private var jump by boolean("Jump", false)

        /**
         * The input handler tracks the movement of the player and calculates the predicted future position.
         */
        @Suppress("unused")
        val inputHandler = handler<MovementInputEvent>(
            priority = EventPriorityConvention.OBJECTION_AGAINST_EVERYTHING
        ) { event ->
            val shouldBeActive = player.onGround() && !event.sneak
            if (shouldBeActive) {
                val isOnEdge = player.isCloseToEdge(
                    event.directionalInput,
                    min(player.horizontalSpeed, edgeDistance.toDouble())
                )
                if (isOnEdge) {
                    debugParameter("InputOnEdge") { event.directionalInput }

                    val center = center
                    if (center != null) {
                        val nextTick = PlayerSimulationCache.getSimulationForLocalPlayer().getSnapshotAt(1)
                        debugGeometry("Center") {
                            ModuleDebug.DebuggedPoint(center, Color4b.BLUE, 0.05)
                        }

                        val currentDistance = center.subtract(player.position()).horizontalDistanceSqr()
                        val nextDistance = center.subtract(nextTick.pos).horizontalDistanceSqr()

                        debugParameter("CurrentDistance") { currentDistance }
                        debugParameter("NextDistance") { nextDistance }

                        if (nextDistance <= currentDistance) {
                            return@handler
                        }
                    }

                    if (overwriteTicks == 0) {
                        overwriteTicks = keepTicks.random()
                    }

                    if (sneakTicks == 0) {
                        sneakTicks = sneak.random()
                    }
                }
            }

            if (overwriteTicks > 0) {
                debugParameter("OverwriteInputTicks") { overwriteTicks }
                overwriteTicks--

                when {
                    mode == OnEdgeMode.INVERT -> {
                        event.directionalInput = event.directionalInput.invert()
                        event.jump = false
                    }
                    (mode == OnEdgeMode.CENTER || player.horizontalSpeed > 0.05) -> {
                        val center = center ?: player.blockPosition().bottomCenter
                        val degrees = getDegreesRelativeToView(
                            center.subtract(player.position()),
                            player.yRot
                        )
                        event.directionalInput = getDirectionalInputForDegrees(
                            DirectionalInput.NONE,
                            degrees, deadAngle = 20.0F
                        )
                    }
                    // [STOP] and [SNEAK] mode is not powerful enough to prevent falling off
                    // so we use [CENTER] to fix speed when the player is moving too fast
                    mode == OnEdgeMode.STOP -> {
                        event.directionalInput = DirectionalInput.NONE
                        event.jump = false
                    }
                }

                // Can do cool tricks with jumping
                if (jump) {
                    event.jump = true
                }
            }

            if (sneakTicks > 0) {
                sneakTicks--
                event.sneak = true
            }

            // Find last good position to stand on
            val blockPos = player.blockPosition().bottomCenter
            if (!player.wouldBeCloseToFallOff(blockPos)) {
                center = blockPos
            }
        }

        override fun disable() {
            center = null
            overwriteTicks = 0
            super.disable()
        }

    }

}
