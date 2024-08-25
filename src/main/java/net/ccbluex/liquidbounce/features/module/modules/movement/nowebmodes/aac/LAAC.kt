/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.NoWebMode
import net.ccbluex.liquidbounce.utils.extensions.tryJump

object LAAC : NoWebMode("LAAC") {
    override fun onUpdate() {
        if (!mc.player.isInWeb()) {
            return
        }

        mc.player.jumpMovementFactor = if (mc.player.movementInput.moveStrafe != 0f) 1f else 1.21f

        if (!mc.options.sneakKey.isPressed)
            mc.player.velocityY = 0.0

        if (mc.player.onGround)
            mc.player.tryJump()
    }
}
