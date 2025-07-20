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

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket

/**
 * Duplicate exempt grim
 * This is a technique that allows you to bypass the grim anti-cheat.
 *
 * It abuses the C06 duplicate exempt to bypass the velocity check.
 *
 * After sending a finish-mining digging packet that coincides with the player's
 * collision box and canceling the knockback packet sent by the server before the player's movement packet is sent,
 * grim seems to ignore the player's knockback
 *
 * https://github.com/GrimAnticheat/Grim/issues/1133
 */
internal object VelocityExemptGrim117 : VelocityMode("ExemptGrim117") {

    private var alternativeBypass by boolean("AlternativeBypass", true)

    private var canCancel = false

    override fun enable() {
        canCancel = false
    }

    @Suppress("unused")
    private val packetHandler = sequenceHandler<PacketEvent> { event ->
        val packet = event.packet

        // Check for damage to make sure it will only cancel
        // damage velocity (that all we need) and not affect other types of velocity
        if (packet is EntityDamageS2CPacket && packet.entityId == player.id) {
            canCancel = true
        }

        if ((packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id || packet is ExplosionS2CPacket)
            && canCancel) {
            event.cancelEvent()
            waitTicks(1)
            repeat(if (alternativeBypass) 4 else 1) {
                network.sendPacket(Full(player.x, player.y, player.z, player.yaw, player.pitch, player.isOnGround,
                    player.horizontalCollision))
            }
            network.sendPacket(
                PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, player.blockPos, player.horizontalFacing.opposite
                )
            )
            canCancel = false
        }
    }

}
