/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.aac

import net.ccbluex.liquidbounce.event.EventManager.callEvent
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
        val player = mc.thePlayer ?: return

        if (!isMoving || player.isInWater || player.isInLava ||
                player.isOnLadder || player.isRiding || player.hurtTime > 0) return
        if (player.onGround && player.isCollidedVertically) {
            // MotionXYZ
            val yawRad = player.rotationYaw.toRadians()
            player.motionX -= sin(yawRad) * 0.202f
            player.motionZ += cos(yawRad) * 0.202f
            player.motionY = 0.405
            callEvent(JumpEvent(0.405f))
            strafe()
        } else if (player.fallDistance < 0.31f) {
            if (getBlock(player.position) is BlockCarpet) // why?
                return

            // Motion XZ
            player.jumpMovementFactor = if (player.moveStrafing == 0f) 0.027f else 0.021f
            player.motionX *= 1.001
            player.motionZ *= 1.001

            // Motion Y
            if (!thePlayer.isCollidedHorizontally) player.motionY -= 0.014999993f
        } else player.jumpMovementFactor = 0.02f
    }

    override fun onDisable() {
        mc.thePlayer.jumpMovementFactor = 0.02f
    }
}