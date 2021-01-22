/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.ui.client.hud.element.*
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowFontShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.ccbluex.liquidbounce.value.*
import java.awt.Color

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
	private val backgroundColorAlphaValue = IntegerValue("Background-Alpha", 0, 0, 255)

	private val rainbowShaderXValue = FloatValue("RainbowShader-X", -1000F, -2000F, 2000F)
	private val rainbowShaderYValue = FloatValue("RainbowShader-Y", -1000F, -2000F, 2000F)

	private val saturationValue = FloatValue("HSB-Saturation", 0.9f, 0f, 1f)
	private val brightnessValue = FloatValue("HSB-Brightness", 1f, 0f, 1f)

	private val rainbowSpeedValue = IntegerValue("Rainbow-Speed", 10, 1, 10)

	private val spaceValue = FloatValue("Space", 0F, 0F, 5F)
	private val textHeightValue = FloatValue("TextHeight", 11F, 1F, 20F)
	private val textYValue = FloatValue("TextY", 1F, 0F, 20F)
	private val shadow = BoolValue("Shadow", true)

	private val fontValue = FontValue("Font", Fonts.font35)

	/**
	 * Draw element
	 */
	override fun drawElement(): Border
	{
		var y = 0F
		var width = 0F

		val fontRenderer = fontValue.get()

		val colorMode = colorModeValue.get()
		val customColor = Color(redValue.get(), greenValue.get(), blueValue.get(), 1).rgb

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
		val rainbowSpeed = 11 - rainbowSpeedValue.get().coerceAtLeast(1).coerceAtMost(10)
		val rainbowShaderX = if (rainbowShaderXValue.get() == 0.0F) 0.0F else 1.0F / rainbowShaderXValue.get()
		val rainbowShaderY = if (rainbowShaderYValue.get() == 0.0F) 0.0F else 1.0F / rainbowShaderYValue.get()
		val rainbowShaderOffset = System.currentTimeMillis() % 10000 / 10000F

		val backgroundRainbowShader = backgroundColorMode.equals("RainbowShader", ignoreCase = true)
		val textRainbowShader = colorMode.equals("RainbowShader", ignoreCase = true)
		val rectRainbowShader = rectColorMode.equals("RainbowShader", ignoreCase = true)

		assumeNonVolatile = true

		mc.thePlayer!!.activePotionEffects.forEachIndexed { index, effect ->
			val potion = functions.getPotionById(effect.potionID)
			val potionColor = potion.liquidColor

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

			val string = "${functions.formatI18n(potion.name)} $amplifierString\u00A7f: \u00A77${effect.getDurationString()}"
			val stringWidth = fontRenderer.getStringWidth(string).toFloat()

			if (width < stringWidth) width = stringWidth

			when (side.horizontal)
			{
				Side.Horizontal.MIDDLE, Side.Horizontal.RIGHT ->
				{
					val xPos = -stringWidth - 2
					val yPos = (if (side.vertical == Side.Vertical.DOWN) -textSpacer else textSpacer) * if (side.vertical == Side.Vertical.DOWN) index + 1 else index

					// Draw Background
					RainbowShader.begin(backgroundRainbowShader, rainbowShaderX, rainbowShaderY, System.currentTimeMillis() % 10000 / 10000F).use {
						val xPosCorrection = if (rectMode.equals("right", true)) 5 else 2
						val x2Pos = if (rectMode.equals("right", true)) -3F else 0F
						val color = when
						{
							backgroundRainbowShader -> -16777216
							backgroundColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbow(backgroundColorAlpha, speed = rainbowSpeed, saturation = saturation, brightness = brightness).rgb
							backgroundColorMode.equals("Custom", ignoreCase = true) -> backgroundCustomColor
							else -> potionColor
						}

						RenderUtils.drawRect(xPos - xPosCorrection, yPos, x2Pos, yPos + textHeight, color)
					}

					// Draw String
					RainbowFontShader.begin(textRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
						val xPosCorrection = if (rectMode.equals("right", true)) 3 else 0
						val color = when
						{
							textRainbowShader -> 0
							colorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbow(speed = rainbowSpeed, saturation = saturation, brightness = brightness).rgb
							colorMode.equals("Custom", ignoreCase = true) -> customColor
							else -> potionColor
						}

						fontRenderer.drawString(string, xPos - xPosCorrection, yPos + textY, color, shadow.get())
					}

					// Draw Rect
					if (!rectMode.equals("none", true)) RainbowShader.begin(rectRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
						val rectColor = when
						{
							rectRainbowShader -> 0
							rectColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbow(rectColorAlpha, speed = rainbowSpeed, saturation = saturation, brightness = brightness).rgb
							rectColorMode.equals("Custom", ignoreCase = true) -> customColor
							else -> potionColor
						}

						when
						{
							rectMode.equals("left", true) -> RenderUtils.drawRect(xPos - 5, yPos, xPos - 2, yPos + textHeight, rectColor)
							rectMode.equals("right", true) -> RenderUtils.drawRect(-3F, yPos, 0F, yPos + textHeight, rectColor)
						}
					}
				}

				Side.Horizontal.LEFT ->
				{
					val xPos = (if (rectMode.equals("left", true)) 5 else 2).toFloat()
					val yPos = (if (side.vertical == Side.Vertical.DOWN) -textSpacer else textSpacer) * if (side.vertical == Side.Vertical.DOWN) index + 1 else index

					// Draw Background
					RainbowShader.begin(backgroundRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
						val xPosCorrection = if (rectMode.equals("right", true)) 5 else 2
						val color = when
						{
							backgroundRainbowShader -> 0
							backgroundColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbow(backgroundColorAlpha, speed = rainbowSpeed, saturation = saturation, brightness = brightness).rgb
							backgroundColorMode.equals("Custom", ignoreCase = true) -> backgroundCustomColor
							else -> potionColor
						}

						RenderUtils.drawRect(0F, yPos, xPos + width + xPosCorrection, yPos + textHeight, color)
					}

					classProvider.getGlStateManager().resetColor()

					// Draw String
					RainbowFontShader.begin(textRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
						val color = when
						{
							textRainbowShader -> 0
							colorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbow(speed = rainbowSpeed, saturation = saturation, brightness = brightness).rgb
							colorMode.equals("Custom", ignoreCase = true) -> customColor
							else -> potionColor
						}

						fontRenderer.drawString(string, xPos, yPos + textY, color, shadow.get())
					}

					// Draw Rect
					RainbowShader.begin(rectRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
						if (!rectMode.equals("none", true))
						{
							val rectColor = when
							{
								rectRainbowShader -> 0
								rectColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbow(rectColorAlpha, speed = rainbowSpeed, saturation = saturation, brightness = brightness).rgb
								rectColorMode.equals("Custom", ignoreCase = true) -> customColor
								else -> potionColor
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

			y -= fontRenderer.fontHeight
		}

		assumeNonVolatile = false

		if (width == 0F) width = 40F

		if (y == 0F) y = -10F

		return Border(2F, fontRenderer.fontHeight.toFloat(), -width - 2F, y + fontRenderer.fontHeight - 2F)
	}
}
