/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.verus

import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe

/*
* Working on Verus: b3896/b3901
* Tested on: eu.loyisa.cn, anticheat-test.com
*
* Not that useful, but why not ;]
*/
object VerusGlide : FlyMode("VerusGlide") {

    override fun onUpdate() {
        val player = mc.thePlayer ?: return
        if (player.isInWater || player.isInLava || player.isInWeb || player.isOnLadder) return

        if (!player.onGround && player.fallDistance > 1) {
            // Good job verus
            player.motionY = -0.09800000190734863
            strafe(0.3345f)
        }
    }
}
