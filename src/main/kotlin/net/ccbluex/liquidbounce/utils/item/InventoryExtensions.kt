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

package net.ccbluex.liquidbounce.utils.item

import com.viaversion.viaversion.api.connection.UserConnection
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper
import com.viaversion.viaversion.api.type.Type
import com.viaversion.viaversion.protocols.protocol1_12to1_11_1.Protocol1_12To1_11_1
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.ServerboundPackets1_9_3
import io.netty.util.AttributeKey
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.block.Blocks

fun convertClientSlotToServerSlot(slot: Int): Int {
    return when (slot) {
        in 0..8 -> 36 + slot
        in 9..35 -> slot
        in 36..39 -> 39 - slot + 5
        40 -> 45
        else -> throw IllegalArgumentException()
    }
}

/**
 * Sends an open inventory packet using ViaFabricPlus code. This is only for older versions.
 */

// https://github.com/FlorianMichael/ViaFabricPlus/blob/602d723945d011d7cd9ca6f4ed7312d85f9bdf36/src/main/java/de/florianmichael/viafabricplus/injection/mixin/fixes/minecraft/MixinMinecraftClient.java#L118-L130
fun openInventorySilently() {
    runCatching {
        val isViaFabricPlusLoaded = AttributeKey.exists("viafabricplus-via-connection")

        if (!isViaFabricPlusLoaded) {
            return
        }

        val localViaConnection = AttributeKey.valueOf<UserConnection>("viafabricplus-via-connection")

        val viaConnection = mc.networkHandler?.connection?.channel?.attr(localViaConnection)?.get() ?: return

        if (viaConnection.protocolInfo.pipeline.contains(Protocol1_12To1_11_1::class.java)) {
            viaConnection.channel?.eventLoop()?.submit {
                val clientStatus = PacketWrapper.create(ServerboundPackets1_9_3.CLIENT_STATUS, viaConnection)
                clientStatus.write(Type.VAR_INT, 2) // Open Inventory Achievement

                runCatching {
                    clientStatus.sendToServer(Protocol1_12To1_11_1::class.java)
                }
            }
        }
    }
}

/**
 * A list of blocks, which are useless, so inv cleaner and scaffold won't count them as blocks
 */
var notABlock = hashSetOf(Blocks.CAKE, Blocks.TNT, Blocks.SAND, Blocks.CACTUS, Blocks.ANVIL)

/**
 * Configurable to configure the dynamic rotation engine
 */
class InventoryConstraintsConfigurable : Configurable("InventoryConstraints") {
    internal var delay by intRange("Delay", 2..4, 0..20)
    internal val invOpen by boolean("InvOpen", false)
    internal val noMove by boolean("NoMove", false)
}
