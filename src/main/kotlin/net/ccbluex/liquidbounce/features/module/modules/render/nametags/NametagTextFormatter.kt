/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
import net.ccbluex.liquidbounce.features.module.modules.misc.nameprotect.sanitizeWithNameProtect
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.mc
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

class NametagTextFormatter(private val entity: Entity) {
    fun format(): Text {
        val outputText = Text.empty()

        if (ModuleNametags.distance) {
            outputText.append(this.distanceText).append(" ")
        }
        if (ModuleNametags.ping) {
            outputText.append(this.pingText).append(" ")
        }

        val nameString = entity.displayName!!.sanitizeWithNameProtect().string

        outputText.append(nameString.asText().styled { it.withColor(this.nameColor) })

        if (ModuleNametags.Health.enabled) {
            outputText.append(" ").append(this.healthText)
        }

        if (this.isBot) {
            outputText.append(" ").append("Bot".asText().styled { it.withColor(Formatting.RED).withBold(true) })
        }

        return outputText
    }

    private val isBot = ModuleAntiBot.isBot(entity)

    private val nameColor: TextColor
        get() {
            val tagColor = EntityTaggingManager.getTag(this.entity).color

            return when {
                isBot -> Formatting.DARK_AQUA.toTextColor()
                entity.isInvisible -> Formatting.GOLD.toTextColor()
                entity.isSneaking -> Formatting.DARK_RED.toTextColor()
                tagColor != null -> TextColor.fromRgb(tagColor.toRGBA())
                else -> Formatting.GRAY.toTextColor()
            }
        }

    private val distanceText: Text
        get() {
            val playerDistanceRounded = mc.player!!.distanceTo(entity).roundToInt()

            return withColor("${playerDistanceRounded}m", Formatting.GRAY)
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

            return regular(" [") + withColor(playerPing.toString() + "ms", coloringBasedOnPing) + regular("]")
        }

    private val healthText: Text
        get() {
            if (entity !is LivingEntity) {
                return regular("")
            }

            val actualHealth = entity.getActualHealth(ModuleNametags.Health.fromScoreboard).toInt()

            val healthColor = when {
                // Perhaps you should modify the values here
                actualHealth >= 14 -> Formatting.GREEN
                actualHealth >= 8 -> Formatting.YELLOW
                else -> Formatting.RED
            }

            return withColor("$actualHealth HP", healthColor)

        }
}

private fun Formatting.toTextColor(): TextColor {
    return TextColor(this.colorValue!!, this.name)
}

operator fun MutableText.plus(text: MutableText): MutableText {
    this.append(text)

    return this
}
