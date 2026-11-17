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

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.NoneMode
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.config.utils.asRefreshable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerSafeWalkEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.render.debug.DebuggedPoint
import net.ccbluex.liquidbounce.features.module.modules.render.debug.debugGeometry
import net.ccbluex.liquidbounce.features.module.modules.render.debug.debugParameter
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.entity.horizontalSpeed
import net.ccbluex.liquidbounce.utils.entity.isCloseToEdge
import net.ccbluex.liquidbounce.utils.entity.wouldBeCloseToFallOff
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.math.bottomCenter
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
    private val modes = choices("Mode", 1) { safeWalkChoices(it) } // Default safe mode

    /**
     * [stops] tells the modes whether an edge has to stop the player at all, Scaffold says no while it places the
     * next block in time. A caller placing blocks passes its [blockCount] and gets [Ledge] as well.
     */
    fun safeWalkChoices(
        mode: ModeValueGroup<Mode>,
        stops: () -> Boolean = { true },
        blockCount: (() -> Int)? = null,
    ): Array<Mode> {
        val choices = arrayOf(
            NoneMode(mode),
            Safe(mode, stops),
            OnEdge(mode, stops),
            Sneak(mode, stops),
        )
        return if (blockCount != null) choices + Ledge(mode, stops, blockCount) else choices
    }

    private class Safe(override val parent: ModeValueGroup<Mode>, private val stops: () -> Boolean) : Mode("Safe") {

        @Suppress("unused")
        val safeWalkHandler = handler<PlayerSafeWalkEvent> { event ->
            event.isSafeWalk = stops()
        }

    }

    /**
     * Sneaks at an edge. Anticheats simulate that, while [Safe] holds the player back without sneaking.
     */
    private class Sneak(override val parent: ModeValueGroup<Mode>, private val stops: () -> Boolean) : Mode("Sneak") {

        private var sneaked = false

        @Suppress("unused")
        val inputHandler = handler<MovementInputEvent>(priority = EventPriorityConvention.SAFETY_FEATURE) { event ->
            val sneaks = player.onGround() && player.isCloseToEdge(event.directionalInput) && stops()
            // One tick longer, so it doesn't stand up on the edge it just stopped at
            if (sneaks || sneaked) {
                event.sneak = true
            }
            sneaked = sneaks
        }

        override fun disable() {
            sneaked = false
            super.disable()
        }

    }

    /**
     * Walks over the edge as if nothing was there and only acts on the last tick before leaving it, when [stops]
     * says the block won't go down in time: one of the [actions], picked at random.
     */
    private class Ledge(
        override val parent: ModeValueGroup<Mode>,
        private val stops: () -> Boolean,
        private val blockCount: () -> Int,
    ) : Mode("Ledge") {

        private enum class Action(override val tag: String) : Tagged {
            JUMP("Jump"),
            SNEAK("Sneak"),

            /** Lets go of the keys, the player still slides a little. */
            STOP_INPUT("StopInput"),
            BACKWARDS("Backwards"),
        }

        private val actions by multiEnumChoice("Actions", Action.SNEAK, canBeNone = false)

        /** Below this many blocks a jump could leave the player in the air without one, so it sneaks. */
        private val sneakBelowBlocks by int("SneakBelowBlocks", 3, 0..10)
        private val sneakTime by intRange("SneakTime", 1..1, 1..10, suffix = "ticks")
        private var sneakTicks = 0

        @Suppress("unused")
        val inputHandler = handler<MovementInputEvent>(priority = EventPriorityConvention.SAFETY_FEATURE) { event ->
            if (sneakTicks > 0) {
                sneakTicks--
                event.sneak = true
                return@handler
            }

            val leaves = player.onGround() && event.directionalInput.isMoving && leavesEdge(event)
            if (!leaves || !stops()) {
                return@handler
            }

            val action = if (blockCount() < sneakBelowBlocks) Action.SNEAK else actions.random()
            debugParameter("Action") { action }
            when (action) {
                Action.JUMP -> event.jump = true
                Action.SNEAK -> {
                    event.sneak = true
                    sneakTicks = sneakTime.random() - 1
                }
                Action.STOP_INPUT -> event.directionalInput = DirectionalInput.NONE
                Action.BACKWARDS -> event.directionalInput = event.directionalInput.invert()
            }
        }

        /**
         * Whether the step [event] asks for leaves the block, so that the next one falls. Just short of leaving counts
         * too: the simulation is off by a little now and then.
         */
        private fun leavesEdge(event: MovementInputEvent): Boolean {
            val input = SimulatedPlayer.SimulatedPlayerInput(
                event.directionalInput,
                jumping = false,
                sprinting = player.isSprinting,
                sneaking = false,
            )
            val simulation = SimulatedPlayer.fromClientPlayer(input)
            simulation.tick()
            return simulation.clipLedged || player.wouldBeCloseToFallOff(simulation.pos)
        }

        override fun disable() {
            sneakTicks = 0
            super.disable()
        }

    }

    private class OnEdge(override val parent: ModeValueGroup<Mode>, private val stops: () -> Boolean) : Mode("OnEdge") {

        private val edgeDistance = floatRange("Distance", 0.1f..0.15f, 0.05f..0.5f).asRefreshable()
        private var center: Vec3? = null

        private enum class OnEdgeMode(override val tag: String) : Tagged {
            STOP("Stop"),
            INVERT("Invert"),
            CENTER("Center"),
        }

        /**
         * Defines how many ticks we should keep running the [mode]
         */
        private val keepTicks by intRange("Keep", 1..2, 1..20, suffix = "ticks")
        private var overwriteTicks = 0

        private val mode by enumChoice("Mode", OnEdgeMode.STOP)
        private val sneak by intRange("Sneak", 0..0, 0..20, suffix = "ticks")
        private var sneakTicks = 0
        private val jump by boolean("Jump", false)

        /**
         * The input handler tracks the movement of the player and calculates the predicted future position.
         */
        @Suppress("unused")
        val inputHandler = handler<MovementInputEvent>(
            priority = EventPriorityConvention.OBJECTION_AGAINST_EVERYTHING
        ) { event ->
            val shouldBeActive = player.onGround() && !event.sneak && stops()
            if (shouldBeActive) {
                val isOnEdge = player.isCloseToEdge(
                    event.directionalInput,
                    min(player.horizontalSpeed, edgeDistance.current.toDouble())
                )
                if (isOnEdge) {
                    debugParameter("InputOnEdge") { event.directionalInput }

                    val center = center
                    if (center != null) {
                        val nextTick = PlayerSimulationCache.getSimulationForLocalPlayer().getSnapshotAt(1)
                        debugGeometry("Center") {
                            DebuggedPoint(center, Color4b.BLUE, 0.05)
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
                        edgeDistance.refresh()
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
            edgeDistance.refresh()
            super.disable()
        }

    }

}
