/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.minecraft.block.BlockCarpet
import kotlin.math.cos
import kotlin.math.sin

class AACHop3313 : SpeedMode("AACHop3.3.13") {
    override fun onMotion() {}
    override fun onUpdate() {
        val thePlayer = mc.thePlayer ?: return

        if (!MovementUtils.isMoving || thePlayer.isInWater || thePlayer.isInLava ||
                thePlayer.isOnLadder || thePlayer.isRiding || thePlayer.hurtTime > 0) return
        if (thePlayer.onGround && thePlayer.isCollidedVertically) {
            // MotionXYZ
            val yawRad = thePlayer.rotationYaw * 0.017453292f
            thePlayer.motionX -= sin(yawRad) * 0.202f
            thePlayer.motionZ += cos(yawRad) * 0.202f
            thePlayer.motionY = 0.405
            LiquidBounce.eventManager.callEvent(JumpEvent(0.405f))
            MovementUtils.strafe()
        } else if (thePlayer.fallDistance < 0.31f) {
            if (getBlock(thePlayer.position) is BlockCarpet) // why?
                return

            // Motion XZ
            thePlayer.jumpMovementFactor = if (thePlayer.moveStrafing == 0f) 0.027f else 0.021f
            thePlayer.motionX *= 1.001
            thePlayer.motionZ *= 1.001

            // Motion Y
            if (!thePlayer.isCollidedHorizontally) thePlayer.motionY -= 0.014999993f
        } else thePlayer.jumpMovementFactor = 0.02f
    }

    override fun onMove(event: MoveEvent) {}
    override fun onDisable() {
        mc.thePlayer!!.jumpMovementFactor = 0.02f
    }
}