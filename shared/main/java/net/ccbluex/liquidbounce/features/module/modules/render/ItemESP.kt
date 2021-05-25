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
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbowRGB
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.shader.shaders.OutlineShader
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue

@ModuleInfo(name = "ItemESP", description = "Allows you to see items through walls.", category = ModuleCategory.RENDER)
class ItemESP : Module()
{
	private val modeValue = ListValue("Mode", arrayOf("Box", "Hydra", "ShaderOutline"), "Box")
	private val shaderRadiusValue = FloatValue("ShaderRadius", 2f, 0.5f, 5f)
	private val colorRedValue = IntegerValue("R", 0, 0, 255)
	private val colorGreenValue = IntegerValue("G", 255, 0, 255)
	private val colorBlueValue = IntegerValue("B", 0, 0, 255)
	private val colorAlphaValue = IntegerValue("Alpha", 35, 0, 255)

	private val boxOutlineRedValue = IntegerValue("Box-Outline-Red", 255, 0, 255)
	private val boxOutlineGreenValue = IntegerValue("Box-Outline-Green", 255, 0, 255)
	private val boxOutlineBlueValue = IntegerValue("Box-Outline-Blue", 255, 0, 255)
	private val boxOutlineAlphaValue = IntegerValue("Box-Outline-Alpha", 90, 0, 255)

	private val colorRainbow = BoolValue("Rainbow", true)

	private val saturationValue = FloatValue("HSB-Saturation", 1.0f, 0.0f, 1.0f)
	private val brightnessValue = FloatValue("HSB-Brightness", 1.0f, 0.0f, 1.0f)

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent?)
	{
		val mode = modeValue.get().toLowerCase()
		if (mode != "shaderoutline")
		{
			val theWorld = mc.theWorld ?: return

			val color = if (colorRainbow.get()) rainbowRGB(alpha = colorAlphaValue.get(), saturation = saturationValue.get(), brightness = brightnessValue.get()) else ColorUtils.createRGB(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get(), colorAlphaValue.get())

			val hydraESP = mode == "hydra"

			val provider = classProvider

			val boxOutlineColor = ColorUtils.createRGB(boxOutlineRedValue.get(), boxOutlineGreenValue.get(), boxOutlineBlueValue.get(), boxOutlineAlphaValue.get())

			theWorld.loadedEntityList.filter { provider.isEntityItem(it) || provider.isEntityArrow(it) }.forEach { RenderUtils.drawEntityBox(it, color, boxOutlineColor, hydraESP) }
		}
	}

	@EventTarget
	fun onRender2D(event: Render2DEvent)
	{
		val theWorld = mc.theWorld ?: return

		if (modeValue.get().equals("ShaderOutline", ignoreCase = true))
		{
			val partialTicks = event.partialTicks

			OutlineShader.INSTANCE.startDraw(partialTicks)

			val renderManager = mc.renderManager
			val provider = classProvider

			try
			{
				theWorld.loadedEntityList.filter { provider.isEntityItem(it) || provider.isEntityArrow(it) }.forEach { renderManager.renderEntityStatic(it, partialTicks, true) }
			}
			catch (ex: Exception)
			{
				ClientUtils.logger.error("An error occurred while rendering all item entities for shader esp", ex)
			}

			OutlineShader.INSTANCE.stopDraw(if (colorRainbow.get()) rainbowRGB(saturation = saturationValue.get(), brightness = brightnessValue.get()) else ColorUtils.createRGB(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get()), shaderRadiusValue.get(), 1f)
		}
	}

	override val tag: String
		get() = modeValue.get()
}
