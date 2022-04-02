/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketAnimation
import net.minecraft.network.play.server.S0BPacketAnimation

class SPacketAnimationImpl<T : S0BPacketAnimation>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketAnimation {
    override val animationType: Int
        get() = wrapped.animationType
    override val entityID: Int
        get() = wrapped.entityID
}

inline fun ISPacketAnimation.unwrap(): S0BPacketAnimation = (this as SPacketAnimationImpl<*>).wrapped
inline fun S0BPacketAnimation.wrap(): ISPacketAnimation = SPacketAnimationImpl(this)