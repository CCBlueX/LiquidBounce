/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketEntityEffect
import net.minecraft.network.play.server.SPacketEntityEffect

class SPacketEntityEffectImpl<out T : SPacketEntityEffect>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketEntityEffect
{
	override val entityId: Int
		get() = wrapped.entityId
}

fun ISPacketEntityEffect.unwrap(): SPacketEntityEffect = (this as SPacketEntityEffectImpl<*>).wrapped
fun SPacketEntityEffect.wrap(): ISPacketEntityEffect = SPacketEntityEffectImpl(this)
