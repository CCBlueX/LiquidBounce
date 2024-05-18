/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.ncp

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.LongJump.canBoost
import net.ccbluex.liquidbounce.features.module.modules.movement.LongJump.jumped
import net.ccbluex.liquidbounce.features.module.modules.movement.LongJump.ncpBoost
import net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.LongJumpMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.speed

object NCP : LongJumpMode("NCP") {
    override fun onUpdate() {
        speed *= if (canBoost) ncpBoost else 1f
        canBoost = false
    }

    override fun onMove(event: MoveEvent) {
        if (!isMoving && jumped) {
            mc.thePlayer.motionX = 0.0
            mc.thePlayer.motionZ = 0.0
            event.zeroXZ()
        }
    }
}