/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayerPosLook
import net.minecraft.network.play.client.C03PacketPlayer

class CPacketPlayerPosLookImpl<T : C03PacketPlayer.C06PacketPlayerPosLook>(wrapped: T) : PacketImpl<T>(wrapped), ICPacketPlayerPosLook

inline fun ICPacketPlayerPosLook.unwrap(): C03PacketPlayer.C06PacketPlayerPosLook = (this as CPacketPlayerPosLookImpl<*>).wrapped
inline fun C03PacketPlayer.C06PacketPlayerPosLook.wrap(): ICPacketPlayerPosLook = CPacketPlayerPosLookImpl(this)