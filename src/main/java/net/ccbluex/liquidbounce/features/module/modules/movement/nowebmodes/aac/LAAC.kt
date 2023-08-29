/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.NoWebMode

object LAAC : NoWebMode("LAAC") {
    override fun onUpdate() {
        if (!mc.thePlayer.isInWeb) {
            return
        }

        thePlayer.jumpMovementFactor = if (thePlayer.movementInput.moveStrafe != 0f) 1f else 1.21f

        if (!mc.gameSettings.keyBindSneak.isKeyDown)
            thePlayer.motionY = 0.0

        if (thePlayer.onGround)
            thePlayer.jump()    
    }
}
