package net.ccbluex.liquidbounce.features.module.modules.movement.flies.other

import net.ccbluex.liquidbounce.api.minecraft.util.BlockPos
import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import kotlin.random.Random

class BlockWalkFly : FlyMode("BlockWalk")
{
    override fun onUpdate()
    {
        val thePlayer = mc.thePlayer ?: return

        if (Random.nextBoolean()) mc.netHandler.networkManager.sendPacketWithoutEvent(CPacketPlayerBlockPlacement(BlockPos(0, -1, 0), 0, thePlayer.inventory.getCurrentItemInHand(), 0F, 0F, 0F))
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
}
