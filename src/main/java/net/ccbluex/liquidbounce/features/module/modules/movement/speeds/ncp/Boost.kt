/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.ncp

import net.ccbluex.liquidbounce.api.minecraft.client.entity.EntityPlayerSP
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.divide
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.multiply

class Boost : SpeedMode("Boost")
{
    private var motionDelay = 0
    private var groundTimes = 0
    override fun onMotion(eventState: EventState)
    {
        if (eventState != EventState.PRE) return

        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        var moveSpeed = 3.1981
        var offset = 4.69

        val shouldOffset = theWorld.getCollidingBoundingBoxes(thePlayer, thePlayer.entityBoundingBox.offset(thePlayer.motionX / offset, 0.0, thePlayer.motionZ / offset)).isEmpty()

        if (thePlayer.onGround)
        {
            if (groundTimes < 5) groundTimes++
        }
        else groundTimes = 0

        if (groundTimes == 5 && shouldSpeedUp(thePlayer))
        {
            if (!thePlayer.sprinting) offset += 0.8

            if (thePlayer.moveStrafing != 0f)
            {
                moveSpeed -= 0.1
                offset += 0.5
            }

            if (thePlayer.isInWater) moveSpeed -= 0.1

            when (motionDelay++)
            {
                1 -> thePlayer.multiply(moveSpeed)

                2 -> thePlayer.divide(1.458)

                4 ->
                {
                    if (shouldOffset) thePlayer.setPosition(thePlayer.posX + thePlayer.motionX / offset, thePlayer.posY, thePlayer.posZ + thePlayer.motionZ / offset)
                    motionDelay = 0
                }
            }
        }
    }

    override fun onUpdate()
    {
    }

    override fun onMove(event: MoveEvent)
    {
    }

    private fun shouldSpeedUp(thePlayer: EntityPlayerSP): Boolean = !thePlayer.isInLava && !thePlayer.isOnLadder && !thePlayer.sneaking && thePlayer.isMoving
}
