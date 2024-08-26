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
import net.minecraft.network.packet.c2s.play.KeepAliveC2SPacket

object KeepAlive : FlyMode("KeepAlive") {
	override fun onUpdate() {
		sendPacket(KeepAliveC2SPacket())
		mc.player.abilities.flying = false

		mc.player.velocityY = when {
			mc.options.jumpKey.isPressed -> vanillaSpeed.toDouble()
			mc.options.sneakKey.isPressed -> -vanillaSpeed.toDouble()
			else -> 0.0
		}

		strafe(vanillaSpeed, true)
	}
}
