/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.player.Blink
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.RotationUtils.faceBlock
import net.ccbluex.liquidbounce.utils.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getCenterDistance
import net.ccbluex.liquidbounce.utils.block.BlockUtils.searchBlocks
import net.ccbluex.liquidbounce.utils.extensions.eyes
import net.ccbluex.liquidbounce.utils.extensions.getVec
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BlockValue
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.block.Block
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing

object ChestAura : Module("ChestAura", ModuleCategory.WORLD) {

    private val range by FloatValue("Range", 5F, 1F..6F)
    private val delay by IntegerValue("Delay", 100, 50..200)
    private val throughWalls by BoolValue("ThroughWalls", true)
    private val visualSwing by BoolValue("VisualSwing", true)
    private val chest by BlockValue("Chest", Block.getIdFromBlock(Blocks.chest))
    private val rotations by BoolValue("Rotations", true)

    private var currentBlock: BlockPos? = null
    private val timer = MSTimer()

    val clickedBlocks = mutableListOf<BlockPos>()

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (Blink.state || KillAura.isBlockingChestAura)
            return

        val thePlayer = mc.thePlayer
        val theWorld = mc.theWorld

        when (event.eventState) {
            EventState.PRE -> {
                if (mc.currentScreen is GuiContainer)
                    timer.reset()

                val radius = range + 1

                val eyesPos = thePlayer.eyes

                currentBlock = searchBlocks(radius.toInt())
                        .filter {
                            Block.getIdFromBlock(it.value) == chest && it.key !in clickedBlocks
                                    && getCenterDistance(it.key) < range
                        }
                        .filter {
                            if (throughWalls)
                                return@filter true

                            val blockPos = it.key
                            val movingObjectPosition = theWorld.rayTraceBlocks(eyesPos, blockPos.getVec(), false, true, false)

                            movingObjectPosition != null && movingObjectPosition.blockPos == blockPos
                        }
                        .minByOrNull { getCenterDistance(it.key) }?.key

                if (rotations)
                    setTargetRotation((faceBlock(currentBlock ?: return)
                            ?: return).rotation)
            }

            EventState.POST -> if (currentBlock != null && timer.hasTimePassed(delay)) {
                if (mc.playerController.onPlayerRightClick(thePlayer, mc.theWorld, thePlayer.heldItem, currentBlock!!,
                                EnumFacing.DOWN, currentBlock!!.getVec())) {
                    if (visualSwing)
                        thePlayer.swingItem()
                    else
                        sendPacket(C0APacketAnimation())

                    clickedBlocks += currentBlock!!
                    currentBlock = null
                    timer.reset()
                }
            }
        }
    }

    override fun onDisable() = clickedBlocks.clear()
}