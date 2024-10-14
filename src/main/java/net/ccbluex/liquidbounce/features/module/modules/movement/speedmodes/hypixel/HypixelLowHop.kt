/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.hypixel

import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.glide
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.speed
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.minecraft.potion.Potion

/**
 * Working on Hypixel (Watchdog)
 * Tested on: play.hypixel.net
 * Credit: @LiquidSquid / Nextgen
 */
object HypixelLowHop : SpeedMode("HypixelLowHop") {

    private var airTicks = 0

    override fun onUpdate() {
        val player = mc.thePlayer ?: return
        if (!isMoving || player.fallDistance > 1.2) return

        if (player.onGround) {
            player.tryJump()
            strafe()
            airTicks = 0
            return
        } else {
            airTicks++

            when (airTicks) {
                1 -> {
                    strafe()
                }
                5 -> player.motionY -= 0.1905189780583944
                4 -> player.motionY -= 0.03
                6 -> player.motionY *= 1.01
                7 -> if (glide) player.motionY /= 1.5
            }

            if (airTicks >= 7 && glide) {
                strafe(speed = speed.coerceAtLeast(0.281F), strength = 0.7)
            }

            if (player.hurtTime == 9) {
                strafe()
            }

            if ((player.getActivePotionEffect(Potion.moveSpeed)?.amplifier ?: 0) == 2) {
                when (airTicks) {
                    1, 2, 5, 6, 8 -> {
                        player.motionX *= 1.2
                        player.motionZ *= 1.2
                    }
                }
            }
        }
    }

    override fun onJump(event: JumpEvent) {
        val player = mc.thePlayer ?: return
        if (!isMoving) return
        val atLeast = 0.281F + 0.13F * (player.getActivePotionEffect(Potion.moveSpeed)?.amplifier ?: 0)

        strafe(speed = speed.coerceAtLeast(atLeast))
    }

    override fun onDisable() {
        airTicks = 0
    }
}
