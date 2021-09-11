package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ICPacketAbilities
import net.minecraft.network.play.client.C13PacketPlayerAbilities

class CPacketAbilitiesImpl<out T : C13PacketPlayerAbilities>(wrapped: T) : PacketImpl<T>(wrapped), ICPacketAbilities
{
	override var flying: Boolean
		get() = wrapped.isFlying
		set(value)
		{
			wrapped.isFlying = value
		}

	override fun equals(other: Any?): Boolean = other is CPacketAbilitiesImpl<C13PacketPlayerAbilities> && run {
		val unwrappedOther = other.unwrap()
		wrapped.isInvulnerable == unwrappedOther.isInvulnerable && wrapped.isFlying == unwrappedOther.isFlying && wrapped.isAllowFlying == unwrappedOther.isAllowFlying && wrapped.isCreativeMode == unwrappedOther.isCreativeMode && wrapped.flySpeed == unwrappedOther.flySpeed && wrapped.walkSpeed == unwrappedOther.walkSpeed
	}

	override fun toString(): String = "CPacketAbilitiesImpl[invulnerable=${wrapped.isInvulnerable}, flying=${wrapped.isFlying}, allowFlying=${wrapped.isAllowFlying}, creativeMode=${wrapped.isCreativeMode}, flySpeed=${wrapped.flySpeed}, walkSpeed=${wrapped.walkSpeed}]"
}

fun ICPacketAbilities.unwrap(): C13PacketPlayerAbilities = (this as CPacketAbilitiesImpl<*>).wrapped
fun C13PacketPlayerAbilities.wrap(): ICPacketAbilities = CPacketAbilitiesImpl(this)
