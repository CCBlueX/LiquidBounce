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
package net.ccbluex.liquidbounce.utils.network

import com.viaversion.viabackwards.protocol.v1_12to1_11_1.Protocol1_12To1_11_1
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper
import com.viaversion.viaversion.api.type.Types
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ServerboundPackets1_9_3

// https://github.com/ViaVersion/ViaFabricPlus/blob/ecd5d188187f2ebaaad8ded0ffe53538911f7898/src/main/java/de/florianmichael/viafabricplus/injection/mixin/fixes/minecraft/MixinMinecraftClient.java#L124-L130
class OpenInventorySilentlyPacket : LegacyPacket {

    override val protocol = Protocol1_12To1_11_1::class.java

    override val packetType = ServerboundPackets1_9_3.CLIENT_COMMAND

    override fun write(packetWrapper: PacketWrapper) {
        packetWrapper.write(Types.VAR_INT, 2) // Open Inventory Achievement
    }

}
