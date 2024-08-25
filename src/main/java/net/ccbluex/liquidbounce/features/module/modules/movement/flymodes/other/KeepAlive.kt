/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.vanillaSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.minecraft.network.play.client.C00PacketKeepAlive

object KeepAlive : FlyMode("KeepAlive") {
	override fun onUpdate() {
		sendPacket(C00PacketKeepAlive())
		mc.player.capabilities.isFlying = false

		mc.player.motionY = when {
			mc.gameSettings.keyBindJump.isKeyDown -> vanillaSpeed.toDouble()
			mc.gameSettings.keyBindSneak.isKeyDown -> -vanillaSpeed.toDouble()
			else -> 0.0
		}

		strafe(vanillaSpeed, true)
	}
}
