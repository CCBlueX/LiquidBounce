package net.ccbluex.liquidbounce.features.module.modules.movement.flies.redesky

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.extensions.forward
import net.ccbluex.liquidbounce.utils.extensions.strafe
import net.ccbluex.liquidbounce.utils.extensions.zeroXZ

class RedeSkyGlide : FlyMode("RedeSky-Glide")
{
	override fun onEnable()
	{
		val thePlayer = mc.thePlayer ?: return

		if (thePlayer.onGround) redeskyVClip(thePlayer, Fly.redeskyVClipHeight.get())
	}

	override fun onUpdate()
	{
		val thePlayer = mc.thePlayer ?: return

		mc.timer.timerSpeed = 0.3f

		redeskyPacketHClip(thePlayer, 7.0)
		redeskyPacketVClip(thePlayer, 10.0)

		redeskyVClip(thePlayer, -0.5f)
		thePlayer.forward(2.0)

		thePlayer.strafe(1F)

		thePlayer.motionY = -0.01
	}

	override fun onDisable()
	{
		val thePlayer = mc.thePlayer ?: return

		thePlayer.zeroXZ()
		redeskyPacketHClip(thePlayer, 0.0)
	}

	private fun redeskyPacketHClip(thePlayer: IEntity, horizontal: Double)
	{
		val func = functions

		val playerYaw = WMathHelper.toRadians(thePlayer.rotationYaw)

		mc.netHandler.networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(thePlayer.posX + horizontal * -func.sin(playerYaw), thePlayer.posY, thePlayer.posZ + horizontal * func.cos(playerYaw), false))
	}

	private fun redeskyVClip(thePlayer: IEntity, vertical: Float)
	{
		thePlayer.setPosition(thePlayer.posX, thePlayer.posY + vertical, thePlayer.posZ)
	}

	private fun redeskyPacketVClip(thePlayer: IEntity, vertical: Double)
	{
		mc.netHandler.networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(thePlayer.posX, thePlayer.posY + vertical, thePlayer.posZ, false))
	}
}
