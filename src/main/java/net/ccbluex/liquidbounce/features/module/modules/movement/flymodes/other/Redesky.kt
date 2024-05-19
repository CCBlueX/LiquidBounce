/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.redeskyHeight
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.component1
import net.ccbluex.liquidbounce.utils.extensions.component2
import net.ccbluex.liquidbounce.utils.extensions.component3
import net.ccbluex.liquidbounce.utils.extensions.toRadiansD
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import kotlin.math.cos
import kotlin.math.sin

object Redesky : FlyMode("Redesky") {
	override fun onEnable() {
		if (player.onGround)
			player.setPosition(player.posX, player.posY + redeskyHeight, player.posZ)
	}

	override fun onUpdate() {
		val (x, y, z) = player

		mc.timer.timerSpeed = 0.3f

		val yaw = player.rotationYaw.toRadiansD()
		val sinYaw = sin(yaw)
		val cosYaw = cos(yaw)

		// Simplified version of the original code (weird af)
		sendPackets(
			C04PacketPlayerPosition(x - sinYaw * 7, y, z + cosYaw * 7, false),
			C04PacketPlayerPosition(x, y + 10, z, false)
		)

		player.setPosition(x, y - 0.5, z)
		player.setPosition(x - sinYaw * 2, y, z + cosYaw * 2)

		player.motionX = -sinYaw
		player.motionY = -0.01
		player.motionZ = cosYaw
	}

	override fun onDisable() {
		sendPacket(
			C04PacketPlayerPosition(player.posX, player.posY, player.posZ, player.onGround)
		)
	}
}
