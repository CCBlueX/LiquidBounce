/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.NoWebMode

object AAC : NoWebMode("AAC") {
    override fun onUpdate() {
        if (!mc.player.isInWeb()) {
            return
        }

        mc.player.jumpMovementFactor = 0.59f

        if (!mc.options.sneakKey.isPressed)
            mc.player.velocityY = 0.0
    }
}
