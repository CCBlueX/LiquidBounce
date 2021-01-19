/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.ui.client.hud.element.*
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FontValue

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

	private val fontValue = FontValue("Font", Fonts.font35)
	private val shadow = BoolValue("Shadow", true)

	/**
	 * Draw element
	 */
	override fun drawElement(): Border
	{
		var y = 0F
		var width = 0F

		val fontRenderer = fontValue.get()

		assumeNonVolatile = true

		for (effect in mc.thePlayer!!.activePotionEffects)
		{
			val potion = functions.getPotionById(effect.potionID)

			val number = when
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
				effect.amplifier > 10 -> "X+"
				else -> "I"
			}

			val name = "${functions.formatI18n(potion.name)} $number\u00A7f: \u00A77${effect.getDurationString()}"
			val stringWidth = fontRenderer.getStringWidth(name).toFloat()

			if (width < stringWidth) width = stringWidth

			fontRenderer.drawString(name, -stringWidth, y, potion.liquidColor, shadow.get())
			y -= fontRenderer.fontHeight
		}

		assumeNonVolatile = false

		if (width == 0F) width = 40F

		if (y == 0F) y = -10F

		return Border(2F, fontRenderer.fontHeight.toFloat(), -width - 2F, y + fontRenderer.fontHeight - 2F)
	}
}
