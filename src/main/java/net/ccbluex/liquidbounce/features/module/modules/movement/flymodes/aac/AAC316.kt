/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition

object AAC316 : FlyMode("AAC3.1.6-Gomme") {
	private var tick = 0
	private var noFlag = false
	
	override fun onUpdate() {
		player.capabilities.isFlying = true

		if (tick == 2) {
			player.motionY += 0.05
		} else if (tick > 2) {
			player.motionY -= 0.05
			tick = 0
		}

		tick++

		if (!noFlag)
			sendPacket(
				C04PacketPlayerPosition(player.posX, player.posY, player.posZ, player.onGround)
			)

		if (player.posY <= 0.0) noFlag = true
	}

	override fun onDisable() {
		noFlag = false
	}
}