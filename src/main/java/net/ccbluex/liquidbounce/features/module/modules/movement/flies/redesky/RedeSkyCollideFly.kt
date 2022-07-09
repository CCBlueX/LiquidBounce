package net.ccbluex.liquidbounce.features.module.modules.movement.flies.redesky

import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.boost

class RedeSkyCollideFly : FlyMode("RedeSky-Collide")
{
    private val ticks = 0

    override fun onUpdate()
    {
    }

    override fun onMove(event: MoveEvent)
    {
        val thePlayer = mc.thePlayer ?: return

        mc.timer.timerSpeed = Fly.redeSkyCollideTimerValue.get()
        RotationUtils.reset()
        if (mc.gameSettings.keyBindForward.isKeyDown)
        {
            var speed = Fly.redeSkyCollideSpeedValue.get() / 100f + ticks * (Fly.redeSkyCollideBoostValue.get() / 100f)
            val maxSpeed = Fly.redeSkyCollideMaxSpeedValue.get() / 100f
            if (speed > maxSpeed) speed = maxSpeed

            thePlayer.boost(speed, thePlayer.rotationYaw)

            event.x = thePlayer.motionX
            event.z = thePlayer.motionZ
        }
    }

    override fun onBlockBB(event: BlockBBEvent)
    {
        val thePlayer = mc.thePlayer ?: return

        if (event.block is BlockAir && event.y < thePlayer.posY) event.boundingBox = AxisAlignedBB(event.x.toDouble(), event.y.toDouble(), event.z.toDouble(), event.x + 1.0, thePlayer.posY, event.z + 1.0)
    }

    override fun onDisable()
    {
        (mc.thePlayer ?: return).motionY = 0.0
    }
}
