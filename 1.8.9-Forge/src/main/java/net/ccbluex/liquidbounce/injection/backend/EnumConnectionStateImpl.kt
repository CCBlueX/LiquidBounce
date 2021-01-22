/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.IEnumConnectionState
import net.minecraft.network.EnumConnectionState

class EnumConnectionStateImpl(val wrapped: EnumConnectionState) : IEnumConnectionState
{
	override val isHandshake: Boolean
		get() = wrapped == EnumConnectionState.HANDSHAKING

	override fun equals(other: Any?): Boolean = other is EnumConnectionStateImpl && other.wrapped == wrapped
}

 fun IEnumConnectionState.unwrap(): EnumConnectionState = (this as EnumConnectionStateImpl).wrapped
 fun EnumConnectionState.wrap(): IEnumConnectionState = EnumConnectionStateImpl(this)
