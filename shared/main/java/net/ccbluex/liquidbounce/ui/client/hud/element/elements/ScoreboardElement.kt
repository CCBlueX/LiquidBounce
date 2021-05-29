/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.api.minecraft.scoreboard.IScoreObjective
import net.ccbluex.liquidbounce.api.minecraft.util.WEnumChatFormatting
import net.ccbluex.liquidbounce.features.module.modules.render.NoScoreboard
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.createRGB
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowFontShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.ccbluex.liquidbounce.value.*
import kotlin.math.min

/**
 * CustomHUD scoreboard
 *
 * Allows to move and customize minecraft scoreboard
 */
@ElementInfo(name = "Scoreboard", force = true)
class ScoreboardElement(x: Double = 5.0, y: Double = 0.0, scale: Float = 1F, side: Side = Side(Side.Horizontal.RIGHT, Side.Vertical.MIDDLE)) : Element(x, y, scale, side)
{
	private val textRedValue = IntegerValue("Text-R", 255, 0, 255)
	private val textGreenValue = IntegerValue("Text-G", 255, 0, 255)
	private val textBlueValue = IntegerValue("Text-B", 255, 0, 255)

	private val titleColorModeValue = ListValue("Title-Color", arrayOf("Custom", "Rainbow", "RainbowShader"), "Custom")
	private val titleColorRedValue = IntegerValue("Title-R", 255, 0, 255)
	private val titleColorGreenValue = IntegerValue("Title-G", 255, 0, 255)
	private val titleColorBlueValue = IntegerValue("Title-B", 255, 0, 255)

	private val backgroundColorRedValue = IntegerValue("Background-R", 0, 0, 255)
	private val backgroundColorGreenValue = IntegerValue("Background-G", 0, 0, 255)
	private val backgroundColorBlueValue = IntegerValue("Background-B", 0, 0, 255)
	private val backgroundColorAlphaValue = IntegerValue("Background-Alpha", 95, 0, 255)

	private val rectValue = ListValue("Rect", arrayOf("None", "Left", "Right"), "None")
	private val rectWidthValue = FloatValue("Rect-Width", 3F, 1.5F, 5F)

	private val rectColorModeValue = ListValue("Rect-Color", arrayOf("Custom", "Rainbow", "RainbowShader"), "Custom")
	private val rectColorRedValue = IntegerValue("Rect-R", 0, 0, 255)
	private val rectColorGreenValue = IntegerValue("Rect-G", 111, 0, 255)
	private val rectColorBlueValue = IntegerValue("Rect-B", 255, 0, 255)
	private val rectColorBlueAlpha = IntegerValue("Rect-Alpha", 255, 0, 255)

	private val saturationValue = FloatValue("HSB-Saturation", 0.9f, 0f, 1f)
	private val brightnessValue = FloatValue("HSB-Brightness", 1f, 0f, 1f)

	private val rainbowSpeedValue = IntegerValue("Rainbow-Speed", 10, 1, 10)
	private val rainbowOffsetValue = IntegerValue("Rainbow-IndexOffset", 0, -100, 100)

	private val rainbowShaderXValue = FloatValue("RainbowShader-X", -1000F, -2000F, 2000F)
	private val rainbowShaderYValue = FloatValue("RainbowShader-Y", -1000F, -2000F, 2000F)

	private val shadowValue = BoolValue("Shadow", false)
	private val fontValue = FontValue("Font", Fonts.minecraftFont)

	/**
	 * Draw element
	 */
	override fun drawElement(): Border?
	{
		if (NoScoreboard.state) return null

		val worldScoreboard = (mc.theWorld ?: return null).scoreboard

		var currObjective: IScoreObjective? = null
		val playerTeam = worldScoreboard.getPlayersTeam((mc.thePlayer ?: return null).name)

		if (playerTeam != null)
		{
			val colorIndex = playerTeam.chatFormat.colorIndex

			if (colorIndex >= 0) currObjective = worldScoreboard.getObjectiveInDisplaySlot(3 + colorIndex)
		}

		val objective = currObjective ?: worldScoreboard.getObjectiveInDisplaySlot(1) ?: return null

		val fontRenderer = fontValue.get()
		val fontHeight = fontRenderer.fontHeight.toFloat()

		val saturation = saturationValue.get()
		val brightness = brightnessValue.get()
		val rainbowSpeed = rainbowSpeedValue.get()

		val rainbowShaderX = if (rainbowShaderXValue.get() == 0.0F) 0.0F else 1.0F / rainbowShaderXValue.get()
		val rainbowShaderY = if (rainbowShaderYValue.get() == 0.0F) 0.0F else 1.0F / rainbowShaderYValue.get()
		val rainbowShaderOffset = System.currentTimeMillis() % 10000 * 0.0001f

		val textColor = createRGB(textRedValue.get(), textGreenValue.get(), textBlueValue.get())
		val backColor = createRGB(backgroundColorRedValue.get(), backgroundColorGreenValue.get(), backgroundColorBlueValue.get(), backgroundColorAlphaValue.get())

		val titleColorMode = titleColorModeValue.get()

		val titleRainbowShader = titleColorMode.equals("RainbowShader", ignoreCase = true)

		val titleColor = when
		{
			titleRainbowShader -> 0
			titleColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbowRGB(offset = 400000000L, speed = rainbowSpeed, saturation = saturation, brightness = brightness)
			else -> createRGB(titleColorRedValue.get(), titleColorGreenValue.get(), titleColorBlueValue.get())
		}

		val rainbowOffset = 400000000L + 40000000L * rainbowOffsetValue.get()

		val rectColorMode = rectColorModeValue.get()
		val rectCustomColor = createRGB(rectColorRedValue.get(), rectColorGreenValue.get(), rectColorBlueValue.get(), rectColorBlueAlpha.get())

		val rectRainbowShader = rectColorMode.equals("RainbowShader", ignoreCase = true)

		val scoreboard = objective.scoreboard

		var scoreCollection = scoreboard.getSortedScores(objective)
		val scoreCollectionSize = scoreCollection.size

		val scores = scoreCollection.filter { !it.playerName.startsWith("#") }
		val scoresSize = scores.size

		scoreCollection = if (scoresSize > 15) scores.subList(min(scoresSize, scoreCollectionSize - 15), scoresSize) else scores

		var maxWidth = fontRenderer.getStringWidth(objective.displayName)

		val func = functions

		maxWidth = maxWidth.coerceAtLeast(scoreCollection.map { score ->
			val playerName = score.playerName
			fontRenderer.getStringWidth("${func.scoreboardFormatPlayerName(scoreboard.getPlayersTeam(playerName), playerName)}: ${WEnumChatFormatting.RED}${score.scorePoints}")
		}.min() ?: -1)

		val maxHeight = scoreCollectionSize * fontHeight

		val rectMode = rectValue.get()

		val leftRect = rectMode.equals("Left", ignoreCase = true)
		val rightRect = rectMode.equals("Right", ignoreCase = true)

		val rectWidth = rectWidthValue.get()

		val backgroundXStart = -maxWidth - 3F + if (leftRect) rectWidth else if (rightRect) -rectWidth else 0F
		val backgroundXEnd = 5F + if (leftRect) rectWidth else if (rightRect) -rectWidth else 0F

		RenderUtils.drawRect(backgroundXStart - 2F, -2F, backgroundXEnd, maxHeight + fontHeight, backColor)

		val shadow = shadowValue.get()

		scoreCollection.forEachIndexed { index, score ->
			val playerName = score.playerName
			val team = scoreboard.getPlayersTeam(playerName)

			val formattedPlayerName = func.scoreboardFormatPlayerName(team, playerName)
			val scorePoints = "${WEnumChatFormatting.RED}${score.scorePoints}"

			val width = 5F + (if (leftRect) 1F else if (rightRect) -1F else 0F) * (rectWidth + 1F)
			val height = maxHeight - index * fontHeight

			RenderUtils.resetColor()

			fontRenderer.drawString(formattedPlayerName, backgroundXStart, height, textColor, shadow)
			fontRenderer.drawString(scorePoints, (width - fontRenderer.getStringWidth(scorePoints)), height, textColor, shadow)

			if (index == scoreCollectionSize - 1)
			{
				val displayName = ColorUtils.stripColor(objective.displayName)

				RenderUtils.resetColor()

				RainbowFontShader.begin(titleRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
					fontRenderer.drawString(displayName, backgroundXStart + (maxWidth shr 1) - fontRenderer.getStringWidth(displayName) * 0.5F, height - fontHeight, titleColor, shadow)
				}
			}

			val rectColor = when
			{
				rectRainbowShader -> 0
				rectColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbowRGB(offset = index * rainbowOffset, speed = rainbowSpeed, saturation = saturation, brightness = brightness)
				else -> rectCustomColor
			}

			if (rightRect || leftRect)
			{
				RainbowShader.begin(rectRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
					val startY = if (index == scoreCollectionSize - 1) -2F else height
					val endY = if (index == 0) fontHeight else height + fontHeight * 2F

					if (rightRect) RenderUtils.drawRect(5F - rectWidth, startY, 5F, endY, rectColor) else RenderUtils.drawRect(backgroundXStart - 2F, startY, backgroundXStart - rectWidth - 2F, endY, rectColor)
				}
			}
		}

		return Border(-maxWidth - 5.0F - if (rightRect) rectWidth else 0F, -2F, 5F + if (leftRect) rectWidth else 0F, maxHeight + fontHeight)
	}
}
