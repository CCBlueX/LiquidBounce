/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketEntityAction
import net.minecraft.network.play.client.CPacketEntityAction

class CPacketEntityActionImpl<out T : CPacketEntityAction>(wrapped: T) : PacketImpl<T>(wrapped), ICPacketEntityAction
{
	override val action: ICPacketEntityAction.WAction
		get() = wrapped.action.wrap()
}

fun ICPacketEntityAction.unwrap(): CPacketEntityAction = (this as CPacketEntityActionImpl<*>).wrapped
fun CPacketEntityAction.wrap(): ICPacketEntityAction = CPacketEntityActionImpl(this)
