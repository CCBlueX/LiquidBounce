/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.LongJumpMode

object Hycraft : LongJumpMode("Hycraft") {
    override fun onUpdate() {
        if (mc.player.velocityY < 0) {
            mc.player.velocityY *= 0.75f
            mc.player.flyingSpeed = 0.055f
        } else {
            mc.player.velocityY += 0.02f
            mc.player.flyingSpeed = 0.08f
        }
    }
}