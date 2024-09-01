/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.ncp

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.ncpMotion
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.component1
import net.ccbluex.liquidbounce.utils.extensions.component2
import net.ccbluex.liquidbounce.utils.extensions.component3
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionOnly

object NCP : FlyMode("NCP") {
	override fun onEnable() {
		if (!mc.player.onGround) return

		val (x, y, z) = mc.player

		repeat(65) {
			sendPackets(
				PositionOnly(x, y + 0.049, z, false),
				PositionOnly(x, y, z, false)
			)
		}

		sendPacket(PositionOnly(x, y + 0.1, z, true))

		mc.player.velocityX *= 0.1
		mc.player.velocityZ *= 0.1
		mc.player.swingHand()
	}

	override fun onUpdate() {
		mc.player.velocityY =
			if (mc.options.sneakKey.isPressed) -0.5
			else -ncpMotion.toDouble()

		strafe()
	}

	override fun onPacket(event: PacketEvent) {
		val packet = event.packet

		if (packet is PlayerMoveC2SPacket)
			packet.onGround = true
	}

}
