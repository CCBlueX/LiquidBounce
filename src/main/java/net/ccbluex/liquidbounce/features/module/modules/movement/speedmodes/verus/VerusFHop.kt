/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.verus

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.extensions.tryJump

object VerusFHop : SpeedMode("VerusFHop") {
    override fun onMotion() {
        val player = mc.thePlayer ?: return

        if (player.movementInput.moveForward != 0f && player.movementInput.moveStrafe != 0f) {
            if (player.onGround) {
                if (player.movementInput.moveForward != 0f && player.movementInput.moveStrafe != 0f) {
                  strafe(0.4825f)
              } else {
                  strafe(0.535f)
              }

                player.tryJump()
            } else {
              if (player.movementInput.moveForward != 0f && player.movementInput.moveStrafe != 0f) {
                  strafe(0.334f)
              } else {
                  strafe(0.3345f)
              }
          }
    }
}
