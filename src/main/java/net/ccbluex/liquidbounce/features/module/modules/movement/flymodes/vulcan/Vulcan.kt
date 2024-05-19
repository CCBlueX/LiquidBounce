/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.vulcan

import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode

object Vulcan : FlyMode("Vulcan") {
    override fun onUpdate() {
        if (!player.onGround && player.fallDistance > 0) {
            if (player.ticksExisted % 2 == 0) {
                player.motionY = -0.155
            } else {
                player.motionY = -0.1
            }
        }
    }
}
