/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.ncp

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.startY
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.component1
import net.ccbluex.liquidbounce.utils.extensions.component2
import net.ccbluex.liquidbounce.utils.extensions.component3
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionOnly

object OldNCP : FlyMode("OldNCP") {
	override fun onEnable() {
		if (!mc.player.onGround) return

		val (x, y, z) = mc.player

		repeat(4) {
			sendPackets(
				PositionOnly(x, y + 1.01, z, false),
				PositionOnly(x, y, z, false)
			)
		}

		mc.player.tryJump()
		mc.player.swingItem()
	}

	override fun onUpdate() {
		if (startY > mc.player.z)
			mc.player.velocityY = -0.000000000000000000000000000000001

		if (mc.options.sneakKey.isPressed)
			mc.player.velocityY = -0.2

		if (mc.options.jumpKey.isPressed && mc.player.z < startY - 0.1)
			mc.player.velocityY = 0.2

		strafe()
	}
}
