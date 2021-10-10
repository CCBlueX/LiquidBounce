/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayerBlockPlacement
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.injection.backend.utils.wrap
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock

class CPacketPlayerBlockPlacementImpl<out T : CPacketPlayerTryUseItemOnBlock>(wrapped: T) : PacketImpl<T>(wrapped), ICPacketPlayerBlockPlacement
{
	override val position: WBlockPos
		get() = wrapped.pos.wrap()
	override val stack: IItemStack?
		get() = Backend.BACKEND_UNSUPPORTED()
}

fun ICPacketPlayerBlockPlacement.unwrap(): CPacketPlayerTryUseItemOnBlock = (this as CPacketPlayerBlockPlacementImpl<*>).wrapped
fun CPacketPlayerTryUseItemOnBlock.wrap(): ICPacketPlayerBlockPlacement = CPacketPlayerBlockPlacementImpl(this)
