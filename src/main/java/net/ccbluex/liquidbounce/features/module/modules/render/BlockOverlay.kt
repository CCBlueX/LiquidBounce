/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.block.BlockUtils.canBeClicked
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.extensions.component1
import net.ccbluex.liquidbounce.utils.extensions.component2
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbow
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBorderedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawFilledBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawSelectionBoundingBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.glColor
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.Block
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager.*
import net.minecraft.util.BlockPos
import org.lwjgl.opengl.GL11.*
import java.awt.Color

object BlockOverlay : Module("BlockOverlay", Category.RENDER, gameDetecting = false, hideModule = false) {
    private val mode by ListValue("Mode", arrayOf("Box", "OtherBox", "Outline"), "Box")
    private val depth3D by BoolValue("Depth3D", false)
    private val thickness by FloatValue("Thickness", 2F, 1F..5F)

    val info by BoolValue("Info", false)

    private val colorRainbow by BoolValue("Rainbow", false)
        private val colorRed by IntegerValue("R", 68, 0..255) { !colorRainbow }
        private val colorGreen by IntegerValue("G", 117, 0..255) { !colorRainbow }
        private val colorBlue by IntegerValue("B", 255, 0..255) { !colorRainbow }

    val currentBlock: BlockPos?
        get() {
            val blockPos = mc.objectMouseOver?.blockPos ?: return null

            if (canBeClicked(blockPos) && mc.theWorld.worldBorder.contains(blockPos))
                return blockPos

            return null
        }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        val blockPos = currentBlock ?: return

        val block = getBlock(blockPos) ?: return
        val partialTicks = event.partialTicks

        val color = if (colorRainbow) rainbow(alpha = 0.4F) else Color(colorRed,
                colorGreen, colorBlue, (0.4F * 255).toInt())

        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_LINE_SMOOTH)
        glHint(GL_LINE_SMOOTH_HINT, GL_NICEST)
        glColor(color)
        glLineWidth(thickness)
        glDisable(GL_TEXTURE_2D)
        if (depth3D) glDisable(GL_DEPTH_TEST)
        glDepthMask(false)

        block.setBlockBoundsBasedOnState(mc.theWorld, blockPos)

        val thePlayer = mc.thePlayer ?: return

        val x = thePlayer.lastTickPosX + (thePlayer.posX - thePlayer.lastTickPosX) * partialTicks
        val y = thePlayer.lastTickPosY + (thePlayer.posY - thePlayer.lastTickPosY) * partialTicks
        val z = thePlayer.lastTickPosZ + (thePlayer.posZ - thePlayer.lastTickPosZ) * partialTicks

        val axisAlignedBB = block.getSelectedBoundingBox(mc.theWorld, blockPos)
            .expand(0.0020000000949949026, 0.0020000000949949026, 0.0020000000949949026)
            .offset(-x, -y, -z)

        when (mode.lowercase()) {
            "box" -> {
                drawFilledBox(axisAlignedBB)
                drawSelectionBoundingBox(axisAlignedBB)
            }
            "otherbox" -> drawFilledBox(axisAlignedBB)
            "outline" -> drawSelectionBoundingBox(axisAlignedBB)
        }

        if (depth3D) glEnable(GL_DEPTH_TEST)
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
        glDisable(GL_LINE_SMOOTH)
        glDepthMask(true)
        resetColor()
    }

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (info) {
            val blockPos = currentBlock ?: return
            val block = getBlock(blockPos) ?: return

            val info = "${block.localizedName} §7ID: ${Block.getIdFromBlock(block)}"
            val (width, height) = ScaledResolution(mc)

            drawBorderedRect(
                    width / 2 - 2F,
                    height / 2 + 5F,
                    width / 2 + Fonts.font40.getStringWidth(info) + 2F,
                    height / 2 + 16F,
                    3F, Color.BLACK.rgb, Color.BLACK.rgb
            )

            resetColor()
            Fonts.font40.drawString(info, width / 2f, height / 2f + 7f, Color.WHITE.rgb, false)
        }
    }
}