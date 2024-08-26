/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.vanillaSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.component1
import net.ccbluex.liquidbounce.utils.extensions.component2
import net.ccbluex.liquidbounce.utils.extensions.component3
import net.ccbluex.liquidbounce.utils.extensions.toRadiansD
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionOnly
import kotlin.math.cos
import kotlin.math.sin

object MineSecure : FlyMode("MineSecure") {
	private val timer = MSTimer()

	override fun onUpdate() {
		mc.player.abilities.flying = false

		mc.player.velocityY =
			if (mc.options.sneakKey.isPressed) 0.0
			else -0.01

		strafe(vanillaSpeed, true)

		if (!timer.hasTimePassed(150) || !mc.options.jumpKey.isPressed)
			return

		val (x, y, z) = mc.player

		sendPackets(
			PositionOnly(x, y + 5, z, false),
			PositionOnly(0.5, -1000.0, 0.5, false)
		)

		val yaw = mc.player.yaw.toRadiansD()

		mc.player.updatePosition(x - sin(yaw) * 0.4, y, z + cos(yaw) * 0.4)
		timer.reset()
	}
}
