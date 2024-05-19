/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.hypixel

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.startY
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.extensions.stop
import net.ccbluex.liquidbounce.utils.timing.TickTimer
import java.math.BigDecimal
import java.math.RoundingMode

object FreeHypixel : FlyMode("FreeHypixel") {
	private val timer = TickTimer()
	private var startYaw = 0f
	private var startPitch = 0f

	override fun onEnable() {
		timer.reset()

		player.setPositionAndUpdate(player.posX, player.posY + 0.42, player.posZ)

		startYaw = player.rotationYaw
		startPitch = player.rotationPitch
	}

	override fun onUpdate() {
		if (timer.hasTimePassed(10)) {
			player.capabilities.isFlying = true
			return
		} else {
			player.rotationYaw = startYaw
			player.rotationPitch = startPitch
			player.stop()
		}

		if (startY == BigDecimal(player.posY).setScale(3, RoundingMode.HALF_DOWN).toDouble())
			timer.update()
	}

	override fun onMove(event: MoveEvent) {
		if (!timer.hasTimePassed(10))
			event.zero()
	}
}
