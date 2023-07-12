package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.rewinside

import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.component1
import net.ccbluex.liquidbounce.utils.extensions.component2
import net.ccbluex.liquidbounce.utils.extensions.component3
import net.ccbluex.liquidbounce.utils.extensions.toRadiansD
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import kotlin.math.cos
import kotlin.math.sin

object TeleportRewinside : FlyMode("TeleportRewinside") {
	override fun onUpdate() {
		val (startX, startY, startZ) = mc.thePlayer

		val yawRad = -mc.thePlayer.rotationYaw.toRadiansD()
		val pitchRad = -mc.thePlayer.rotationPitch.toRadiansD()
		val distance = 9.9

		val endX = sin(yawRad) * cos(pitchRad) * distance + startX
		val endZ = cos(yawRad) * cos(pitchRad) * distance + startZ

		sendPackets(
			C04PacketPlayerPosition(endX, startY + 2, endZ, true),
			C04PacketPlayerPosition(startX, startY + 2, startZ, true)
		)

		mc.thePlayer.motionY = 0.0
	}
}