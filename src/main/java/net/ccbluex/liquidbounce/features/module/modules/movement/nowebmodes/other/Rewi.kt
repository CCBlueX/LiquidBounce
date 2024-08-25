/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.NoWebMode
import net.ccbluex.liquidbounce.utils.extensions.tryJump

object Rewi : NoWebMode("Rewi") {
    override fun onUpdate() {
        if (!mc.player.isInWeb) {
            return
        }
        mc.player.jumpMovementFactor = 0.42f

        if (mc.player.onGround)
            mc.player.tryJump()
    }
}
