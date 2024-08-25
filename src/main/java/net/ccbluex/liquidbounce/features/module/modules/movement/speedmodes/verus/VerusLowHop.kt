/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.verus

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.minecraft.potion.Potion

object VerusLowHop : SpeedMode("VerusLowHop") {

    private var speed = 0.0f
    private var airTicks = 0

    override fun onUpdate() {
        val player = mc.player ?: return
        if (player.isInWater || player.isInLava || player.isInWeb || player.isOnLadder) return
        
        if (isMoving) {
            if (player.onGround) {
                airTicks = 0
                speed = if (player.isPotionActive(Potion.moveSpeed)
                    && player.getActivePotionEffect(Potion.moveSpeed).amplifier >= 1)
                        0.5f else 0.36f

                player.tryJump()
            } else {
                if (airTicks == 0) {
                    player.velocityY = -0.09800000190734863
                }

                airTicks++
                speed *= 0.98f
            }

            strafe(speed)
        }
    }
}
