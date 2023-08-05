package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.other

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall.minFallDistance
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.RotationUtils.faceBlock
import net.ccbluex.liquidbounce.utils.VecRotation
import net.ccbluex.liquidbounce.utils.extensions.eyes
import net.ccbluex.liquidbounce.utils.misc.FallingPlayer
import net.ccbluex.liquidbounce.utils.timer.TickTimer
import net.minecraft.init.Blocks.web
import net.minecraft.init.Items.water_bucket
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemBucket
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import kotlin.math.ceil
import kotlin.math.sqrt

object MLG : NoFallMode("MLG") {
    @EventTarget
    private fun onMotionUpdate(event: MotionEvent) {
        val mlgTimer = TickTimer()
        val thePlayer = mc.thePlayer
        var currentMlgRotation: VecRotation? = null
        var currentMlgItemIndex = 0
        var currentMlgBlock: BlockPos? = null

        if (event.eventState == EventState.PRE) {
            currentMlgRotation = null

            mlgTimer.update()

            if (mlgTimer.hasTimePassed(10)) return

            if (thePlayer.fallDistance > minFallDistance) {
                val fallingPlayer = FallingPlayer(thePlayer)

                val maxDist = mc.playerController.blockReachDistance + 1.5

                val collision =
                    fallingPlayer.findCollision(ceil(1.0 / thePlayer.motionY * -maxDist).toInt()) ?: return

                var ok = thePlayer.eyes
                    .distanceTo(
                        Vec3(collision.pos).addVector(0.5, 0.5, 0.5)
                    ) < mc.playerController.blockReachDistance + sqrt(0.75)

                if (thePlayer.motionY < collision.pos.y + 1 - thePlayer.posY) {
                    ok = true
                }

                if (!ok) return

                var index = -1

                for (i in 36..44) {
                    val itemStack = thePlayer.inventoryContainer.getSlot(i).stack

                    if (itemStack != null && (itemStack.item == water_bucket || itemStack.item is ItemBlock && (itemStack.item as ItemBlock).block == web)
                    ) {
                        index = i - 36

                        if (thePlayer.inventory.currentItem == index) break
                    }
                }
                if (index == -1) return

                currentMlgItemIndex = index
                currentMlgBlock = collision.pos

                if (thePlayer.inventory.currentItem != index) {
                    sendPacket(C09PacketHeldItemChange(index))
                }

                currentMlgRotation = faceBlock(collision.pos)
                currentMlgRotation?.rotation?.toPlayer(thePlayer)
            }
        } else if (currentMlgRotation != null) {
            val stack = thePlayer.inventory.getStackInSlot(currentMlgItemIndex)

            if (stack.item is ItemBucket) {
                mc.playerController.sendUseItem(thePlayer, mc.theWorld, stack)
            } else {
                if (mc.playerController.sendUseItem(thePlayer, mc.theWorld, stack)) {
                    mlgTimer.reset()
                }
            }
            if (thePlayer.inventory.currentItem != currentMlgItemIndex)
                sendPacket(
                    C09PacketHeldItemChange(thePlayer.inventory.currentItem)
                )
        }
    }
}