/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.spartan

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.vanillaSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.component1
import net.ccbluex.liquidbounce.utils.extensions.component2
import net.ccbluex.liquidbounce.utils.extensions.component3
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionOnly

object BugSpartan : FlyMode("BugSpartan") {
	override fun onEnable() {
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
		mc.player.abilities.flying = false

		mc.player.velocityY = when {
			mc.options.jumpKey.isPressed -> vanillaSpeed.toDouble()
			mc.options.sneakKey.isPressed -> -vanillaSpeed.toDouble()
			else -> 0.0
		}

		strafe(vanillaSpeed, true)
	}
}
