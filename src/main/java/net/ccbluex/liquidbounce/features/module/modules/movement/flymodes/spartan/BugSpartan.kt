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
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition

object BugSpartan : FlyMode("BugSpartan") {
	override fun onEnable() {
		val (x, y, z) = mc.player

		repeat(65) {
			sendPackets(
				C04PacketPlayerPosition(x, y + 0.049, z, false),
				C04PacketPlayerPosition(x, y, z, false)
			)
		}

		sendPacket(C04PacketPlayerPosition(x, y + 0.1, z, true))

		mc.player.motionX *= 0.1
		mc.player.motionZ *= 0.1
		mc.player.swingItem()
	}

	override fun onUpdate() {
		mc.player.abilities.isFlying = false

		mc.player.motionY = when {
			mc.gameSettings.keyBindJump.isKeyDown -> vanillaSpeed.toDouble()
			mc.gameSettings.keyBindSneak.isKeyDown -> -vanillaSpeed.toDouble()
			else -> 0.0
		}

		strafe(vanillaSpeed, true)
	}
}
