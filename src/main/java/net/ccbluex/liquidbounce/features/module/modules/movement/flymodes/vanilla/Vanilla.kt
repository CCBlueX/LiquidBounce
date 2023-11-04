/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.vanilla

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.handleVanillaKickBypass
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.vanillaSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.MovementUtils.direction
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import kotlin.math.cos
import kotlin.math.sin

object Vanilla : FlyMode("Vanilla") {
	override fun onMove(event: MoveEvent) {
        val thePlayer = mc.thePlayer ?: return
		mc.thePlayer.capabilities.isFlying = false

		if (isMoving) {
			val dir = direction;
			
			val xDir = -sin(dir) * vanillaSpeed
			val zDir = cos(dir) * vanillaSpeed
			
			event.x = xDir
			event.z = zDir
			thePlayer.motionX = xDir * 0.9
			thePlayer.motionZ = zDir * 0.9
		}
		else
		{
			event.x = 0.0
			event.z = 0.0
			thePlayer.motionX = 0.0
			thePlayer.motionZ = 0.0
		}

        thePlayer.onGround = false
        thePlayer.isInWeb = false

        thePlayer.capabilities.isFlying = false

        var ySpeed = 0.0

        if (mc.gameSettings.keyBindJump.isKeyDown)
            ySpeed += vanillaSpeed

        if (mc.gameSettings.keyBindSneak.isKeyDown)
            ySpeed -= vanillaSpeed

        thePlayer.motionY = ySpeed
        event.y = ySpeed

		handleVanillaKickBypass()
	}
}
