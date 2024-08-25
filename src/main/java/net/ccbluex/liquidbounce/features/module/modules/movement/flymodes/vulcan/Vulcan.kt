/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.vulcan

import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode

object Vulcan : FlyMode("Vulcan") {
    override fun onUpdate() {
        if (!mc.player.onGround && mc.player.fallDistance > 0) {
            if (mc.player.ticksAlive % 2 == 0) {
                mc.player.velocityY = -0.155
            } else {
                mc.player.velocityY = -0.1
            }
        }
    }
}
