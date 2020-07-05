/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketHeldItemChange
import net.minecraft.network.play.client.C09PacketHeldItemChange

class CPacketHeldItemChangeImpl<T : C09PacketHeldItemChange>(wrapped: T) : PacketImpl<T>(wrapped), ICPacketHeldItemChange {
    override val slotId: Int
        get() = wrapped.slotId
}

inline fun ICPacketHeldItemChange.unwrap(): C09PacketHeldItemChange = (this as CPacketHeldItemChangeImpl<*>).wrapped
inline fun C09PacketHeldItemChange.wrap(): ICPacketHeldItemChange = CPacketHeldItemChangeImpl(this)