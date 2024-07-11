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
package net.ccbluex.liquidbounce.script.bindings.api

import net.ccbluex.liquidbounce.utils.client.network
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

object JsNetworkUtil {

    @JvmName("movePlayerGround")
    fun movePlayerGround(onGround: Boolean) = network.sendPacket(PlayerMoveC2SPacket.OnGroundOnly(onGround))

    @JvmName("movePlayerPosition")
    fun movePlayerPosition(x: Double, y: Double, z: Double, onGround: Boolean) =
        network.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround))

    @JvmName("movePlayerPositionAndLook")
    fun movePlayerPositionAndLook(x: Double, y: Double, z: Double, yaw: Float, pitch: Float, onGround: Boolean) =
        network.sendPacket(PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, onGround))

    @JvmName("movePlayerLook")
    fun movePlayerLook(yaw: Float, pitch: Float, onGround: Boolean) =
        network.sendPacket(PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, onGround))

    @JvmName("sendChatMessage")
    fun sendChatMessage(message: String) = network.sendChatMessage(message)

    @JvmName("sendCommand")
    fun sendCommand(command: String) = network.sendCommand(command)

}
