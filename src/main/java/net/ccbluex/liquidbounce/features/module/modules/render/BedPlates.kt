/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import kotlinx.coroutines.*
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FontValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.block.Block
import net.minecraft.block.BlockBed
import net.minecraft.block.state.IBlockState
import net.minecraft.client.gui.Gui
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Made by Zywl
 */
object BedPlates : Module("BedPlates", Category.RENDER, hideModule = false) {

    private val yDifference by IntegerValue("Y-difference", 2, -5..10)
    private val updateRate by IntegerValue("Update rate (ms)", 1000, 250..5000)
    private val showDistance by BoolValue("Show distance", true)
    private val maxRenderDistance by object : IntegerValue("MaxRenderDistance", 100, 1..200) {
        override fun onUpdate(value: Int) {
            maxRenderDistanceSq = value.toDouble().pow(2.0)
        }
    }
    private var maxRenderDistanceSq = 0.0
        set(value) {
            field = if (value <= 0.0) maxRenderDistance.toDouble().pow(2.0) else value
        }

    private val onlyRenderFirstBed by BoolValue("Only render first bed", false)
    private val layers by IntegerValue("Layers", 3, 1..10)
    private val disableShadowBorder by BoolValue("Border", false)
    private val font by FontValue("Font", Fonts.minecraftFont)
    private val fontShadow by BoolValue("Shadow", true)

    private var bed: Array<BlockPos>? = null
    private val beds: MutableList<BlockPos?> = mutableListOf()
    private val bedBlocks: MutableList<MutableList<Block>> = mutableListOf()
    private var lastWorld: WorldClient? = null
    private var searchJob: Job? = null
    private val searchTimer: MSTimer = MSTimer()

    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        mc.thePlayer?.let {
            mc.theWorld?.let {
                if (searchJob?.isActive != true && searchTimer.hasTimePassed(updateRate.toLong())) {
                    startSearchJob()
                }
            }
        }
    }

    // Start a new job to search for beds
    private fun startSearchJob() {
        searchJob = coroutineScope.launch {
            val blockList = mutableListOf<BlockPos>()
            val bedBlockLists = mutableListOf<MutableList<Block>>()
            val bedSet = mutableSetOf<BlockPos>()

            val radius = maxRenderDistance
            for (i in -radius..radius) {
                for (j in -radius..radius) {
                    for (k in -radius..radius) {
                        val blockPos = BlockPos(mc.thePlayer.posX + j, mc.thePlayer.posY + i, mc.thePlayer.posZ + k)
                        val blockState: IBlockState = mc.theWorld.getBlockState(blockPos)

                        if (blockState.block == Blocks.bed && blockState.getValue(BlockBed.PART) == BlockBed.EnumPartType.FOOT) {
                            handleBedBlock(blockPos, blockList, bedBlockLists, bedSet, blockState)
                        }
                    }
                }
            }

            updateBedLists(blockList, bedBlockLists)
        }
    }

    // Handle a bed block found during the search
    private suspend fun handleBedBlock(
        blockPos: BlockPos,
        blockList: MutableList<BlockPos>,
        bedBlockLists: MutableList<MutableList<Block>>,
        bedSet: MutableSet<BlockPos>,
        blockState: IBlockState
    ) {
        if (onlyRenderFirstBed) {
            handleFirstBedBlock(blockPos, blockList, bedBlockLists, blockState)
        } else if (!bedSet.contains(blockPos)) {
            blockList.add(blockPos)
            bedSet.add(blockPos)
            bedBlockLists.add(mutableListOf())
        }
    }

    // Handle the first bed block found, if only rendering the first bed
    private suspend fun handleFirstBedBlock(
        blockPos: BlockPos,
        blockList: MutableList<BlockPos>,
        bedBlockLists: MutableList<MutableList<Block>>,
        blockState: IBlockState
    ) {
        if (this.bed != null && blockPos == this.bed!![0]) return

        this.bed = arrayOf(blockPos, blockPos.offset(blockState.getValue(BlockBed.FACING)))
        blockList.add(this.bed!![0])
        bedBlockLists.add(mutableListOf())
    }

    // Update the bed lists with the results of the search
    private fun updateBedLists(
        blockList: List<BlockPos>,
        bedBlockLists: List<MutableList<Block>>
    ) {
        searchTimer.reset()
        synchronized(beds) {
            if (beds.size != blockList.size || !beds.containsAll(blockList)) {
                beds.clear()
                beds.addAll(blockList)
                bedBlocks.clear()
                bedBlocks.addAll(bedBlockLists)
            }
        }
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        if (event.worldClient !== lastWorld) {
            cancelSearchJob()
            clearBeds()
            lastWorld = event.worldClient
        }
    }

    // Cancel the ongoing search job
    private fun cancelSearchJob() {
        searchJob?.cancel()
    }

    // Clear the bed lists
    private fun clearBeds() {
        beds.clear()
        bedBlocks.clear()
        bed = null
    }

    // Event listener for rendering
    @EventTarget
    fun onRender(event: Render3DEvent) {
        mc.thePlayer?.let {
            mc.theWorld?.let {
                renderBeds()
            }
        }
    }

    // Render the beds found
    private fun renderBeds() {
        if (onlyRenderFirstBed && bed != null) {
            renderFirstBed()
        } else {
            renderAllBeds()
        }
    }

    // Render the first bed found
    private fun renderFirstBed() {
        if (mc.theWorld.getBlockState(bed!![0]).block !is BlockBed) {
            bed = null
            return
        }
        findAndRenderBed(bed!![0], 0)
    }

    // Render all beds found
    private fun renderAllBeds() {
        if (beds.isEmpty()) return

        beds.forEachIndexed { index, blockPos ->
            if (blockPos != null && mc.theWorld.getBlockState(blockPos).block is BlockBed) {
                findAndRenderBed(blockPos, index)
            }
        }
    }

    // Find and render a specific bed
    private fun findAndRenderBed(blockPos: BlockPos, index: Int) {
        findBed(blockPos.x.toDouble(), blockPos.y.toDouble(), blockPos.z.toDouble(), index)
        drawPlate(blockPos, index)
    }

    // Draw a plate above a bed
    private fun drawPlate(blockPos: BlockPos, index: Int) {
        GL11.glPushMatrix()
        setupRenderState(blockPos)

        val blocks = bedBlocks[index]
        val rectWidth = max(17.5, blocks.size * 17.5)

        drawBorder(rectWidth)
        drawDistance(blockPos)
        drawBlockIcons(blocks)

        restoreRenderState()
        GL11.glPopMatrix()
    }

    // Setup OpenGL state for rendering
    private fun setupRenderState(blockPos: BlockPos) {
        val rotateX = if (mc.gameSettings.thirdPersonView == 2) -1.0f else 1.0f
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glTranslatef(
            (blockPos.x - mc.renderManager.viewerPosX + 0.5).toFloat(),
            (blockPos.y - mc.renderManager.viewerPosY + yDifference + 1).toFloat(),
            (blockPos.z - mc.renderManager.viewerPosZ + 0.5).toFloat()
        )
        GL11.glNormal3f(0.0f, 1.0f, 0.0f)
        GL11.glRotatef(-mc.renderManager.playerViewY, 0.0f, 1.0f, 0.0f)
        GL11.glRotatef(mc.renderManager.playerViewX, rotateX, 0.0f, 0.0f)
        GL11.glScaled(-0.01666666753590107, -0.01666666753590107, 0.01666666753590107)
    }

    // Draw the border around the plate
    private fun drawBorder(rectWidth: Double) {
        if (disableShadowBorder) {
            RenderUtils.drawRect(
                (rectWidth / -2).toInt(),
                (-0.5f).toInt(),
                (rectWidth - 2.5).toInt(),
                26.5f.toInt(),
                Color(0, 0, 0, 90).rgb
            )
        }
    }

    // Draw the distance text above the bed
    private fun drawDistance(blockPos: BlockPos) {
        if (showDistance) {
            val dist = "${mc.thePlayer.getDistance(blockPos.x.toDouble(), blockPos.y.toDouble(), blockPos.z.toDouble()).roundToInt()}m"
            font.drawString(
                dist,
                (-font.getStringWidth(dist) / 2).toFloat(),
                0f,
                Color(255, 255, 255, 255).rgb,
                fontShadow
            )
        }
    }

    // Draw block icons above the bed
    private fun drawBlockIcons(blocks: List<Block>) {
        var offset = (blocks.size * -17.5) / 2
        blocks.forEach { block ->
            val texture = getBlockTexture(block)
            mc.textureManager.bindTexture(texture)
            Gui.drawModalRectWithCustomSizedTexture(offset.toInt(), 10, 0f, 0f, 15, 15, 16f, 16f)
            offset += 17.5
        }
    }

    // Restore OpenGL state after rendering
    private fun restoreRenderState() {
        GlStateManager.disableBlend()
        GL11.glEnable(GL11.GL_DEPTH_TEST)
    }

    // Get the texture resource location for a block
    private fun getBlockTexture(block: Block): ResourceLocation {
        return when (block) {
            Blocks.bed -> ResourceLocation("minecraft:textures/items/bed.png")
            Blocks.obsidian -> ResourceLocation("minecraft:textures/blocks/obsidian.png")
            Blocks.stained_hardened_clay -> ResourceLocation("minecraft:textures/blocks/hardened_clay_stained_white.png")
            Blocks.stained_glass -> ResourceLocation("minecraft:textures/blocks/glass.png")
            Blocks.water -> ResourceLocation("minecraft:textures/blocks/water_still.png")
            Blocks.planks -> ResourceLocation("minecraft:textures/blocks/planks_oak.png")
            Blocks.wool -> ResourceLocation("minecraft:textures/blocks/wool_colored_white.png")
            else -> ResourceLocation("minecraft:textures/blocks/stone.png")
        }
    }

    // Find the bed in the specified coordinates
    private fun findBed(x: Double, y: Double, z: Double, index: Int): Boolean {
        val bedPos = BlockPos(x, y, z)
        val world = mc.theWorld ?: return false
        val bedBlock: Block = world.getBlockState(bedPos).block

        while (bedBlocks.size <= index) {
            bedBlocks.add(mutableListOf())
        }

        bedBlocks[index].clear()
        if (beds.size <= index) {
            beds.add(bedPos)
        } else {
            beds[index] = bedPos
        }

        if (bedBlock != Blocks.bed) {
            return false
        }

        bedBlocks[index].add(Blocks.bed)
        addSurroundingBlocks(bedPos, index)

        return true
    }

    // Add surrounding valid blocks to the bed's block list
    private fun addSurroundingBlocks(bedPos: BlockPos, index: Int) {
        val directions = arrayOf(
            intArrayOf(0, 1, 0),
            intArrayOf(1, 0, 0),
            intArrayOf(-1, 0, 0),
            intArrayOf(0, 0, 1),
            intArrayOf(0, 0, -1)
        )

        for (dir in directions) {
            for (layer in 1..layers) {
                val currentPos = bedPos.add(dir[0] * layer, dir[1] * layer, dir[2] * layer)
                val currentBlock: Block = mc.theWorld.getBlockState(currentPos).block

                if (currentBlock == Blocks.air) {
                    break
                }

                if (isValidBedBlock(currentBlock) && !bedBlocks[index].contains(currentBlock)) {
                    bedBlocks[index].add(currentBlock)
                }
            }
        }
    }

    // Check if a block is a valid bed block
    private fun isValidBedBlock(block: Block): Boolean {
        return block == Blocks.wool || block == Blocks.stained_hardened_clay || block == Blocks.stained_glass || block == Blocks.planks || block == Blocks.log || block == Blocks.log2 || block == Blocks.end_stone || block == Blocks.obsidian || block == Blocks.water
    }

    override fun onDisable() {
        cancelSearchJob()
        clearBeds()
    }
}
