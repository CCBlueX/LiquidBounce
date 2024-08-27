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
        val player = mc.player ?: return

        if (!isMoving || player.isTouchingWater || player.isTouchingLava ||
                player.isClimbing || player.isRiding || player.hurtTime > 0) return
        if (player.onGround && player.horizontalCollision) {
            // MotionXYZ
            val yawRad = player.yaw.toRadians()
            player.velocityX -= sin(yawRad) * 0.202f
            player.velocityZ += cos(yawRad) * 0.202f
            player.velocityY = 0.405
            callEvent(JumpEvent(0.405f, EventState.PRE))
            strafe()
        } else if (player.fallDistance < 0.31f) {
            if (getBlock(player.position) is BlockCarpet) // why?
                return

            // Motion XZ
            player.flyingSpeed = if (player.input.movementSideways == 0f) 0.027f else 0.021f
            player.velocityX *= 1.001
            player.velocityZ *= 1.001

            // Motion Y
            if (!player.isCollidedHorizontally) player.velocityY -= 0.014999993f
        } else player.flyingSpeed = 0.02f
    }

    override fun onDisable() {
        mc.player.flyingSpeed = 0.02f
    }
}