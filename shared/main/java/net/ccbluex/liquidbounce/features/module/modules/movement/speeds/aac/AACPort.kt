/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock

class AACPort : SpeedMode("AACPort")
{
	override fun onMotion(eventState: EventState)
	{
	}

	override fun onUpdate()
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		if (!MovementUtils.isMoving(thePlayer)) return

		val dir = MovementUtils.getDirection(thePlayer)
		var speed = 0.2
		val maxSpeed = (LiquidBounce.moduleManager[Speed::class.java] as Speed).portMax.get()

		while (speed <= maxSpeed)
		{
			val func = functions

			val x = thePlayer.posX - func.sin(dir) * speed
			val z = thePlayer.posZ + func.cos(dir) * speed

			val provider = classProvider

			if (thePlayer.posY < thePlayer.posY.toInt() + 0.5 && !provider.isBlockAir(getBlock(theWorld, WBlockPos(x, thePlayer.posY, z)))) break

			thePlayer.sendQueue.addToSendQueue(provider.createCPacketPlayerPosition(x, thePlayer.posY, z, true))
			speed += 0.2
		}
	}

	override fun onMove(event: MoveEvent)
	{
	}
}
