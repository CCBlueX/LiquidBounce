/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.intave

import net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.NoWebMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.extensions.tryJump

object NewMineBlaze : NoWebMode("NewMineBlaze") {
    override fun onUpdate() {
        player ?: return

        if (!player.isInWeb) {
            return
        }

        if (isMoving && player.moveStrafing == 0.0f) {
            if (player.onGround) {
                if (player.ticksExisted % 3 == 0) {
                    strafe(0.734f)
                } else {
                    player.tryJump()
                    strafe(0.346f)
                }
            }
        }
    }
}