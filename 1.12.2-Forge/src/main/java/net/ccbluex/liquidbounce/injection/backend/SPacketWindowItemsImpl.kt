/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketWindowItems
import net.minecraft.network.play.server.SPacketWindowItems

class SPacketWindowItemsImpl<T : SPacketWindowItems>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketWindowItems {
    override val windowId: Int
        get() = wrapped.windowId
}

inline fun ISPacketWindowItems.unwrap(): SPacketWindowItems = (this as SPacketWindowItemsImpl<*>).wrapped
inline fun SPacketWindowItems.wrap(): ISPacketWindowItems = SPacketWindowItemsImpl(this)