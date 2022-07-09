package net.ccbluex.liquidbounce.features.module.modules.movement.flies.redesky

import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode

class RedeSkySmoothFly : FlyMode("RedeSky-Smooth")
{
	private val ticks = 0

	override fun onEnable()
	{
		val thePlayer = mc.thePlayer ?: return

		thePlayer.motionY += Fly.redeSkySmoothMotionValue.get()
		thePlayer.isAirBorne = true
	}

	override fun onUpdate()
	{
	}

	override fun onMove(event: MoveEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		if (ticks > 10 && (thePlayer.isCollidedHorizontally || thePlayer.isCollidedVertically || thePlayer.onGround))
		{
			Fly.state = false
			return
		}

		val speed = Fly.redeSkySmoothSpeedValue.get() / 10f + ticks * (Fly.redeSkySmoothSpeedChangeValue.get() / 1000f)
		mc.timer.timerSpeed = Fly.redeSkySmoothTimerValue.get()
		thePlayer.capabilities.flySpeed = speed
		thePlayer.capabilities.isFlying = true
		thePlayer.setPosition(thePlayer.posX, thePlayer.posY - if (Fly.redeSkySmoothDropoffAValue.get()) Fly.redeSkySmoothDropoffValue.get() / 1000f * ticks else Fly.redeSkySmoothDropoffValue.get() / 300f, thePlayer.posZ)
	}

	override fun onBlockBB(event: BlockBBEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		if (classProvider.isBlockAir(event.block) && event.y < thePlayer.posY) event.boundingBox = classProvider.createAxisAlignedBB(event.x.toDouble(), event.y.toDouble(), event.z.toDouble(), event.x + 1.0, thePlayer.posY, event.z + 1.0)
	}

	override fun onDisable()
	{
		(mc.thePlayer ?: return).capabilities.isFlying = false
	}
}
