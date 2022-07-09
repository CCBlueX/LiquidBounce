package net.ccbluex.liquidbounce.features.module.modules.movement.flies.other

import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.StepEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.CPSCounter
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.extensions.strafe
import net.ccbluex.liquidbounce.utils.timer.MSTimer

class MineplexFly : FlyMode("Mineplex")
{
    private val clipTimer = MSTimer()

    private fun canFly(thePlayer: IEntityPlayer) = thePlayer.inventory.getCurrentItemInHand() == null

    override fun onUpdate()
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return
        val timer = mc.timer
        val gameSettings = mc.gameSettings

        if (canFly(thePlayer))
        {
            val x = thePlayer.posX
            val y = thePlayer.posY
            val z = thePlayer.posZ

            if (gameSettings.keyBindJump.isKeyDown && clipTimer.hasTimePassed(100))
            {
                thePlayer.setPosition(x, y + 0.6, z)
                clipTimer.reset()
            }

            if (thePlayer.sneaking && clipTimer.hasTimePassed(100))
            {
                thePlayer.setPosition(x, y - 0.6, z)
                clipTimer.reset()
            }

            val blockPos = WBlockPos(x, thePlayer.entityBoundingBox.minY - 1, z)
            val vec = WVec3(blockPos) + WVec3(classProvider.getEnumFacing(EnumFacingType.UP).directionVec) + 0.4

            CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)

            mc.playerController.onPlayerRightClick(thePlayer, theWorld, null, blockPos, classProvider.getEnumFacing(EnumFacingType.UP), WVec3(vec.xCoord * 0.4f, vec.yCoord * 0.4f, vec.zCoord * 0.4f))

            thePlayer.strafe(0.27f)

            timer.timerSpeed = 1 + Fly.mineplexSpeedValue.get()
        }
        else
        {
            timer.timerSpeed = 1.0f
            Fly.state = false
            ClientUtils.displayChatMessage(thePlayer, "\u00A78[\u00A7c\u00A7lMineplex-\u00A7a\u00A7lFly\u00A78] \u00A7aSelect an empty slot to fly.")
        }
    }

    override fun onPacket(event: PacketEvent)
    {
        val packet = event.packet

        if (canFly(mc.thePlayer ?: return) && classProvider.isCPacketPlayer(packet)) packet.asCPacketPlayer().onGround = true
    }

    override fun onBlockBB(event: BlockBBEvent)
    {
        val thePlayer = mc.thePlayer ?: return

        if (classProvider.isBlockAir(event.block) && canFly(thePlayer) && event.y < thePlayer.posY) event.boundingBox = classProvider.createAxisAlignedBB(event.x.toDouble(), event.y.toDouble(), event.z.toDouble(), event.x + 1.0, thePlayer.posY, event.z + 1.0)
    }

    override fun onJump(event: JumpEvent)
    {
        if (canFly(mc.thePlayer ?: return)) event.cancelEvent()
    }

    override fun onStep(event: StepEvent)
    {
        if (canFly(mc.thePlayer ?: return)) event.stepHeight = 0f
    }
}
