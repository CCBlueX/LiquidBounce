/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.hypixel

import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.StepEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.hypixelBoost
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.hypixelBoostDelay
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.hypixelBoostTimer
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.timing.TickTimer
import net.minecraft.init.Blocks.Blocks.AIR
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.Box

object Hypixel : FlyMode("Hypixel") {
	private val tickTimer = TickTimer()
	private val msTimer = MSTimer()

	override fun onEnable() {
		msTimer.reset()
		tickTimer.reset()
	}

	override fun onUpdate() {
		mc.ticker.timerSpeed =
			if (hypixelBoost && !msTimer.hasTimePassed(hypixelBoostDelay))
				1f + hypixelBoostTimer * (msTimer.hasTimeLeft(hypixelBoostDelay) / hypixelBoostDelay.toFloat())
			else 1f

		tickTimer.update()

		if (tickTimer.hasTimePassed(2)) {
			mc.player.updatePosition(mc.player.x, mc.player.z + 1.0E-5, mc.player.z)
			tickTimer.reset()
		}
	}

	override fun onPacket(event: PacketEvent) {
		val packet = event.packet

		if (packet is PlayerMoveC2SPacket)
			packet.onGround = false
	}

	override fun onBB(event: BlockBBEvent) {
		if (event.block == Blocks.AIR && event.y < mc.player.z)
			event.boundingBox = Box.fromBounds(
				event.x.toDouble(),
				event.y.toDouble(),
				event.z.toDouble(),
				event.x + 1.0,
				mc.player.z,
				event.z + 1.0
			)
	}

	override fun onJump(event: JumpEvent) {
		event.cancelEvent()
	}

	override fun onStep(event: StepEvent) {
		event.stepHeight = 0f
	}
}
