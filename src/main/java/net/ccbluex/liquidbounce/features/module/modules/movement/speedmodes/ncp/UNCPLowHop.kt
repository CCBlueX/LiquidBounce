/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.ncp

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.extensions.tryJump

/*
* Working on UNCP/NCP & Verus b3896/b3901
* Tested on: eu.loyisa.cn, anticheat-test.com
* Credit: @liquidsquid1
*/
object UNCPLowHop : SpeedMode("UNCPLowHop") {

    private var airTick = 0

    override fun onUpdate() {
        val player = mc.player ?: return
        if (player.isTouchingWater || player.isTouchingLava || player.isInWeb() || player.isClimbing) return

        if (isMoving) {
            if (player.onGround) {
                player.tryJump()
                airTick = 0
            } else {
                if (airTick == 4) {
                    player.velocityY = -0.09800000190734863
                }

                airTick++
            }

            strafe()
        }
    }
}
