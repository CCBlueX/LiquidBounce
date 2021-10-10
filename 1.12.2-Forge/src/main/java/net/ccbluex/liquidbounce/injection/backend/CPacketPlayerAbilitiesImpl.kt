package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayerAbilities
import net.minecraft.network.play.client.CPacketPlayerAbilities

class CPacketPlayerAbilitiesImpl<out T : CPacketPlayerAbilities>(wrapped: T) : PacketImpl<T>(wrapped), ICPacketPlayerAbilities
{
	override var flying: Boolean
		get() = wrapped.isFlying
		set(value)
		{
			wrapped.isFlying = value
		}

	override fun equals(other: Any?): Boolean = other is CPacketPlayerAbilitiesImpl<CPacketPlayerAbilities> && run {
		val unwrappedOther = other.unwrap()
		wrapped.isInvulnerable == unwrappedOther.isInvulnerable && wrapped.isFlying == unwrappedOther.isFlying && wrapped.isAllowFlying == unwrappedOther.isAllowFlying && wrapped.isCreativeMode == unwrappedOther.isCreativeMode && wrapped.flySpeed == unwrappedOther.flySpeed && wrapped.walkSpeed == unwrappedOther.walkSpeed
	}

	override fun toString(): String = "CPacketAbilitiesImpl[invulnerable=${wrapped.isInvulnerable}, flying=${wrapped.isFlying}, allowFlying=${wrapped.isAllowFlying}, creativeMode=${wrapped.isCreativeMode}, flySpeed=${wrapped.flySpeed}, walkSpeed=${wrapped.walkSpeed}]"
}

fun ICPacketPlayerAbilities.unwrap(): CPacketPlayerAbilities = (this as CPacketPlayerAbilitiesImpl<*>).wrapped
fun CPacketPlayerAbilities.wrap(): ICPacketPlayerAbilities = CPacketPlayerAbilitiesImpl(this)
