/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

/**
 * Freeze module
 *
 * Allows you to freeze yourself without the server knowing.
 */
object ModuleFreeze : Module("Freeze", Category.MOVEMENT) {
    private val modes = choices("Mode", Legit, arrayOf(Legit, Semi)).apply { tagBy(this) }

    abstract class FreezeMode(name: String) : Choice(name) {

        override val parent: ChoiceConfigurable<FreezeMode>
            get() = modes
    }

    private val disableOnFlag by boolean("DisableOnFlag", true)

    private var velocityX = 0.0
    private var velocityY = 0.0
    private var velocityZ = 0.0
    private var x = 0.0
    private var y = 0.0
    private var z = 0.0

    override fun enable() {
        velocityX = player.velocity.x
        velocityY = player.velocity.y
        velocityZ = player.velocity.z
        x = player.x
        y = player.y
        z = player.z
    }

    override fun disable() {
        player.velocity.x = velocityX
        player.velocity.y = velocityY
        player.velocity.z = velocityZ
    }

    // Cancels all packets
    object Legit : FreezeMode("Legit") {
        val moveHandler = handler<PlayerMoveEvent> { event ->
            event.movement.x = 0.0
            event.movement.y = 0.0
            event.movement.z = 0.0
            player.pos.x = x
            player.pos.y = y
            player.pos.z = z
        }

        val packetHandler = handler<PacketEvent> { event ->
            if (event.origin == TransferOrigin.RECEIVE) {
                if (event.packet is PlayerPositionLookS2CPacket && disableOnFlag) {
                    enabled = false

                    return@handler
                }

                event.cancelEvent()
            }
        }
    }

    // Only cancels PlayerMove packets sent by the client
    object Semi : FreezeMode("Semi") {
        val moveHandler = handler<PlayerMoveEvent> { event ->
            event.movement.x = 0.0
            event.movement.y = 0.0
            event.movement.z = 0.0
            player.pos.x = x
            player.pos.y = y
            player.pos.z = z
        }

        val packetHandler = handler<PacketEvent> { event ->
            when (event.packet) {
                is PlayerPositionLookS2CPacket -> {
                    if (disableOnFlag) {
                        enabled = false
                        return@handler
                    }
                }

                is PlayerMoveC2SPacket -> event.cancelEvent()
            }
        }
    }
}
