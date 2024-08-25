/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.aac

import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.aacSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.startY
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawPlatform
import net.minecraft.network.packet.c2s.play.C03PacketPlayer
import java.awt.Color

object AAC1910 : FlyMode("AAC1.9.10") {

	private var jump = 0.0

	override fun onEnable() {
		jump = 3.8
	}
	override fun onUpdate() {
		if (mc.options.jumpKey.isPressed)
			jump += 0.2

		if (mc.options.sneakKey.isPressed)
			jump -= 0.2

		if (startY + jump > mc.player.z) {
			sendPacket(C03PacketPlayer(true))
			mc.player.velocityY = 0.8
			strafe(aacSpeed)
		}

		// TODO: Doesn't this always overwrite the strafe(aacSpeed)?
		strafe()
	}

	override fun onRender3D(event: Render3DEvent) {
		drawPlatform(startY + jump, Color(0, 0, 255, 90), 1.0)
	}
}
