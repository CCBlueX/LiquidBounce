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
        val player = mc.thePlayer ?: return

        var speed = 3.1981
        var offset = 4.69
        var shouldOffset = true

        if (mc.theWorld.getCollidingBoundingBoxes(player, player.entityBoundingBox.offset(player.motionX / offset, 0.0, player.motionZ / offset)).isNotEmpty()) {
            shouldOffset = false
        }

        if (player.onGround && ground < 1f)
            ground += 0.2f
        if (!player.onGround)
            ground = 0f

        if (ground == 1f && shouldSpeedUp()) {
            if (!player.isSprinting)
                offset += 0.8

            if (player.moveStrafing != 0f) {
                speed -= 0.1
                offset += 0.5
            }
            if (player.isInWater)
                speed -= 0.1


            motionDelay += 1
            when (motionDelay) {
                1 -> {
                    player.motionX *= speed
                    player.motionZ *= speed
                }
                2 -> {
                    player.motionX /= 1.458
                    player.motionZ /= 1.458
                }
                4 -> {
                    if (shouldOffset) player.setPosition(player.posX + player.motionX / offset, player.posY, player.posZ + player.motionZ / offset)
                    motionDelay = 0
                }
            }
        }
    }


    private fun shouldSpeedUp() =
        !mc.thePlayer.isInLava && !mc.thePlayer.isOnLadder && !mc.thePlayer.isSneaking && isMoving
}