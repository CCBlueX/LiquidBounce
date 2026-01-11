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
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleCombineMobs
import net.ccbluex.liquidbounce.utils.text.PlainText
import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.joinToText
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.textOf
import net.ccbluex.liquidbounce.utils.client.withColor
import net.ccbluex.liquidbounce.utils.combat.EntityTaggingManager
import net.ccbluex.liquidbounce.utils.entity.getActualHealth
import net.ccbluex.liquidbounce.utils.entity.hasHealthScoreboard
import net.ccbluex.liquidbounce.utils.entity.ping
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextColor
import net.minecraft.ChatFormatting
import java.util.function.Function
import kotlin.math.roundToInt

private val COUNT_STYLE = Style.EMPTY.applyFormats(ChatFormatting.AQUA, ChatFormatting.BOLD)

private val BOT_STYLE = Style.EMPTY.applyFormats(ChatFormatting.RED, ChatFormatting.BOLD)

private val BABY_TEXT = "Baby ".asPlainText()

private val BOT_TEXT = "Bot".asPlainText(BOT_STYLE)

internal object NametagTextFormatter : Configurable("Text") {

    private val parts by multiEnumChoice(
        "Parts",
        ObjectLinkedOpenHashSet(Part.entries),
        canBeNone = false
    )

    private enum class Part(override val choiceName: String) : NamedChoice, Function<Entity, Component?> {
        DISTANCE("Distance") {
            override fun apply(t: Entity): Component? {
                if (t === player) return null

                val playerDistanceRounded = player.distanceTo(t).roundToInt()
                return "${playerDistanceRounded}m".asPlainText(ChatFormatting.GRAY)
            }
        },

        PING("Ping") {
            private val leftBracket = "[".asPlainText(ChatFormatting.GRAY)
            private val rightBracket = "]".asPlainText(ChatFormatting.GRAY)

            override fun apply(t: Entity): Component? {
                val entity = t as? Player ?: return null

                val playerPing = entity.ping

                val coloringBasedOnPing = when {
                    playerPing > 200 -> ChatFormatting.RED
                    playerPing > 100 -> ChatFormatting.YELLOW
                    else -> ChatFormatting.GREEN
                }

                return textOf(
                    leftBracket,
                    "${playerPing}ms".asPlainText(coloringBasedOnPing),
                    rightBracket,
                )
            }
        },

        NAME("Name") {
            override fun apply(entity: Entity): Component = buildList(4) {
                val name = entity.displayName
                val nameColor = entity.nameColor

                if (entity is LivingEntity && entity.isBaby) {
                    this += BABY_TEXT
                }

                this += if (nameColor != null) {
                    name.copy().withColor(nameColor)
                } else {
                    name
                }

                if (ModuleCombineMobs.running) {
                    val count = ModuleCombineMobs.getCombinedCount(entity)
                    if (count > 1) {
                        this += PlainText.SPACE
                        this += ("x $count").asPlainText(COUNT_STYLE)
                    }
                }
            }.asText()
        },

        HEALTH("Health") {
            override fun apply(t: Entity): Component? {
                val entity = t as? LivingEntity ?: return null

                val actualHealth = (entity.getActualHealth() +
                    if (entity.hasHealthScoreboard()) 0f else entity.absorptionAmount).toInt()

                val healthColor = when {
                    actualHealth >= 14 -> ChatFormatting.GREEN
                    actualHealth >= 8 -> ChatFormatting.YELLOW
                    else -> ChatFormatting.RED
                }

                return "$actualHealth HP".asPlainText(healthColor)
            }
        },

        BOT_MARK("BotMark") {
            override fun apply(t: Entity): Component? {
                return if (t.isBot) BOT_TEXT else null
            }
        },
    }

    fun format(entity: Entity): Component {
        return parts.mapNotNull { it.apply(entity) }.joinToText(PlainText.SPACE)
    }

}

private val Entity.isBot get() = ModuleAntiBot.isBot(this)

private val Entity.nameColor: TextColor?
    get() {
        val tagColor = EntityTaggingManager.getTag(this).color

        return when {
            isBot -> ChatFormatting.DARK_AQUA.toTextColor()
            isInvisible -> ChatFormatting.GOLD.toTextColor()
            isShiftKeyDown -> ChatFormatting.DARK_RED.toTextColor()
            tagColor != null -> tagColor.toTextColor()
            else -> null
        }
    }

private fun ChatFormatting.toTextColor(): TextColor {
    return TextColor.fromLegacyFormat(this)!!
}
