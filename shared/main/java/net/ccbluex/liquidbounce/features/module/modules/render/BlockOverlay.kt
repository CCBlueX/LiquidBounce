/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.world.IWorld
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.injection.backend.Backend
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.block.BlockUtils.canBeClicked
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.render.ColorUtils.createRGB
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbowRGB
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import org.lwjgl.opengl.GL11

@ModuleInfo(name = "BlockOverlay", description = "Allows you to change the design of the block overlay.", category = ModuleCategory.RENDER)
class BlockOverlay : Module()
{
	private val colorRedValue = IntegerValue("R", 68, 0, 255)
	private val colorGreenValue = IntegerValue("G", 117, 0, 255)
	private val colorBlueValue = IntegerValue("B", 255, 0, 255)
	private val colorAlphaValue = IntegerValue("Alpha", 102, 0, 255)

	private val colorRainbow = BoolValue("Rainbow", false)
	private val rainbowSpeedValue = IntegerValue("Rainbow-Speed", 10, 1, 10)
	private val saturationValue = FloatValue("HSB-Saturation", 1.0f, 0.0f, 1.0f)
	private val brightnessValue = FloatValue("HSB-Brightness", 1.0f, 0.0f, 1.0f)

	private val drawHydraESPValue = BoolValue("Hydra", false)

	val infoValue = BoolValue("Info", false)

	fun getCurrentBlock(theWorld: IWorld): WBlockPos?
	{
		val blockPos = mc.objectMouseOver?.blockPos ?: return null

		if (canBeClicked(theWorld, blockPos) && blockPos in theWorld.worldBorder) return blockPos

		return null
	}

	@EventTarget
	fun onRender3D(event: Render3DEvent)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		val blockPos = getCurrentBlock(theWorld) ?: return

		val lastTickPosX = thePlayer.lastTickPosX
		val lastTickPosY = thePlayer.lastTickPosY
		val lastTickPosZ = thePlayer.lastTickPosZ

		val block = theWorld.getBlockState(blockPos).block

		val partialTicks = event.partialTicks

		val alpha = colorAlphaValue.get()
		val rainbowSpeed = rainbowSpeedValue.get()
		val color = if (colorRainbow.get()) rainbowRGB(alpha = alpha, speed = rainbowSpeed, saturation = saturationValue.get(), brightness = brightnessValue.get()) else createRGB(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get(), alpha)

		val glStateManager = classProvider.glStateManager

		glStateManager.enableBlend()
		glStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO)
		RenderUtils.glColor(color)
		GL11.glLineWidth(2F)
		glStateManager.disableTexture2D()
		GL11.glDepthMask(false)

		@Suppress("ConstantConditionIf") if (Backend.MINECRAFT_VERSION_MINOR < 12) block.setBlockBoundsBasedOnState(theWorld, blockPos)

		val x = lastTickPosX + (thePlayer.posX - lastTickPosX) * partialTicks
		val y = lastTickPosY + (thePlayer.posY - lastTickPosY) * partialTicks
		val z = lastTickPosZ + (thePlayer.posZ - lastTickPosZ) * partialTicks

		val boxExpandSize = 0.002
		val axisAlignedBB = block.getSelectedBoundingBox(theWorld, theWorld.getBlockState(blockPos), blockPos).expand(boxExpandSize, boxExpandSize, boxExpandSize).offset(-x, -y, -z)

		RenderUtils.drawSelectionBoundingBox(axisAlignedBB, drawHydraESPValue.get())
		RenderUtils.drawFilledBox(axisAlignedBB)
		GL11.glDepthMask(true)

		glStateManager.enableTexture2D()
		glStateManager.disableBlend()
		glStateManager.resetColor()
	}

	@EventTarget
	fun onRender2D(@Suppress("UNUSED_PARAMETER") event: Render2DEvent)
	{
		if (infoValue.get())
		{
			val theWorld = mc.theWorld ?: return

			val blockPos = getCurrentBlock(theWorld) ?: return
			val block = getBlock(theWorld, blockPos)

			val info = "${block.localizedName} \u00A77ID: ${functions.getIdFromBlock(block)}"

			val provider = classProvider

			val scaledResolution = provider.createScaledResolution(mc)

			val middleScreenX = scaledResolution.scaledWidth shr 1
			val middleScreenY = scaledResolution.scaledHeight shr 1

			RenderUtils.drawBorderedRect(middleScreenX - 2F, middleScreenY + 5F, (middleScreenX + Fonts.font40.getStringWidth(info)) + 2F, middleScreenY + 16F, 3F, -16777216, -16777216)

			provider.glStateManager.resetColor()
			Fonts.font40.drawString(info, middleScreenX.toFloat(), middleScreenY + 7f, 0xffffff, false)
		}
	}
}
