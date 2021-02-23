/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.api.minecraft.potion.IPotionEffect
import net.ccbluex.liquidbounce.ui.client.hud.element.*
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.ColorUtils.applyAlphaChannel
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbow
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowFontShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.ccbluex.liquidbounce.value.*
import java.awt.Color
import kotlin.math.roundToInt

/**
 * CustomHUD effects element
 *
 * Shows a list of active potion effects
 */
@ElementInfo(name = "Effects")
class Effects(
	x: Double = 2.0, y: Double = 10.0, scale: Float = 1F, side: Side = Side(Side.Horizontal.RIGHT, Side.Vertical.DOWN)
) : Element(x, y, scale, side)
{
	private val colorModeValue = ListValue("ColorMode", arrayOf("PotionColor", "Custom", "Rainbow", "RainbowShader"), "PotionColor")
	private val redValue = IntegerValue("Red", 255, 0, 255)
	private val greenValue = IntegerValue("Green", 255, 0, 255)
	private val blueValue = IntegerValue("Blue", 255, 0, 255)

	private val timeTypeValue = ListValue("TimeType", arrayOf("String", "Ticks", "Both"), "String")
	private val timeColorModeValue = ListValue("Time-Color", arrayOf("PotionColor", "Custom", "Rainbow", "RainbowShader"), "Custom")
	private val timeRedValue = IntegerValue("Time-Red", 192, 0, 255)
	private val timeGreenValue = IntegerValue("Time-Green", 192, 0, 255)
	private val timeBlueValue = IntegerValue("Time-Blue", 192, 0, 255)
	private val timeSpaceValue = FloatValue("Time-Space", 2.5F, 1F, 5F)
	private val timeHighlightValue = BoolValue("HighlightTimeWhenReachesEnd", true)

	private val rectValue = ListValue("Rect", arrayOf("None", "Left", "Right"), "None")
	private val rectColorModeValue = ListValue("Rect-Color", arrayOf("PotionColor", "Custom", "Rainbow", "RainbowShader"), "PotionColor")
	private val rectColorRedValue = IntegerValue("Rect-R", 255, 0, 255)
	private val rectColorGreenValue = IntegerValue("Rect-G", 255, 0, 255)
	private val rectColorBlueValue = IntegerValue("Rect-B", 255, 0, 255)
	private val rectColorBlueAlpha = IntegerValue("Rect-Alpha", 255, 0, 255)

	private val backgroundColorModeValue = ListValue("Background-Color", arrayOf("Custom", "PotionColor", "Rainbow", "RainbowShader"), "Custom")
	private val backgroundColorRedValue = IntegerValue("Background-R", 0, 0, 255)
	private val backgroundColorGreenValue = IntegerValue("Background-G", 0, 0, 255)
	private val backgroundColorBlueValue = IntegerValue("Background-B", 0, 0, 255)
	private val backgroundColorAlphaValue = IntegerValue("Background-Alpha", 255, 0, 255)

	private val rainbowShaderXValue = FloatValue("RainbowShader-X", -1000F, -2000F, 2000F)
	private val rainbowShaderYValue = FloatValue("RainbowShader-Y", -1000F, -2000F, 2000F)

	private val saturationValue = FloatValue("HSB-Saturation", 0.9f, 0f, 1f)
	private val brightnessValue = FloatValue("HSB-Brightness", 1f, 0f, 1f)

	private val rainbowSpeedValue = IntegerValue("Rainbow-Speed", 10, 1, 10)

	private val spaceValue = FloatValue("Space", 0F, 0F, 5F)
	private val textHeightValue = FloatValue("TextHeight", 11F, 1F, 20F)
	private val textYValue = FloatValue("TextY", 1F, 0F, 20F)
	private val shadowValue = BoolValue("Shadow", true)

	private val fontValue = FontValue("Font", Fonts.font35)

	private var effects = emptyList<IPotionEffect>()

	private var x2 = 0
	private var y2 = 0F

	/**
	 * Draw element
	 */
	override fun drawElement(): Border?
	{
		val fontRenderer = fontValue.get()

		val colorMode = colorModeValue.get()
		val customColor = Color(redValue.get(), greenValue.get(), blueValue.get(), 1).rgb

		val timeDistance = timeSpaceValue.get()
		val timeColorMode = timeColorModeValue.get()
		val timeCustomColor = Color(timeRedValue.get(), timeGreenValue.get(), timeBlueValue.get(), 1).rgb

		val backgroundColorMode = backgroundColorModeValue.get()
		val backgroundColorAlpha = backgroundColorAlphaValue.get()
		val backgroundCustomColor = Color(backgroundColorRedValue.get(), backgroundColorGreenValue.get(), backgroundColorBlueValue.get(), backgroundColorAlpha).rgb

		val rectMode = rectValue.get()
		val rectColorMode = rectColorModeValue.get()
		val rectColorAlpha = rectColorBlueAlpha.get()
		val rectCustomColor = Color(rectColorRedValue.get(), rectColorGreenValue.get(), rectColorBlueValue.get(), rectColorAlpha).rgb

		val space = spaceValue.get()
		val textHeight = textHeightValue.get()
		val textY = textYValue.get()
		val textSpacer = textHeight + space

		val saturation = saturationValue.get()
		val brightness = brightnessValue.get()
		val rainbowSpeed = rainbowSpeedValue.get()
		val rainbowShaderX = if (rainbowShaderXValue.get() == 0.0F) 0.0F else 1.0F / rainbowShaderXValue.get()
		val rainbowShaderY = if (rainbowShaderYValue.get() == 0.0F) 0.0F else 1.0F / rainbowShaderYValue.get()
		val rainbowShaderOffset = System.currentTimeMillis() % 10000 * 0.0001f

		val shadow = shadowValue.get()

		val backgroundRainbowShader = backgroundColorMode.equals("RainbowShader", ignoreCase = true)
		val textRainbowShader = colorMode.equals("RainbowShader", ignoreCase = true)
		val timeRainbowShader = timeColorMode.equals("RainbowShader", ignoreCase = true)
		val rectRainbowShader = rectColorMode.equals("RainbowShader", ignoreCase = true)

		assumeNonVolatile = true

		val provider = classProvider

		effects.forEachIndexed { index, effect ->
			val potionID = effect.potionID
			val potion = functions.getPotionById(potionID)
			val potionColor = applyAlphaChannel(potion.liquidColor, 255) // Apply default alpha channel

			val string = formatEffect(effect)
			val timeString = formatRemainingTime(effect)
			val width = fontRenderer.getStringWidth(string).toFloat() + timeDistance + fontRenderer.getStringWidth(timeString)

			val backgroundColor = when
			{
				backgroundRainbowShader -> 0
				backgroundColorMode.equals("Rainbow", ignoreCase = true) -> rainbow(alpha = backgroundColorAlpha, speed = rainbowSpeed, saturation = saturation, brightness = brightness).rgb
				backgroundColorMode.equals("PotionColor", ignoreCase = true) -> applyAlphaChannel(potionColor, backgroundColorAlpha)
				else -> backgroundCustomColor
			}

			val textColor = when
			{
				textRainbowShader -> 0
				colorMode.equals("Rainbow", ignoreCase = true) -> rainbow(speed = rainbowSpeed, saturation = saturation, brightness = brightness).rgb
				colorMode.equals("PotionColor", ignoreCase = true) -> potionColor
				else -> customColor
			}

			val timeColor = if (timeHighlightValue.get() && effect.duration <= 300) Color.red.rgb
			else when
			{
				timeRainbowShader -> 0
				timeColorMode.equals("Rainbow", ignoreCase = true) -> rainbow(speed = rainbowSpeed, saturation = saturation, brightness = brightness).rgb
				timeColorMode.equals("PotionColor", ignoreCase = true) -> potionColor
				else -> timeCustomColor
			}

			val rectColor = when
			{
				rectRainbowShader -> 0
				rectColorMode.equals("Rainbow", ignoreCase = true) -> rainbow(alpha = rectColorAlpha, speed = rainbowSpeed, saturation = saturation, brightness = brightness).rgb
				rectColorMode.equals("PotionColor", ignoreCase = true) -> applyAlphaChannel(potionColor, rectColorAlpha)
				else -> rectCustomColor
			}

			when (side.horizontal)
			{
				Side.Horizontal.MIDDLE, Side.Horizontal.RIGHT ->
				{
					val xPos = -width - 2
					val yPos = (if (side.vertical == Side.Vertical.DOWN) -textSpacer else textSpacer) * if (side.vertical == Side.Vertical.DOWN) index + 1 else index

					// Draw Background
					RainbowShader.begin(backgroundRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
						val xPosCorrection = if (rectMode.equals("right", true)) 5 else 2
						val x2Pos = if (rectMode.equals("right", true)) -3F else 0F

						RenderUtils.drawRect(xPos - xPosCorrection, yPos, x2Pos, yPos + textHeight, backgroundColor)
					}

					// Draw String
					RainbowFontShader.begin(textRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
						val xPosCorrection = if (rectMode.equals("right", true)) 3 else 0

						fontRenderer.drawString(string, xPos - xPosCorrection, yPos + textY, textColor, shadow)
					}

					// Draw remaining time
					RainbowFontShader.begin(timeRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
						val xPosCorrection = fontRenderer.getStringWidth(string) + timeDistance - if (rectMode.equals("right", true)) 3 else 0

						fontRenderer.drawString(timeString, xPos + xPosCorrection, yPos + textY, timeColor, shadow)
					}

					// Draw Rect
					if (!rectMode.equals("none", true)) RainbowShader.begin(rectRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
						when
						{
							rectMode.equals("left", true) -> RenderUtils.drawRect(xPos - 5, yPos, xPos - 2, yPos + textHeight, rectColor)
							rectMode.equals("right", true) -> RenderUtils.drawRect(-3F, yPos, 0F, yPos + textHeight, rectColor)
						}
					}
				}

				Side.Horizontal.LEFT ->
				{
					val xPos = (if (rectMode.equals("left", true)) 5f else 2f)
					val yPos = (if (side.vertical == Side.Vertical.DOWN) -textSpacer else textSpacer) * if (side.vertical == Side.Vertical.DOWN) index + 1 else index

					// Draw Background
					RainbowShader.begin(backgroundRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
						val xPosCorrection = if (rectMode.equals("right", true)) 5 else 2

						RenderUtils.drawRect(0F, yPos, xPos + width + xPosCorrection, yPos + textHeight, backgroundColor)
					}

					provider.glStateManager.resetColor()

					// Draw String
					RainbowFontShader.begin(textRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
						fontRenderer.drawString(string, xPos, yPos + textY, textColor, shadow)
					}

					// Draw remaining time
					RainbowFontShader.begin(timeRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
						fontRenderer.drawString(timeString, xPos + fontRenderer.getStringWidth(string) + timeDistance, yPos + textY, timeColor, shadow)
					}

					// Draw Rect
					if (!rectMode.equals("none", true)) RainbowShader.begin(rectRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
						when
						{
							rectMode.equals("left", true) -> RenderUtils.drawRect(0F, yPos - 1, 3F, yPos + textHeight, rectColor)
							rectMode.equals("right", true) -> RenderUtils.drawRect(xPos + width + 2, yPos, xPos + width + 2 + 3, yPos + textHeight, rectColor)
						}
					}
				}
			}
		}

		// Draw border
		if (provider.isGuiHudDesigner(mc.currentScreen))
		{
			x2 = Int.MIN_VALUE

			if (effects.isEmpty())
			{
				return if (side.horizontal == Side.Horizontal.LEFT) Border(0F, -1F, 20F, 20F)
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

			return Border(0F, 0F, x2 - 7F, y2 - if (side.vertical == Side.Vertical.DOWN) 1F else 0F)
		}

		assumeNonVolatile = false

		return null
	}

	override fun updateElement()
	{
		val font = fontValue.get()
		val timeDistance = timeSpaceValue.get()

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
		return when (timeTypeValue.get().toLowerCase())
		{
			"ticks" -> "${effect.duration} ticks"
			"both" -> "${effect.getDurationString()} (${effect.duration} ticks)"
			else -> effect.getDurationString()
		}
	}
}
