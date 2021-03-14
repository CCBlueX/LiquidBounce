/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketKeepAlive
import net.minecraft.network.play.client.CPacketKeepAlive

class CPacketKeepAliveImpl<out T : CPacketKeepAlive>(wrapped: T) : PacketImpl<T>(wrapped), ICPacketKeepAlive {

}

 fun ICPacketKeepAlive.unwrap(): CPacketKeepAlive = (this as CPacketKeepAliveImpl<*>).wrapped
 fun CPacketKeepAlive.wrap(): ICPacketKeepAlive = CPacketKeepAliveImpl(this)
