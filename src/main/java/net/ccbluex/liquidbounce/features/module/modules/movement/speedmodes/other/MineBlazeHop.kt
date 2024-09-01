/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.extensions.tryJump

/*
* Working on Intave: 14
* Tested on: mc.mineblaze.net
* Credit: @thatonecoder & @larryngton / Intave14
*/
object MineBlazeHop : SpeedMode("MineBlazeHop") {

    override fun onUpdate() {
        val player = mc.player ?: return

        if (!isMoving || player.isTouchingWater || player.isTouchingLava || player.isInWeb() || player.isClimbing) return

        if (player.onGround) {
            player.tryJump()

            if (player.isSprinting) strafe(strength = Speed.strafeStrength.toDouble())

            mc.ticker.timerSpeed = Speed.groundTimer
        } else {
            mc.ticker.timerSpeed = Speed.airTimer
        }

        if (Speed.boost && player.velocityY > 0.003 && player.isSprinting) {
            player.velocityX *= 1.0015
            player.velocityZ *= 1.0015
        }
    }
}
