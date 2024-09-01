/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_WEBSITE
import net.ccbluex.liquidbounce.features.module.modules.render.NoScoreboard
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRectInt
import net.ccbluex.liquidbounce.value.*
import net.minecraft.scoreboard.ScoreObjective
import net.minecraft.scoreboard.ScorePlayerTeam
import net.minecraft.util.EnumChatFormatting
import org.lwjgl.opengl.GL11.*
import java.awt.Color

/**
 * CustomHUD scoreboard
 *
 * Allows to move and customize minecraft scoreboard
 */
@ElementInfo(name = "Scoreboard", force = true)
class ScoreboardElement(x: Double = 5.0, y: Double = 0.0, scale: Float = 1F,
                        side: Side = Side(Side.Horizontal.RIGHT, Side.Vertical.MIDDLE)) : Element(x, y, scale, side) {

    private val textRed by IntegerValue("Text-R", 255, 0..255)
    private val textGreen by IntegerValue("Text-G", 255, 0..255)
    private val textBlue by IntegerValue("Text-B", 255, 0..255)

    private val backgroundColorRed by IntegerValue("Background-R", 0, 0..255)
    private val backgroundColorGreen by IntegerValue("Background-G", 0, 0..255)
    private val backgroundColorBlue by IntegerValue("Background-B", 0, 0..255)
    private val backgroundColorAlpha by IntegerValue("Background-Alpha", 95, 0..255)

    private val roundedRectRadius by FloatValue("Rounded-Radius", 3F, 0F..5F)

    private val rect by BoolValue("Rect", false)
        private val rectColorMode by ListValue("Rect-Color", arrayOf("Custom", "Rainbow"), "Custom") { rect }
            private val rectColorRed by IntegerValue("Rect-R", 0, 0..255) { rect && rectColorMode == "Custom"}
            private val rectColorGreen by IntegerValue("Rect-G", 111, 0..255) { rect && rectColorMode == "Custom"}
            private val rectColorBlue by IntegerValue("Rect-B", 255, 0..255) { rect && rectColorMode == "Custom"}
            private val rectColorAlpha by IntegerValue("Rect-Alpha", 255, 0..255) { rect && rectColorMode == "Custom"}

    private val serverIp by ListValue("ServerIP", arrayOf("Normal", "None", "Client", "Website"), "Normal")
    private val shadow by BoolValue("Shadow", false)
    private val font by FontValue("Font", Fonts.minecraftFont)

    /**
     * Draw element
     */
    override fun drawElement(): Border? {
        if (NoScoreboard.handleEvents())
            return null

        val fontRenderer = font
        val textColor = textColor().rgb
        val backColor = backgroundColor().rgb

        val rectColorMode = rectColorMode
        val rectCustomColor = Color(rectColorRed, rectColorGreen, rectColorBlue, rectColorAlpha).rgb

        val worldScoreboard = mc.world.scoreboard
        var currObjective: ScoreObjective? = null
        val playerTeam = worldScoreboard.getPlayersTeam(mc.player.name)

        if (playerTeam != null) {
            val colorIndex = playerTeam.chatFormat.colorIndex

            if (colorIndex >= 0)
                currObjective = worldScoreboard.getObjectiveInDisplaySlot(3 + colorIndex)
        }

        val objective = currObjective ?: worldScoreboard.getObjectiveInDisplaySlot(1) ?: return null

        val scoreboard = objective.scoreboard
        var scoreCollection = scoreboard.getSortedScores(objective)
        val scores = scoreCollection.filter { it.playerName?.startsWith("#") == false }

        scoreCollection = if (scores.size > 15)
            scores.drop(scoreCollection.size - 15)
        else
            scores

        var maxWidth = fontRenderer.getStringWidth(objective.displayName)

        for (score in scoreCollection) {
            val scorePlayerTeam = scoreboard.getPlayersTeam(score.playerName)
            val width = "${ScorePlayerTeam.formatPlayerName(scorePlayerTeam, score.playerName)}: ${EnumChatFormatting.RED}${score.scorePoints}"
            maxWidth = maxWidth.coerceAtLeast(fontRenderer.getStringWidth(width))
        }

        val maxHeight = scoreCollection.size * fontRenderer.FONT_HEIGHT
        val l1 = -maxWidth - 3 - if (rect) 3 else 0

        drawRoundedRectInt(l1 - 4, -4, 7, (2 + maxHeight + fontRenderer.FONT_HEIGHT), backColor, roundedRectRadius)

        scoreCollection.forEachIndexed { index, score ->
            val team = scoreboard.getPlayersTeam(score.playerName)

            var name = ScorePlayerTeam.formatPlayerName(team, score.playerName)
            val scorePoints = "${EnumChatFormatting.RED}${score.scorePoints}"

            val width = 5 - if (rect) 4 else 0
            val height = maxHeight - index * fontRenderer.FONT_HEIGHT.toFloat()

            glColor4f(1f, 1f, 1f, 1f)

            if (serverIp != "Normal") {
                runCatching {
                    val nameWithoutFormatting = name?.replace(EnumChatFormatting.RESET.toString(), "")
                        ?.replace(Regex("[\u00a7&][0-9a-fk-or]"), "")?.trim()
                    val trimmedServerIP = mc.currentServerData?.serverIP?.trim()?.lowercase()

                    val domainRegex = Regex("\\b(?:[a-zA-Z0-9](?:[a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,63}\\b")
                    val containsDomain = nameWithoutFormatting?.let { domainRegex.containsMatchIn(it) } ?: false

                    runCatching {
                        if (nameWithoutFormatting?.lowercase() == trimmedServerIP || containsDomain) {
                            val colorCode = name?.substring(0, 2) ?: "§9"
                            name = when (serverIp.lowercase()) {
                                "none" -> ""
                                "client" -> "$colorCode$CLIENT_NAME"
                                "website" -> "$colorCode$CLIENT_WEBSITE"
                                else -> return null
                            }
                        }
                    }.onFailure {
                        LOGGER.error("Error while changing Scoreboard Server IP: ${it.message}")
                    }
                }.onFailure {
                    LOGGER.error("Failed to run: ${it.message}")
                }
            }

            fontRenderer.draw(name, l1.toFloat(), height, textColor, shadow)
            fontRenderer.draw(scorePoints, (width - fontRenderer.getStringWidth(scorePoints)).toFloat(), height, textColor, shadow)

            if (index == scoreCollection.size - 1) {
                val displayName = objective.displayName

                glColor4f(1f, 1f, 1f, 1f)

                fontRenderer.draw(
                    displayName,
                    (l1 + maxWidth / 2 - fontRenderer.getStringWidth(displayName) / 2).toFloat(),
                    height - fontRenderer.FONT_HEIGHT,
                    textColor,
                    shadow
                )
            }

            if (rect) {
                val rectColor = when (rectColorMode) {
                    "Rainbow" -> ColorUtils.rainbow(400000000L * index).rgb
                    else -> rectCustomColor
                }

                drawRoundedRect(
                    2F,
                    if (index == scoreCollection.size - 1) -2F else height,
                    5F,
                    if (index == 0) fontRenderer.FONT_HEIGHT.toFloat() else height + fontRenderer.FONT_HEIGHT * 2F,
                    rectColor,
                    roundedRectRadius
                )
            }
        }

        return Border(-maxWidth - 5f - if (rect) 3 else 0, -2F, 5F, maxHeight + fontRenderer.FONT_HEIGHT.toFloat())
    }

    private fun backgroundColor() = Color(backgroundColorRed, backgroundColorGreen,
            backgroundColorBlue, backgroundColorAlpha)

    private fun textColor() = Color(textRed, textGreen, textBlue)

}