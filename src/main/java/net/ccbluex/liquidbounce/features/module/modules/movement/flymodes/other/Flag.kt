/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.component1
import net.ccbluex.liquidbounce.utils.extensions.component2
import net.ccbluex.liquidbounce.utils.extensions.component3
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionOnly

object Flag : FlyMode("Flag") {
	override fun onUpdate() {
		val (x, y, z) = mc.player
		
		sendPackets(
			PositionOnly(
				x + mc.player.velocityX * 999,
				y + (if (mc.options.jumpKey.isPressed) 1.5624 else 0.00000001) - if (mc.options.sneakKey.isPressed) 0.0624 else 0.00000002,
				z + mc.player.velocityZ * 999,
				true
			),
			PositionOnly(
				x + mc.player.velocityX * 999,
				y - 6969,
				z + mc.player.velocityZ * 999,
				true
			)
		)

		mc.player.updatePosition(x + mc.player.velocityX * 11, y, z + mc.player.velocityZ * 11)
		mc.player.velocityY = 0.0
	}
}
