/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
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
		val (startX, startY, startZ) = player

		val yawRad = -player.rotationYaw.toRadiansD()
		val pitchRad = -player.rotationPitch.toRadiansD()
		val distance = 9.9

		val endX = sin(yawRad) * cos(pitchRad) * distance + startX
		val endZ = cos(yawRad) * cos(pitchRad) * distance + startZ

		sendPackets(
			C04PacketPlayerPosition(endX, startY + 2, endZ, true),
			C04PacketPlayerPosition(startX, startY + 2, startZ, true)
		)

		player.motionY = 0.0
	}
}
