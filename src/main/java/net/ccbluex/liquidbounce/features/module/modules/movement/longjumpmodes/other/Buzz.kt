/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.LongJumpMode
import net.ccbluex.liquidbounce.utils.MovementUtils

object Buzz : LongJumpMode("Buzz") {
    override fun onUpdate() {
        mc.thePlayer.motionY += 0.4679942989799998
        MovementUtils.speed *= 0.7578698f
    }
}
