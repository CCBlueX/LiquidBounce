package net.ccbluex.liquidbounce.features.module.modules.movement.flies.ncp

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.DamageOnStart
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class NCPGlide : FlyMode("NCP")
{
	override val damageOnStart: DamageOnStart
		get() = DamageOnStart.NCP

	override fun onEnable()
	{
		val thePlayer = mc.thePlayer ?: return

		if (!thePlayer.onGround) return

		thePlayer.motionX *= 0.1
		thePlayer.motionZ *= 0.1

		thePlayer.swingItem()
	}

	override fun onUpdate()
	{
		val thePlayer = mc.thePlayer ?: return

		thePlayer.motionY = (-Fly.ncpMotionValue.get()).toDouble()
		if (mc.gameSettings.keyBindSneak.isKeyDown) thePlayer.motionY = -0.5
		MovementUtils.strafe(thePlayer)
	}

	override fun onPacket(event: PacketEvent)
	{
		val packet = event.packet

		if (classProvider.isCPacketPlayer(packet)) packet.asCPacketPlayer().onGround = true
	}
}
