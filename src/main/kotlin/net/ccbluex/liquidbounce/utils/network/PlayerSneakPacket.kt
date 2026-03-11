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

package net.ccbluex.liquidbounce.utils.network

import com.viaversion.viaversion.api.protocol.packet.PacketWrapper
import com.viaversion.viaversion.api.type.Types
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.packet.ServerboundPackets1_21_5
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.Protocol1_21_5To1_21_6
import net.ccbluex.liquidbounce.utils.client.player

/**
 * https://github.com/ViaVersion/ViaFabricPlus/blob/56c4959000e68d77fd415b89af7a95478d825079/src/main/java/com/viaversion/viafabricplus/injection/mixin/features/movement/sprinting_and_sneaking/MixinClientPlayerEntity.java#L251-L264
 */
data class PlayerSneakPacket private constructor(val sneaking: Boolean) : LegacyPacket {

    override val protocol get() = Protocol1_21_5To1_21_6::class.java

    override val packetType get() = ServerboundPackets1_21_5.PLAYER_COMMAND

    override fun write(packetWrapper: PacketWrapper) {
        packetWrapper.write(Types.VAR_INT, player.id)
        packetWrapper.write(Types.VAR_INT, if (sneaking) 0 else 1)
        packetWrapper.write(Types.VAR_INT, 0) // No data
    }

    companion object {
        @JvmField
        val START = PlayerSneakPacket(true)
        @JvmField
        val STOP = PlayerSneakPacket(false)
    }

}
