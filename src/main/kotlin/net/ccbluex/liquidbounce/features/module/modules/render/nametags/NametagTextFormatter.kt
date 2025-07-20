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
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.bold
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.withColor
import net.ccbluex.liquidbounce.utils.combat.EntityTaggingManager
import net.ccbluex.liquidbounce.utils.entity.getActualHealth
import net.ccbluex.liquidbounce.utils.entity.ping
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.text.TextColor
import net.minecraft.util.Formatting
import kotlin.math.roundToInt

@Suppress("MagicNumber")
class NametagTextFormatter(private val entity: Entity) {
    fun format(): Text {
        val outputText = Text.empty()

        if (NametagShowOptions.DISTANCE.isShowing()) {
            outputText.append(this.distanceText).append(" ")
        }
        if (NametagShowOptions.PING.isShowing()) {
            outputText.append(this.pingText).append(" ")
        }

        val name = entity.displayName!!
        val nameColor = this.nameColor

        val nameText: Text = if (nameColor != null) {
            name.string.asText().withColor(nameColor)
        } else {
            name
        }

        outputText.append(nameText)

        if (NametagShowOptions.HEALTH.isShowing()) {
            outputText.append(" ").append(this.healthText)
        }

        if (this.isBot) {
            outputText.append(" ").append("Bot".asText().formatted().bold(true).withColor(Formatting.RED))
        }

        return outputText
    }

    private val isBot = ModuleAntiBot.isBot(entity)

    private val nameColor: TextColor?
        get() {
            val tagColor = EntityTaggingManager.getTag(this.entity).color

            return when {
                isBot -> Formatting.DARK_AQUA.toTextColor()
                entity.isInvisible -> Formatting.GOLD.toTextColor()
                entity.isSneaking -> Formatting.DARK_RED.toTextColor()
                tagColor != null -> TextColor.fromRgb(tagColor.toARGB())
                else -> null
            }
        }

    private val distanceText: Text
        get() {
            val playerDistanceRounded = player.distanceTo(entity).roundToInt()

            return "${playerDistanceRounded}m".asText().formatted(Formatting.GRAY)
        }

    private fun getPing(entity: Entity): Int? {
        return (entity as? PlayerEntity)?.ping
    }

    private val pingText: Text
        get() {
            val playerPing = getPing(entity) ?: return Text.of("")

            val coloringBasedOnPing = when {
                playerPing > 200 -> Formatting.RED
                playerPing > 100 -> Formatting.YELLOW
                else -> Formatting.GREEN
            }

            return regular(" [")
                .append(
                    (playerPing.toString() + "ms").asText().formatted(coloringBasedOnPing)
                )
                .append(regular("]"))
        }

    private val healthText: Text
        get() {
            if (entity !is LivingEntity) {
                return regular("")
            }

            val actualHealth = entity.getActualHealth().toInt()

            val healthColor = when {
                // Perhaps you should modify the values here
                actualHealth >= 14 -> Formatting.GREEN
                actualHealth >= 8 -> Formatting.YELLOW
                else -> Formatting.RED
            }

            return "$actualHealth HP".asText().formatted(healthColor)

        }
}

private fun Formatting.toTextColor(): TextColor {
    return TextColor.fromFormatting(this)!!
}
