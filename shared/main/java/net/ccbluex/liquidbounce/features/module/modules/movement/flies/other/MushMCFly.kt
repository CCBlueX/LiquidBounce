package net.ccbluex.liquidbounce.features.module.modules.movement.flies.other

import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.DamageOnStart
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.extensions.strafe
import net.ccbluex.liquidbounce.utils.extensions.zeroXYZ
import net.ccbluex.liquidbounce.utils.timer.MSTimer

class MushMCFly : FlyMode("MushMC")
{
	override val damageOnStart: DamageOnStart
		get() = DamageOnStart.NONE

	private val mushTimer = MSTimer()
	private var mushAfterJump = false

	override fun onEnable()
	{
		val thePlayer = mc.thePlayer ?: return
		val networkManager = mc.netHandler.networkManager

		val x = thePlayer.posX
		val y = thePlayer.posY
		val z = thePlayer.posZ

		mushTimer.reset()
		thePlayer.setPosition(x, y + 0.1, z)
		thePlayer.jump()

		repeat(3) {
			networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(x, y + 1.01, z, false))
			networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(x, y, z, false))
		}

		networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(x, y + 0.15, z, false))
		networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(x, y, z, false))
		networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(x, y, z, true))

		mushAfterJump = false
	}

	override fun onUpdate()
	{
		val thePlayer = mc.thePlayer ?: return
		val networkManager = mc.netHandler.networkManager
		val provider = classProvider

		if (!mushAfterJump)
		{
			if (thePlayer.onGround) mushAfterJump = true
			return
		}

		val x = thePlayer.posX
		val y = thePlayer.posY
		val z = thePlayer.posZ

		thePlayer.zeroXYZ()

		if (mc.gameSettings.keyBindForward.isKeyDown) thePlayer.strafe(Fly.mushMCSpeedValue.get())
		if (Fly.mushMCBoostDelay.get() != 0 && mushTimer.hasTimePassed((Fly.mushMCBoostDelay.get() * 300).toLong()))
		{
			repeat(3) {
				networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(x, y + 1.01, z, false))
				networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(x, y, z, false))
			}

			networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(x, y + 0.15, z, false))
			networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(x, y, z, false))
			networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(x, y, z, true))

			mushTimer.reset()
		}

		thePlayer.motionY = 0.0
	}

	override fun onJump(event: JumpEvent)
	{
		event.cancelEvent()
	}
}
