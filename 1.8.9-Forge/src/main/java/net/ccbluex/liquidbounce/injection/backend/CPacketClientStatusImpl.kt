/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketClientStatus
import net.minecraft.network.play.client.C16PacketClientStatus

class CPacketClientStatusImpl<T : C16PacketClientStatus>(wrapped: T) : PacketImpl<T>(wrapped), ICPacketClientStatus

inline fun ICPacketClientStatus.unwrap(): C16PacketClientStatus = (this as CPacketClientStatusImpl<*>).wrapped
inline fun C16PacketClientStatus.wrap(): ICPacketClientStatus = CPacketClientStatusImpl(this)