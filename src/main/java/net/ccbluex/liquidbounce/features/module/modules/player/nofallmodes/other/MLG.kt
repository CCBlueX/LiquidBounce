/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.other

import net.ccbluex.liquidbounce.event.EventState.PRE
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall.minFallDistance
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall.retrieveDelay
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.RotationUtils.faceBlock
import net.ccbluex.liquidbounce.utils.VecRotation
import net.ccbluex.liquidbounce.utils.extensions.eyes
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverSlot
import net.ccbluex.liquidbounce.utils.misc.FallingPlayer
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.timing.TickTimer
import net.minecraft.init.Blocks.web
import net.minecraft.init.Items.water_bucket
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemBucket
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import kotlin.math.ceil

object MLG : NoFallMode("MLG") {

    private val mlgTimer = TickTimer()
    private val retrieveTimer = MSTimer()
    private var currentMlgRotation: VecRotation? = null
    private var currentMlgBlock: BlockPos? = null
    private var mlgInProgress = false
    private var bucketUsed = false

    override fun onMotion(event: MotionEvent) {
        val thePlayer = mc.thePlayer

        if (event.eventState == PRE) {
            currentMlgRotation = null

            mlgTimer.update()

            if (!mlgTimer.hasTimePassed(10)) return

            if (thePlayer.fallDistance > minFallDistance) {
                val fallingPlayer = FallingPlayer(thePlayer)

                val maxDist = mc.playerController.blockReachDistance + 1.5

                val collision = fallingPlayer.findCollision(ceil(1.0 / thePlayer.motionY * -maxDist).toInt()) ?: return

                if ((thePlayer.motionY < collision.pos.y + 1 - thePlayer.posY) || thePlayer.eyes.distanceTo(
                        Vec3(
                            collision.pos
                        ).addVector(0.5, 0.5, 0.5)
                    ) < mc.playerController.blockReachDistance + 0.866025
                ) {
                    var index: Int? = null

                    for (i in 36..44) {
                        val itemStack = thePlayer.inventoryContainer.getSlot(i).stack ?: continue

                        if (itemStack.item == water_bucket || itemStack.item is ItemBlock && (itemStack.item as ItemBlock).block == web) {
                            index = i - 36

                            if (thePlayer.inventory.currentItem == index) break
                        }
                    }

                    index ?: return

                    currentMlgBlock = collision.pos

                    serverSlot = index

                    currentMlgRotation = faceBlock(collision.pos)
                    currentMlgRotation?.rotation?.toPlayer(thePlayer)
                    mlgInProgress = true
                    bucketUsed = false
                }
            }
        } else if (currentMlgRotation != null && mlgInProgress && !bucketUsed) {
            val stack = thePlayer?.inventory?.getStackInSlot(serverSlot)

            // If used item was a water bucket, try to pick it back up later
            if (mc.playerController.sendUseItem(thePlayer, mc.theWorld, stack) && stack?.item is ItemBucket) {
                mlgInProgress = false
                bucketUsed = true
                mlgTimer.reset()
                retrieveTimer.reset()
            }
        }

        if (retrieveTimer.hasTimePassed(retrieveDelay) && !mlgInProgress && bucketUsed) {
            // Auto-retrieve water bucket.
            val stack = thePlayer?.inventory?.getStackInSlot(serverSlot)

            if (stack?.item is ItemBucket && mc.playerController.sendUseItem(thePlayer, mc.theWorld, stack)) {
                bucketUsed = false
                retrieveTimer.reset()
            }

            serverSlot = thePlayer.inventory.currentItem
        }
    }
}
