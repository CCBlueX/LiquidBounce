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
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.block.Block
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.render.GlStateManager.*
import net.minecraft.util.BlockPos
import org.lwjgl.opengl.GL11.*
import java.awt.Color

object BlockOverlay : Module("BlockOverlay", Category.RENDER, gameDetecting = false, hideModule = false) {
    val info by BoolValue("Info", false)

    private val colorRainbow by BoolValue("Rainbow", false)
        private val colorRed by IntegerValue("R", 68, 0..255) { !colorRainbow }
        private val colorGreen by IntegerValue("G", 117, 0..255) { !colorRainbow }
        private val colorBlue by IntegerValue("B", 255, 0..255) { !colorRainbow }

    val currentBlock: BlockPos?
        get() {
            val blockPos = mc.objectMouseOver?.blockPos ?: return null

            if (canBeClicked(blockPos) && mc.world.worldBorder.contains(blockPos))
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

        enableBlend()
        tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        glColor(color)
        glLineWidth(2F)
        disableTexture2D()
        glDepthMask(false)

        block.setBlockBoundsBasedOnState(mc.world, blockPos)


        val thePlayer = mc.player ?: return

        val x = thePlayer.lastTickPosX + (thePlayer.posX - thePlayer.lastTickPosX) * partialTicks
        val y = thePlayer.lastTickPosY + (thePlayer.posY - thePlayer.lastTickPosY) * partialTicks
        val z = thePlayer.lastTickPosZ + (thePlayer.posZ - thePlayer.lastTickPosZ) * partialTicks

        val axisAlignedBB = block.getSelectedBoundingBox(mc.world, blockPos)
            .expand(0.0020000000949949026, 0.0020000000949949026, 0.0020000000949949026)
            .offset(-x, -y, -z)

        drawSelectionBoundingBox(axisAlignedBB)
        drawFilledBox(axisAlignedBB)
        glDepthMask(true)
        enableTexture2D()
        disableBlend()
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