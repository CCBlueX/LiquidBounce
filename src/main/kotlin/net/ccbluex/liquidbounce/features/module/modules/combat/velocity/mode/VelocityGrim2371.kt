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

package net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode

import net.ccbluex.liquidbounce.event.events.BlinkPacketEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.blink.BlinkManager
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.network.isLocalPlayerDamage
import net.ccbluex.liquidbounce.utils.network.isLocalPlayerVelocity
import net.ccbluex.liquidbounce.utils.raytracing.traceFromPlayer
import net.minecraft.network.protocol.common.ServerboundPongPacket
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult

internal object VelocityGrim2371 : VelocityMode("Grim2371") {

    private var cancelNextVelocity = false
    private var delay = false
    private var needClick = false

    private var waitForPing = false
    private var waitForUpdate = false

    private var hitResult: BlockHitResult? = null
    private var shouldSkip = false

    private var freezeTicks = 0
    private const val MAX_FREEZE_TICKS = 20 // To prevent freezing

    override fun enable() {
        cancelNextVelocity = false
        delay = false
        needClick = false
        waitForUpdate = false
        hitResult = null
        shouldSkip = false
        freezeTicks = 0
    }

    override fun disable() {
        BlinkManager.flush(TransferOrigin.INCOMING)
    }

    @Suppress("unused")
    private val packetHandler = sequenceHandler<PacketEvent> { event ->
        val packet = event.packet

        when (packet) {
            is ServerboundInteractPacket, is ServerboundUseItemOnPacket ->
                shouldSkip = true

            is ServerboundMovePlayerPacket if packet.hasPosition() && waitForUpdate ->
                event.cancelEvent()

            is ServerboundPongPacket if waitForPing -> {
                waitTicks(1)
                waitForUpdate = false
                waitForPing = false
            }
        }

        if (event.isCancelled) {
            return@sequenceHandler
        }

        if (packet is ClientboundBlockUpdatePacket && waitForUpdate && packet.pos == player.blockPosition()) {
            waitTicks(1)
            waitForPing = true
            needClick = false
            return@sequenceHandler
        }

        if (waitForUpdate || delay) {
            return@sequenceHandler
        }

        // Check for damage to make sure it will only cancel damage velocity (that all we need),
        // and not affect other types of velocity
        if (packet.isLocalPlayerDamage()) {
            cancelNextVelocity = true
        } else if (cancelNextVelocity && event.packet.isLocalPlayerVelocity()) {
            event.cancelEvent()
            delay = true
            cancelNextVelocity = false
            needClick = true
        }
    }

    @Suppress("unused")
    private val queuePacketHandler = handler<BlinkPacketEvent> { event ->
        if (waitForUpdate || !delay || event.origin != TransferOrigin.INCOMING) {
            return@handler
        }

        event.action = BlinkManager.Action.QUEUE
    }

    @Suppress("unused")
    private val playerTickHandler = handler<PlayerTickEvent> { event ->
        if (needClick && !shouldSkip && !player.isUsingItem) {
            hitResult = traceFromPlayer(
                rotation = RotationManager.serverRotation.copy(pitch = 90F)
            ).takeIf {
                it.blockPos.relative(it.direction) == player.blockPosition()
            }
        }

        hitResult?.let { hitResult ->
            delay = false

            BlinkManager.flush(TransferOrigin.INCOMING)

            if (interaction.useItemOn(player, InteractionHand.MAIN_HAND, hitResult).consumesAction()) {
                player.swing(InteractionHand.MAIN_HAND)
            }

            if (RotationManager.serverRotation.pitch != 90f) {
                network.send(
                    ServerboundMovePlayerPacket.Rot(
                        player.yRot,
                        90f,
                        player.onGround(),
                        player.horizontalCollision
                    )
                )
            } else {
                network.send(
                    ServerboundMovePlayerPacket.StatusOnly(
                        player.onGround(),
                        player.horizontalCollision
                    )
                )
            }

            freezeTicks = 0
            waitForUpdate = true
            this.hitResult = null
            needClick = false
        }

        if (waitForUpdate) {
            event.cancelEvent()
            freezeTicks++
            if (freezeTicks > MAX_FREEZE_TICKS) {
                waitForUpdate = false
                waitForPing = false
                needClick = false
            }
        }

        shouldSkip = false
    }
}
