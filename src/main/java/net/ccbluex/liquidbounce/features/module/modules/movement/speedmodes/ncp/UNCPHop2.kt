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
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.potion.Potion

object UNCPHop2 : SpeedMode("UNCPHop2") {
    private var speed = 0.0f
    private var tick = 0

    override fun onUpdate() {
        val player = mc.player ?: return
        if (player.isTouchingWater || player.isTouchingLava || player.isInWeb() || player.isClimbing) return

        if (isMoving) {
            if (player.onGround) {
                speed = if (player.hasStatusEffect(StatusEffect.SPEED)
                    && mc.player.hasStatusEffect(StatusEffect.SPEED))
                    0.4563f else 0.3385f

                player.tryJump()
            } else {
                speed *= 0.98f
            }

            if (player.velocityDirty && player.fallDistance > 2) {
                mc.ticker.timerSpeed = 1f
                return
            }

            strafe(speed, false)

            if (!player.onGround && ++tick % 3 == 0) {
                mc.ticker.timerSpeed = 1.0815f
                tick = 0
            } else {
                mc.ticker.timerSpeed = 0.9598f
            }
        } else {
            mc.ticker.timerSpeed = 1f
        }
    }

    override fun onDisable() {
        mc.ticker.timerSpeed = 1f
    }
}