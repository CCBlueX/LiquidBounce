/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketConfirmTransaction
import net.minecraft.network.play.client.CPacketConfirmTransaction
import net.minecraft.network.play.server.SPacketConfirmTransaction

class CPacketConfirmTransactionImpl<out T : CPacketConfirmTransaction>(wrapped: T) : PacketImpl<T>(wrapped), ICPacketConfirmTransaction
{
	override val windowId: Int
		get() = wrapped.windowId

	override val uid: Short
		get() = wrapped.uid

	override val accepted: Boolean
		get() = wrapped.accepted
}

fun ICPacketConfirmTransaction.unwrap(): CPacketConfirmTransaction = (this as CPacketConfirmTransactionImpl<*>).wrapped
fun CPacketConfirmTransaction.wrap(): ICPacketConfirmTransaction = CPacketConfirmTransactionImpl(this)
