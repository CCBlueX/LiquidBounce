/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.network.play.client

import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketConfirmTransaction
import net.ccbluex.liquidbounce.injection.backend.minecraft.network.PacketImpl
import net.minecraft.network.play.client.C0FPacketConfirmTransaction

class CPacketConfirmTransactionImpl<out T : C0FPacketConfirmTransaction>(wrapped: T) : PacketImpl<T>(wrapped), ICPacketConfirmTransaction
{
	override val windowId: Int
		get() = wrapped.windowId

	override val uid: Short
		get() = wrapped.uid

	override val accepted: Boolean
		get() = wrapped.accepted
}

fun ICPacketConfirmTransaction.unwrap(): C0FPacketConfirmTransaction = (this as CPacketConfirmTransactionImpl<*>).wrapped
fun C0FPacketConfirmTransaction.wrap(): ICPacketConfirmTransaction = CPacketConfirmTransactionImpl(this)
