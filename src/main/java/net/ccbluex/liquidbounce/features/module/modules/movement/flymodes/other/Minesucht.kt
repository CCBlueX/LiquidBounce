/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.*
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionOnly

object Minesucht : FlyMode("Minesucht") {
	private var minesuchtTP = 0L
	
	override fun onUpdate() {
		val (x, y, z) = mc.player

		if (!mc.options.forwardKey.isPressed) return

		if (System.currentTimeMillis() - minesuchtTP > 99) {
			val vec = mc.player.eyes + mc.player.getLook(1f) * 7.0

			if (mc.player.fallDistance > 0.8) {
				sendPackets(
					PositionOnly(x, y + 50, z, false),
					PositionOnly(x, y + 20, z, true)
				)
				mc.player.fall(100f, 100f)
				mc.player.fallDistance = 0f
			}
			sendPackets(
				PositionOnly(vec.xCoord, y + 50, vec.zCoord, true),
				PositionOnly(x, y, z, false),
				PositionOnly(vec.xCoord, y, vec.zCoord, true),
				PositionOnly(x, y, z, false)
			)
			minesuchtTP = System.currentTimeMillis()
		} else {
			sendPackets(
				PositionOnly(x, y, z, false),
				PositionOnly(x, y, z, true)
			)
		}
	}
}
