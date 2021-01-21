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
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowFontShader
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
	private val colorModeValue = ListValue("ColorMode", arrayOf("PotionColor", "Custom", "Rainbow", "RainbowShader"), "PotionLiquidColor")
	private val redValue = IntegerValue("Red", 255, 0, 255)
	private val greenValue = IntegerValue("Green", 255, 0, 255)
	private val blueValue = IntegerValue("Blue", 255, 0, 255)
	private val alphaValue = IntegerValue("Alpha", 255, 0, 255)

	private val rainbowShaderXValue = FloatValue("RainbowShader-X", -1000F, -2000F, 2000F)
	private val rainbowShaderYValue = FloatValue("RainbowShader-Y", -1000F, -2000F, 2000F)

	private val saturationValue = FloatValue("HSB-Saturation", 0.9f, 0f, 1f)
	private val brightnessValue = FloatValue("HSB-Brightness", 1f, 0f, 1f)

	private val rainbowSpeedValue = IntegerValue("Rainbow-Speed", 10, 1, 10)

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
		val customColor = Color(redValue.get(), greenValue.get(), blueValue.get(), alphaValue.get()).rgb
		val saturation = saturationValue.get()
		val brightness = brightnessValue.get()
		val rainbowSpeed = 11 - rainbowSpeedValue.get().coerceAtLeast(1).coerceAtMost(10)
		val rainbowShaderX = if (rainbowShaderXValue.get() == 0.0F) 0.0F else 1.0F / rainbowShaderXValue.get()
		val rainbowShaderY = if (rainbowShaderYValue.get() == 0.0F) 0.0F else 1.0F / rainbowShaderYValue.get()
		val rainbowShaderOffset = System.currentTimeMillis() % 10000 / 10000F

		assumeNonVolatile = true

		// TODO: Add Rect, Background support

		for (effect in mc.thePlayer!!.activePotionEffects)
		{
			val potion = functions.getPotionById(effect.potionID)

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

			val name = "${functions.formatI18n(potion.name)} $amplifierString\u00A7f: \u00A77${effect.getDurationString()}"
			val stringWidth = fontRenderer.getStringWidth(name).toFloat()

			if (width < stringWidth) width = stringWidth

			val textRainbowShader = colorMode.equals("RainbowShader", ignoreCase = true)

			RainbowFontShader.begin(textRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
				val color = when
				{
					textRainbowShader -> 0
					colorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbow(speed = rainbowSpeed, saturation = saturation, brightness = brightness).rgb
					colorMode.equals("Custom", ignoreCase = true) -> customColor
					else -> potion.liquidColor
				}

				fontRenderer.drawString(name, -stringWidth, y, color, shadow.get())
			}

			y -= fontRenderer.fontHeight
		}

		assumeNonVolatile = false

		if (width == 0F) width = 40F

		if (y == 0F) y = -10F

		return Border(2F, fontRenderer.fontHeight.toFloat(), -width - 2F, y + fontRenderer.fontHeight - 2F)
	}
}
