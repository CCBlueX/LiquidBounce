/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.aac

import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.aacMotion
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import org.lwjgl.input.Keyboard
import java.awt.Color

object AAC3312 : FlyMode("AAC3.3.12") {
	override fun onUpdate() {
		if (mc.player.posY < -70)
			mc.player.motionY = aacMotion.toDouble()

		mc.timer.timerSpeed = 1f

		if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
			mc.timer.timerSpeed = 0.2f
			mc.rightClickDelayTimer = 0
		}
	}

	override fun onRender3D(event: Render3DEvent) {
		RenderUtils.drawPlatform(-70.0, Color(0, 0, 255, 90), 1.0)
	}
}
