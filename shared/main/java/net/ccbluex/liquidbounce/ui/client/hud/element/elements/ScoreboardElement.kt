/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import net.ccbluex.liquidbounce.api.minecraft.scoreboard.IScoreObjective
import net.ccbluex.liquidbounce.api.minecraft.util.WEnumChatFormatting
import net.ccbluex.liquidbounce.features.module.modules.render.NoScoreboard
import net.ccbluex.liquidbounce.ui.client.hud.element.*
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.opengl.GL11
import java.awt.Color

/**
 * CustomHUD scoreboard
 *
 * Allows to move and customize minecraft scoreboard
 */
@ElementInfo(name = "Scoreboard", force = true)
class ScoreboardElement(
	x: Double = 5.0, y: Double = 0.0, scale: Float = 1F, side: Side = Side(Side.Horizontal.RIGHT, Side.Vertical.MIDDLE)
) : Element(x, y, scale, side)
{

	private val textRedValue = IntegerValue("Text-R", 255, 0, 255)
	private val textGreenValue = IntegerValue("Text-G", 255, 0, 255)
	private val textBlueValue = IntegerValue("Text-B", 255, 0, 255)

	private val backgroundColorRedValue = IntegerValue("Background-R", 0, 0, 255)
	private val backgroundColorGreenValue = IntegerValue("Background-G", 0, 0, 255)
	private val backgroundColorBlueValue = IntegerValue("Background-B", 0, 0, 255)
	private val backgroundColorAlphaValue = IntegerValue("Background-Alpha", 95, 0, 255)

	private val rectValue = BoolValue("Rect", false)
	private val rectColorModeValue = ListValue("Rect-Color", arrayOf("Custom", "Rainbow"), "Custom")
	private val rectColorRedValue = IntegerValue("Rect-R", 0, 0, 255)
	private val rectColorGreenValue = IntegerValue("Rect-G", 111, 0, 255)
	private val rectColorBlueValue = IntegerValue("Rect-B", 255, 0, 255)
	private val rectColorBlueAlpha = IntegerValue("Rect-Alpha", 255, 0, 255)

	private val saturationValue = FloatValue("HSB-Saturation", 0.9f, 0f, 1f)
	private val brightnessValue = FloatValue("HSB-Brightness", 1f, 0f, 1f)

	private val rainbowSpeedValue = IntegerValue("Rainbow-Speed", 10, 1, 10)

	private val shadowValue = BoolValue("Shadow", false)
	private val fontValue = FontValue("Font", Fonts.minecraftFont)

	/**
	 * Draw element
	 */
	override fun drawElement(): Border?
	{
		if (NoScoreboard.state) return null

		val fontRenderer = fontValue.get()
		val textColor = textColor().rgb
		val backColor = backgroundColor().rgb

		val rectColorMode = rectColorModeValue.get()
		val rectCustomColor = Color(
			rectColorRedValue.get(), rectColorGreenValue.get(), rectColorBlueValue.get(), rectColorBlueAlpha.get()
		).rgb

		val saturation = saturationValue.get()
		val brightness = brightnessValue.get()
		val rainbowSpeed = rainbowSpeedValue.get()

		val worldScoreboard = (mc.theWorld ?: return null).scoreboard
		var currObjective: IScoreObjective? = null
		val playerTeam = worldScoreboard.getPlayersTeam((mc.thePlayer ?: return null).name)

		if (playerTeam != null)
		{
			val colorIndex = playerTeam.chatFormat.colorIndex

			if (colorIndex >= 0) currObjective = worldScoreboard.getObjectiveInDisplaySlot(3 + colorIndex)
		}

		val objective = currObjective ?: worldScoreboard.getObjectiveInDisplaySlot(1) ?: return null

		val scoreboard = objective.scoreboard
		var scoreCollection = scoreboard.getSortedScores(objective)
		val scores = Lists.newArrayList(Iterables.filter(scoreCollection) { input ->
			input?.playerName != null && !input.playerName.startsWith("#")
		})

		scoreCollection = if (scores.size > 15) Lists.newArrayList(Iterables.skip(scores, scoreCollection.size - 15))
		else scores

		var maxWidth = fontRenderer.getStringWidth(objective.displayName)

		for (score in scoreCollection)
		{
			val scorePlayerTeam = scoreboard.getPlayersTeam(score.playerName)
			val width = "${functions.scoreboardFormatPlayerName(scorePlayerTeam, score.playerName)}: ${WEnumChatFormatting.RED}${score.scorePoints}"
			maxWidth = maxWidth.coerceAtLeast(fontRenderer.getStringWidth(width))
		}

		val maxHeight = scoreCollection.size * fontRenderer.fontHeight
		val l1 = -maxWidth - 3 - if (rectValue.get()) 3 else 0



		RenderUtils.drawRect(l1 - 2, -2, 5, (maxHeight + fontRenderer.fontHeight), backColor)

		scoreCollection.forEachIndexed { index, score ->
			val team = scoreboard.getPlayersTeam(score.playerName)

			val name = functions.scoreboardFormatPlayerName(team, score.playerName)
			val scorePoints = "${WEnumChatFormatting.RED}${score.scorePoints}"

			val width = 5 - if (rectValue.get()) 4 else 0
			val height = maxHeight - index * fontRenderer.fontHeight.toFloat()

			GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)

			fontRenderer.drawString(name, l1.toFloat(), height, textColor, shadowValue.get())
			fontRenderer.drawString(scorePoints, (width - fontRenderer.getStringWidth(scorePoints)).toFloat(), height, textColor, shadowValue.get())

			if (index == scoreCollection.size - 1)
			{
				val displayName = objective.displayName

				GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)

				fontRenderer.drawString(displayName, (l1 + (maxWidth shr 1) - (fontRenderer.getStringWidth(displayName) shr 1)).toFloat(), (height - fontRenderer.fontHeight), textColor, shadowValue.get())
			}

			if (rectValue.get())
			{
				val rectColor = when
				{
					rectColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbow(400000000L * index, speed = rainbowSpeed, saturation = saturation, brightness = brightness).rgb
					else -> rectCustomColor
				}

				RenderUtils.drawRect(2F, if (index == scoreCollection.size - 1) -2F else height, 5F, if (index == 0) fontRenderer.fontHeight.toFloat() else height + fontRenderer.fontHeight * 2F, rectColor)
			}
		}

		return Border(-maxWidth - 5.0f - if (rectValue.get()) 3 else 0, -2F, 5F, maxHeight + fontRenderer.fontHeight.toFloat())
	}

	private fun backgroundColor() = Color(
		backgroundColorRedValue.get(), backgroundColorGreenValue.get(), backgroundColorBlueValue.get(), backgroundColorAlphaValue.get()
	)

	private fun textColor() = Color(
		textRedValue.get(), textGreenValue.get(), textBlueValue.get()
	)

}
