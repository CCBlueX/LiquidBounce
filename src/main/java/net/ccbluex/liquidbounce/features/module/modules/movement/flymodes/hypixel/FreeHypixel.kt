package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.hypixel

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.startY
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.extensions.stop
import net.ccbluex.liquidbounce.utils.timer.TickTimer
import java.math.BigDecimal
import java.math.RoundingMode

object FreeHypixel : FlyMode("FreeHypixel") {
	private val timer = TickTimer()
	private var startYaw = 0f
	private var startPitch = 0f

	override fun onEnable() {
		timer.reset()

		mc.thePlayer.setPositionAndUpdate(mc.thePlayer.posX, mc.thePlayer.posY + 0.42, mc.thePlayer.posZ)

		startYaw = mc.thePlayer.rotationYaw
		startPitch = mc.thePlayer.rotationPitch
	}

	override fun onUpdate() {
		if (timer.hasTimePassed(10)) {
			mc.thePlayer.capabilities.isFlying = true
			return
		} else {
			mc.thePlayer.rotationYaw = startYaw
			mc.thePlayer.rotationPitch = startPitch
			mc.thePlayer.stop()
		}

		if (startY == BigDecimal(mc.thePlayer.posY).setScale(3, RoundingMode.HALF_DOWN).toDouble())
			timer.update()
	}

	override fun onMove(event: MoveEvent) {
		if (!timer.hasTimePassed(10))
			event.zero()
	}
}