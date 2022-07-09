/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.network.play.client

import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketEntityAction
import net.ccbluex.liquidbounce.injection.backend.minecraft.network.PacketImpl
import net.ccbluex.liquidbounce.injection.backend.utils.wrap
import net.minecraft.network.play.client.C0BPacketEntityAction

class CPacketEntityActionImpl<out T : C0BPacketEntityAction>(wrapped: T) : PacketImpl<T>(wrapped), ICPacketEntityAction
{
	override val action: ICPacketEntityAction.WAction
		get() = wrapped.action.wrap()
}

fun ICPacketEntityAction.unwrap(): C0BPacketEntityAction = (this as CPacketEntityActionImpl<*>).wrapped
fun C0BPacketEntityAction.wrap(): ICPacketEntityAction = CPacketEntityActionImpl(this)
