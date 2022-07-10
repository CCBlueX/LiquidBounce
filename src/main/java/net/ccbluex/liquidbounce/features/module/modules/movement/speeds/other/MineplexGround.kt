/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.CPSCounter
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.plus
import net.ccbluex.liquidbounce.utils.extensions.strafe
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3

class MineplexGround : SpeedMode("Mineplex-Ground")
{
    private var spoofSlot = false
    private var moveSpeed = 0f

    override fun onMotion(eventState: EventState)
    {
        if (eventState != EventState.PRE) return

        val thePlayer = mc.thePlayer ?: return

        val inventory = thePlayer.inventory
        if (!thePlayer.isMoving || !thePlayer.onGround || inventory.getCurrentItem() == null || thePlayer.isUsingItem) return

        spoofSlot = false

        (0..8).firstOrNull { inventory.getStackInSlot(it) == null }?.let {
            mc.netHandler.addToSendQueue(C09PacketHeldItemChange(it))
            spoofSlot = true
        }
    }

    override fun onUpdate()
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        if (!thePlayer.isMoving || !thePlayer.onGround || thePlayer.isUsingItem)
        {
            moveSpeed = 0f
            return
        }

        if (!spoofSlot && thePlayer.inventory.getCurrentItem() != null)
        {
            ClientUtils.displayChatMessage(thePlayer, "\u00A78[\u00A7c\u00A7lMineplex\u00A7aSpeed\u00A78] \u00A7cYou need one empty slot.")
            return
        }

        val blockPos = BlockPos(thePlayer.posX, thePlayer.entityBoundingBox.minY - 1, thePlayer.posZ)

        val vec = Vec3(blockPos) + Vec3(EnumFacing.UP.directionVec) + 0.4

        CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)

        mc.playerController.onPlayerRightClick(thePlayer, theWorld, null, blockPos, EnumFacing.UP, Vec3(vec.xCoord * 0.4f, vec.yCoord * 0.4f, vec.zCoord * 0.4f))

        val targetSpeed = Speed.mineplexGroundSpeedValue.get()

        if (targetSpeed > moveSpeed) moveSpeed += targetSpeed * 0.125f
        if (moveSpeed >= targetSpeed) moveSpeed = targetSpeed

        thePlayer.strafe(moveSpeed)

        if (!spoofSlot) mc.netHandler.addToSendQueue(C09PacketHeldItemChange(thePlayer.inventory.currentItem))
    }

    override fun onMove(event: MoveEvent)
    {
    }

    override fun onDisable()
    {
        moveSpeed = 0f
        mc.netHandler.addToSendQueue(C09PacketHeldItemChange((mc.thePlayer ?: return).inventory.currentItem))
        spoofSlot = false
    }
}
