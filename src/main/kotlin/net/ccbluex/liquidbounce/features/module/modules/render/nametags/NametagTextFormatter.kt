/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleCombineMobs
import net.ccbluex.liquidbounce.utils.client.PlainText
import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.withColor
import net.ccbluex.liquidbounce.utils.combat.EntityTaggingManager
import net.ccbluex.liquidbounce.utils.entity.getActualHealth
import net.ccbluex.liquidbounce.utils.entity.hasHealthScoreboard
import net.ccbluex.liquidbounce.utils.entity.ping
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.text.TextColor
import net.minecraft.util.Formatting
import kotlin.math.roundToInt

private val COUNT_STYLE = Style.EMPTY.withFormatting(Formatting.AQUA, Formatting.BOLD)

private val BOT_STYLE = Style.EMPTY.withFormatting(Formatting.RED, Formatting.BOLD)

private val BABY_TEXT = "Baby ".asPlainText()

private val BOT_TEXT = "Bot".asPlainText(BOT_STYLE)

object NametagTextFormatter {

    fun format(entity: Entity): Text {
        val outputText = mutableListOf<Text>()

        if (NametagShowOptions.DISTANCE.isShowing()) {
            outputText += entity.distanceText
            outputText += PlainText.SPACE
        }
        if (NametagShowOptions.PING.isShowing() && entity is PlayerEntity) {
            outputText += entity.pingText
            outputText += PlainText.SPACE
        }

        val name = entity.displayName!!
        val nameColor = entity.nameColor

        if ((entity as? MobEntity)?.isBaby == true) {
            outputText += BABY_TEXT
        }

        outputText += if (nameColor != null) {
            name.copy().withColor(nameColor)
        } else {
            name
        }

        if (ModuleCombineMobs.running) {
            val count = ModuleCombineMobs.getCombinedCount(entity)
            if (count > 1) {
                val countText = ("x $count").asPlainText(COUNT_STYLE)
                outputText += PlainText.SPACE
                outputText += countText
            }
        }

        if (NametagShowOptions.HEALTH.isShowing() && entity is LivingEntity) {
            outputText += PlainText.SPACE
            outputText += entity.healthText
        }

        if (entity.isBot) {
            outputText += PlainText.SPACE
            outputText += BOT_TEXT
        }

        return outputText.asText()
    }

    private val Entity.isBot get() = ModuleAntiBot.isBot(this)

    private val Entity.nameColor: TextColor?
        get() {
            val tagColor = EntityTaggingManager.getTag(this).color

            return when {
                isBot -> Formatting.DARK_AQUA.toTextColor()
                isInvisible -> Formatting.GOLD.toTextColor()
                isSneaking -> Formatting.DARK_RED.toTextColor()
                tagColor != null -> tagColor.toTextColor()
                else -> null
            }
        }

    private val Entity.distanceText: Text
        get() {
            val playerDistanceRounded = player.distanceTo(this).roundToInt()

            return "${playerDistanceRounded}m".asPlainText(Formatting.GRAY)
        }

    private val PlayerEntity.pingText: Text
        get() {
            val playerPing = this.ping

            val coloringBasedOnPing = when {
                playerPing > 200 -> Formatting.RED
                playerPing > 100 -> Formatting.YELLOW
                else -> Formatting.GREEN
            }

            return regular(" [")
                .append(
                    (playerPing.toString() + "ms").asPlainText(coloringBasedOnPing)
                )
                .append(regular("]"))
        }

    private val LivingEntity.healthText: Text
        get() {
            val actualHealth = (this.getActualHealth() +
                if (this.hasHealthScoreboard()) 0f else this.absorptionAmount).toInt()

            val healthColor = when {
                actualHealth >= 14 -> Formatting.GREEN
                actualHealth >= 8 -> Formatting.YELLOW
                else -> Formatting.RED
            }

            return "$actualHealth HP".asPlainText(healthColor)
        }
}

private fun Formatting.toTextColor(): TextColor {
    return TextColor.fromFormatting(this)!!
}
