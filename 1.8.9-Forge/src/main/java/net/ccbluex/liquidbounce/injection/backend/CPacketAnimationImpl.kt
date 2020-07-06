/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketAnimation
import net.minecraft.network.play.client.C0APacketAnimation

class CPacketAnimationImpl<T : C0APacketAnimation>(wrapped: T) : PacketImpl<T>(wrapped), ICPacketAnimation {

}

inline fun ICPacketAnimation.unwrap(): C0APacketAnimation = (this as CPacketAnimationImpl<*>).wrapped
inline fun C0APacketAnimation.wrap(): ICPacketAnimation = CPacketAnimationImpl(this)