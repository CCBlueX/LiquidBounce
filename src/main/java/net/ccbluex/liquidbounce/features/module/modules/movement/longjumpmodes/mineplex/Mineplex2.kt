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

            player.jumpMovementFactor = 0.1f
            if (player.fallDistance > 1.5f) {
                player.jumpMovementFactor = 0f
                player.motionY = (-10f).toDouble()
            }

            MovementUtils.strafe()
        }
    }

    override fun onJump(event: JumpEvent) {
        if (player.isCollidedHorizontally) {
            event.motion = 2.31f
            LongJump.canMineplexBoost = true
            player.onGround = false
        }
    }
}