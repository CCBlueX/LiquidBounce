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

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleFreeze
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ScaffoldBlockItemSelection.isValidBlock
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.ccbluex.liquidbounce.utils.entity.isCloseToEdge
import net.ccbluex.liquidbounce.utils.entity.wouldFallIntoVoid
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.SAFETY_FEATURE
import net.ccbluex.liquidbounce.utils.kotlin.matchesAll
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.ccbluex.liquidbounce.utils.network.sendPacketSilently
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import java.util.function.Predicate

/**
 * An eagle module
 *
 * Legit trick to build faster.
 */
object ModuleEagle : ClientModule(
    "Eagle", ModuleCategories.PLAYER,
    aliases = listOf("FastBridge", "BridgeAssistant", "LegitScaffold")
) {

    private val edgeDistance by floatRange("EdgeDistance", 0.4f..0.6f, 0.01f..1.3f)
        .onChanged {
            currentEdgeDistance = it.random()
        }

    private var currentEdgeDistance: Float = edgeDistance.random()
    private var wasSneaking = false
    private var sneakCaptured = false

    internal val isClutching
        get() = Clutch.isRescuing

    private fun shouldActivateEagle(event: MovementInputEvent, conditionsMet: Boolean): Boolean {
        if (player.abilities.flying || !conditionsMet) {
            return false
        }

        return player.isCloseToEdge(event.directionalInput, currentEdgeDistance.toDouble())
    }

    private fun updateSneakCapture(originalSneak: Boolean, active: Boolean) {
        if (!Conditional.controlsSneak) {
            sneakCaptured = false
            return
        }

        when {
            !sneakCaptured && active && originalSneak -> sneakCaptured = true
            sneakCaptured && !originalSneak -> sneakCaptured = false
        }
    }

    private fun shouldOverrideSneak(conditionsMet: Boolean, active: Boolean): Boolean {
        return conditionsMet && Conditional.controlsSneak && (active || sneakCaptured)
    }

    private fun updateSneakState(isSneaking: Boolean) {
        if (isSneaking) {
            wasSneaking = true
            return
        }

        if (wasSneaking) {
            currentEdgeDistance = edgeDistance.random()
            wasSneaking = false
        }
    }

    private object Conditional : ToggleableValueGroup(this, "Conditional", true) {
        private val conditions by multiEnumChoice(
            "Conditions",
            Condition.ON_GROUND
        )

        val pitch by floatRange("Pitch", -90f..90f, -90f..90f)

        val controlsSneak
            get() = enabled && Condition.SNEAK in conditions

        fun shouldSneak(event: MovementInputEvent) =
            !enabled || player.xRot.coerceIn(-90f, 90f) in pitch && conditions.matchesAll(event)

        @Suppress("unused")
        private enum class Condition(override val tag: String) : Tagged, Predicate<MovementInputEvent> {
            LEFT("Left"),
            RIGHT("Right"),
            FORWARDS("Forwards"),
            BACKWARDS("Backwards"),
            HOLDING_BLOCKS("HoldingBlocks"),
            ON_GROUND("OnGround"),
            SNEAK("Sneak");

            override fun test(event: MovementInputEvent): Boolean = when (this) {
                LEFT -> event.directionalInput.left
                RIGHT -> event.directionalInput.right
                FORWARDS -> event.directionalInput.forwards
                BACKWARDS -> event.directionalInput.backwards
                HOLDING_BLOCKS -> isValidBlock(player.mainHandItem) || isValidBlock(player.offhandItem)
                ON_GROUND -> player.onGround()
                SNEAK -> event.sneak
            }
        }
    }

    private object Clutch : ToggleableValueGroup(this, "Clutch", false) {

        private val predictionTicks by int("PredictionTicks", 10, 1..40, "ticks")
        private val minFallDistance by float("MinFallDistance", 0.5f, 0.0f..5.0f)
        private val maxStuckTicks by int("MaxStuckTicks", 20, 1..100, "ticks")

        var isRescuing = false
            private set

        private var enabledScaffold = false
        private var rescueTicks = 0
        private var failedRescue = false

        private fun isFallPredictedIntoVoid(): Boolean {
            if (player.onGround() || player.abilities.flying || player.isSpectator || player.isDeadOrDying ||
                !player.wouldFallIntoVoid(player.position())
            ) {
                return false
            }

            val snapshots = PlayerSimulationCache.getSimulationForLocalPlayer()
                .getSnapshotsBetween(1..predictionTicks)

            return snapshots.any { snapshot ->
                snapshot.fallDistance >= minFallDistance && player.wouldFallIntoVoid(snapshot.pos)
            }
        }

        private fun startRescue() {
            if (ModuleScaffold.enabled || ModuleFreeze.enabled || ModuleScaffold.blockCount == 0) {
                return
            }

            isRescuing = true
            rescueTicks = 0
            enabledScaffold = true
            ModuleScaffold.enabled = true

            if (!ModuleScaffold.running) {
                stopRescue(failed = true)
            }
        }

        private fun stopRescue(failed: Boolean = false) {
            if (enabledScaffold && ModuleScaffold.enabled) {
                ModuleScaffold.enabled = false
            }

            enabledScaffold = false
            isRescuing = false
            rescueTicks = 0
            failedRescue = failed
        }

        override fun onDisabled() {
            stopRescue()
            failedRescue = false
        }

        @Suppress("unused")
        private val gameTickHandler = handler<GameTickEvent> {
            if (!isRescuing) {
                val fallPredictedIntoVoid = isFallPredictedIntoVoid()

                if (!fallPredictedIntoVoid) {
                    failedRescue = false
                } else if (!failedRescue) {
                    startRescue()
                }
                return@handler
            }

            rescueTicks++

            when {
                !ModuleScaffold.running -> stopRescue(failed = true)
                !player.wouldFallIntoVoid(player.position()) -> stopRescue()
                rescueTicks >= maxStuckTicks -> stopRescue(failed = true)
            }
        }

        @Suppress("unused")
        private val playerTickHandler = handler<PlayerTickEvent>(priority = SAFETY_FEATURE) { event ->
            if (isRescuing) {
                event.cancelEvent()
            }
        }

        @Suppress("unused")
        private val packetHandler = handler<PacketEvent>(priority = SAFETY_FEATURE) { event ->
            if (!isRescuing || event.origin != TransferOrigin.OUTGOING || event.isCancelled) {
                return@handler
            }

            when (val packet = event.packet) {
                is ServerboundMovePlayerPacket -> event.cancelEvent()
                is ServerboundUseItemOnPacket -> {
                    event.cancelEvent()

                    val rotation = RotationManager.currentRotation ?: player.rotation
                    sendPacketSilently(
                        ServerboundMovePlayerPacket.Rot(
                            rotation.yaw,
                            rotation.pitch,
                            player.onGround(),
                            player.horizontalCollision
                        )
                    )
                    sendPacketSilently(packet)
                }
            }
        }

        @Suppress("unused")
        private val worldChangeHandler = handler<WorldChangeEvent> {
            stopRescue()
            failedRescue = false
        }

    }

    init {
        tree(Conditional)
        tree(Clutch)
    }

    override fun onDisabled() {
        wasSneaking = false
        sneakCaptured = false
        super.onDisabled()
    }

    @Suppress("unused")
    private val handleMovementInput = handler<MovementInputEvent>(priority = SAFETY_FEATURE) { event ->
        debugParameter("EdgeDistance") { currentEdgeDistance }

        val originalSneak = mc.options.keyShift.isDown
        val conditionsMet = Conditional.shouldSneak(event)
        val isActive = shouldActivateEagle(event, conditionsMet)

        updateSneakCapture(originalSneak, isActive)

        val controlsSneak = shouldOverrideSneak(conditionsMet, isActive)

        event.sneak = if (controlsSneak) {
            isActive
        } else {
            originalSneak || isActive
        }

        updateSneakState(event.sneak)
    }

}
