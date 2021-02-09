/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class AAC3_3_11Ground : SpeedMode("AAC3.3.11-Ground") // Was AACGround
{
	override fun onUpdate()
	{
		val thePlayer = mc.thePlayer ?: return

		if (!MovementUtils.isMoving(thePlayer)) return

		mc.timer.timerSpeed = (LiquidBounce.moduleManager[Speed::class.java] as Speed).aacGroundTimerValue.get()
		mc.netHandler.addToSendQueue(classProvider.createCPacketPlayerPosition(thePlayer.posX, thePlayer.posY, thePlayer.posZ, true))
	}

	override fun onMotion(eventState: EventState)
	{
	}

	override fun onMove(event: MoveEvent)
	{
	}

	override fun onDisable()
	{
		mc.timer.timerSpeed = 1f
	}
}
