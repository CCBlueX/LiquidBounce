/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.render.nametags

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.nameprotect.sanitizeForeignInput
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleCombineMobs
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.combat.EntityTaggingManager
import net.ccbluex.liquidbounce.utils.entity.getActualHealth
import net.ccbluex.liquidbounce.utils.entity.hasHealthScoreboard
import net.ccbluex.liquidbounce.utils.entity.ping
import net.ccbluex.liquidbounce.utils.entity.shortName
import net.ccbluex.liquidbounce.utils.entity.simpleDisplayName
import net.ccbluex.liquidbounce.utils.text.withFormat
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.util.FormattedCharSequence
import net.minecraft.util.FormattedCharSequence.codepoint
import net.minecraft.util.FormattedCharSequence.composite
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.GameType
import kotlin.math.roundToInt

internal object NametagTextFormatter : ValueGroup("Text") {

    private val COUNT_STYLE = Style.EMPTY.applyFormats(ChatFormatting.AQUA, ChatFormatting.BOLD)

    private val BOT_STYLE = Style.EMPTY.applyFormats(ChatFormatting.RED, ChatFormatting.BOLD)

    private val BABY_TEXT = "Baby ".withFormat()

    private val BOT_TEXT = "Bot".withFormat(BOT_STYLE)

    private val SPACE = codepoint(' '.code, Style.EMPTY)

    private val leftBracket = codepoint('['.code, Style.EMPTY.applyFormat(ChatFormatting.GRAY))
    private val rightBracket = codepoint(']'.code, Style.EMPTY.applyFormat(ChatFormatting.GRAY))

    private val parts by multiEnumChoice(
        "Parts",
        ObjectLinkedOpenHashSet(Part.entries),
        canBeNone = false
    )

    private enum class Part(override val tag: String) : Tagged {
        DISTANCE("Distance") {
            override fun apply(entity: Entity): FormattedCharSequence? {
                if (entity === player) return null

                val playerDistanceRounded = player.distanceTo(entity).roundToInt()
                return "${playerDistanceRounded}m".withFormat(ChatFormatting.GRAY)
            }
        },

        PING("Ping") {
            override fun apply(entity: Entity): FormattedCharSequence? {
                if (entity !is Player) return null

                val playerPing = entity.ping

                val coloringBasedOnPing = when {
                    playerPing > 200 -> ChatFormatting.RED
                    playerPing > 100 -> ChatFormatting.YELLOW
                    else -> ChatFormatting.GREEN
                }

                return composite(
                    leftBracket,
                    "${playerPing}ms".withFormat(coloringBasedOnPing),
                    rightBracket,
                )
            }
        },

        NAME("Name") {
            override fun apply(entity: Entity): FormattedCharSequence {
                val isBaby = entity is LivingEntity && entity.isBaby

                val displayName = entity.simpleDisplayName

                val coloredName = (entity.nameColor?.let { nameColor ->
                    displayName.copy().withColor(nameColor)
                } ?: displayName).sanitizeForeignInput()

                val count = ModuleCombineMobs.getCombinedCount(entity)
                return when {
                    isBaby && count > 1 -> composite(BABY_TEXT, coloredName, " ($count)".withFormat(COUNT_STYLE))
                    isBaby -> composite(BABY_TEXT, coloredName)
                    count > 1 -> composite(coloredName, " ($count)".withFormat(COUNT_STYLE))
                    else -> coloredName
                }
            }
        },

        HEALTH("Health") {
            override fun apply(entity: Entity): FormattedCharSequence? {
                if (entity !is LivingEntity) return null

                val actualHealth = (entity.getActualHealth() +
                    if (entity.hasHealthScoreboard()) 0f else entity.absorptionAmount).toInt()

                val healthColor = when {
                    actualHealth >= 14 -> ChatFormatting.GREEN
                    actualHealth >= 8 -> ChatFormatting.YELLOW
                    else -> ChatFormatting.RED
                }

                return "$actualHealth HP".withFormat(healthColor)
            }
        },

        GAME_MODE("GameMode") {
            override fun apply(entity: Entity): FormattedCharSequence? {
                if (entity !is Player) return null

                val gameMode = entity.gameMode() ?: return null

                val gameModeColor = when (gameMode) {
                    GameType.SURVIVAL -> ChatFormatting.GREEN
                    GameType.CREATIVE -> ChatFormatting.RED
                    GameType.ADVENTURE -> ChatFormatting.YELLOW
                    GameType.SPECTATOR -> ChatFormatting.GRAY
                }

                return composite(
                    leftBracket,
                    gameMode.shortName().withFormat(gameModeColor),
                    rightBracket,
                )
            }
        },

        BOT_MARK("BotMark") {
            override fun apply(entity: Entity): FormattedCharSequence? {
                return if (entity.isBot) BOT_TEXT else null
            }
        };

        abstract fun apply(entity: Entity): FormattedCharSequence?
    }

    fun format(entity: Entity): FormattedCharSequence {
        return FormattedCharSequence.fromList(
            buildList {
                for (part in parts) {
                    val s = part.apply(entity) ?: continue
                    if (this.isNotEmpty()) this += SPACE
                    this += s
                }
            }
        )
    }

}

private val Entity.isBot get() = ModuleAntiBot.isBot(this)

private val Entity.nameColor: TextColor?
    get() = when {
        isBot -> ChatFormatting.DARK_AQUA.toTextColor()
        isInvisible -> ChatFormatting.GOLD.toTextColor()
        isShiftKeyDown -> ChatFormatting.DARK_RED.toTextColor()
        else -> EntityTaggingManager.getTag(this).color?.toTextColor()
    }

private fun ChatFormatting.toTextColor(): TextColor {
    return TextColor.fromLegacyFormat(this)!!
}
