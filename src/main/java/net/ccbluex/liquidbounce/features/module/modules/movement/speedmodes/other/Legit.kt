/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump

object Legit : SpeedMode("Legit") {
    override fun onStrafe() {
        val player = mc.player ?: return

        if (mc.player.onGround && isMoving) {
            player.tryJump()
        }
    }

    override fun onUpdate() {
        val player = mc.player ?: return

        player.isSprinting = player.movementInput.moveForward > 0.8
    }
}
