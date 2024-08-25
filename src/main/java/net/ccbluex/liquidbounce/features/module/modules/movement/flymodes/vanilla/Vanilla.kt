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
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.extensions.stopXZ
import kotlin.math.cos
import kotlin.math.sin

object Vanilla : FlyMode("Vanilla") {
	override fun onMove(event: MoveEvent) {
        val thePlayer = mc.player ?: return

		strafe(vanillaSpeed, true, event)

        thePlayer.onGround = false
        thePlayer.isInWeb = false

        theplayer.abilities.isFlying = false

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
