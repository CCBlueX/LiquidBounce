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
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.block.BlockUtils.canBeClicked
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbow
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBorderedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawFilledBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawSelectionBoundingBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.glColor
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.block.Block
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager.*
import net.minecraft.util.BlockPos
import org.lwjgl.opengl.GL11.*
import java.awt.Color

@ModuleInfo(name = "BlockOverlay", description = "Allows you to change the design of the block overlay.", category = ModuleCategory.RENDER)
class BlockOverlay : Module() {
    val infoValue = BoolValue("Info", false)

    private val colorRainbow = BoolValue("Rainbow", false)
    private val colorRedValue = object : IntegerValue("R", 68, 0, 255) {
        override fun isSupported() = !colorRainbow.get()
    }
    private val colorGreenValue = object : IntegerValue("G", 117, 0, 255) {
        override fun isSupported() = !colorRainbow.get()
    }
    private val colorBlueValue = object : IntegerValue("B", 255, 0, 255) {
        override fun isSupported() = !colorRainbow.get()
    }

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

        val block = mc.theWorld.getBlockState(blockPos).block
        val partialTicks = event.partialTicks

        val color = if (colorRainbow.get()) rainbow(alpha = 0.4F) else Color(colorRedValue.get(),
                colorGreenValue.get(), colorBlueValue.get(), (0.4F * 255).toInt())

        enableBlend()
        tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        glColor(color)
        glLineWidth(2F)
        disableTexture2D()
        glDepthMask(false)

        block.setBlockBoundsBasedOnState(mc.theWorld, blockPos)


        val thePlayer = mc.thePlayer ?: return

        val x = thePlayer.lastTickPosX + (thePlayer.posX - thePlayer.lastTickPosX) * partialTicks
        val y = thePlayer.lastTickPosY + (thePlayer.posY - thePlayer.lastTickPosY) * partialTicks
        val z = thePlayer.lastTickPosZ + (thePlayer.posZ - thePlayer.lastTickPosZ) * partialTicks

        val axisAlignedBB = block.getSelectedBoundingBox(mc.theWorld, blockPos)
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
        if (infoValue.get()) {
            val blockPos = currentBlock ?: return
            val block = getBlock(blockPos) ?: return

            val info = "${block.localizedName} §7ID: ${Block.getIdFromBlock(block)}"
            val scaledResolution = ScaledResolution(mc)

            drawBorderedRect(
                    scaledResolution.scaledWidth / 2 - 2F,
                    scaledResolution.scaledHeight / 2 + 5F,
                    scaledResolution.scaledWidth / 2 + Fonts.font40.getStringWidth(info) + 2F,
                    scaledResolution.scaledHeight / 2 + 16F,
                    3F, Color.BLACK.rgb, Color.BLACK.rgb
            )

            resetColor()
            Fonts.font40.drawString(info, scaledResolution.scaledWidth / 2f, scaledResolution.scaledHeight / 2f + 7f, Color.WHITE.rgb, false)
        }
    }
}