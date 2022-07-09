/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.api.minecraft.potion.IPotionEffect
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.ui.font.assumeNonVolatile
import net.ccbluex.liquidbounce.utils.render.ColorUtils.applyAlphaChannel
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbowRGB
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowFontShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.ccbluex.liquidbounce.value.*
import kotlin.math.roundToInt

/**
 * CustomHUD effects element
 *
 * Shows a list of active potion effects
 */
@ElementInfo(name = "Effects")
class Effects(x: Double = 2.0, y: Double = 10.0, scale: Float = 1F, side: Side = Side(Side.Horizontal.RIGHT, Side.Vertical.DOWN)) : Element(x, y, scale, side)
{
	private val textGroup = ValueGroup("Text")

	private val textShadowValue = BoolValue("Shadow", true, "Shadow")

	private val textSpaceValue = FloatValue("Space", 0F, 0F, 5F, "Space")
	private val textHeightValue = FloatValue("Height", 11F, 1F, 20F, "TextHeight")
	private val textYValue = FloatValue("Y", 1F, 0F, 20F, "TextY")

	private val textColorGroup = ValueGroup("Color")
	private val textColorModeValue = ListValue("Mode", arrayOf("PotionColor", "Custom", "Rainbow", "RainbowShader"), "PotionColor", "ColorMode")
	private val textColorValue = RGBColorValue("Color", 255, 255, 255, Triple("Red", "Green", "Blue"))

	private val textTimeGroup = ValueGroup("Time")
	private val textTimeModeValue = ListValue("Mode", arrayOf("String", "Ticks", "Both"), "String", "TimeType")
	private val textTimeSpaceValue = FloatValue("Space", 2.5F, 1F, 5F, "Time-Space")

	private val textTimeColorGroup = ValueGroup("Color")
	private val textTimeColorModeValue = ListValue("Mode", arrayOf("PotionColor", "Custom", "Rainbow", "RainbowShader"), "Custom", "Time-Color")
	private val textTimeColorValue = RGBColorValue("Color", 192, 192, 192, Triple("Time-Red", "Time-Green", "Time-Blue"))
	private val textTimeHighlightValue = BoolValue("HighlightTimeWhenReachesEnd", true, "HighlightTimeWhenReachesEnd")

	private val rectGroup = ValueGroup("Rect")
	private val rectModeValue = ListValue("Mode", arrayOf("None", "Left", "Right"), "None", "Rect")
	private val rectWidthValue = FloatValue("Width", 3F, 1.5F, 5F, "Rect-Width")

	private val rectColorGroup = ValueGroup("Color")
	private val rectColorModeValue = ListValue("Mode", arrayOf("PotionColor", "Custom", "Rainbow", "RainbowShader"), "PotionColor", "Rect-Color")
	private val rectColorValue = RGBAColorValue("Color", 255, 255, 255, 255, listOf("Rect-R", "Rect-G", "Rect-B", "Rect-Alpha"))

	private val backgroundColorGroup = ValueGroup("BackgroundColor")
	private val backgroundColorModeValue = ListValue("Mode", arrayOf("Custom", "PotionColor", "Rainbow", "RainbowShader"), "Custom", "Background-Color")
	private val backgroundColorValue = RGBAColorValue("Color", 0, 0, 0, 255, listOf("Background-R", "Background-G", "Background-B", "Background-Alpha"))

	private val rainbowShaderGroup = object : ValueGroup("RainbowShader")
	{
		override fun showCondition() = arrayOf(textColorModeValue.get(), textTimeColorModeValue.get(), rectColorModeValue.get(), backgroundColorModeValue.get()).any { it.equals("RainbowShader", ignoreCase = true) }
	}
	private val rainbowShaderXValue = FloatValue("X", -1000F, -2000F, 2000F, "RainbowShader-X")
	private val rainbowShaderYValue = FloatValue("Y", -1000F, -2000F, 2000F, "RainbowShader-Y")

	private val rainbowGroup = object : ValueGroup("Rainbow")
	{
		override fun showCondition() = arrayOf(textColorModeValue.get(), textTimeColorModeValue.get(), rectColorModeValue.get(), backgroundColorModeValue.get()).any { it.equals("Rainbow", ignoreCase = true) }
	}
	private val rainbowSpeedValue = IntegerValue("Speed", 10, 1, 10, "Rainbow-Speed")
	private val rainbowSaturationValue = FloatValue("Saturation", 0.9f, 0f, 1f, "HSB-Saturation")
	private val rainbowBrightnessValue = FloatValue("Brightness", 1f, 0f, 1f, "HSB-Brightness")

	private val fontValue = FontValue("Font", Fonts.font35)

	init
	{
		textColorGroup.addAll(textColorModeValue, textColorValue)
		textTimeColorGroup.addAll(textTimeColorModeValue, textTimeColorValue, textTimeHighlightValue)
		textTimeGroup.addAll(textTimeModeValue, textTimeSpaceValue, textTimeColorGroup)
		textGroup.addAll(textColorGroup, textTimeGroup, textSpaceValue, textHeightValue, textYValue)

		rectColorGroup.addAll(rectColorModeValue, rectColorValue)
		rectGroup.addAll(rectModeValue, rectWidthValue, rectColorGroup)

		backgroundColorGroup.addAll(backgroundColorModeValue, backgroundColorValue)

		rainbowShaderGroup.addAll(rainbowShaderXValue, rainbowShaderYValue)
		rainbowGroup.addAll(rainbowSpeedValue, rainbowSaturationValue, rainbowBrightnessValue)
	}

	private var effects = emptyList<IPotionEffect>()

	private var x2 = 0
	private var y2 = 0F

	/**
	 * Draw element
	 */
	override fun drawElement(): Border?
	{
		val fontRenderer = fontValue.get()

		val textColorMode = textColorModeValue.get()
		val textCustomColor = textColorValue.get()

		val timeDistance = textTimeSpaceValue.get()
		val timeColorMode = textTimeColorModeValue.get()
		val timeCustomColor = textTimeColorValue.get()

		val backgroundColorMode = backgroundColorModeValue.get()
		val backgroundCustomColor = backgroundColorValue.get()

		val rectMode = rectModeValue.get()
		val rightRect = rectMode.equals("right", true)
		val leftRect = rectMode.equals("left", true)

		val rectWidth = rectWidthValue.get()

		val rectColorMode = rectColorModeValue.get()
		val rectCustomColor = rectColorValue.get()

		val space = textSpaceValue.get()
		val textHeight = textHeightValue.get()
		val textY = textYValue.get()
		val textSpacer = textHeight + space

		val saturation = rainbowSaturationValue.get()
		val brightness = rainbowBrightnessValue.get()
		val rainbowSpeed = rainbowSpeedValue.get()
		val rainbowShaderX = if (rainbowShaderXValue.get() == 0.0F) 0.0F else 1.0F / rainbowShaderXValue.get()
		val rainbowShaderY = if (rainbowShaderYValue.get() == 0.0F) 0.0F else 1.0F / rainbowShaderYValue.get()
		val rainbowShaderOffset = System.currentTimeMillis() % 10000 * 0.0001f

		val textShadow = textShadowValue.get()

		assumeNonVolatile {
			val provider = classProvider

			val renderText = { text: String, x: Float, y: Float, colorMode: String, potionColor: Int, customColor: Int, function: (Int) -> Int ->
				var useRainbowShader = false
				val color = function(when (colorMode.toLowerCase())
				{
					"rainbowshader" ->
					{
						useRainbowShader = true
						16777215
					}

					"rainbow" -> rainbowRGB(speed = rainbowSpeed, saturation = saturation, brightness = brightness)
					"potioncolor" -> potionColor
					else -> customColor
				})

				RainbowFontShader.begin(useRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
					fontRenderer.drawString(text, x, y, color, textShadow)
				}
			}

			val renderRect = { xStart: Float, yStart: Float, xEnd: Float, yEnd: Float, colorMode: String, potionColor: Int, customColor: Int ->
				var useRainbowShader = false
				val color = when (colorMode.toLowerCase())
				{
					"rainbowshader" ->
					{
						useRainbowShader = true
						0
					}

					"rainbow" -> rainbowRGB(alpha = customColor shr 24 and 0xFF, speed = rainbowSpeed, saturation = saturation, brightness = brightness)
					"potioncolor" -> applyAlphaChannel(potionColor, customColor shr 24 and 0xFF)
					else -> customColor
				}

				RainbowShader.begin(useRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
					RenderUtils.drawRect(xStart, yStart, xEnd, yEnd, color)
				}
			}

			val renderFunc: (index: Int, width: Float, string: String, timeString: String, remainingTime: Int, potionColor: Int) -> Unit = when (side.horizontal)
			{
				Side.Horizontal.MIDDLE, Side.Horizontal.RIGHT -> { index, width, string, timeString, remainingTime, potionColor ->
					val xPos = -width - 2
					val yPos = (if (side.vertical == Side.Vertical.DOWN) -textSpacer else textSpacer) * if (side.vertical == Side.Vertical.DOWN) index + 1 else index

					// Draw Background
					renderRect(xPos - (2F + if (rightRect) rectWidth else 0F), yPos, if (rightRect) -3F else 0F, yPos + textHeight, backgroundColorMode, potionColor, backgroundCustomColor)

					// Draw String
					renderText(string, xPos - if (rightRect) rectWidth else 0F, yPos + textY, textColorMode, potionColor, textCustomColor) { it }

					// Draw remaining time
					renderText(timeString, xPos + (fontRenderer.getStringWidth(string) + timeDistance - if (rightRect) rectWidth else 0F), yPos + textY, timeColorMode, potionColor, timeCustomColor) { if (textTimeHighlightValue.get() && remainingTime <= 300) -65536 else it }

					// Draw Rect
					if (leftRect || rightRect) when
					{
						leftRect -> renderRect(xPos - 2F - rectWidth, yPos, xPos - 2F, yPos + textHeight, rectColorMode, potionColor, rectCustomColor)
						rightRect -> renderRect(-rectWidth, yPos, 0F, yPos + textHeight, rectColorMode, potionColor, rectCustomColor)
					}
				}

				Side.Horizontal.LEFT -> { index, width, string, timeString, remainingTime, potionColor ->
					val xPos = (if (leftRect) 5f else 2f)
					val yPos = (if (side.vertical == Side.Vertical.DOWN) -textSpacer else textSpacer) * if (side.vertical == Side.Vertical.DOWN) index + 1 else index

					// Draw Background
					renderRect(0F, yPos, xPos + width + (2F + if (rightRect) rectWidth else 0F), yPos + textHeight, backgroundColorMode, potionColor, backgroundCustomColor)

					provider.glStateManager.resetColor()

					// Draw String
					renderText(string, xPos, yPos + textY, textColorMode, potionColor, textCustomColor) { it }

					// Draw remaining time
					renderText(timeString, xPos + fontRenderer.getStringWidth(string) + timeDistance, yPos + textY, timeColorMode, potionColor, timeCustomColor) { if (textTimeHighlightValue.get() && remainingTime <= 300) -65536 else it }

					// Draw Rect
					if (leftRect || rightRect) when
					{
						leftRect -> renderRect(0F, yPos - 1, rectWidth, yPos + textHeight, rectColorMode, potionColor, rectCustomColor)
						rightRect -> renderRect(xPos + width + 2F, yPos, xPos + width + 2F + rectWidth, yPos + textHeight, rectColorMode, potionColor, rectCustomColor)
					}
				}
			}

			effects.forEachIndexed { index, effect ->
				val string = formatEffect(effect)
				val timeString = formatRemainingTime(effect)

				renderFunc(index, fontRenderer.getStringWidth(string).toFloat() + timeDistance + fontRenderer.getStringWidth(timeString), string, timeString, effect.duration, applyAlphaChannel(functions.getPotionById(effect.potionID).liquidColor, 255))
			}

			// Draw border
			if (provider.isGuiHudDesigner(mc.currentScreen))
			{
				x2 = Int.MIN_VALUE

				if (effects.isEmpty())
				{
					return@drawElement if (side.horizontal == Side.Horizontal.LEFT) Border(0F, -1F, 20F, 20F)
					else Border(0F, -1F, -20F, 20F)
				}

				effects.map { (fontRenderer.getStringWidth(formatEffect(it)) + timeDistance + fontRenderer.getStringWidth(formatRemainingTime(it))).roundToInt() }.forEach {
					when (side.horizontal)
					{
						Side.Horizontal.RIGHT, Side.Horizontal.MIDDLE ->
						{
							val xPos = -it - 2
							if (x2 == Int.MIN_VALUE || xPos < x2) x2 = xPos
						}

						Side.Horizontal.LEFT ->
						{
							val xPos = it + 14
							if (x2 == Int.MIN_VALUE || xPos > x2) x2 = xPos
						}
					}
				}

				y2 = (if (side.vertical == Side.Vertical.DOWN) -textSpacer else textSpacer) * effects.size

				return@drawElement Border(0F, 0F, x2 - 7F, y2 - if (side.vertical == Side.Vertical.DOWN) 1F else 0F)
			}
		}

		return null
	}

	override fun updateElement()
	{
		val font = fontValue.get()
		val timeDistance = textTimeSpaceValue.get()

		effects = (mc.thePlayer ?: return).activePotionEffects.sortedBy {
			-font.getStringWidth(formatEffect(it) + timeDistance + formatRemainingTime(it))
		}
	}

	private fun formatEffect(effect: IPotionEffect): String
	{
		val func = functions

		val potion = func.getPotionById(effect.potionID)

		val amplifierString = when
		{
			effect.amplifier == 1 -> "II"
			effect.amplifier == 2 -> "III"
			effect.amplifier == 3 -> "IV"
			effect.amplifier == 4 -> "V"
			effect.amplifier == 5 -> "VI"
			effect.amplifier == 6 -> "VII"
			effect.amplifier == 7 -> "VIII"
			effect.amplifier == 8 -> "IX"
			effect.amplifier == 9 -> "X"
			effect.amplifier > 10 -> "* ${effect.amplifier + 1}"
			else -> "I"
		}

		return "${func.formatI18n(potion.name)} $amplifierString"
	}

	private fun formatRemainingTime(effect: IPotionEffect): String
	{
		return when (textTimeModeValue.get().toLowerCase())
		{
			"ticks" -> "${effect.duration} ticks"
			"both" -> "${effect.getDurationString()} (${effect.duration} ticks)"
			else -> effect.getDurationString()
		}
	}
}
