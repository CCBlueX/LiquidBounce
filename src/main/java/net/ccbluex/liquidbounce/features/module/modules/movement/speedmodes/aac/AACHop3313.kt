/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.aac

import net.ccbluex.liquidbounce.event.EventManager.callEvent
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.extensions.toRadians
import net.minecraft.block.BlockCarpet
import kotlin.math.cos
import kotlin.math.sin

object AACHop3313 : SpeedMode("AACHop3.3.13") {
    override fun onUpdate() {
        val thePlayer = mc.player ?: return

        if (!isMoving || thePlayer.isTouchingWater || thePlayer.isTouchingLava ||
                thePlayer.isClimbing || thePlayer.isRiding || thePlayer.hurtTime > 0) return
        if (thePlayer.onGround && thePlayer.isCollidedVertically) {
            // MotionXYZ
            val yawRad = theplayer.yaw.toRadians()
            thePlayer.velocityX -= sin(yawRad) * 0.202f
            thePlayer.velocityZ += cos(yawRad) * 0.202f
            thePlayer.velocityY = 0.405
            callEvent(JumpEvent(0.405f, EventState.PRE))
            strafe()
        } else if (thePlayer.fallDistance < 0.31f) {
            if (getBlock(thePlayer.position) is BlockCarpet) // why?
                return

            // Motion XZ
            thePlayer.jumpMovementFactor = if (thePlayer.moveStrafing == 0f) 0.027f else 0.021f
            thePlayer.velocityX *= 1.001
            thePlayer.velocityZ *= 1.001

            // Motion Y
            if (!thePlayer.isCollidedHorizontally) thePlayer.velocityY -= 0.014999993f
        } else thePlayer.jumpMovementFactor = 0.02f
    }

    override fun onDisable() {
        mc.player.jumpMovementFactor = 0.02f
    }
}