/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.entity.player

import net.ccbluex.liquidbounce.api.minecraft.entity.player.IPlayerCapabilities
import net.minecraft.entity.player.PlayerCapabilities

class PlayerCapabilitiesImpl(val wrapped: PlayerCapabilities) : IPlayerCapabilities
{
	override val allowFlying: Boolean
		get() = wrapped.allowFlying
	override var isFlying: Boolean
		get() = wrapped.isFlying
		set(value)
		{
			wrapped.isFlying = value
		}
	override val isCreativeMode: Boolean
		get() = wrapped.isCreativeMode

	override val disableDamage: Boolean
		get() = wrapped.disableDamage

	override var flySpeed: Float
		get() = wrapped.flySpeed
		set(value)
		{
			wrapped.flySpeed = value
		}

	override fun equals(other: Any?): Boolean = other is PlayerCapabilitiesImpl && other.wrapped == wrapped
}

fun IPlayerCapabilities.unwrap(): PlayerCapabilities = (this as PlayerCapabilitiesImpl).wrapped
fun PlayerCapabilities.wrap(): IPlayerCapabilities = PlayerCapabilitiesImpl(this)
