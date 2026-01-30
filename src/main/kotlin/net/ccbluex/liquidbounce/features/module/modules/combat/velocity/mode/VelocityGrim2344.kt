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

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.utils.network.isLocalPlayerVelocity
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket

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
internal object VelocityGrim2344 : VelocityMode("Grim2344-117") {

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
        if (packet is ClientboundDamageEventPacket && packet.entityId == player.id) {
            canCancel = true
        }

        if (packet.isLocalPlayerVelocity() && canCancel) {
            event.cancelEvent()
            waitTicks(1)
            repeat(if (alternativeBypass) 4 else 1) {
                network.send(
                    PosRot(
                        player.x, player.y, player.z, player.yRot, player.xRot, player.onGround(),
                        player.horizontalCollision
                    )
                )
            }
            network.send(
                ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                    player.blockPosition(),
                    player.direction.opposite
                )
            )
            canCancel = false
        }
    }

}
