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

        if (!isMoving || player.isInWater || player.isInLava || player.isInWeb || player.isOnLadder) return

        if (player.onGround) {
            player.tryJump()

            if (player.isSprinting) strafe(Speed.strafeStrength)

            mc.timer.timerSpeed = Speed.groundTimer
        } else {
            mc.timer.timerSpeed = Speed.airTimer
        }

        if (Speed.boost && player.motionY > 0.003 && player.isSprinting) {
            player.motionX *= 1.0015
            player.motionZ *= 1.0015
        }
    }
}
