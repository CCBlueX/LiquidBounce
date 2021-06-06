/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.client.hud.element.Side.Horizontal
import net.ccbluex.liquidbounce.ui.client.hud.element.Side.Vertical
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.AnimationUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.createRGB
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowFontShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.ccbluex.liquidbounce.value.*
import java.awt.Color

/**
 * CustomHUD Arraylist element
 *
 * Shows a list of enabled modules
 */
@ElementInfo(name = "Arraylist", single = true)
class Arraylist(x: Double = 1.0, y: Double = 2.0, scale: Float = 1F, side: Side = Side(Horizontal.RIGHT, Vertical.UP)) : Element(x, y, scale, side)
{

	private val colorModeValue = ListValue("Text-Color", arrayOf("Custom", "Random", "Rainbow", "RainbowShader"), "Custom")
	private val colorRedValue = IntegerValue("Text-R", 0, 0, 255)
	private val colorGreenValue = IntegerValue("Text-G", 111, 0, 255)
	private val colorBlueValue = IntegerValue("Text-B", 255, 0, 255)
	private val shadow = BoolValue("ShadowText", true)

	private val rectValue = ListValue("Rect", arrayOf("None", "Left", "Right", "Frame"), "None")
	private val rectWidthValue = FloatValue("Rect-Width", 3F, 1.5F, 5F)
	private val rectFrameWidthValue = FloatValue("Rect-Frame-Width", 3F, 1.5F, 5F)

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
		val slideAnimationSpeed = 21F - animationSpeedValue.get().coerceIn(0.01f, 20f)
		val deltaTime = RenderUtils.deltaTime

		val uppercase = upperCaseValue.get()

		LiquidBounce.moduleManager.modules.filter(Module::array).filter { it.state || it.slide != 0F }.forEach { module ->
			var moduleName = module.name
			var moduleTag = formatTag(module)

			if (uppercase)
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
		val customColor = createRGB(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get())

		// Rect Mode & Color
		val rectMode = rectValue.get()
		val frame = rectMode.equals("Frame", ignoreCase = true)
		val rightRect = rectMode.equals("Right", ignoreCase = true)
		val leftRect = rectMode.equals("Left", ignoreCase = true)

		val rectWidth = rectWidthValue.get()
		val frameWidth = rectFrameWidthValue.get()

		val rectColorMode = rectColorModeValue.get()
		val rectColorAlpha = rectColorBlueAlpha.get()
		val rectCustomColor = createRGB(rectColorRedValue.get(), rectColorGreenValue.get(), rectColorBlueValue.get(), rectColorAlpha)

		// Background Color
		val backgroundColorMode = backgroundColorModeValue.get()
		val backgroundColorAlpha = backgroundColorAlphaValue.get()
		val backgroundCustomColor = createRGB(backgroundColorRedValue.get(), backgroundColorGreenValue.get(), backgroundColorBlueValue.get(), backgroundColorAlpha)

		// Tag Mode & Color
		val tagColorMode = tagColorModeValue.get()
		val customTagColor = createRGB(tagColorRedValue.get(), tagColorGreenValue.get(), tagColorBlueValue.get())

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

					if (uppercase)
					{
						moduleName = moduleName.toUpperCase()
						moduleTag = moduleTag.toUpperCase()
					}

					val xPos = -module.slide - 2f
					val yPos = (if (verticalSide == Vertical.DOWN) -textSpacer else textSpacer) * if (verticalSide == Vertical.DOWN) index + 1 else index
					val moduleRandomColor = Color.getHSBColor(module.hue, saturation, brightness).rgb
					val currentRainbowOffset = index * rainbowOffset

					val nextModuleIndex = if (verticalSide == Vertical.DOWN) -1 else 1
					val nextModuleXPos = if (index + nextModuleIndex == modules.size) 0F else -modules[(index + nextModuleIndex).coerceIn(0, modules.size - 1)].slide - 2F

					// Draw Background
					val backgroundRainbowShader = backgroundColorMode.equals("RainbowShader", ignoreCase = true)
					RainbowShader.begin(backgroundRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
						val xPosCorrection = 2F + if (rightRect) rectWidth else 0F
						val x2Pos = if (rightRect) -rectWidth else 0F
						val color = when
						{
							backgroundRainbowShader -> -16777216
							backgroundColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbowRGB(alpha = backgroundColorAlpha, offset = currentRainbowOffset, speed = rainbowSpeed, saturation = saturation, brightness = brightness)
							backgroundColorMode.equals("Random", ignoreCase = true) -> moduleRandomColor
							else -> backgroundCustomColor
						}

						RenderUtils.drawRect(xPos - xPosCorrection, yPos, x2Pos, yPos + textHeight, color)
					}

					provider.glStateManager.resetColor()

					// Draw Module Name
					val textRainbowShader = colorMode.equals("RainbowShader", ignoreCase = true)
					RainbowFontShader.begin(textRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
						val xPosCorrection = if (rightRect) rectWidth else 0F
						val color = when
						{
							textRainbowShader -> 0
							colorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbowRGB(offset = currentRainbowOffset, speed = rainbowSpeed, saturation = saturation, brightness = brightness)
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
							val xPosCorrection = fontRenderer.getStringWidth(moduleName) + tagSpace - if (rightRect) rectWidth else 0F
							val color = when
							{
								tagRainbowShader -> 0
								tagColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbowRGB(offset = currentRainbowOffset, speed = rainbowSpeed, saturation = saturation, brightness = brightness)
								tagColorMode.equals("Random", ignoreCase = true) -> moduleRandomColor
								else -> customTagColor
							}

							fontRenderer.drawString(moduleTag, xPos + xPosCorrection, yPos + textY, color, textShadow)
						}
					}

					// Draw Rect
					if (rightRect || leftRect || frame)
					{
						val rectRainbowShader = rectColorMode.equals("RainbowShader", ignoreCase = true)
						RainbowShader.begin(rectRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
							val rectColor = when
							{
								rectRainbowShader -> 0
								rectColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbowRGB(alpha = rectColorAlpha, offset = currentRainbowOffset, speed = rainbowSpeed, saturation = saturation, brightness = brightness)
								rectColorMode.equals("Random", ignoreCase = true) -> moduleRandomColor
								else -> rectCustomColor
							}

							val frameStartX = xPos - 2F - rectWidth
							when
							{
								leftRect || frame -> RenderUtils.drawRect(frameStartX, yPos, xPos - 2F, yPos + textHeight, rectColor)
								rightRect -> RenderUtils.drawRect(-rectWidth, yPos, 0F, yPos + textHeight, rectColor)
							}

							if (frame)
							{
								val frameEndX = if (nextModuleXPos == 0F) 0F else nextModuleXPos - 2F - rectWidth
								val frameEndY = yPos + textHeight + if (frameStartX > frameEndX) -frameWidth else frameWidth

								RenderUtils.drawRect(frameStartX, yPos + textHeight, frameEndX, frameEndY, rectColor)
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

					val nextModuleIndex = if (verticalSide == Vertical.DOWN) -1 else 1
					val isLastModule = index + nextModuleIndex == modules.size
					val nextModule = modules[(index + nextModuleIndex).coerceIn(0, modules.size - 1)]

					var nextModuleName = nextModule.name
					var nextModuleTag = formatTag(nextModule)

					if (uppercase)
					{
						moduleName = moduleName.toUpperCase()
						moduleTag = moduleTag.toUpperCase()

						nextModuleName = nextModuleName.toUpperCase()
						nextModuleTag = nextModuleTag.toUpperCase()
					}

					val width = fontRenderer.getStringWidth(moduleName) + if (moduleTag.isBlank()) 0f else tagSpace + fontRenderer.getStringWidth(moduleTag)
					val xPos = -(width - module.slide) + 2F + if (leftRect) rectWidth else 0F
					val yPos = (if (verticalSide == Vertical.DOWN) -textSpacer else textSpacer) * if (verticalSide == Vertical.DOWN) index + 1 else index
					val moduleRandomColor = Color.getHSBColor(module.hue, saturation, brightness).rgb
					val currentRainbowOffset = index * rainbowOffset

					val nextModuleWidth = if (isLastModule) 0F else fontRenderer.getStringWidth(nextModuleName) + if (nextModuleTag.isBlank()) 0f else tagSpace + fontRenderer.getStringWidth(nextModuleTag)
					val nextModuleXPos = if (isLastModule) 0F else -(nextModuleWidth - nextModule.slide) + 2F

					// Draw Background
					val backgroundRainbowShader = backgroundColorMode.equals("RainbowShader", ignoreCase = true)
					RainbowShader.begin(backgroundRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
						val xPosCorrection = 2F + if (rightRect) rectWidth else 0F
						val color = when
						{
							backgroundRainbowShader -> 0
							backgroundColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbowRGB(alpha = backgroundColorAlpha, offset = currentRainbowOffset, speed = rainbowSpeed, saturation = saturation, brightness = brightness)
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
							colorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbowRGB(offset = currentRainbowOffset, speed = rainbowSpeed, saturation = saturation, brightness = brightness)
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
								tagColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbowRGB(offset = currentRainbowOffset, speed = rainbowSpeed, saturation = saturation, brightness = brightness)
								tagColorMode.equals("Random", ignoreCase = true) -> moduleRandomColor
								else -> customTagColor
							}

							fontRenderer.drawString(moduleTag, xPos + fontRenderer.getStringWidth(moduleName) + tagSpace, yPos + textY, color, textShadow)
						}
					}

					// Draw Rect
					val rectColorRainbowShader = rectColorMode.equals("RainbowShader", ignoreCase = true)

					RainbowShader.begin(rectColorRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
						if (rightRect || leftRect || frame)
						{
							val rectColor = when
							{
								rectColorRainbowShader -> 0
								rectColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbowRGB(alpha = rectColorAlpha, offset = currentRainbowOffset, speed = rainbowSpeed, saturation = saturation, brightness = brightness)
								rectColorMode.equals("Random", ignoreCase = true) -> moduleRandomColor
								else -> rectCustomColor
							}

							val frameEndX = xPos + width + 2F + rectWidth
							when
							{
								leftRect -> RenderUtils.drawRect(0F, yPos - 1F, rectWidth, yPos + textHeight, rectColor)
								rightRect || frame -> RenderUtils.drawRect(xPos + width + 2F, yPos, frameEndX, yPos + textHeight, rectColor)
							}

							if (frame)
							{
								val frameStartX = if (nextModuleXPos == 0F) 0F else (nextModuleXPos + nextModuleWidth + rectWidth + 2F)

								RenderUtils.drawRect(frameStartX, yPos + textHeight, frameEndX, yPos + textHeight + if (frameEndX < frameStartX) -frameWidth else frameWidth, rectColor)
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

		RenderUtils.resetColor()

		return null
	}

	private fun formatTag(module: Module): String
	{
		val originalTagString = if (tags.get()) module.tag ?: "" else ""

		return if (originalTagString.isNotBlank()) when (tagTypeValue.get().toLowerCase())
		{
			"square-bracket" -> "[$originalTagString\u00A7r]"
			"round-bracket" -> "($originalTagString\u00A7r)"
			else -> "$originalTagString\u00A7r"
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
