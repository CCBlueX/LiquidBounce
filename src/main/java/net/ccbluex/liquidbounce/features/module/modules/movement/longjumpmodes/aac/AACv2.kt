/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.LongJumpMode
import net.ccbluex.liquidbounce.utils.MovementUtils

object AACv2 : LongJumpMode("AACv2") {
    override fun onUpdate() {
        player.jumpMovementFactor = 0.09f
        player.motionY += 0.01320999999999999
        player.jumpMovementFactor = 0.08f
        MovementUtils.strafe()
    }
}