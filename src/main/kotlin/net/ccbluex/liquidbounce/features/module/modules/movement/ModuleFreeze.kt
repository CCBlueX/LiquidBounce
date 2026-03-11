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

import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.BlinkPacketEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.blink.BlinkManager.Action
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleEasyPearl
import net.ccbluex.liquidbounce.render.drawLineStrip
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.sendPacketSilently
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayerCache
import net.ccbluex.liquidbounce.utils.input.InputTracker.isPressedOnAny
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.math.toVec3f
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.network.protocol.common.ServerboundPongPacket
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import kotlin.math.abs
import kotlin.random.Random

/**
 * Freeze module
 *
 * Allows you to freeze yourself without the server knowing.
 */
object ModuleFreeze : ClientModule("Freeze", ModuleCategories.MOVEMENT, disableOnQuit = true) {

    private val modes = choices("Mode", Stationary, arrayOf(Queue, Cancel, Stationary))
        .apply { tagBy(this) }
    private val disableOnFlag by boolean("DisableOnFlag", true)
    private val notification by boolean("Notification", false)
    private val balance by boolean("BalanceWarp", false)

    // todo: use global balance system
    private var missedOutTick = 0
    private var warpInProgress = false

    override fun onEnabled() {
        missedOutTick = 0
        super.onEnabled()
    }

    override fun onDisabled() {
        if (balance) {
            warpInProgress = true
            while (missedOutTick > 0) {
                // todo: does not run module tick if running at game tick layer
                player.tick()
                missedOutTick--
            }
            warpInProgress = false
        }

        missedOutTick = 0
        super.onDisabled()
    }

    /**
     * Acts as timer = 0 replacement
     */
    @Suppress("unused")
    private val moveHandler = handler<PlayerTickEvent> { event ->
        if (warpInProgress) return@handler

        event.cancelEvent()
        missedOutTick++
    }

    @Suppress("unused", "MagicNumber")
    val renderHandler = handler<WorldRenderEvent> { event ->
        if (!balance || missedOutTick < 0 || warpInProgress) {
            return@handler
        }

        // Create a simulated player from the client player, as we cannot use the player simulation cache
        // since we are going to modify the player's yaw and pitch
        val directionalInput = DirectionalInput(
            mc.options.keyUp.isPressedOnAny,
            mc.options.keyDown.isPressedOnAny,
            mc.options.keyLeft.isPressedOnAny,
            mc.options.keyRight.isPressedOnAny
        )

        val simulatedPlayer = SimulatedPlayer.fromClientPlayer(
            SimulatedPlayer.SimulatedPlayerInput.fromClientPlayer(
                directionalInput,
                mc.options.keyJump.isPressedOnAny,
                mc.options.keySprint.isPressedOnAny || player.isSprinting,
                mc.options.keyShift.isPressedOnAny
            )
        )

        // Alter the simulated player's yaw and pitch to match the camera
        simulatedPlayer.yRot = event.camera.yRot()
        simulatedPlayer.xRot = event.camera.xRot()

        // Create a cache for the simulated player
        val simulatedPlayerCache = SimulatedPlayerCache(simulatedPlayer)
        val cachedPositions = simulatedPlayerCache
            .getSnapshotsBetween(0 until this.missedOutTick)

        renderEnvironmentForWorld(event.matrixStack) {
            drawLineStrip(
                argb = Color4b(0x00, 0x80, 0xFF, 0xFF).argb,
                positions = cachedPositions.mapToArray { relativeToCamera(it.pos).toVec3f() },
            )
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        if (event.packet is ClientboundPlayerPositionPacket) {
            missedOutTick = 0
            if (disableOnFlag) {
                if (notification) {
                    notification(
                        this.name,
                        message("disabledOnFlag"),
                        NotificationEvent.Severity.INFO
                    )
                }
                enabled = false
            }
        }
    }

    /**
     * Queue network communication - acts as network lag
     */
    object Queue : Mode("Queue") {

        override val parent: ModeValueGroup<Mode>
            get() = modes

        private val origin by multiEnumChoice("Origin", TransferOrigin.OUTGOING)

        @Suppress("unused")
        private val fakeLagHandler = handler<BlinkPacketEvent>(
            priority = EventPriorityConvention.SAFETY_FEATURE
        ) { event ->
            if (origin.any { origin -> origin == event.origin }) {
                event.action = Action.QUEUE
            }
        }

    }

    /**
     * Cancel network communication
     */
    object Cancel : Mode("Cancel") {

        private val origin by multiEnumChoice("Origin", TransferOrigin.OUTGOING)

        override val parent: ModeValueGroup<Mode>
            get() = modes

        @Suppress("unused")
        private val packetHandler = handler<PacketEvent> { event ->
            if (origin.any { origin -> origin == event.origin }) {
                event.cancelEvent()
            }
        }

    }

    /**
     * Stationary freeze - only cancel movement but keeps network communication intact
     */
    object Stationary : Mode("Stationary") {
        /**
         * Bypasses Grim's BadPacketsR and Matrix7 Timer Check
         */
        private val cancelC0B by boolean("CancelC0B",true)
        private val yawOffset = FloatOffsetGenerator()
        private val pitchOffset = FloatOffsetGenerator()

        override val parent: ModeValueGroup<Mode>
            get() = modes

        /**
         * Bypasses Grim's duplicate rotation check
         */
        private class FloatOffsetGenerator : FloatIterator() {
            private var prev = 0f
            override fun hasNext() = true
            override fun nextFloat(): Float {
                var offset: Float
                do {
                    offset = Random.nextDouble(0.002, 0.01).toFloat()
                } while (abs(offset - prev) < 1.0E-6F)
                return offset.also { prev = it }
            }
        }

        @Suppress("unused")
        private val packetEventHandler = handler<PacketEvent> { event ->
            val yaw = RotationManager.currentRotation?.yaw ?: player.yRot
            val pitch = RotationManager.currentRotation?.pitch ?: player.xRot
            val yawOffset = yawOffset.nextFloat()
            val pitchOffset = pitchOffset.nextFloat()

            when (val packet = event.packet) {

                is ServerboundPongPacket -> {
                    if (cancelC0B) {
                        event.cancelEvent()
                    }
                }

                is ServerboundUseItemPacket -> {
                    event.cancelEvent()
                    sendPacketSilently(
                        ServerboundMovePlayerPacket.Rot(
                            ModuleEasyPearl.currentTargetRotation?.yaw ?: (player.yRot + yawOffset),
                            ModuleEasyPearl.currentTargetRotation?.pitch ?: (player.xRot + pitchOffset),
                            player.onGround(),
                            player.horizontalCollision
                        )
                    )
                    sendPacketSilently(
                        ServerboundUseItemPacket(
                            packet.hand,
                            packet.sequence,
                            yaw + yawOffset,
                            pitch + pitchOffset,
                        )
                    )
                }

                is ServerboundInteractPacket -> {
                    event.cancelEvent()
                    sendPacketSilently(
                        ServerboundMovePlayerPacket.Rot(
                            yaw + yawOffset,
                            pitch + pitchOffset,
                            player.onGround(),
                            player.horizontalCollision
                        )
                    )
                    sendPacketSilently(packet)
                }

                is ServerboundUseItemOnPacket -> {
                    event.cancelEvent()
                    sendPacketSilently(
                        ServerboundMovePlayerPacket.Rot(
                            yaw + yawOffset,
                            pitch + pitchOffset,
                            player.onGround(),
                            player.horizontalCollision
                        )
                    )
                    sendPacketSilently(packet)
                }
            }
        }

    }

}
