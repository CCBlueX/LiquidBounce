/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketChangeGameState
import net.minecraft.network.play.server.SPacketChangeGameState

class SPacketChangeGameStateImpl<out T : SPacketChangeGameState>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketChangeGameState
{
	override val gameState: Int
		get() = wrapped.gameState
}

fun ISPacketChangeGameState.unwrap(): SPacketChangeGameState = (this as SPacketChangeGameStateImpl<*>).wrapped
fun SPacketChangeGameState.wrap(): ISPacketChangeGameState = SPacketChangeGameStateImpl(this)
