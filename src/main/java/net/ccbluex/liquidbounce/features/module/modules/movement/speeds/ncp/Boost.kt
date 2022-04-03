/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.ncp

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class Boost : SpeedMode("Boost") {
    private var motionDelay = 0
    private var ground = 0f
    override fun onMotion() {
        val thePlayer = mc.thePlayer ?: return

        var speed = 3.1981
        var offset = 4.69
        var shouldOffset = true

        if (mc.theWorld!!.getCollidingBoundingBoxes(thePlayer, thePlayer.entityBoundingBox.offset(thePlayer.motionX / offset, 0.0, thePlayer.motionZ / offset)).isNotEmpty()) {
            shouldOffset = false
        }

        if (thePlayer.onGround && ground < 1.0f)
            ground += 0.2f
        if (!thePlayer.onGround)
            ground = 0.0f

        if (ground == 1.0f && shouldSpeedUp()) {
            if (!thePlayer.isSprinting)
                offset += 0.8

            if (thePlayer.moveStrafing != 0f) {
                speed -= 0.1
                offset += 0.5
            }
            if (thePlayer.isInWater)
                speed -= 0.1


            motionDelay += 1
            when (motionDelay) {
                1 -> {
                    thePlayer.motionX *= speed
                    thePlayer.motionZ *= speed
                }
                2 -> {
                    thePlayer.motionX /= 1.458
                    thePlayer.motionZ /= 1.458
                }
                4 -> {
                    if (shouldOffset) thePlayer.setPosition(thePlayer.posX + thePlayer.motionX / offset, thePlayer.posY, thePlayer.posZ + thePlayer.motionZ / offset)
                    motionDelay = 0
                }
            }
        }
    }

    override fun onUpdate() {}
    override fun onMove(event: MoveEvent) {}

    private fun shouldSpeedUp(): Boolean {
        return !mc.thePlayer!!.isInLava && !mc.thePlayer!!.isOnLadder && !mc.thePlayer!!.isSneaking && MovementUtils.isMoving
    }
}