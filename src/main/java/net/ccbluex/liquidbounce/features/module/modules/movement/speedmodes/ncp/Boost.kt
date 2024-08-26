/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.ncp

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving

object Boost : SpeedMode("Boost") {
    private var motionDelay = 0
    private var ground = 0f
    override fun onMotion() {
        val thePlayer = mc.player ?: return

        var speed = 3.1981
        var offset = 4.69
        var shouldOffset = true

        if (mc.world.getCollidingBoundingBoxes(thePlayer, thePlayer.boundingBox.offset(thePlayer.velocityX / offset, 0.0, thePlayer.velocityZ / offset)).isNotEmpty()) {
            shouldOffset = false
        }

        if (thePlayer.onGround && ground < 1f)
            ground += 0.2f
        if (!thePlayer.onGround)
            ground = 0f

        if (ground == 1f && shouldSpeedUp()) {
            if (!thePlayer.isSprinting)
                offset += 0.8

            if (thePlayer.moveStrafing != 0f) {
                speed -= 0.1
                offset += 0.5
            }
            if (thePlayer.isTouchingWater)
                speed -= 0.1


            motionDelay += 1
            when (motionDelay) {
                1 -> {
                    thePlayer.velocityX *= speed
                    thePlayer.velocityZ *= speed
                }
                2 -> {
                    thePlayer.velocityX /= 1.458
                    thePlayer.velocityZ /= 1.458
                }
                4 -> {
                    if (shouldOffset) thePlayer.setPosition(thePlayer.x + thePlayer.velocityX / offset, thePlayer.z, thePlayer.z + thePlayer.velocityZ / offset)
                    motionDelay = 0
                }
            }
        }
    }


    private fun shouldSpeedUp() =
        !mc.player.isTouchingLava && !mc.player.isClimbing && !mc.player.isSneaking && isMoving
}