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

import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleEasyPearl
import net.ccbluex.liquidbounce.render.drawLineStrip
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager.Action
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.sendPacketSilently
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayerCache
import net.ccbluex.liquidbounce.utils.input.InputTracker.isPressedOnAny
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.math.toVec3
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.network.packet.c2s.play.*
import kotlin.math.abs
import kotlin.random.Random

/**
 * Freeze module
 *
 * Allows you to freeze yourself without the server knowing.
 */
object ModuleFreeze : ClientModule("Freeze", Category.MOVEMENT, disableOnQuit = true) {

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
            mc.options.forwardKey.isPressedOnAny,
            mc.options.backKey.isPressedOnAny,
            mc.options.leftKey.isPressedOnAny,
            mc.options.rightKey.isPressedOnAny
        )

        val simulatedPlayer = SimulatedPlayer.fromClientPlayer(
            SimulatedPlayer.SimulatedPlayerInput.fromClientPlayer(
                directionalInput,
                mc.options.jumpKey.isPressedOnAny,
                mc.options.sprintKey.isPressedOnAny || player.isSprinting,
                mc.options.sneakKey.isPressedOnAny
            )
        )

        // Alter the simulated player's yaw and pitch to match the camera
        simulatedPlayer.yaw = event.camera.yaw
        simulatedPlayer.pitch = event.camera.pitch

        // Create a cache for the simulated player
        val simulatedPlayerCache = SimulatedPlayerCache(simulatedPlayer)
        val cachedPositions = simulatedPlayerCache
            .getSnapshotsBetween(0 until this.missedOutTick)

        renderEnvironmentForWorld(event.matrixStack) {
            drawLineStrip(
                argb = Color4b(0x00, 0x80, 0xFF, 0xFF).toARGB(),
                positions = cachedPositions.mapToArray { relativeToCamera(it.pos).toVec3() },
            )
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        if (event.packet is PlayerPositionLookS2CPacket) {
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
    object Queue : Choice("Queue") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        private val origin by multiEnumChoice("Origin", TransferOrigin.OUTGOING)

        @Suppress("unused")
        private val fakeLagHandler = handler<QueuePacketEvent>(
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
    object Cancel : Choice("Cancel") {

        private val origin by multiEnumChoice("Origin", TransferOrigin.OUTGOING)

        override val parent: ChoiceConfigurable<Choice>
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
    object Stationary : Choice("Stationary") {
        /**
         * Bypasses Grim's BadPacketsR and Matrix7 Timer Check
         */
        private val cancelC0B by boolean("CancelC0B",true)
        private val yawOffset = FloatOffsetGenerator()
        private val pitchOffset = FloatOffsetGenerator()

        override val parent: ChoiceConfigurable<Choice>
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
            val yaw = RotationManager.currentRotation?.yaw ?: player.yaw
            val pitch = RotationManager.currentRotation?.pitch ?: player.pitch
            val yawOffset = yawOffset.nextFloat()
            val pitchOffset = pitchOffset.nextFloat()

            when (val packet = event.packet) {

                is CommonPongC2SPacket -> {
                    if (cancelC0B) {
                        event.cancelEvent()
                    }
                }

                is PlayerInteractItemC2SPacket -> {
                    event.cancelEvent()
                    sendPacketSilently(
                        PlayerMoveC2SPacket.LookAndOnGround(
                            ModuleEasyPearl.currentTargetRotation?.yaw ?: (player.yaw + yawOffset),
                            ModuleEasyPearl.currentTargetRotation?.pitch ?: (player.pitch + pitchOffset),
                            player.isOnGround,
                            player.horizontalCollision
                        )
                    )
                    sendPacketSilently(
                        PlayerInteractItemC2SPacket(
                            packet.hand,
                            packet.sequence,
                            yaw + yawOffset,
                            pitch + pitchOffset,
                        )
                    )
                }

                is PlayerInteractEntityC2SPacket -> {
                    event.cancelEvent()
                    sendPacketSilently(
                        PlayerMoveC2SPacket.LookAndOnGround(
                            yaw + yawOffset,
                            pitch + pitchOffset,
                            player.isOnGround,
                            player.horizontalCollision
                        )
                    )
                    sendPacketSilently(packet)
                }

                is PlayerInteractBlockC2SPacket -> {
                    event.cancelEvent()
                    sendPacketSilently(
                        PlayerMoveC2SPacket.LookAndOnGround(
                            yaw + yawOffset,
                            pitch + pitchOffset,
                            player.isOnGround,
                            player.horizontalCollision
                        )
                    )
                    sendPacketSilently(packet)
                }
            }
        }

    }

}
