package net.ccbluex.liquidbounce.features.module.modules.movement.flies.other

import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.StepEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode

class MCCentralFly : FlyMode("MCCentral")
{
    override fun onEnable()
    {
        mc.timer.timerSpeed = Fly.mccTimerSpeedValue.get()
    }

    override fun onUpdate()
    {
    }

    override fun onPacket(event: PacketEvent)
    {
        val packet = event.packet

        if (packet is CPacketPlayer) packet.asCPacketPlayer().onGround = true
    }

    override fun onBlockBB(event: BlockBBEvent)
    {
        val thePlayer = mc.thePlayer ?: return

        if (event.block is BlockAir && event.y < thePlayer.posY) event.boundingBox = AxisAlignedBB(event.x.toDouble(), event.y.toDouble(), event.z.toDouble(), event.x + 1.0, thePlayer.posY, event.z + 1.0)
    }

    override fun onJump(event: JumpEvent)
    {
        event.cancelEvent()
    }

    override fun onStep(event: StepEvent)
    {
        event.stepHeight = 0f
    }
}
