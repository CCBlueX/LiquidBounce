/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.other

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall.autoMLG
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall.bucketUsed
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall.currentMlgBlock
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall.mlgInProgress
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall.mlgRotation
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall.retrieveDelay
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall.rotations
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall.shouldUse
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall.swing
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.RotationUtils.getVectorForRotation
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverSlot
import net.ccbluex.liquidbounce.utils.misc.FallingPlayer
import net.ccbluex.liquidbounce.utils.timing.*
import net.minecraft.block.Blocks
import net.minecraft.block.CobwebBlock
import net.minecraft.item.Items
import net.minecraft.item.BlockItem
import net.minecraft.item.BucketItem
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import kotlin.math.ceil

object MLG : NoFallMode("MLG") {

    override fun onMotion(event: MotionEvent) {
        val player = mc.player ?: return
        val mlgSlot = findMlgSlot() ?: return

        if (event.eventState != EventState.POST) return

        val fallingPlayer = FallingPlayer(player)
        val maxDist = mc.interactionManager.reachDistance + 1.5
        val collision = fallingPlayer.findCollision(ceil(1.0 / player.velocityY * -maxDist).toInt()) ?: return

        if (player.velocityY < collision.pos.y + 1 - player.y || player.eyes.distanceTo(Vec3d(collision.pos).add(0.5, 0.5, 0.5)) < mc.interactionManager.reachDistance + 0.866025) {
            if (player.fallDistance < NoFall.minFallDistance) return
            currentMlgBlock = collision.pos

            when (autoMLG.lowercase()) {
                "pick" -> {
                    player.inventory.selectedSlot = mlgSlot - 36
                   mc.interactionManager.syncSelectedSlot()
                }
                "spoof", "switch" -> serverSlot = mlgSlot - 36
            }

            mlgRotation = currentMlgBlock?.toVec()?.let { RotationUtils.toRotation(it, false, player) }

            if (rotations) {
                mlgRotation?.let {
                    RotationUtils.setTargetRotation(
                        mlgRotation!!,
                        if (NoFall.keepRotation) NoFall.keepTicks else 1,
                        turnSpeed = NoFall.minHorizontalSpeed.get()..NoFall.maxHorizontalSpeed.get() to NoFall.minVerticalSpeed.get()..NoFall.maxVerticalSpeed.get(),
                        angleThresholdForReset = NoFall.angleThresholdUntilReset,
                        smootherMode = NoFall.smootherMode,
                        startOffSlow = NoFall.startRotatingSlow,
                        slowDownOnDirChange = NoFall.slowDownOnDirectionChange,
                        useStraightLinePath = NoFall.useStraightLinePath,
                        minRotationDifference = NoFall.minRotationDifference
                    )
                }
            }

            shouldUse = true
        }
    }

    override fun onTick() {
        val player = mc.player ?: return
        val mlgSlot = findMlgSlot()
        val stack = mlgSlot?.let { player.inventory.getInvStack(mlgSlot) } ?: return

        if (shouldUse && !bucketUsed) {
            TickedActions.TickScheduler(NoFall) += {
                when (stack.item) {
                    Items.WATER_BUCKET -> {
                        player.sendUseItem(stack)
                    }
                    is BlockItem -> {
                        val blocks = (stack.item as BlockItem).block
                            if (blocks is CobwebBlock) {
                            val raytrace = performBlockRaytrace(mlgRotation?.fixedSensitivity()!!, mc.interactionManager.reachDistance)

                            if (raytrace != null) {
                                currentMlgBlock?.let { placeBlock(it, raytrace.direction, raytrace.pos, stack) }
                            }
                        }
                    }
                }
            }

            mlgInProgress = true
            bucketUsed = true
        }

        if (shouldUse) {
            WaitTickUtils.scheduleTicks(retrieveDelay) {
                if (!shouldUse) return@scheduleTicks // Without this, it'll retrieve twice idk.

                if (stack.item is BucketItem) {
                    player.sendUseItem(stack)
                }

                shouldUse = false
            }
        }

        if (mlgInProgress && !shouldUse) {
            WaitTickUtils.scheduleTicks(retrieveDelay + 2) {
                serverSlot = player.inventory.selectedSlot

                mlgInProgress = false
                bucketUsed = false
            }
        }
    }

    private fun placeBlock(blockPos: BlockPos, side: Direction, pos: Vec3d, stack: ItemStack) {
        val player = mc.player ?: return

        tryToPlaceBlock(stack, blockPos, side, pos)

        // Since we violate vanilla slot switch logic if we send the packets now, we arrange them for the next tick
        if (autoMLG == "Switch")
            serverSlot = player.inventory.selectedSlot

        switchBlockNextTickIfPossible(stack)
    }

    private fun tryToPlaceBlock(
        stack: ItemStack,
        clickPos: BlockPos,
        side: Direction,
        pos: Vec3d,
    ): Boolean {
        val player = mc.player ?: return false

        val prevSize = stack.count

        val clickedSuccessfully = player.onPlayerRightClick(clickPos, side, pos, stack)

        if (clickedSuccessfully) {
            if (swing) player.swingHand() else sendPacket(HandSwingC2SPacket())

            if (stack.count <= 0) {
                player.inventory.main[serverSlot] = null
                ForgeEventFactory.onPlayerDestroyItem(player, stack)
            } else if (stack.count != prevSize || mc.interactionManager.currentGameMode.isCreative)
                mc.entityRenderDispatcher.itemRenderer.resetEquippedProgress()

            currentMlgBlock = null
            mlgRotation = null
        } else {
            if (player.sendUseItem(stack))
                mc.entityRenderDispatcher.itemRenderer.resetEquippedProgress2()
        }

        return clickedSuccessfully
    }

    private fun switchBlockNextTickIfPossible(stack: ItemStack) {
        val player = mc.player ?: return
        if (autoMLG in arrayOf("Off","Switch")) return
        if (stack.count > 0) return

        val switchSlot = findMlgSlot() ?: return

        TickedActions.TickScheduler(NoFall) += {
            if (autoMLG == "Pick") {
                player.inventory.selectedSlot = switchSlot - 36
               mc.interactionManager.syncSelectedSlot()
            } else {
                serverSlot = switchSlot - 36
            }
        }
    }

    private fun performBlockRaytrace(rotation: Rotation, maxReach: Float): BlockHitResult? {
        val player = mc.player ?: return null
        val world = mc.world ?: return null

        val eyes = player.eyes
        val rotationVec = getVectorForRotation(rotation)

        val reach = eyes + (rotationVec * maxReach.toDouble())

        return world.rayTrace(eyes, reach, false, true, false)
    }

    private fun findMlgSlot(): Int? {
        val player = mc.player ?: return null

        for (i in 36..44) {
            val itemStack = player.inventory.getInvStack(i) ?: continue

            if (itemStack.item == Items.WATER_BUCKET ||
                (itemStack.item is BlockItem && (itemStack.item as BlockItem).block == Blocks.COBWEB)) {
                return i
            }
        }

        return null
    }
}