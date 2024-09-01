/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.verus

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.extensions.isInWeb
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.minecraft.entity.effect.StatusEffect

object VerusLowHopNew : SpeedMode("VerusLowHopNew") {

    private var speed = 0.0f
    private var airTicks = 0

    override fun onUpdate() {
        val player = mc.player ?: return
        if (player.isTouchingWater || player.isTouchingLava || player.isInWeb() || player.isClimbing) return

        if (isMoving) {
            if (player.onGround) {
                player.tryJump()
                airTicks = 0

                // Checks the presence of Speed potion effect 1 & 2+
                if (player.hasStatusEffect(StatusEffect.SPEED)) {
                    val amplifier = player.getEffectInstance(StatusEffect.SPEED).amplifier

                    speed = when {
                        amplifier == 1 -> 0.55f
                        amplifier >= 2 -> 0.7f
                        else -> 0.33f
                    }
                }

                // Checks the presence of Slowness potion effect.
                speed = if (player.hasStatusEffect(StatusEffect.SLOWNESS)
                    && player.getEffectInstance(StatusEffect.SLOWNESS).amplifier == 1) {
                    0.3f
                } else {
                    0.33f
                }
            } else {
                if (airTicks == 0) {
                    mc.player.velocityY = -0.09800000190734863
                }

                airTicks++
                speed *= 0.99f
            }

            strafe(speed, false)
        }
    }
}
