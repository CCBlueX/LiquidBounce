/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.features.module.modules.render.NoScoreboard
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowFontShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.ccbluex.liquidbounce.value.*
import net.minecraft.scoreboard.ScoreObjective
import net.minecraft.scoreboard.ScorePlayerTeam
import net.minecraft.util.EnumChatFormatting
import kotlin.math.min

/**
 * CustomHUD scoreboard
 *
 * Allows to move and customize minecraft scoreboard
 */
@ElementInfo(name = "Scoreboard", force = true)
class ScoreboardElement(x: Double = 5.0, y: Double = 0.0, scale: Float = 1F, side: Side = Side(Side.Horizontal.RIGHT, Side.Vertical.MIDDLE)) : Element(x, y, scale, side)
{
    private val textGroup = ValueGroup("Text")
    private val textShadowValue = BoolValue("Shadow", false)
    private val textColorValue = RGBColorValue("Color", 255, 255, 255, Triple("Text-R", "Text-G", "Text-B"))

    private val titleColorGroup = ValueGroup("TitleColor")
    private val titleColorModeValue = ListValue("Mode", arrayOf("Custom", "Rainbow", "RainbowShader"), "Custom", "Title-Color")
    private val titleColorValue = RGBColorValue("Color", 255, 255, 255, Triple("Title-R", "Title-G", "Title-B"))

    private val backgroundColorValue = RGBAColorValue("BackgroundColor", 0, 0, 0, 95, listOf("Background-R", "Background-G", "Background-B", "Background-Alpha"))

    private val rectGroup = ValueGroup("Rect")
    private val rectModeValue = ListValue("Mode", arrayOf("None", "Left", "Right"), "None", "Rect")
    private val rectWidthValue = FloatValue("Width", 3F, 1.5F, 5F, "Rect-Width")

    private val rectColorGroup = ValueGroup("Color")
    private val rectColorModeValue = ListValue("Mode", arrayOf("Custom", "Rainbow", "RainbowShader"), "Custom", "Rect-Color")
    private val rectColorValue = RGBAColorValue("Color", 0, 111, 255, 255, listOf("Rect-R", "Rect-G", "Rect-B", "Rect-Alpha"))

    private val rainbowGroup = object : ValueGroup("Rainbow")
    {
        override fun showCondition() = rectColorModeValue.get().equals("Rainbow", ignoreCase = true) || titleColorModeValue.get().equals("Rainbow", ignoreCase = true)
    }
    private val rainbowSpeedValue = IntegerValue("Speed", 10, 1, 10, "Rainbow-Speed")
    private val rainbowOffsetValue = IntegerValue("IndexOffset", 0, -100, 100, "Rainbow-IndexOffset")
    private val rainbowSaturationValue = FloatValue("Saturation", 0.9f, 0f, 1f, "HSB-Saturation")
    private val rainbowBrightnessValue = FloatValue("Brightness", 1f, 0f, 1f, "HSB-Brightness")

    private val rainbowShaderGroup = object : ValueGroup("RainbowShader")
    {
        override fun showCondition() = rectColorModeValue.get().equals("RainbowShader", ignoreCase = true) || titleColorModeValue.get().equals("RainbowShader", ignoreCase = true)
    }
    private val rainbowShaderXValue = FloatValue("X", -1000F, -2000F, 2000F, "RainbowShader-X")
    private val rainbowShaderYValue = FloatValue("Y", -1000F, -2000F, 2000F, "RainbowShader-Y")

    private val fontValue = FontValue("Font", Fonts.minecraftFont)

    init
    {
        textGroup.addAll(textShadowValue, textColorValue)
        titleColorGroup.addAll(titleColorModeValue, titleColorValue)
        rectGroup.addAll(rectModeValue, rectWidthValue, rectColorGroup)
        rectColorGroup.addAll(rectColorModeValue, rectColorValue)
        rainbowGroup.addAll(rainbowSpeedValue, rainbowOffsetValue, rainbowSaturationValue, rainbowBrightnessValue)
        rainbowShaderGroup.addAll(rainbowShaderXValue, rainbowShaderYValue)
    }

    /**
     * Draw element
     */
    override fun drawElement(): Border?
    {
        if (NoScoreboard.state) return null

        val worldScoreboard = (mc.theWorld ?: return null).scoreboard

        var currObjective: ScoreObjective? = null
        val playerTeam = worldScoreboard.getPlayersTeam((mc.thePlayer ?: return null).name)

        if (playerTeam != null)
        {
            val colorIndex = playerTeam.chatFormat.colorIndex

            if (colorIndex >= 0) currObjective = worldScoreboard.getObjectiveInDisplaySlot(3 + colorIndex)
        }

        val objective = currObjective ?: worldScoreboard.getObjectiveInDisplaySlot(1) ?: return null

        val fontRenderer = fontValue.get()
        val fontHeight = fontRenderer.FONT_HEIGHT.toFloat()

        val saturation = rainbowSaturationValue.get()
        val brightness = rainbowBrightnessValue.get()
        val rainbowSpeed = rainbowSpeedValue.get()

        val rainbowShaderX = if (rainbowShaderXValue.get() == 0.0F) 0.0F else 1.0F / rainbowShaderXValue.get()
        val rainbowShaderY = if (rainbowShaderYValue.get() == 0.0F) 0.0F else 1.0F / rainbowShaderYValue.get()
        val rainbowShaderOffset = System.currentTimeMillis() % 10000 * 0.0001f

        val textColor = textColorValue.get()
        val backColor = backgroundColorValue.get()

        val titleColorMode = titleColorModeValue.get()

        val titleRainbowShader = titleColorMode.equals("RainbowShader", ignoreCase = true)

        val titleColor = when
        {
            titleRainbowShader -> 0
            titleColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbowRGB(offset = 400000000L, speed = rainbowSpeed, saturation = saturation, brightness = brightness)
            else -> titleColorValue.get()
        }

        val rainbowOffset = 400000000L + 40000000L * rainbowOffsetValue.get()

        val rectColorMode = rectColorModeValue.get()
        val rectCustomColor = rectColorValue.get()

        val rectRainbowShader = rectColorMode.equals("RainbowShader", ignoreCase = true)

        val scoreboard = objective.scoreboard

        var scoreCollection = scoreboard.getSortedScores(objective)
        val scoreCollectionSize = scoreCollection.size

        val scores = scoreCollection.filter { !it.playerName.startsWith("#") }
        val scoresSize = scores.size

        scoreCollection = if (scoresSize > 15) scores.subList(min(scoresSize, scoreCollectionSize - 15), scoresSize) else scores

        var maxWidth = fontRenderer.getStringWidth(objective.displayName)

        maxWidth = maxWidth.coerceAtLeast(scoreCollection.minOfOrNull { score ->
            val playerName = score.playerName
            fontRenderer.getStringWidth("${ScorePlayerTeam.formatPlayerName(scoreboard.getPlayersTeam(playerName), playerName)}: ${EnumChatFormatting.RED}${score.scorePoints}")
        } ?: -1)

        val maxHeight = scoreCollectionSize * fontHeight

        val rectMode = rectModeValue.get()

        val leftRect = rectMode.equals("Left", ignoreCase = true)
        val rightRect = rectMode.equals("Right", ignoreCase = true)

        val rectWidth = rectWidthValue.get()

        val backgroundXStart = -maxWidth - 3F + if (leftRect) rectWidth else if (rightRect) -rectWidth else 0F
        val backgroundXEnd = 5F + if (leftRect) rectWidth else if (rightRect) -rectWidth else 0F

        RenderUtils.drawRect(backgroundXStart - 2F, -2F, backgroundXEnd, maxHeight + fontHeight, backColor)

        val shadow = textShadowValue.get()

        scoreCollection.forEachIndexed { index, score ->
            val playerName = score.playerName
            val team = scoreboard.getPlayersTeam(playerName)

            val formattedPlayerName = ScorePlayerTeam.formatPlayerName(team, playerName)
            val scorePoints = "${EnumChatFormatting.RED}${score.scorePoints}"

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
