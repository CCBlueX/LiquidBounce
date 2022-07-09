/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.client.entity

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.network.IINetHandlerPlayClient
import net.ccbluex.liquidbounce.api.minecraft.util.IIChatComponent
import net.ccbluex.liquidbounce.api.minecraft.util.IMovementInput
import net.ccbluex.liquidbounce.injection.backend.minecraft.client.network.wrap
import net.ccbluex.liquidbounce.injection.backend.minecraft.util.unwrap
import net.ccbluex.liquidbounce.injection.backend.minecraft.util.wrap
import net.minecraft.client.entity.EntityPlayerSP

open class EntityPlayerSPImpl<out T : EntityPlayerSP>(wrapped: T) : AbstractClientPlayerImpl<T>(wrapped), IEntityPlayerSP
{
	override var horseJumpPowerCounter: Int
		get() = wrapped.horseJumpPowerCounter
		set(value)
		{
			wrapped.horseJumpPowerCounter = value
		}
	override var horseJumpPower: Float
		get() = wrapped.horseJumpPower
		set(value)
		{
			wrapped.horseJumpPower = value
		}
	override val sendQueue: IINetHandlerPlayClient
		get() = wrapped.sendQueue.wrap()
	override val movementInput: IMovementInput
		get() = wrapped.movementInput.wrap()
	override val sneaking: Boolean
		get() = wrapped.isSneaking
	override var serverSprintState: Boolean
		get() = wrapped.serverSprintState
		set(value)
		{
			wrapped.serverSprintState = value
		}

	override val timeInPortal: Float
		get() = wrapped.timeInPortal

	override fun sendChatMessage(msg: String) = wrapped.sendChatMessage(msg)

	override fun respawnPlayer() = wrapped.respawnPlayer()

	override fun addChatMessage(component: IIChatComponent) = wrapped.addChatMessage(component.unwrap())

	override fun closeScreen() = wrapped.closeScreen()
}

fun IEntityPlayerSP.unwrap(): EntityPlayerSP = (this as EntityPlayerSPImpl<*>).wrapped
fun EntityPlayerSP.wrap(): IEntityPlayerSP = EntityPlayerSPImpl(this)
