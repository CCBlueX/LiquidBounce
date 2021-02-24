/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.client.hud.element.*
import net.ccbluex.liquidbounce.ui.client.hud.element.Side.Horizontal
import net.ccbluex.liquidbounce.ui.client.hud.element.Side.Vertical
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.AnimationUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowFontShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.opengl.GL11
import java.awt.Color

/**
 * CustomHUD Arraylist element
 *
 * Shows a list of enabled modules
 */
@ElementInfo(name = "Arraylist", single = true)
class Arraylist(
	x: Double = 1.0, y: Double = 2.0, scale: Float = 1F, side: Side = Side(Horizontal.RIGHT, Vertical.UP)
) : Element(x, y, scale, side)
{

	private val colorModeValue = ListValue("Text-Color", arrayOf("Custom", "Random", "Rainbow", "RainbowShader"), "Custom")
	private val colorRedValue = IntegerValue("Text-R", 0, 0, 255)
	private val colorGreenValue = IntegerValue("Text-G", 111, 0, 255)
	private val colorBlueValue = IntegerValue("Text-B", 255, 0, 255)
	private val shadow = BoolValue("ShadowText", true)

	private val rectValue = ListValue("Rect", arrayOf("None", "Left", "Right"), "None")
	private val rectColorModeValue = ListValue("Rect-Color", arrayOf("Custom", "Random", "Rainbow", "RainbowShader"), "Rainbow")
	private val rectColorRedValue = IntegerValue("Rect-R", 255, 0, 255)
	private val rectColorGreenValue = IntegerValue("Rect-G", 255, 0, 255)
	private val rectColorBlueValue = IntegerValue("Rect-B", 255, 0, 255)
	private val rectColorBlueAlpha = IntegerValue("Rect-Alpha", 255, 0, 255)

	private val tags = BoolValue("Tags", true)
	private val tagTypeValue = ListValue("TagType", arrayOf("Space-Only", "Square-Bracket", "Round-Bracket"), "Space-Only")
	private val tagColorModeValue = ListValue("Tag-Color", arrayOf("Custom", "Random", "Rainbow", "RainbowShader"), "Custom")
	private val tagColorRedValue = IntegerValue("Tag-R", 180, 0, 255)
	private val tagColorGreenValue = IntegerValue("Tag-G", 180, 0, 255)
	private val tagColorBlueValue = IntegerValue("Tag-B", 180, 0, 255)
	private val tagSpaceValue = FloatValue("TagSpace", 2.5F, 1F, 5F)

	private val backgroundColorModeValue = ListValue("Background-Color", arrayOf("Custom", "Random", "Rainbow", "RainbowShader"), "Custom")
	private val backgroundColorRedValue = IntegerValue("Background-R", 0, 0, 255)
	private val backgroundColorGreenValue = IntegerValue("Background-G", 0, 0, 255)
	private val backgroundColorBlueValue = IntegerValue("Background-B", 0, 0, 255)
	private val backgroundColorAlphaValue = IntegerValue("Background-Alpha", 255, 0, 255)
	private val upperCaseValue = BoolValue("UpperCase", false)

	private val saturationValue = FloatValue("HSB-Saturation", 1f, 0f, 1f)
	private val brightnessValue = FloatValue("HSB-Brightness", 1f, 0f, 1f)

	private val rainbowSpeedValue = IntegerValue("Rainbow-Speed", 10, 1, 10)
	private val rainbowOffsetValue = IntegerValue("Rainbow-IndexOffset", 0, -100, 100)

	private val rainbowShaderXValue = FloatValue("RainbowShader-X", -1000F, -2000F, 2000F)
	private val rainbowShaderYValue = FloatValue("RainbowShader-Y", -1000F, -2000F, 2000F)

	private val spaceValue = FloatValue("Space", 0F, 0F, 5F)
	private val textHeightValue = FloatValue("TextHeight", 11F, 1F, 20F)
	private val textYValue = FloatValue("TextY", 1F, 0F, 20F)

	private val animationSpeedValue = FloatValue("AnimationSpeed", 17F, 0.1F, 20F)

	private val fontValue = FontValue("Font", Fonts.font40)

	private var x2 = 0
	private var y2 = 0F

	private var modules = emptyList<Module>()

	override fun drawElement(): Border?
	{
		val fontRenderer = fontValue.get()

		AWTFontRenderer.assumeNonVolatile = true

		val tagSpace = tagSpaceValue.get()

		// Slide animation - update every render
		val slideAnimationSpeed = 21F - animationSpeedValue.get().coerceAtLeast(0.01F).coerceAtMost(20F)
		val deltaTime = RenderUtils.deltaTime
		LiquidBounce.moduleManager.modules.filter(Module::array).filter { it.state || it.slide != 0F }.forEach { module ->
			var moduleName = module.name
			var moduleTag = formatTag(module)

			if (upperCaseValue.get())
			{
				moduleName = moduleName.toUpperCase()
				moduleTag = moduleTag.toUpperCase()
			}

			val width = fontRenderer.getStringWidth(moduleName) + if (moduleTag.isEmpty()) 0f else tagSpace + fontRenderer.getStringWidth(moduleTag)

			if (module.state)
			{
				if (module.slide < width)
				{
					module.slide = AnimationUtils.easeOut(module.slideStep, width) * width
					module.slideStep += deltaTime / slideAnimationSpeed
				}
			}
			else if (module.slide > 0)
			{
				module.slide = AnimationUtils.easeOut(module.slideStep, width) * width
				module.slideStep -= deltaTime / slideAnimationSpeed
			}

			module.slide = module.slide.coerceIn(0F, width)
			module.slideStep = module.slideStep.coerceIn(0F, width)
		}

		// Color
		val colorMode = colorModeValue.get()
		val customColor = Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get(), 1).rgb

		// Rect Mode & Color
		val rectMode = rectValue.get()
		val rectColorMode = rectColorModeValue.get()
		val rectColorAlpha = rectColorBlueAlpha.get()
		val rectCustomColor = Color(rectColorRedValue.get(), rectColorGreenValue.get(), rectColorBlueValue.get(), rectColorAlpha).rgb

		// Background Color
		val backgroundColorMode = backgroundColorModeValue.get()
		val backgroundColorAlpha = backgroundColorAlphaValue.get()
		val backgroundCustomColor = Color(backgroundColorRedValue.get(), backgroundColorGreenValue.get(), backgroundColorBlueValue.get(), backgroundColorAlpha).rgb

		// Tag Mode & Color
		val tagColorMode = tagColorModeValue.get()
		val customTagColor = Color(tagColorRedValue.get(), tagColorGreenValue.get(), tagColorBlueValue.get(), 1).rgb

		// Text Shadow
		val textShadow = shadow.get()

		// Text Y
		val textY = textYValue.get()

		// Text Spacer
		val space = spaceValue.get()
		val textHeight = textHeightValue.get()
		val textSpacer = textHeight + space

		// Rainbow
		val saturation = saturationValue.get()
		val brightness = brightnessValue.get()
		val rainbowSpeed = rainbowSpeedValue.get()
		val rainbowOffset = 400000000L + 40000000L * rainbowOffsetValue.get()

		// Rainbow Shader
		val rainbowShaderX = if (rainbowShaderXValue.get() == 0.0F) 0.0F else 1.0F / rainbowShaderXValue.get()
		val rainbowShaderY = if (rainbowShaderYValue.get() == 0.0F) 0.0F else 1.0F / rainbowShaderYValue.get()
		val rainbowShaderOffset = System.currentTimeMillis() % 10000 * 0.0001f

		val horizontalSide = side.horizontal
		val verticalSide = side.vertical

		val provider = classProvider

		// Draw arraylist
		when (horizontalSide)
		{
			Horizontal.RIGHT, Horizontal.MIDDLE ->
			{
				modules.forEachIndexed { index, module ->
					var moduleName = module.name
					var moduleTag = formatTag(module)

					if (upperCaseValue.get())
					{
						moduleName = moduleName.toUpperCase()
						moduleTag = moduleTag.toUpperCase()
					}

					val xPos = -module.slide - 2
					val yPos = (if (verticalSide == Vertical.DOWN) -textSpacer else textSpacer) * if (verticalSide == Vertical.DOWN) index + 1 else index
					val moduleRandomColor = Color.getHSBColor(module.hue, saturation, brightness).rgb
					val currentRainbowOffset = index * rainbowOffset

					// Draw Background
					val backgroundRainbowShader = backgroundColorMode.equals("RainbowShader", ignoreCase = true)
					RainbowShader.begin(backgroundRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
						val xPosCorrection = if (rectMode.equals("right", true)) 5 else 2
						val x2Pos = if (rectMode.equals("right", true)) -3F else 0F
						val color = when
						{
							backgroundRainbowShader -> -16777216
							backgroundColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbow(offset = currentRainbowOffset, alpha = backgroundColorAlpha, speed = rainbowSpeed, saturation = saturation, brightness = brightness).rgb
							backgroundColorMode.equals("Random", ignoreCase = true) -> moduleRandomColor
							else -> backgroundCustomColor
						}

						RenderUtils.drawRect(xPos - xPosCorrection, yPos, x2Pos, yPos + textHeight, color)
					}

					provider.glStateManager.resetColor()

					// Draw Module Name
					val textRainbowShader = colorMode.equals("RainbowShader", ignoreCase = true)
					RainbowFontShader.begin(textRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
						val xPosCorrection = if (rectMode.equals("right", true)) 3 else 0
						val color = when
						{
							textRainbowShader -> 0
							colorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbow(offset = currentRainbowOffset, speed = rainbowSpeed, saturation = saturation, brightness = brightness).rgb
							colorMode.equals("Random", ignoreCase = true) -> moduleRandomColor
							else -> customColor
						}

						fontRenderer.drawString(moduleName, xPos - xPosCorrection, yPos + textY, color, textShadow)
					}

					// Draw Tag
					if (moduleTag.isNotBlank())
					{
						val tagRainbowShader = tagColorMode.equals("RainbowShader", ignoreCase = true)
						RainbowFontShader.begin(tagRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
							val xPosCorrection = fontRenderer.getStringWidth(moduleName) + tagSpace - if (rectMode.equals("right", true)) 3 else 0
							val color = when
							{
								tagRainbowShader -> 0
								tagColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbow(offset = currentRainbowOffset, speed = rainbowSpeed, saturation = saturation, brightness = brightness).rgb
								tagColorMode.equals("Random", ignoreCase = true) -> moduleRandomColor
								else -> customTagColor
							}

							fontRenderer.drawString(moduleTag, xPos + xPosCorrection, yPos + textY, color, textShadow)
						}
					}

					// Draw Rect
					if (!rectMode.equals("none", true))
					{
						val rectRainbowShader = rectColorMode.equals("RainbowShader", ignoreCase = true)
						RainbowShader.begin(rectRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
							val rectColor = when
							{
								rectRainbowShader -> 0
								rectColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbow(offset = currentRainbowOffset, alpha = rectColorAlpha, speed = rainbowSpeed, saturation = saturation, brightness = brightness).rgb
								rectColorMode.equals("Random", ignoreCase = true) -> moduleRandomColor
								else -> rectCustomColor
							}

							when
							{
								rectMode.equals("left", true) -> RenderUtils.drawRect(xPos - 5, yPos, xPos - 2, yPos + textHeight, rectColor)
								rectMode.equals("right", true) -> RenderUtils.drawRect(-3F, yPos, 0F, yPos + textHeight, rectColor)
							}
						}
					}
				}
			}

			Horizontal.LEFT ->
			{
				modules.forEachIndexed { index, module ->
					var moduleName = module.name
					var moduleTag = formatTag(module)

					if (upperCaseValue.get())
					{
						moduleName = moduleName.toUpperCase()
						moduleTag = moduleTag.toUpperCase()
					}

					val width = fontRenderer.getStringWidth(moduleName) + if (moduleTag.isBlank()) 0f else tagSpace + fontRenderer.getStringWidth(moduleTag)
					val xPos = -(width - module.slide) + if (rectMode.equals("left", true)) 5 else 2
					val yPos = (if (verticalSide == Vertical.DOWN) -textSpacer else textSpacer) * if (verticalSide == Vertical.DOWN) index + 1 else index
					val moduleRandomColor = Color.getHSBColor(module.hue, saturation, brightness).rgb
					val currentRainbowOffset = index * rainbowOffset

					// Draw Background
					val backgroundRainbowShader = backgroundColorMode.equals("RainbowShader", ignoreCase = true)
					RainbowShader.begin(backgroundRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
						val xPosCorrection = if (rectMode.equals("right", true)) 5 else 2
						val color = when
						{
							backgroundRainbowShader -> 0
							backgroundColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbow(offset = currentRainbowOffset, alpha = backgroundColorAlpha, speed = rainbowSpeed, saturation = saturation, brightness = brightness).rgb
							backgroundColorMode.equals("Random", ignoreCase = true) -> moduleRandomColor
							else -> backgroundCustomColor
						}

						RenderUtils.drawRect(0F, yPos, xPos + width + xPosCorrection, yPos + textHeight, color)
					}

					provider.glStateManager.resetColor()

					// Draw Module Name
					val textRainbowShader = colorMode.equals("RainbowShader", ignoreCase = true)
					RainbowFontShader.begin(textRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
						val color = when
						{
							textRainbowShader -> 0
							colorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbow(offset = currentRainbowOffset, speed = rainbowSpeed, saturation = saturation, brightness = brightness).rgb
							colorMode.equals("Random", ignoreCase = true) -> moduleRandomColor
							else -> customColor
						}

						fontRenderer.drawString(moduleName, xPos, yPos + textY, color, textShadow)
					}

					// Draw Tag
					if (moduleTag.isNotBlank())
					{
						val tagRainbowShader = tagColorMode.equals("RainbowShader", ignoreCase = true)
						RainbowFontShader.begin(tagRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
							val color = when
							{
								tagRainbowShader -> 0
								tagColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbow(offset = currentRainbowOffset, speed = rainbowSpeed, saturation = saturation, brightness = brightness).rgb
								tagColorMode.equals("Random", ignoreCase = true) -> moduleRandomColor
								else -> customTagColor
							}

							fontRenderer.drawString(moduleTag, xPos + fontRenderer.getStringWidth(moduleName) + tagSpace, yPos + textY, color, textShadow)
						}
					}

					// Draw Rect
					val rectColorRainbowShader = rectColorMode.equals("RainbowShader", ignoreCase = true)

					RainbowShader.begin(rectColorRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
						if (!rectMode.equals("none", true))
						{
							val rectColor = when
							{
								rectColorRainbowShader -> 0
								rectColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbow(offset = currentRainbowOffset, alpha = rectColorAlpha, speed = rainbowSpeed, saturation = saturation, brightness = brightness).rgb
								rectColorMode.equals("Random", ignoreCase = true) -> moduleRandomColor
								else -> rectCustomColor
							}

							when
							{
								rectMode.equals("left", true) -> RenderUtils.drawRect(0F, yPos - 1, 3F, yPos + textHeight, rectColor)
								rectMode.equals("right", true) -> RenderUtils.drawRect(xPos + width + 2, yPos, xPos + width + 2 + 3, yPos + textHeight, rectColor)
							}
						}
					}
				}
			}
		}

		// Draw border
		if (provider.isGuiHudDesigner(mc.currentScreen))
		{
			x2 = Int.MIN_VALUE

			if (modules.isEmpty()) return if (horizontalSide == Horizontal.LEFT) Border(0F, -1F, 20F, 20F) else Border(0F, -1F, -20F, 20F)

			modules.map { it.slide.toInt() }.forEach { slide ->
				when (horizontalSide)
				{
					Horizontal.RIGHT, Horizontal.MIDDLE ->
					{
						val xPos = -slide - 2
						if (x2 == Int.MIN_VALUE || xPos < x2) x2 = xPos
					}

					Horizontal.LEFT ->
					{
						val xPos = slide + 14
						if (x2 == Int.MIN_VALUE || xPos > x2) x2 = xPos
					}
				}
			}

			y2 = (if (verticalSide == Vertical.DOWN) -textSpacer else textSpacer) * modules.size

			return Border(0F, 0F, x2 - 7F, y2 - if (verticalSide == Vertical.DOWN) 1F else 0F)
		}

		AWTFontRenderer.assumeNonVolatile = false

		GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)

		return null
	}

	private fun formatTag(module: Module): String
	{
		val originalTagString = if (tags.get()) module.tag ?: "" else ""

		return if (originalTagString.isNotBlank()) when (tagTypeValue.get().toLowerCase())
		{
			"square-bracket" -> "[$originalTagString]"
			"round-bracket" -> "($originalTagString)"
			else -> originalTagString
		}
		else originalTagString
	}

	override fun updateElement()
	{
		val font = fontValue.get()
		val tagSpace = tagSpaceValue.get()

		modules = LiquidBounce.moduleManager.modules.filter(Module::array).filter { it.slide > 0 }.sortedBy {
			var name = it.name
			var tag = formatTag(it)

			if (upperCaseValue.get())
			{
				name = name.toUpperCase()
				tag = tag.toUpperCase()
			}

			-(font.getStringWidth(name) + if (tag.isEmpty()) 0f else tagSpace + font.getStringWidth(tag))
		}
	}
}
