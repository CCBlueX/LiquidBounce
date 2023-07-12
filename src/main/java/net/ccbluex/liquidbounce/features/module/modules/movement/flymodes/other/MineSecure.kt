package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.vanillaSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.component1
import net.ccbluex.liquidbounce.utils.extensions.component2
import net.ccbluex.liquidbounce.utils.extensions.component3
import net.ccbluex.liquidbounce.utils.extensions.toRadiansD
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import kotlin.math.cos
import kotlin.math.sin

object MineSecure : FlyMode("MineSecure") {
	private val timer = MSTimer()

	override fun onUpdate() {
		mc.thePlayer.capabilities.isFlying = false

		mc.thePlayer.motionY =
			if (mc.gameSettings.keyBindSneak.isKeyDown) 0.0
			else -0.01

		strafe(vanillaSpeed, true)

		if (!timer.hasTimePassed(150) || !mc.gameSettings.keyBindJump.isKeyDown)
			return

		val (x, y, z) = mc.thePlayer

		sendPackets(
			C04PacketPlayerPosition(x, y + 5, z, false),
			C04PacketPlayerPosition(0.5, -1000.0, 0.5, false)
		)

		val yaw = mc.thePlayer.rotationYaw.toRadiansD()

		mc.thePlayer.setPosition(x - sin(yaw) * 0.4, y, z + cos(yaw) * 0.4)
		timer.reset()
	}
}