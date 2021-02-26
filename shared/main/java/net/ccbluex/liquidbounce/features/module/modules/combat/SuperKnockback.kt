/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketEntityAction
import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.value.IntegerValue

@ModuleInfo(name = "SuperKnockback", description = "Increases knockback dealt to other entities.", category = ModuleCategory.COMBAT)
class SuperKnockback : Module()
{
	private val hurtTimeValue = IntegerValue("HurtTime", 10, 0, 10)

	@EventTarget
	fun onAttack(event: AttackEvent)
	{
		val provider = classProvider

		val targetEntity = event.targetEntity

		if (targetEntity != null && provider.isEntityLivingBase(targetEntity))
		{
			if (targetEntity.asEntityLivingBase().hurtTime > hurtTimeValue.get()) return

			val thePlayer = mc.thePlayer ?: return
			val netHandler = mc.netHandler

			if (thePlayer.sprinting) netHandler.addToSendQueue(provider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.STOP_SPRINTING))

			netHandler.addToSendQueue(provider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.START_SPRINTING))
			netHandler.addToSendQueue(provider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.STOP_SPRINTING))
			netHandler.addToSendQueue(provider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.START_SPRINTING))
			thePlayer.sprinting = true
			thePlayer.serverSprintState = true
		}
	}
}
