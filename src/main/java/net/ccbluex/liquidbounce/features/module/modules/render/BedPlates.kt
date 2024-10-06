/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.block.BlockUtils.BEDWARS_BLOCKS
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlockTexture
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.ccbluex.liquidbounce.utils.render.shader.shaders.GradientFontShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.GradientShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowFontShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.ccbluex.liquidbounce.value.*
import net.minecraft.block.Block
import net.minecraft.block.BlockBed
import net.minecraft.block.state.IBlockState
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager.resetColor
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

object BedPlates : Module("BedPlates", Category.RENDER, hideModule = false) {
    private val renderYOffset by IntegerValue("RenderYOffset", 1, 0..5)
    
    private val maxRenderDistance by object : IntegerValue("MaxRenderDistance", 100, 1..200) {
        override fun onUpdate(value: Int) {
            maxRenderDistanceSq = value.toDouble().pow(2.0)
        }
    }
    private var maxRenderDistanceSq = 0.0
        set(value) {
            field = if (value <= 0.0) maxRenderDistance.toDouble().pow(2.0) else value
        }

    private val maxLayers by IntegerValue("MaxLayers", 5, 1..10)
    private val scale by FloatValue("Scale", 3F, 1F..5F)

    private val textMode by ListValue("Text-Color", arrayOf("Custom", "Rainbow", "Gradient"), "Custom")
    private val textRed by IntegerValue("Text-R", 255, 0..255) { textMode == "Custom" }
    private val textGreen by IntegerValue("Text-G", 255, 0..255) { textMode == "Custom" }
    private val textBlue by IntegerValue("Text-B", 255, 0..255) { textMode == "Custom" }

    private val gradientTextSpeed by FloatValue("Text-Gradient-Speed", 1f, 0.5f..10f) { textMode == "Gradient" }

    // TODO: Make Color picker to fix this mess :/
    private val gradientTextRed1 by FloatValue("Text-Gradient-R1", 255f, 0f..255f) { textMode == "Gradient" }
    private val gradientTextGreen1 by FloatValue("Text-Gradient-G1", 0f, 0f..255f) { textMode == "Gradient" }
    private val gradientTextBlue1 by FloatValue("Text-Gradient-B1", 0f, 0f..255f) { textMode == "Gradient" }

    private val gradientTextRed2 by FloatValue("Text-Gradient-R2", 0f, 0f..255f) { textMode == "Gradient" }
    private val gradientTextGreen2 by FloatValue("Text-Gradient-G2", 255f, 0f..255f) { textMode == "Gradient" }
    private val gradientTextBlue2 by FloatValue("Text-Gradient-B2", 0f, 0f..255f) { textMode == "Gradient" }

    private val gradientTextRed3 by FloatValue("Text-Gradient-R3", 0f, 0f..255f) { textMode == "Gradient" }
    private val gradientTextGreen3 by FloatValue("Text-Gradient-G3", 0f, 0f..255f) { textMode == "Gradient" }
    private val gradientTextBlue3 by FloatValue("Text-Gradient-B3", 255f, 0f..255f) { textMode == "Gradient" }

    private val gradientTextRed4 by FloatValue("Text-Gradient-R4", 0f, 0f..255f) { textMode == "Gradient" }
    private val gradientTextGreen4 by FloatValue("Text-Gradient-G4", 0f, 0f..255f) { textMode == "Gradient" }
    private val gradientTextBlue4 by FloatValue("Text-Gradient-B4", 0f, 0f..255f) { textMode == "Gradient" }

    private val roundedRectRadius by FloatValue("Rounded-Radius", 3F, 0F..5F)

    private val backgroundMode by ListValue("Background-Color", arrayOf("Custom", "Rainbow", "Gradient"), "Custom")
    private val backgroundRed by IntegerValue("Background-R", 0, 0..255) { backgroundMode == "Custom" }
    private val backgroundGreen by IntegerValue("Background-G", 0, 0..255) { backgroundMode == "Custom" }
    private val backgroundBlue by IntegerValue("Background-B", 0, 0..255) { backgroundMode == "Custom" }
    private val backgroundAlpha by IntegerValue("Background-Alpha", 100, 0..255) { backgroundMode == "Custom" }
    
    private val gradientBackgroundSpeed by FloatValue("Background-Gradient-Speed", 1f, 0.5f..10f) { backgroundMode == "Gradient" }

    // TODO: Make Color picker to fix this mess :/
    private val gradientBackgroundRed1 by FloatValue("Background-Gradient-R1", 255f, 0f..255f) { backgroundMode == "Gradient" }
    private val gradientBackgroundGreen1 by FloatValue("Background-Gradient-G1", 0f, 0f..255f) { backgroundMode == "Gradient" }
    private val gradientBackgroundBlue1 by FloatValue("Background-Gradient-B1", 0f, 0f..255f) { backgroundMode == "Gradient" }

    private val gradientBackgroundRed2 by FloatValue("Background-Gradient-R2", 0f, 0f..255f) { backgroundMode == "Gradient" }
    private val gradientBackgroundGreen2 by FloatValue("Background-Gradient-G2", 255f, 0f..255f) { backgroundMode == "Gradient" }
    private val gradientBackgroundBlue2 by FloatValue("Background-Gradient-B2", 0f, 0f..255f) { backgroundMode == "Gradient" }

    private val gradientBackgroundRed3 by FloatValue("Background-Gradient-R3", 0f, 0f..255f) { backgroundMode == "Gradient" }
    private val gradientBackgroundGreen3 by FloatValue("Background-Gradient-G3", 0f, 0f..255f) { backgroundMode == "Gradient" }
    private val gradientBackgroundBlue3 by FloatValue("Background-Gradient-B3", 255f, 0f..255f) { backgroundMode == "Gradient" }

    private val gradientBackgroundRed4 by FloatValue("Background-Gradient-R4", 0f, 0f..255f) { backgroundMode == "Gradient" }
    private val gradientBackgroundGreen4 by FloatValue("Background-Gradient-G4", 0f, 0f..255f) { backgroundMode == "Gradient" }
    private val gradientBackgroundBlue4 by FloatValue("Background-Gradient-B4", 0f, 0f..255f) { backgroundMode == "Gradient" }

    private val textFont by FontValue("Font", Fonts.font35)
    private val textShadow by BoolValue("ShadowText", true)

    private val rainbowX by FloatValue("Rainbow-X", -1000F, -2000F..2000F) { backgroundMode == "Rainbow" }
    private val rainbowY by FloatValue("Rainbow-Y", -1000F, -2000F..2000F) { backgroundMode == "Rainbow" }
    private val gradientX by FloatValue("Gradient-X", -1000F, -2000F..2000F) { backgroundMode == "Gradient" }
    private val gradientY by FloatValue("Gradient-Y", -1000F, -2000F..2000F) { backgroundMode == "Gradient" }

    private var bed: Array<BlockPos>? = null
    private val beds: MutableList<BlockPos?> = mutableListOf()
    private val bedBlocks: MutableList<MutableList<Block>> = mutableListOf()
    private var searchJob: Job? = null

    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    override fun onDisable() {
        if (searchJob?.isActive == true)
            searchJob?.cancel()
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val player = mc.thePlayer ?: return
        val world = mc.theWorld ?: return

        try {
            if (searchJob?.isActive != true) {
                searchJob = coroutineScope.launch {
                    val blockList = mutableListOf<BlockPos>()
                    val bedBlockLists = mutableListOf<MutableList<Block>>()
                    val bedSet = mutableSetOf<BlockPos>()

                    val radius = maxRenderDistance
                    for (i in -radius..radius) {
                        for (j in -radius..radius) {
                            for (k in -radius..radius) {
                                val blockPos = BlockPos(player.posX + j, player.posY + i, player.posZ + k)
                                val blockState: IBlockState = world.getBlockState(blockPos)

                                if (blockState.block == Blocks.bed && blockState.getValue(BlockBed.PART) == BlockBed.EnumPartType.FOOT) {
                                    bedBlocks(blockPos, blockList, bedBlockLists, bedSet)
                                }
                            }
                        }
                    }

                    synchronized(beds) {
                        if (beds.size != blockList.size || !beds.containsAll(blockList)) {
                            beds.clear()
                            beds.addAll(blockList)
                            bedBlocks.clear()
                            bedBlocks.addAll(bedBlockLists)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Failed to run BedPlates Coroutine Job.", e)
        }
    }

    private fun bedBlocks(
        blockPos: BlockPos,
        blockList: MutableList<BlockPos>,
        bedBlockLists: MutableList<MutableList<Block>>,
        bedSet: MutableSet<BlockPos>,
    ) {
        if (!bedSet.contains(blockPos)) {
            blockList.add(blockPos)
            bedSet.add(blockPos)
            bedBlockLists.add(mutableListOf())
        }
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        searchJob?.cancel()
        beds.clear()
        bedBlocks.clear()
        bed = null
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (mc.thePlayer == null || mc.theWorld == null) return
        if (beds.isEmpty()) return

        val bedsCopy = beds.toList()

        bedsCopy.forEachIndexed { index, blockPos ->
            if (blockPos != null && mc.theWorld.getBlockState(blockPos).block is BlockBed) {
                findAndRenderBed(blockPos, index)
            }
        }
    }

    private fun findAndRenderBed(blockPos: BlockPos, index: Int) {
        findBed(blockPos.x.toDouble(), blockPos.y.toDouble(), blockPos.z.toDouble(), index)
        drawPlate(blockPos, index)
    }

    private fun drawPlate(blockPos: BlockPos, index: Int) {
        val player = mc.thePlayer ?: return
        val renderManager = mc.renderManager ?: return
        val rotateX = if (mc.gameSettings.thirdPersonView == 2) -1.0f else 1.0f
        
        val rainbowOffset = System.currentTimeMillis() % 10000 / 10000F
        val rainbowX = if (rainbowX == 0f) 0f else 1f / rainbowX
        val rainbowY = if (rainbowY == 0f) 0f else 1f / rainbowY

        val gradientOffset = System.currentTimeMillis() % 10000 / 10000F
        val gradientX = if (gradientX == 0f) 0f else 1f / gradientX
        val gradientY = if (gradientY == 0f) 0f else 1f / gradientY

        val distance = player.getDistance(blockPos.x.toDouble(), blockPos.y.toDouble(), blockPos.z.toDouble())
        val scale = ((distance / 4F).coerceAtLeast(1.0) / 150F) * scale

        glPushMatrix()

        glEnable(GL_LINE_SMOOTH)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glDisable(GL_DEPTH_TEST)
        glDepthMask(false)

        glTranslatef(
            (blockPos.x - renderManager.viewerPosX + 0.5).toFloat(),
            (blockPos.y - renderManager.viewerPosY + renderYOffset + 1).toFloat(),
            (blockPos.z - renderManager.viewerPosZ + 0.5).toFloat()
        )
        glRotatef(-renderManager.playerViewY, 0F, 1F, 0F)
        glRotatef(renderManager.playerViewX * rotateX, 1F, 0F, 0F)
        glScalef(-scale.toFloat(), -scale.toFloat(), scale.toFloat())

        val blocks = bedBlocks[index]
        val text = "Bed (${distance.roundToInt()}m)"

        var offset = (blocks.size * -13) / 2

        val textWidth = textFont.getStringWidth(text)
        val textHeight = textFont.FONT_HEIGHT

        val rectWidth = max(30.0, textWidth.toDouble() + offset / 2)
        val rectHeight = max(26.5, textHeight.toDouble())

        // Render rect background
        GradientShader.begin(
            backgroundMode == "Gradient",
            gradientX,
            gradientY,
            floatArrayOf(
                gradientBackgroundRed1 / 255f,
                gradientBackgroundGreen1 / 255f,
                gradientBackgroundBlue1 / 255f,
                1f
            ),
            floatArrayOf(
                gradientBackgroundRed2 / 255f,
                gradientBackgroundGreen2 / 255f,
                gradientBackgroundBlue2 / 255f,
                1f
            ),
            floatArrayOf(
                gradientBackgroundRed3 / 255f,
                gradientBackgroundGreen3 / 255f,
                gradientBackgroundBlue3 / 255f,
                1f
            ),
            floatArrayOf(
                gradientBackgroundRed4 / 255f,
                gradientBackgroundGreen4 / 255f,
                gradientBackgroundBlue4 / 255f,
                1f
            ),
            gradientBackgroundSpeed,
            gradientOffset
        ).use {
            RainbowShader.begin(backgroundMode == "Rainbow", rainbowX, rainbowY, rainbowOffset).use {
                drawRoundedRect(
                    (-rectWidth / 1.5 + scale + (offset / 2)).toFloat(),
                    (-rectHeight / 3 - scale).toFloat(),
                    (rectWidth / 1.5 - scale - (offset / 2)).toFloat(),
                    (rectHeight / 1.05 + scale).toFloat(),
                    when (backgroundMode) {
                        "Gradient" -> 0
                        "Rainbow" -> 0
                        else -> Color(backgroundRed, backgroundGreen, backgroundBlue, backgroundAlpha).rgb
                    },
                    roundedRectRadius
                )
            }
        }

        // Render distance text
        GradientFontShader.begin(
            textMode == "Gradient",
            gradientX,
            gradientY,
            floatArrayOf(
                gradientTextRed1 / 255f,
                gradientTextGreen1 / 255f,
                gradientTextBlue1 / 255f,
                1f
            ),
            floatArrayOf(
                gradientTextRed2 / 255f,
                gradientTextGreen2 / 255f,
                gradientTextBlue2 / 255f,
                1f
            ),
            floatArrayOf(
                gradientTextRed3 / 255f,
                gradientTextGreen3 / 255f,
                gradientTextBlue3 / 255f,
                1f
            ),
            floatArrayOf(
                gradientTextRed4 / 255f,
                gradientTextGreen4 / 255f,
                gradientTextBlue4 / 255f,
                1f
            ),
            gradientTextSpeed,
            gradientOffset
        ).use {
            RainbowFontShader.begin(textMode == "Rainbow", rainbowX, rainbowY, rainbowOffset).use {
                textFont.drawString(
                    text,
                    (-textWidth / 2.15F),
                    (1F - textHeight / 2.15F),
                    when (textMode) {
                        "Gradient" -> 0
                        "Rainbow" -> 0
                        else -> Color(textRed, textGreen, textBlue, 1).rgb
                    },
                    textShadow
                )
            }
        }

        blocks.forEach { block ->
            val texture = getBlockTexture(block)
            mc.textureManager.bindTexture(texture)

            Gui.drawModalRectWithCustomSizedTexture(
                offset,
                10,
                0f, 0f,
                12, 12,
                12F, 12F
            )
            offset += 13 + scale.toInt()
        }

        glEnable(GL_DEPTH_TEST)
        glDisable(GL_LINE_SMOOTH)
        glDisable(GL_BLEND)
        glEnable(GL_TEXTURE_2D)
        glDepthMask(true)
        resetColor()

        glPopMatrix()
    }

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

    private fun addSurroundingBlocks(bedPos: BlockPos, index: Int) {
        val world = mc.theWorld ?: return

        val directions = arrayOf(
            BlockPos(0, 1, 0),
            BlockPos(1, 0, 0),
            BlockPos(-1, 0, 0),
            BlockPos(0, 0, 1),
            BlockPos(0, 0, -1)
        )

        for (dir in directions) {
            for (layer in 1..maxLayers) {
                val currentPos = bedPos.add(dir.x * layer, dir.y * layer, dir.z * layer)
                val currentBlock = world.getBlockState(currentPos).block

                if (currentBlock == Blocks.air) break

                if (currentBlock in BEDWARS_BLOCKS && currentBlock !in bedBlocks[index]) {
                    bedBlocks[index].add(currentBlock)
                }
            }
        }
    }
}