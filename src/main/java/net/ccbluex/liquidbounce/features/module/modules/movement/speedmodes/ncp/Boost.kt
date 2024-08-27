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
        val player = mc.player ?: return

        var speed = 3.1981
        var offset = 4.69
        var shouldOffset = true

        if (mc.world.doesBoxCollide(player, player.boundingBox.offset(player.velocityX / offset, 0.0, player.velocityZ / offset)).isNotEmpty()) {
            shouldOffset = false
        }

        if (player.onGround && ground < 1f)
            ground += 0.2f
        if (!player.onGround)
            ground = 0f

        if (ground == 1f && shouldSpeedUp()) {
            if (!player.isSprinting)
                offset += 0.8

            if (player.input.movementSideways != 0f) {
                speed -= 0.1
                offset += 0.5
            }
            if (player.isTouchingWater)
                speed -= 0.1


            motionDelay += 1
            when (motionDelay) {
                1 -> {
                    player.velocityX *= speed
                    player.velocityZ *= speed
                }
                2 -> {
                    player.velocityX /= 1.458
                    player.velocityZ /= 1.458
                }
                4 -> {
                    if (shouldOffset) player.setPosition(player.x + player.velocityX / offset, player.z, player.z + player.velocityZ / offset)
                    motionDelay = 0
                }
            }
        }
    }


    private fun shouldSpeedUp() =
        !mc.player.isTouchingLava && !mc.player.isClimbing && !mc.player.isSneaking && isMoving
}