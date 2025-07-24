/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.NoneChoice
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerSafeWalkEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugGeometry
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.ccbluex.liquidbounce.utils.entity.isCloseToEdge
import net.ccbluex.liquidbounce.utils.entity.sqrtSpeed
import net.ccbluex.liquidbounce.utils.entity.wouldBeCloseToFallOff
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.movement.getDegreesRelativeToView
import net.ccbluex.liquidbounce.utils.movement.getDirectionalInputForDegrees
import net.minecraft.util.math.Vec3d
import kotlin.math.min

/**
 * SafeWalk module
 *
 * Prevents you from falling down as if you were sneaking.
 */
object ModuleSafeWalk : ClientModule("SafeWalk", Category.MOVEMENT) {

    @Suppress("UnusedPrivateProperty")
    private val modes = choices("Mode", 1, ::safeWalkChoices) // Default safe mode

    fun safeWalkChoices(choice: ChoiceConfigurable<Choice>): Array<Choice> {
        return arrayOf(
            NoneChoice(choice),
            Safe(choice),
            OnEdge(choice)
        )
    }

    class Safe(override val parent: ChoiceConfigurable<Choice>) : Choice("Safe") {

        @Suppress("unused")
        val safeWalkHandler = handler<PlayerSafeWalkEvent> { event ->
            event.isSafeWalk = true
        }

    }

    class OnEdge(override val parent: ChoiceConfigurable<Choice>) : Choice("OnEdge") {

        private val edgeDistance by float("Distance", 0.1f, 0.1f..0.5f)
        private var center: Vec3d? = null

        private enum class Mode(override val choiceName: String) : NamedChoice {
            STOP("Stop"),
            INVERT("Invert"),
            CENTER("Center"),
        }

        /**
         * Defines how many ticks we should keep running the [mode]
         */
        private var keepTicks by intRange("Keep", 1..2, 1..20, suffix = "ticks")
        private var overwriteTicks = 0

        private var mode by enumChoice("Mode", Mode.STOP)
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
            val shouldBeActive = player.isOnGround && !event.sneak
            if (shouldBeActive) {
                val isOnEdge = player.isCloseToEdge(
                    event.directionalInput,
                    min(player.sqrtSpeed, edgeDistance.toDouble())
                )
                if (isOnEdge) {
                    debugParameter("InputOnEdge") { event.directionalInput }

                    val center = center
                    if (center != null) {
                        val nextTick = PlayerSimulationCache.getSimulationForLocalPlayer().getSnapshotAt(1)
                        debugGeometry("Center") {
                            ModuleDebug.DebuggedPoint(center, Color4b.BLUE, 0.05)
                        }

                        val currentDistance = center.subtract(player.pos).horizontalLengthSquared()
                        val nextDistance = center.subtract(nextTick.pos).horizontalLengthSquared()

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
                    mode == Mode.INVERT -> {
                        event.directionalInput = event.directionalInput.invert()
                        event.jump = false
                    }
                    (mode == Mode.CENTER || player.sqrtSpeed > 0.05) -> {
                        val center = center ?: player.blockPos.toBottomCenterPos()
                        val degrees = getDegreesRelativeToView(
                            center.subtract(player.pos),
                            player.yaw
                        )
                        event.directionalInput = getDirectionalInputForDegrees(
                            DirectionalInput.NONE,
                            degrees, deadAngle = 20.0F
                        )
                    }
                    // [STOP] and [SNEAK] mode is not powerful enough to prevent falling off
                    // so we use [CENTER] to fix speed when the player is moving too fast
                    mode == Mode.STOP -> {
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
            val blockPos = player.blockPos.toBottomCenterPos()
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
