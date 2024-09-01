/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.intave

import net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.NoWebMode

object OldMineBlaze : NoWebMode("OldMineBlaze") {
    override fun onUpdate() {
        val player = mc.player ?: return

        if (!player.isInWeb()) {
            return
        }

        if (player.input.movementSideways == 0.0F && mc.options.forwardKey.isPressed && player.horizontalCollision) {
            player.flyingSpeed = 0.74F
        } else {
            player.flyingSpeed = 0.2F
            player.onGround = true
        }  
    }
}
