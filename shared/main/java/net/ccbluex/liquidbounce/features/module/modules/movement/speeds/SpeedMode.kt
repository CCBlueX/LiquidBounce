/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.MovementUtils

abstract class SpeedMode(val modeName: String) : MinecraftInstance()
{
	val isActive: Boolean
		get()
		{
			val speed = LiquidBounce.moduleManager[Speed::class.java] as Speed?
			return speed != null && !mc.thePlayer!!.sneaking && speed.state && speed.modeValue.get() == modeName
		}

	abstract fun onMotion(eventState: EventState)
	abstract fun onUpdate()
	abstract fun onMove(event: MoveEvent)

	open fun onTick()
	{
	}

	open fun onEnable()
	{
	}

	open fun onDisable()
	{
	}

	protected fun jump(thePlayer: IEntityPlayerSP)
	{
		thePlayer.jump() // Jump without sprint-jump boost

		// Apply the sprint-jump boost manually to prevent double-boost
		val dir = MovementUtils.getDirection(thePlayer)
		thePlayer.motionX -= functions.sin(dir) * 0.2f
		thePlayer.motionZ += functions.cos(dir) * 0.2f
	}
}
