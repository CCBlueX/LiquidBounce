/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.player.AutoTool
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.RotationUtils.faceBlock
import net.ccbluex.liquidbounce.utils.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlockName
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getCenterDistance
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isFullBlock
import net.ccbluex.liquidbounce.utils.extensions.eyes
import net.ccbluex.liquidbounce.utils.extensions.getBlock
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBlockBox
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.*
import net.minecraft.block.Block
import net.minecraft.init.Blocks.air
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action.*
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
import java.awt.Color

object Fucker : Module("Fucker", ModuleCategory.WORLD) {

    /**
     * SETTINGS
     */

    private val block by BlockValue("Block", 26)
    private val throughWalls by ListValue("ThroughWalls", arrayOf("None", "Raycast", "Around"), "None")
    private val range by FloatValue("Range", 5F, 1F..7F)
    private val action by ListValue("Action", arrayOf("Destroy", "Use"), "Destroy")
    private val instant by BoolValue("Instant", false) { action == "Destroy" || surroundings }
    private val switch by IntegerValue("SwitchDelay", 250, 0..1000)
    private val swing by BoolValue("Swing", true)
    private val rotations by BoolValue("Rotations", true)
    private val surroundings by BoolValue("Surroundings", true)
    private val noHit by BoolValue("NoHit", false)


    /**
     * VALUES
     */

    private var pos: BlockPos? = null
    private var oldPos: BlockPos? = null
    private var blockHitDelay = 0
    private val switchTimer = MSTimer()
    var currentDamage = 0F

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.thePlayer ?: return

        if (noHit && KillAura.state && KillAura.target != null)
            return

        val targetId = block

        if (pos == null || Block.getIdFromBlock(getBlock(pos!!)) != targetId ||
                getCenterDistance(pos!!) > range)
            pos = find(targetId)

        // Reset current breaking when there is no target block
        if (pos == null) {
            currentDamage = 0F
            return
        }

        var currentPos = pos ?: return
        var rotation = faceBlock(currentPos) ?: return

        // Surroundings
        var areSurroundings = false

        if (surroundings) {
            val eyes = thePlayer.eyes
            val blockPos = mc.theWorld.rayTraceBlocks(eyes, rotation.vec, false,
                    false, true)?.blockPos

            if (blockPos != null && blockPos.getBlock() != air) {
                if (currentPos.x != blockPos.x || currentPos.y != blockPos.y || currentPos.z != blockPos.z)
                    areSurroundings = true

                pos = blockPos
                currentPos = pos ?: return
                rotation = faceBlock(currentPos) ?: return
            }
        }

        // Reset switch timer when position changed
        if (oldPos != null && oldPos != currentPos) {
            currentDamage = 0F
            switchTimer.reset()
        }

        oldPos = currentPos

        if (!switchTimer.hasTimePassed(switch))
            return

        // Block hit delay
        if (blockHitDelay > 0) {
            blockHitDelay--
            return
        }

        // Face block
        if (rotations)
            setTargetRotation(rotation.rotation)

        when {
            // Destroy block
            action == "Destroy" || areSurroundings -> {
                // Auto Tool
                if (AutoTool.state)
                    AutoTool.switchSlot(currentPos)

                // Break block
                if (instant) {
                    // CivBreak style block breaking
                    sendPacket(C07PacketPlayerDigging(START_DESTROY_BLOCK, currentPos, EnumFacing.DOWN))

                    if (swing)
                        thePlayer.swingItem()

                    sendPacket(C07PacketPlayerDigging(STOP_DESTROY_BLOCK, currentPos, EnumFacing.DOWN))
                    currentDamage = 0F
                    return
                }

                // Minecraft block breaking
                val block = currentPos.getBlock() ?: return

                if (currentDamage == 0F) {
                    sendPacket(C07PacketPlayerDigging(START_DESTROY_BLOCK, currentPos, EnumFacing.DOWN))

                    if (thePlayer.capabilities.isCreativeMode ||
                            block.getPlayerRelativeBlockHardness(thePlayer, mc.theWorld, pos) >= 1f) {
                        if (swing)
                            thePlayer.swingItem()
                        mc.playerController.onPlayerDestroyBlock(pos, EnumFacing.DOWN)

                        currentDamage = 0F
                        pos = null
                        return
                    }
                }

                if (swing)
                    thePlayer.swingItem()

                currentDamage += block.getPlayerRelativeBlockHardness(thePlayer, mc.theWorld, currentPos)
                mc.theWorld.sendBlockBreakProgress(thePlayer.entityId, currentPos, (currentDamage * 10F).toInt() - 1)

                if (currentDamage >= 1F) {
                    sendPacket(C07PacketPlayerDigging(STOP_DESTROY_BLOCK, currentPos, EnumFacing.DOWN))
                    mc.playerController.onPlayerDestroyBlock(currentPos, EnumFacing.DOWN)
                    blockHitDelay = 4
                    currentDamage = 0F
                    pos = null
                }
            }

            // Use block
            action == "Use" ->
                if (mc.playerController.onPlayerRightClick(thePlayer, mc.theWorld, thePlayer.heldItem, pos, EnumFacing.DOWN,
                            Vec3(currentPos.x.toDouble(), currentPos.y.toDouble(), currentPos.z.toDouble()))) {
                    if (swing)
                        thePlayer.swingItem()

                    blockHitDelay = 4
                    currentDamage = 0F
                    pos = null
                }
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        drawBlockBox(pos ?: return, Color.RED, true)
    }

    /**
     * Find new target block by [targetID]
     */
    /*private fun find(targetID: Int) =
        searchBlocks(rangeValue.get().toInt() + 1).filter {
                    Block.getIdFromBlock(it.value) == targetID && getCenterDistance(it.key) <= rangeValue.get()
                            && (isHitable(it.key) || surroundingsValue.get())
                }.minBy { getCenterDistance(it.key) }?.key*/

    //Removed triple iteration of blocks to improve speed
    /**
     * Find new target block by [targetID]
     */
    private fun find(targetID: Int): BlockPos? {
        val thePlayer = mc.thePlayer ?: return null

        val radius = range.toInt() + 1

        var nearestBlockDistance = Double.MAX_VALUE
        var nearestBlock: BlockPos? = null

        for (x in radius downTo -radius + 1) {
            for (y in radius downTo -radius + 1) {
                for (z in radius downTo -radius + 1) {
                    val blockPos = BlockPos(thePlayer.posX.toInt() + x, thePlayer.posY.toInt() + y,
                            thePlayer.posZ.toInt() + z)
                    val block = getBlock(blockPos) ?: continue

                    if (Block.getIdFromBlock(block) != targetID) continue

                    val distance = getCenterDistance(blockPos)
                    if (distance > range) continue
                    if (nearestBlockDistance < distance) continue
                    if (!isHitable(blockPos) && !surroundings) continue

                    nearestBlockDistance = distance
                    nearestBlock = blockPos
                }
            }
        }

        return nearestBlock
    }

    /**
     * Check if block is hitable (or allowed to hit through walls)
     */
    private fun isHitable(blockPos: BlockPos): Boolean {
        val thePlayer = mc.thePlayer ?: return false

        return when (throughWalls.lowercase()) {
            "raycast" -> {
                val eyesPos = thePlayer.eyes
                val movingObjectPosition = mc.theWorld.rayTraceBlocks(eyesPos,
                        Vec3(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5), false, true, false)

                movingObjectPosition != null && movingObjectPosition.blockPos == blockPos
            }
            "around" -> !isFullBlock(blockPos.down()) || !isFullBlock(blockPos.up()) || !isFullBlock(blockPos.north())
                    || !isFullBlock(blockPos.east()) || !isFullBlock(blockPos.south()) || !isFullBlock(blockPos.west())
            else -> true
        }
    }

    override val tag
        get() = getBlockName(block)
}