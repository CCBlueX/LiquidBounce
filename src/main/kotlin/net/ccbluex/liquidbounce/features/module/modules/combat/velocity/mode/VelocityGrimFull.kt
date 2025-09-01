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

package net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode

import com.google.common.collect.Queues
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.raycast
import net.ccbluex.liquidbounce.utils.client.PacketSnapshot
import net.ccbluex.liquidbounce.utils.client.handlePacket
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult

internal object VelocityGrimFull : VelocityMode("GrimFull") {
    private var canCancel = false
    private var delay = false
    private var needClick = false

    private var waitForPing = false
    private var waitForUpdate = false

    private var hitResult: BlockHitResult? = null
    private var shouldSkip = false
    private val delayedPacketQueue = Queues.newConcurrentLinkedQueue<PacketSnapshot>()

    private var freezeTicks = 0
    private const val MAX_FREEZE_TICKS = 20 // To prevent freezing

    override fun enable() {
        canCancel = false
        delay = false
        needClick = false
        waitForUpdate = false
        hitResult = null
        shouldSkip = false
        delayedPacketQueue.clear()
    }

    override fun disable() {
        delayedPacketQueue.forEach { handlePacket(it.packet) }
        delayedPacketQueue.clear()
    }

    @Suppress("unused")
    private val packetHandler = sequenceHandler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is PlayerInteractEntityC2SPacket || packet is PlayerInteractBlockC2SPacket) {
            shouldSkip = true
        }

        if (packet is PlayerMoveC2SPacket && packet.changesPosition() && waitForUpdate) {
            event.cancelEvent()
        }

        if (packet is CommonPongC2SPacket && waitForPing) {
            waitTicks(1)
            waitForUpdate = false
            waitForPing = false
            return@sequenceHandler
        }

        if (event.isCancelled || event.origin == TransferOrigin.OUTGOING) {
            return@sequenceHandler
        }

        if (waitForUpdate && packet is BlockUpdateS2CPacket && packet.pos.equals(player.blockPos)) {
            waitTicks(1)
            waitForPing = true
            needClick = false
            return@sequenceHandler
        }

        if (waitForUpdate) {
            return@sequenceHandler
        }
        // Delay all the packets.
        if (delay) {
            delayedPacketQueue.add(PacketSnapshot(packet, event.origin, System.currentTimeMillis()))
            event.cancelEvent()
            return@sequenceHandler
        }

        // Check for damage to make sure it will only cancel
        // damage velocity (that all we need) and not affect other types of velocity
        if (packet is EntityDamageS2CPacket && packet.entityId == player.id) {
            canCancel = true
        }

        if ((packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id || packet is ExplosionS2CPacket)
            && canCancel
        ) {
            event.cancelEvent()
            delay = true
            canCancel = false
            needClick = true
        }
    }

    @Suppress("unused")
    private val playerTickHandle = handler<PlayerTickEvent> { event ->
        if (needClick) {
            hitResult = raycast(rotation = Rotation(player.yaw, 90f))
            val pos = hitResult!!.blockPos.offset(hitResult!!.side)
            if (!pos.equals(player.blockPos) || shouldSkip || player.isUsingItem) {
                hitResult = null
            }
        }

        if (hitResult != null) {
            delay = false

            delayedPacketQueue.forEach { handlePacket(it.packet) }
            delayedPacketQueue.clear()

            if (interaction.interactBlock(player, Hand.MAIN_HAND, hitResult) == ActionResult.SUCCESS) {
                player.swingHand(Hand.MAIN_HAND)
            }

            if (RotationManager.serverRotation.pitch != 90f) {
                network.sendPacket(
                    PlayerMoveC2SPacket.LookAndOnGround(
                        player.yaw,
                        90f,
                        player.isOnGround,
                        player.horizontalCollision
                    )
                )
            } else {
                network.sendPacket(
                    PlayerMoveC2SPacket.OnGroundOnly(
                        player.isOnGround,
                        player.horizontalCollision
                    )
                )
            }

            freezeTicks = 0
            waitForUpdate = true
            hitResult = null
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
