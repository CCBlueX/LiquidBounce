package net.ccbluex.liquidbounce.features.module.modules.movement.flies.vanilla

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.extensions.cos
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.moveDirectionRadians
import net.ccbluex.liquidbounce.utils.extensions.sin
import net.ccbluex.liquidbounce.utils.extensions.zeroXYZ
import net.ccbluex.liquidbounce.utils.timer.MSTimer

class TeleportFly : FlyMode("Teleport")
{
    override val mark: Boolean
        get() = false

    private val teleportTimer = MSTimer()

    override fun onUpdate()
    {
        val thePlayer = mc.thePlayer ?: return
        val gameSettings = mc.gameSettings

        val posX = thePlayer.posX
        val posY = thePlayer.posY
        val posZ = thePlayer.posZ

        val jumpKeyDown = gameSettings.keyBindJump.isKeyDown
        val sneakKeyDown = gameSettings.keyBindSneak.isKeyDown

        thePlayer.isSprinting = true
        thePlayer.zeroXYZ()
        val isMoving = thePlayer.isMoving
        if ((isMoving || jumpKeyDown || sneakKeyDown) && teleportTimer.hasTimePassed(Fly.teleportDelayValue.get().toLong()))
        {
            val yaw = thePlayer.moveDirectionRadians
            val speed = Fly.teleportDistanceValue.get().toDouble()
            var x = 0.0
            var y = 0.0
            var z = 0.0

            if (isMoving && !thePlayer.isCollidedHorizontally)
            {
                x = -yaw.sin * speed
                z = yaw.cos * speed
            }

            if (!thePlayer.isCollidedVertically) if (jumpKeyDown && !sneakKeyDown) y = speed else if (!jumpKeyDown && sneakKeyDown) y = -speed

            thePlayer.setPosition(x.let { thePlayer.posX += it; posX }, y.let { thePlayer.posY += it; posY }, z.let { thePlayer.posZ += it; posZ })
            teleportTimer.reset()
        }
    }
}
