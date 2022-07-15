package net.ccbluex.liquidbounce.features.module.modules.player.nofalls

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.VecRotation
import net.ccbluex.liquidbounce.utils.extensions.plus
import net.ccbluex.liquidbounce.utils.misc.FallingPlayer
import net.ccbluex.liquidbounce.utils.timer.TickTimer
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemBucket
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import kotlin.math.ceil
import kotlin.math.pow

class MLG : NoFallMode("MLG")
{
    private val mlgTimer = TickTimer()

    private var currentMlgRotation: VecRotation? = null
    private var currentMlgItemIndex = 0
    private var currentMlgBlock: BlockPos? = null

    override fun onMotion(eventState: EventState)
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return
        if (eventState == EventState.PRE) motionPre(thePlayer, theWorld) else if (currentMlgRotation != null) motionPost(theWorld, thePlayer)
    }

    private fun motionPre(thePlayer: EntityPlayerSP, theWorld: WorldClient)
    {
        currentMlgRotation = null

        mlgTimer.update()

        if (!mlgTimer.hasTimePassed(10)) return

        if (thePlayer.fallDistance > NoFall.mlgMinFallDistance.get())
        {
            val fallingPlayer = FallingPlayer(theWorld, thePlayer, thePlayer.posX, thePlayer.posY, thePlayer.posZ, thePlayer.motionX, thePlayer.motionY, thePlayer.motionZ, thePlayer.rotationYaw, thePlayer.moveStrafing, thePlayer.moveForward)
            val maxDist: Double = mc.playerController.blockReachDistance + 1.5
            val collision = fallingPlayer.findCollision(ceil(1.0 / thePlayer.motionY * -maxDist).toInt()) ?: return
            if (checkReach(thePlayer, collision)) return

            val inventory = thePlayer.inventory

            val mlgItemSlot = findItemSlot(thePlayer, inventory)
            if (!InventoryUtils.tryHoldSlot(thePlayer, mlgItemSlot, lock = true)) return
            currentMlgItemIndex = mlgItemSlot
            currentMlgBlock = collision.pos

            val currentMlgRotation = RotationUtils.faceBlock(theWorld, thePlayer, collision.pos)
            this.currentMlgRotation = currentMlgRotation
            if (currentMlgRotation != null) if (NoFall.mlgSilentRotationValue.get()) RotationUtils.setTargetRotation(currentMlgRotation.rotation, if (NoFall.mlgKeepRotationEnabledValue.get()) NoFall.mlgKeepRotationTicksValue.get() else 0) else currentMlgRotation.rotation.applyRotationToPlayer(thePlayer)
        }
    }

    private fun motionPost(theWorld: WorldClient, thePlayer: EntityPlayerSP)
    {
        val controller = mc.playerController
        val stack = thePlayer.inventory.getStackInSlot(currentMlgItemIndex)
        if (stack != null) if (stack.item is ItemBucket) controller.sendUseItem(thePlayer, theWorld, stack) else if (controller.sendUseItem(thePlayer, theWorld, stack)) mlgTimer.reset()

        InventoryUtils.resetSlot(thePlayer)
    }

    private fun checkReach(thePlayer: EntityPlayerSP, collision: FallingPlayer.CollisionResult): Boolean
    {
        if (thePlayer.motionY < collision.pos.y + 1 - thePlayer.posY) return false
        return Vec3(thePlayer.posX, thePlayer.posY + thePlayer.eyeHeight, thePlayer.posZ).squareDistanceTo(Vec3(collision.pos) + 0.5) > (mc.playerController.blockReachDistance + 0.75).pow(2)
    }

    private fun findItemSlot(thePlayer: EntityPlayerSP, inventory: InventoryPlayer): Int
    {
        var slot = -1
        (0..8).mapNotNull { it to (inventory.getStackInSlot(it) ?: return@mapNotNull null) }.filter { (_, stack) -> stack.item == Items.water_bucket || stack.item is ItemBlock && (stack.item as ItemBlock).block == Blocks.web }.forEach {
            slot = it.first
            if (thePlayer.inventory.currentItem == slot) return@findItemSlot slot
        }
        return slot
    }
}
