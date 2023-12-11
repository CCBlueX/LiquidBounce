/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.searchBlocks
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbow
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBlockBox
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.block.Block
import net.minecraft.init.Blocks.air
import net.minecraft.init.Blocks.bed
import net.minecraft.util.BlockPos
import java.awt.Color
import java.util.LinkedList

object BedProtectionESP : Module("BedProtectionESP", ModuleCategory.RENDER) {
    private val radius by IntegerValue("Radius", 8, 0..32)
    private val maxLayers by IntegerValue("MaxProtectionLayers", 2, 1..8)
    private val down by BoolValue("ConsiderBlocksUnderBed", false)
    private val renderBeds by BoolValue("RenderBedBlocks", true)

    private val colorRainbow by BoolValue("Rainbow", false)
        private val colorRed by IntegerValue("R", 96, 0..255) { !colorRainbow }
        private val colorGreen by IntegerValue("G", 96, 0..255) { !colorRainbow }
        private val colorBlue by IntegerValue("B", 96, 0..255) { !colorRainbow }

    private val searchTimer = MSTimer()
    private val bedBlockList = mutableListOf<BlockPos>()
    private val blocksToRender = mutableListOf<BlockPos>()
    private var thread: Thread? = null

    private fun getBlocksToRender(bedBlock: BlockPos) : Set<BlockPos> {
        val nextLayerAirBlocks = mutableSetOf<BlockPos>()
        val nextLayerBlocks = mutableSetOf<BlockPos>()
        val cachedBlocks = mutableSetOf<BlockPos>()
        var currentLayer = 1
        val currentLayerBlocks = LinkedList<BlockPos>()
        currentLayerBlocks.add(bedBlock)

        while (currentLayerBlocks.isNotEmpty()) {
            val currBlock = currentLayerBlocks.removeFirst()
            val currBlockID = Block.getIdFromBlock(getBlock(currBlock))
            // it's not necessary to make protection layers around unbreakable blocks
            val breakableBlockIDs = listOf(35, 159, 121, 20, 5, 49, 26) // wool, stained clay, end stone, glass, wood, obsidian, bed

            if (breakableBlockIDs.contains(currBlockID)) {
                val blocksAround = mutableListOf(
                    currBlock.north(),
                    currBlock.east(),
                    currBlock.west(),
                    currBlock.south(),
                    currBlock.up(),
                )

                if (down) {
                    blocksAround.add(currBlock.down())
                }

                nextLayerAirBlocks.addAll(
                    blocksAround.filter { blockPos -> getBlock(blockPos) == air }
                )
                nextLayerBlocks.addAll(
                    blocksAround.filter { blockPos -> getBlock(blockPos) != air && !cachedBlocks.contains(blockPos) }
                )
            }

            // move to the next layer
            if (currentLayerBlocks.isEmpty() && nextLayerAirBlocks.isEmpty() && currentLayer < maxLayers) {
                currentLayerBlocks.addAll(nextLayerBlocks)
                cachedBlocks.addAll(nextLayerBlocks)
                nextLayerBlocks.clear()
                currentLayer += 1
            }
        }

        return nextLayerAirBlocks
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (searchTimer.hasTimePassed(1000) && (thread?.isAlive != true)) {
            val radius = radius
            val selectedBlock = Block.getBlockById(26)

            thread = Thread({
                //val blockList = mutableListOf<BlockPos>()

                /*for (x in -radius until radius) {
                    for (y in radius downTo -radius + 1) {
                        for (z in -radius until radius) {
                            val thePlayer = mc.thePlayer

                            val xPos = thePlayer.posX.toInt() + x
                            val yPos = thePlayer.posY.toInt() + y
                            val zPos = thePlayer.posZ.toInt() + z

                            val blockPos = BlockPos(xPos, yPos, zPos)
                            val block = getBlock(blockPos)

                            if (block == selectedBlock && blockList.size < 256) blockList += blockPos
                        }
                    }
                }*/
                val blocks = searchBlocks(radius, setOf(bed))
                searchTimer.reset()

                synchronized(bedBlockList) {
                    bedBlockList.clear()
                    bedBlockList += blocks.keys
                }
                synchronized(blocksToRender) {
                    blocksToRender.clear()
                    bedBlockList.forEach{bedBlock -> blocksToRender += getBlocksToRender(bedBlock) }
                }
            }, "BedProtectionESP-BlockFinder")

            thread!!.start()
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (renderBeds) {
            synchronized(bedBlockList) {
                for (blockPos in bedBlockList) {
                    drawBlockBox(blockPos, Color.RED, true)
                }
            }
        }
        synchronized(blocksToRender) {
            val color = if (colorRainbow) rainbow() else Color(colorRed, colorGreen, colorBlue)
            for (blockPos in blocksToRender) {
                drawBlockBox(blockPos, color, true)
            }
        }
    }
}