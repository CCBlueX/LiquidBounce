/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
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
import net.ccbluex.liquidbounce.utils.render.shader.shaders.GlowShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.OutlineShader
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue

@ModuleInfo(name = "ProphuntESP", description = "Allows you to see disguised players in PropHunt.", category = ModuleCategory.RENDER)
class ProphuntESP : Module()
{
	/**
	 * Options
	 */
	private val modeValue = ListValue("Mode", arrayOf("Box", "Hydra", "ShaderOutline", "ShaderGlow"), "OtherBox")

	private val shaderOutlineRadius = FloatValue("ShaderOutline-Radius", 1.35f, 1f, 2f)
	private val shaderGlowRadius = FloatValue("ShaderGlow-Radius", 2.3f, 2f, 3f)

	private val colorRedValue = IntegerValue("R", 0, 0, 255)
	private val colorGreenValue = IntegerValue("G", 90, 0, 255)
	private val colorBlueValue = IntegerValue("B", 255, 0, 255)
	private val colorAlphaValue = IntegerValue("Alpha", 255, 0, 255)

	private val boxOutlineRedValue = IntegerValue("Box-Outline-Red", 255, 0, 255)
	private val boxOutlineGreenValue = IntegerValue("Box-Outline-Green", 255, 0, 255)
	private val boxOutlineBlueValue = IntegerValue("Box-Outline-Blue", 255, 0, 255)
	private val boxOutlineAlphaValue = IntegerValue("Box-Outline-Alpha", 90, 0, 255)

	private val colorRainbow = BoolValue("Rainbow", false)
	private val saturationValue = FloatValue("HSB-Saturation", 1.0f, 0.0f, 1.0f)
	private val brightnessValue = FloatValue("HSB-Brightness", 1.0f, 0.0f, 1.0f)

	/**
	 * Variables
	 */
	val blocks = hashMapOf<WBlockPos, Long>()

	override fun onDisable()
	{
		synchronized(blocks, blocks::clear)
	}

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent?)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		val mode = modeValue.get().toLowerCase()

		val hydraESP = mode == "hydra"

		val color = if (colorRainbow.get()) rainbowRGB(saturation = saturationValue.get(), brightness = brightnessValue.get()) else ColorUtils.createRGB(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get(), colorAlphaValue.get())

		val boxOutlineColor = ColorUtils.createRGB(boxOutlineRedValue.get(), boxOutlineGreenValue.get(), boxOutlineBlueValue.get(), boxOutlineAlphaValue.get())

		if (mode == "box" || hydraESP) theWorld.loadedEntityList.filter(classProvider::isEntityFallingBlock).forEach { RenderUtils.drawEntityBox(it, color, boxOutlineColor, hydraESP) }

		synchronized(blocks) {
			val iterator: MutableIterator<Map.Entry<WBlockPos, Long>> = blocks.entries.iterator()

			while (iterator.hasNext())
			{
				val entry = iterator.next()

				if (System.currentTimeMillis() - entry.value > 2000L)
				{
					iterator.remove()
					continue
				}

				RenderUtils.drawBlockBox(theWorld, thePlayer, entry.key, color, boxOutlineColor, hydraESP)
			}
		}
	}

	@EventTarget
	fun onRender2D(event: Render2DEvent)
	{
		val theWorld = mc.theWorld ?: return
		val renderManager = mc.renderManager
		val renderPartialTicks = mc.timer.renderPartialTicks

		val provider = classProvider

		val mode = modeValue.get().toLowerCase()
		val shader = when (mode)
		{
			"shaderoutline" -> OutlineShader.INSTANCE
			"shaderglow" -> GlowShader.INSTANCE
			else -> null
		} ?: return

		shader.startDraw(event.partialTicks)

		try
		{
			theWorld.loadedEntityList.filter(provider::isEntityFallingBlock).forEach { renderManager.renderEntityStatic(it, renderPartialTicks, true) }
		}
		catch (ex: Exception)
		{
			ClientUtils.logger.error("An error occurred while rendering all entities for shader esp", ex)
		}

		shader.stopDraw(if (colorRainbow.get()) rainbowRGB(saturation = saturationValue.get(), brightness = brightnessValue.get()) else ColorUtils.createRGB(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get()), when (mode)
		{
			"shadowoutline" -> shaderOutlineRadius.get()
			"shaderglow" -> shaderGlowRadius.get()
			else -> 1f
		}, 1f)
	}

	override val tag: String
		get() = modeValue.get()
}
