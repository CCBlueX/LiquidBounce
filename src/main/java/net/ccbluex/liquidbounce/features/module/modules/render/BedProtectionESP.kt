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
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.searchBlocks
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbow
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBlockBox
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.Block
import net.minecraft.block.Block.getIdFromBlock
import net.minecraft.init.Blocks.*
import net.minecraft.util.math.BlockPos
import java.awt.Color
import java.util.LinkedList

object BedProtectionESP : Module("BedProtectionESP", Category.RENDER, hideModule = false) {
    private val targetBlock by ListValue("TargetBlock", arrayOf("Bed", "DragonEgg"), "Bed")
    private val renderMode by ListValue("LayerRenderMode", arrayOf("Current", "All"), "Current")
    private val radius by IntegerValue("Radius", 8, 0..32)
    private val maxLayers by IntegerValue("MaxProtectionLayers", 2, 1..6)
    private val blockLimit by IntegerValue("BlockLimit", 256, 0..1024)
    private val down by BoolValue("BlocksUnderTarget", false)
    private val renderTargetBlocks by BoolValue("RenderTargetBlocks", true)

    private val colorRainbow by BoolValue("Rainbow", false)
        private val colorRed by IntegerValue("R", 96, 0..255) { !colorRainbow }
        private val colorGreen by IntegerValue("G", 96, 0..255) { !colorRainbow }
        private val colorBlue by IntegerValue("B", 96, 0..255) { !colorRainbow }

    private val searchTimer = MSTimer()
    private val targetBlockList = mutableListOf<BlockPos>()
    private val blocksToRender = mutableSetOf<BlockPos>()
    private var thread: Thread? = null

    private val breakableBlockIDs = arrayOf(35, 24, 159, 121, 20, 5, 49) // wool, sandstone, stained_clay, end_stone, glass, wood, obsidian

    private fun getBlocksToRender(targetBlock: Block, maxLayers: Int, down: Boolean, allLayers: Boolean, blockLimit: Int) {
        val targetBlockID = getIdFromBlock(targetBlock)

        val nextLayerAirBlocks = mutableSetOf<BlockPos>()
        val nextLayerBlocks = mutableSetOf<BlockPos>()
        val cachedBlocks = mutableSetOf<BlockPos>()
        val currentLayerBlocks = LinkedList<BlockPos>()
        var currentLayer = 1


        // get blocks around each target block
        for (block in targetBlockList) {
            currentLayerBlocks.add(block)

            while (currentLayerBlocks.isNotEmpty()) {
                val currBlock = currentLayerBlocks.removeFirst()
                val currBlockID = getIdFromBlock(getBlock(currBlock))

                // it's not necessary to make protection layers around unbreakable blocks
                if (breakableBlockIDs.contains(currBlockID) || (currBlockID == targetBlockID) || (allLayers && currBlockID == 0)) {
                    val blocksAround = mutableListOf(
                        currBlock.north(),
                        currBlock.east(),
                        currBlock.south(),
                        currBlock.west(),
                        currBlock.up(),
                    )

                    if (down) {
                        blocksAround.add(currBlock.down())
                    }

                    nextLayerAirBlocks.addAll(
                        blocksAround.filter { blockPos -> getBlock(blockPos) == air }
                    )
                    nextLayerBlocks.addAll(
                        blocksAround.filter { blockPos ->
                            (allLayers || getBlock(blockPos) != air) && !cachedBlocks.contains(
                                blockPos
                            )
                        }
                    )
                }

                // move to the next layer
                if (currentLayerBlocks.isEmpty() && (allLayers || nextLayerAirBlocks.isEmpty()) && currentLayer < maxLayers) {
                    currentLayerBlocks += nextLayerBlocks
                    cachedBlocks += nextLayerBlocks
                    nextLayerBlocks.clear()
                    currentLayer += 1
                }
            }

            nextLayerBlocks.clear()
            cachedBlocks.clear()
            currentLayer = 1

            for (newBlock in nextLayerAirBlocks) {
                if (blocksToRender.size >= blockLimit) {
                    return
                }
                blocksToRender += newBlock
            }

            nextLayerAirBlocks.clear()
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (searchTimer.hasTimePassed(1000) && (thread?.isAlive != true)) {
            val radius = radius
            val targetBlock = if (targetBlock == "Bed") bed else dragon_egg
            val maxLayers = maxLayers
            val down = down
            val allLayers = renderMode == "All"
            val blockLimit = blockLimit

            thread = Thread({
                val blocks = searchBlocks(radius, setOf(targetBlock), 32)
                searchTimer.reset()

                synchronized(targetBlockList) {
                    targetBlockList.clear()
                    targetBlockList += blocks.keys
                }
                synchronized(blocksToRender) {
                    blocksToRender.clear()
                    getBlocksToRender(targetBlock, maxLayers, down, allLayers, blockLimit)
                }
            }, "BedProtectionESP-BlockFinder")

            thread!!.start()
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (renderTargetBlocks) {
            synchronized(targetBlockList) {
                for (blockPos in targetBlockList) {
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

    override fun onToggle(state: Boolean) {
        targetBlockList.clear()
        blocksToRender.clear()
    }

    override val tag: String
        get() = blocksToRender.size.toString()
}