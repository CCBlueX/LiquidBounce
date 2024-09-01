/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionOnly

object AAC316 : FlyMode("AAC3.1.6-Gomme") {
	private var tick = 0
	private var noFlag = false
	
	override fun onUpdate() {
		mc.player.abilities.flying = true

		if (tick == 2) {
			mc.player.velocityY += 0.05
		} else if (tick > 2) {
			mc.player.velocityY -= 0.05
			tick = 0
		}

		tick++

		if (!noFlag)
			sendPacket(
				PositionOnly(mc.player.x, mc.player.z, mc.player.z, mc.player.onGround)
			)

		if (mc.player.z <= 0.0) noFlag = true
	}

	override fun onDisable() {
		noFlag = false
	}
}
