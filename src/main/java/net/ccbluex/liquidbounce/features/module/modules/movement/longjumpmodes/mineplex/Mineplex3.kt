/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.mineplex

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.LongJumpMode
import net.ccbluex.liquidbounce.utils.MovementUtils

object Mineplex3 : LongJumpMode("Mineplex3") {
    override fun onUpdate() {
        player.jumpMovementFactor = 0.09f
        player.motionY += 0.01320999999999999
        player.jumpMovementFactor = 0.08f
        MovementUtils.strafe()
    }

    override fun onMove(event: MoveEvent) {
        if (player.fallDistance != 0f)
            player.motionY += 0.037
    }
}