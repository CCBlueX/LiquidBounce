/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.network.play.server

import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketEntityEffect
import net.ccbluex.liquidbounce.injection.backend.minecraft.network.PacketImpl
import net.minecraft.network.play.server.S1DPacketEntityEffect

class SPacketEntityEffectImpl<out T : S1DPacketEntityEffect>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketEntityEffect
{
	override val entityId: Int
		get() = wrapped.entityId
}

fun ISPacketEntityEffect.unwrap(): S1DPacketEntityEffect = (this as SPacketEntityEffectImpl<*>).wrapped
fun S1DPacketEntityEffect.wrap(): ISPacketEntityEffect = SPacketEntityEffectImpl(this)
