/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

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
import kotlin.math.sqrt

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
    private val beds: MutableList<BlockPos?> = ArrayList()
    private val bedBlocks: MutableList<MutableList<Block>> = ArrayList()
    private var lastWorld: WorldClient? = null
    private val searchTimer = MSTimer()
    private var thread: Thread? = null

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mc.thePlayer != null && mc.theWorld != null) {
            if (searchTimer.hasTimePassed(updateRate.toLong()) && (thread == null || !thread!!.isAlive)) {
                val radius: Int = maxRenderDistance
                thread = Thread({
                    val blockList: MutableList<BlockPos> = ArrayList()
                    val bedBlockLists: MutableList<MutableList<Block>> = ArrayList()

                    val bedSet: MutableSet<BlockPos> = HashSet()

                    for (i in radius downTo -radius) {
                        for (j in -radius until radius) {
                            for (k in -radius until radius) {
                                val blockPos = BlockPos(mc.thePlayer.posX + j, mc.thePlayer.posY + i, mc.thePlayer.posZ + k)
                                val getBlockState = mc.theWorld.getBlockState(blockPos)

                                if (getBlockState.block == Blocks.bed && getBlockState.getValue(BlockBed.PART) == BlockBed.EnumPartType.FOOT) {
                                    if (onlyRenderFirstBed) {
                                        if (this.bed != null && blockPos == this.bed!![0]) {
                                            return@Thread
                                        }
                                        this.bed = arrayOf(blockPos, blockPos.offset(getBlockState.getValue(BlockBed.FACING)))
                                        blockList.add(this.bed!![0])
                                        bedBlockLists.add(ArrayList())
                                        bedSet.add(this.bed!![0])
                                        continue
                                    } else {
                                        if (!bedSet.contains(blockPos)) {
                                            blockList.add(blockPos)
                                            bedSet.add(blockPos)
                                            bedBlockLists.add(ArrayList())
                                        }
                                    }
                                }
                            }
                        }
                    }

                    searchTimer.reset()
                    synchronized(beds) {
                        if (beds.size != blockList.size || !beds.containsAll(blockList)) {
                            this.beds.clear()
                            this.beds.addAll(blockList)
                            this.bedBlocks.clear()
                            this.bedBlocks.addAll(bedBlockLists)
                        }
                    }
                }, "BedFinder")
                thread!!.start()
            }
        }
    }

    override fun onDisable() {
        if (thread != null && thread!!.isAlive) {
            thread!!.interrupt()
        }
        beds.clear()
        bedBlocks.clear()
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        if (event.worldClient !== lastWorld) {
            if (thread != null && thread!!.isAlive) {
                thread!!.interrupt()
            }
            beds.clear()
            bedBlocks.clear()
            this.bed = null
        }
        lastWorld = event.worldClient
    }

    @EventTarget
    fun onRender(event: Render3DEvent) {
        if (mc.thePlayer != null && mc.theWorld != null) {
            if (onlyRenderFirstBed && this.bed != null) {
                if (mc.theWorld.getBlockState(bed!![0]).block !is BlockBed) {
                    this.bed = null
                    return
                }
                findBed(bed!![0].x.toDouble(), bed!![0].y.toDouble(), bed!![0].z.toDouble(), 0)
                this.drawPlate(bed!![0], 0)
            } else {
                if (beds.isEmpty()) {
                    return
                }
                for (i in beds.indices) {
                    val blockPos = beds[i]
                    if (mc.theWorld.getBlockState(blockPos).block !is BlockBed) {
                        continue
                    }
                    findBed(blockPos!!.x.toDouble(), blockPos.y.toDouble(), blockPos.z.toDouble(), i)
                    this.drawPlate(blockPos, i)
                }
            }
        }
    }

    private fun drawPlate(blockPos: BlockPos, index: Int) {
        val rotateX = if (mc.gameSettings.thirdPersonView == 2) -1.0f else 1.0f
        GL11.glPushMatrix()
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
        val distance =
            sqrt(mc.thePlayer.getDistance(blockPos.x.toDouble(), blockPos.y.toDouble(), blockPos.z.toDouble()))
        GL11.glScaled(-0.01666666753590107 * distance, -0.01666666753590107 * distance, 0.01666666753590107 * distance)

        val blocks: List<Block> = bedBlocks[index]
        val rectWidth = max(17.5, blocks.size * 17.5)

        if (!disableShadowBorder) {
            RenderUtils.drawRect(
                (rectWidth / -2).toInt(),
                (-0.5f).toInt(),
                (rectWidth - 2.5).toInt(),
                26.5f.toInt(),
                Color(0, 0, 0, 90).rgb 
            )
        }

        if (showDistance) {
            val dist = Math.round(
                mc.thePlayer.getDistance(
                    blockPos.x.toDouble(),
                    blockPos.y.toDouble(),
                    blockPos.z.toDouble()
                )
            ).toString() + "m"
            font.drawString(
                dist,
                (-font.getStringWidth(dist) / 2).toFloat(),
                0F,
                Color(255, 255, 255, 255).rgb,
                fontShadow
            )
        }

        var offset = (blocks.size * -17.5) / 2
        for (block in blocks) {
            val texture = getBlockTexture(block)
            mc.textureManager.bindTexture(texture)
            Gui.drawModalRectWithCustomSizedTexture(offset.toInt(), 10, 0f, 0f, 15, 15, 16f, 16f)
            offset += 17.5
        }
        GlStateManager.disableBlend()
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glPopMatrix()
    }

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

    private fun findBed(x: Double, y: Double, z: Double, index: Int): Boolean {
        val bedPos = BlockPos(x, y, z)
        val bedBlock = mc.theWorld.getBlockState(bedPos).block

        while (bedBlocks.size <= index) {
            bedBlocks.add(ArrayList())
        }

        bedBlocks[index].clear()
        beds[index] = null

        if (beds.contains(bedPos) || bedBlock != Blocks.bed) {
            return false
        }

        bedBlocks[index].add(Blocks.bed)
        beds[index] = bedPos

        val directions = arrayOf(
            intArrayOf(0, 1, 0),
            intArrayOf(1, 0, 0),
            intArrayOf(-1, 0, 0),
            intArrayOf(0, 0, 1),
            intArrayOf(0, 0, -1)
        )

        val layersCount: Int = layers

        for (dir in directions) {
            for (layer in 1..layersCount) {
                val currentPos = bedPos.add(dir[0] * layer, dir[1] * layer, dir[2] * layer)
                val currentBlock = mc.theWorld.getBlockState(currentPos).block

                if (currentBlock == Blocks.air) {
                    break
                }

                if (isValidBedBlock(currentBlock) && !bedBlocks[index].contains(currentBlock)) {
                    bedBlocks[index].add(currentBlock)
                }
            }
        }

        return true
    }

    private fun isValidBedBlock(block: Block): Boolean {
        return block == Blocks.wool || block == Blocks.stained_hardened_clay || block == Blocks.stained_glass || block == Blocks.planks || block == Blocks.log || block == Blocks.log2 || block == Blocks.end_stone || block == Blocks.obsidian || block == Blocks.water
    }
}
