/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

/**
 * Freeze module
 *
 * Allows you to freeze yourself without the server knowing.
 */
object ModuleFreeze : Module("Freeze", Category.MOVEMENT) {

    private val disableOnFlag by boolean("DisableOnFlag", true)

    val moveHandler = handler<PlayerMoveEvent> { event ->
        // Set motion to zero
        event.movement.x = 0.0
        event.movement.y = 0.0
        event.movement.z = 0.0
    }

    val packetHandler = handler<PacketEvent> { event ->
        if (mc.world != null && event.origin == TransferOrigin.RECEIVE) {
            if (event.packet is PlayerPositionLookS2CPacket && disableOnFlag) {
                enabled = false

                return@handler
            }

            event.cancelEvent()
        }
    }

}
