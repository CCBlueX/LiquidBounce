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
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.drawLineStrip
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withColor
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager.Action
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayerCache
import net.ccbluex.liquidbounce.utils.input.InputTracker.isPressedOnAny
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.math.toVec3
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

/**
 * Freeze module
 *
 * Allows you to freeze yourself without the server knowing.
 */
object ModuleFreeze : ClientModule("Freeze", Category.MOVEMENT) {

    private val modes = choices("Mode", Queue, arrayOf(Queue, Cancel, Stationary))
        .apply { tagBy(this) }

    private val disableOnFlag by boolean("DisableOnFlag", true)
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
            withColor(Color4b(0x00, 0x80, 0xFF, 0xFF)) {
                drawLineStrip(positions = cachedPositions.map { relativeToCamera(it.pos).toVec3() })
            }
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        if (event.packet is PlayerPositionLookS2CPacket && disableOnFlag) {
            enabled = false
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

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        @Suppress("unused")
        private val packetHandler = handler<PacketEvent> { event ->
            // This might actually be useless since we cancel [PlayerTickEvent] which is responsible for movement
            // as well, so this is just a double check
            when (event.packet) {
                is PlayerMoveC2SPacket -> event.cancelEvent()
            }
        }

    }

}

