/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.minecraft.client.block.IBlock
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayerDigging
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.player.AutoTool
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getCenterDistance
import net.ccbluex.liquidbounce.utils.block.BlockUtils.searchBlocks
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.TickTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import java.awt.Color
import kotlin.math.roundToInt

@ModuleInfo(name = "Nuker", description = "Breaks all blocks around you.", category = ModuleCategory.WORLD)
class Nuker : Module() {

    /**
     * OPTIONS
     */

    private val radiusValue = FloatValue("Radius", 5.2F, 1F, 6F)
    private val throughWallsValue = BoolValue("ThroughWalls", false)
    private val priorityValue = ListValue("Priority", arrayOf("Distance", "Hardness"), "Distance")
    private val rotationsValue = BoolValue("Rotations", true)
    private val layerValue = BoolValue("Layer", false)
    private val hitDelayValue = IntegerValue("HitDelay", 4, 0, 20)
    private val nukeValue = IntegerValue("Nuke", 1, 1, 20)
    private val nukeDelay = IntegerValue("NukeDelay", 1, 1, 20)

    /**
     * VALUES
     */

    private val attackedBlocks = arrayListOf<WBlockPos>()
    private var currentBlock: WBlockPos? = null
    private var blockHitDelay = 0

    private var nukeTimer = TickTimer()
    private var nuke = 0

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        // Block hit delay
        if (blockHitDelay > 0 && !LiquidBounce.moduleManager[FastBreak::class.java]!!.state) {
            blockHitDelay--
            return
        }

        // Reset bps
        nukeTimer.update()
        if (nukeTimer.hasTimePassed(nukeDelay.get())) {
            nuke = 0
            nukeTimer.reset()
        }

        // Clear blocks
        attackedBlocks.clear()

        val thePlayer = mc.thePlayer!!

        if (!mc.playerController.isInCreativeMode) {
            // Default nuker

            val validBlocks = searchBlocks(radiusValue.get().roundToInt() + 1)
                    .filter { (pos, block) ->
                        if (getCenterDistance(pos) <= radiusValue.get() && validBlock(block)) {
                            if (layerValue.get() && pos.y < thePlayer.posY) { // Layer: Break all blocks above you
                                return@filter false
                            }

                            if (!throughWallsValue.get()) { // ThroughWalls: Just break blocks in your sight
                                // Raytrace player eyes to block position (through walls check)
                                val eyesPos = WVec3(thePlayer.posX, thePlayer.entityBoundingBox.minY +
                                        thePlayer.eyeHeight, thePlayer.posZ)
                                val blockVec = WVec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
                                val rayTrace = mc.theWorld!!.rayTraceBlocks(eyesPos, blockVec,
                                        false, true, false)

                                // Check if block is visible
                                rayTrace != null && rayTrace.blockPos == pos
                            }else true // Done
                        }else false // Bad block
                    }.toMutableMap()

            do{
                val (blockPos, block) = when(priorityValue.get()) {
                    "Distance" -> validBlocks.minBy { (pos, block) ->
                        val distance = getCenterDistance(pos)
                        val safePos = WBlockPos(thePlayer.posX, thePlayer.posY - 1, thePlayer.posZ)

                        if (pos.x == safePos.x && safePos.y <= pos.y && pos.z == safePos.z)
                            Double.MAX_VALUE - distance // Last block
                        else
                            distance
                    }
                    "Hardness" -> validBlocks.maxBy { (pos, block) ->
                        val hardness = block.getPlayerRelativeBlockHardness(thePlayer, mc.theWorld!!, pos).toDouble()

                        val safePos = WBlockPos(thePlayer.posX, thePlayer.posY - 1, thePlayer.posZ)
                        if (pos.x == safePos.x && safePos.y <= pos.y && pos.z == safePos.z)
                            Double.MIN_VALUE + hardness // Last block
                        else
                            hardness
                    }
                    else -> return // what? why?
                } ?: return // well I guess there is no block to break :(

                // Reset current damage in case of block switch
                if (blockPos != currentBlock)
                    currentDamage = 0F

                // Change head rotations to next block
                if (rotationsValue.get()) {
                    val rotation = RotationUtils.faceBlock(blockPos) ?: return // In case of a mistake. Prevent flag.
                    RotationUtils.setTargetRotation(rotation.rotation)
                }

                // Set next target block
                currentBlock = blockPos
                attackedBlocks.add(blockPos)

                // Call auto tool
                val autoTool = LiquidBounce.moduleManager.getModule(AutoTool::class.java) as AutoTool
                if (autoTool.state)
                    autoTool.switchSlot(blockPos)

                // Start block breaking
                if (currentDamage == 0F) {
                    mc.netHandler.addToSendQueue(classProvider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.START_DESTROY_BLOCK,
                            blockPos, classProvider.getEnumFacing(EnumFacingType.DOWN)))

                    // End block break if able to break instant
                    if (block.getPlayerRelativeBlockHardness(thePlayer, mc.theWorld!!, blockPos) >= 1F) {
                        currentDamage = 0F
                        thePlayer.swingItem()
                        mc.playerController.onPlayerDestroyBlock(blockPos, classProvider.getEnumFacing(EnumFacingType.DOWN))
                        blockHitDelay = hitDelayValue.get()
                        validBlocks -= blockPos
                        nuke++
                        continue // Next break
                    }
                }

                // Break block
                thePlayer.swingItem()
                currentDamage += block.getPlayerRelativeBlockHardness(thePlayer, mc.theWorld!!, blockPos)
                mc.theWorld!!.sendBlockBreakProgress(thePlayer.entityId, blockPos, (currentDamage * 10F).toInt() - 1)

                // End of breaking block
                if (currentDamage >= 1F) {
                    mc.netHandler.addToSendQueue(classProvider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.STOP_DESTROY_BLOCK, blockPos, classProvider.getEnumFacing(EnumFacingType.DOWN)))
                    mc.playerController.onPlayerDestroyBlock(blockPos, classProvider.getEnumFacing(EnumFacingType.DOWN))
                    blockHitDelay = hitDelayValue.get()
                    currentDamage = 0F
                }
                return // Break out
            } while (nuke < nukeValue.get())
        } else {
            // Fast creative mode nuker (CreativeStorm option)

            // Unable to break with swords in creative mode
            if (classProvider.isItemSword(thePlayer.heldItem?.item))
                return

            // Search for new blocks to break
            searchBlocks(radiusValue.get().roundToInt() + 1)
                    .filter { (pos, block) ->
                        if (getCenterDistance(pos) <= radiusValue.get() && validBlock(block)) {
                            if (layerValue.get() && pos.y < thePlayer.posY) { // Layer: Break all blocks above you
                                return@filter false
                            }

                            if (!throughWallsValue.get()) { // ThroughWalls: Just break blocks in your sight
                                // Raytrace player eyes to block position (through walls check)
                                val eyesPos = WVec3(thePlayer.posX, thePlayer.entityBoundingBox.minY +
                                        thePlayer.eyeHeight, thePlayer.posZ)
                                val blockVec = WVec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
                                val rayTrace = mc.theWorld!!.rayTraceBlocks(eyesPos, blockVec,
                                        false, true, false)

                                // Check if block is visible
                                rayTrace != null && rayTrace.blockPos == pos
                            } else true // Done
                        } else false // Bad block
                    }
                    .forEach { (pos, _) ->
                        // Instant break block
                        mc.netHandler.addToSendQueue(classProvider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.START_DESTROY_BLOCK,
                                pos, classProvider.getEnumFacing(EnumFacingType.DOWN)))
                        thePlayer.swingItem()
                        mc.netHandler.addToSendQueue(classProvider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.STOP_DESTROY_BLOCK,
                                pos, classProvider.getEnumFacing(EnumFacingType.DOWN)))
                        attackedBlocks.add(pos)
                    }
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        // Safe block
        if (!layerValue.get()) {
            val safePos = WBlockPos(mc.thePlayer!!.posX, mc.thePlayer!!.posY - 1, mc.thePlayer!!.posZ)
            val safeBlock = BlockUtils.getBlock(safePos)
            if (safeBlock != null && validBlock(safeBlock))
                RenderUtils.drawBlockBox(safePos, Color.GREEN, true)
        }

        // Just draw all blocks
        for (blockPos in attackedBlocks)
            RenderUtils.drawBlockBox(blockPos, Color.RED, true)
    }

    /**
     * Check if [block] is a valid block to break
     */
    private fun validBlock(block: IBlock) = !classProvider.isBlockAir(block) && !classProvider.isBlockLiquid(block) && !classProvider.isBlockBedrock(block)

    companion object {
        var currentDamage = 0F
    }
}