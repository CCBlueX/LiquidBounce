/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.network.play.server

import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketChangeGameState
import net.ccbluex.liquidbounce.injection.backend.minecraft.network.PacketImpl
import net.minecraft.network.play.server.S2BPacketChangeGameState

class SPacketChangeGameStateImpl<out T : S2BPacketChangeGameState>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketChangeGameState
{
	override val gameState: Int
		get() = wrapped.gameState
}

fun ISPacketChangeGameState.unwrap(): S2BPacketChangeGameState = (this as SPacketChangeGameStateImpl<*>).wrapped
fun S2BPacketChangeGameState.wrap(): ISPacketChangeGameState = SPacketChangeGameStateImpl(this)
