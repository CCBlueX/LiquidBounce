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

		mc.player.setPositionAndUpdate(mc.player.posX, mc.player.posY + 0.42, mc.player.posZ)

		startYaw = mc.player.rotationYaw
		startPitch = mc.player.rotationPitch
	}

	override fun onUpdate() {
		if (timer.hasTimePassed(10)) {
			mc.player.abilities.isFlying = true
			return
		} else {
			mc.player.rotationYaw = startYaw
			mc.player.rotationPitch = startPitch
			mc.player.stop()
		}

		if (startY == BigDecimal(mc.player.posY).setScale(3, RoundingMode.HALF_DOWN).toDouble())
			timer.update()
	}

	override fun onMove(event: MoveEvent) {
		if (!timer.hasTimePassed(10))
			event.zero()
	}
}
