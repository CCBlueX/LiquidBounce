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
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbow
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.shader.shaders.OutlineShader
import net.ccbluex.liquidbounce.value.*
import java.awt.Color

@ModuleInfo(name = "ItemESP", description = "Allows you to see items through walls.", category = ModuleCategory.RENDER)
class ItemESP : Module()
{
	private val modeValue = ListValue("Mode", arrayOf("Box", "OtherBox", "ShaderOutline"), "Box")
	private val colorRedValue = IntegerValue("R", 0, 0, 255)
	private val colorGreenValue = IntegerValue("G", 255, 0, 255)
	private val colorBlueValue = IntegerValue("B", 0, 0, 255)

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

			val color = if (colorRainbow.get()) rainbow(saturation = saturationValue.get(), brightness = brightnessValue.get()) else Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get())
			val drawOutline = mode == "box"

			val provider = classProvider

			theWorld.loadedEntityList.filter { provider.isEntityItem(it) || provider.isEntityArrow(it) }.forEach { RenderUtils.drawEntityBox(it, color, drawOutline) }
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

			OutlineShader.INSTANCE.stopDraw(if (colorRainbow.get()) rainbow(saturation = saturationValue.get(), brightness = brightnessValue.get()) else Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get()), 1f, 1f)
		}
	}
}
