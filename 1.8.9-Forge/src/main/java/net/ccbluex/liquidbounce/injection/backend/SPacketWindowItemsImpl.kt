/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketWindowItems
import net.minecraft.network.play.server.S30PacketWindowItems

class SPacketWindowItemsImpl<T : S30PacketWindowItems>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketWindowItems {
    override val windowId: Int
        get() = wrapped.func_148911_c()
}

inline fun ISPacketWindowItems.unwrap(): S30PacketWindowItems = (this as SPacketWindowItemsImpl<*>).wrapped
inline fun S30PacketWindowItems.wrap(): ISPacketWindowItems = SPacketWindowItemsImpl(this)