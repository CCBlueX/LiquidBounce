/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.mineplex

import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.LongJump
import net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.LongJumpMode
import net.ccbluex.liquidbounce.utils.MovementUtils

object Mineplex2 : LongJumpMode("Mineplex2") {
    override fun onUpdate() {
        run {
            if (!LongJump.canMineplexBoost)
                return@run

            mc.thePlayer.jumpMovementFactor = 0.1f
            if (mc.thePlayer.fallDistance > 1.5f) {
                mc.thePlayer.jumpMovementFactor = 0f
                mc.thePlayer.motionY = (-10f).toDouble()
            }

            MovementUtils.strafe()
        }
    }

    override fun onJump(event: JumpEvent) {
        if (mc.thePlayer.isCollidedHorizontally) {
            event.motion = 2.31f
            LongJump.canMineplexBoost = true
            mc.thePlayer.onGround = false
        }
    }
}