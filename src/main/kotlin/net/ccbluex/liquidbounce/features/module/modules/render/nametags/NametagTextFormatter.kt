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

import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleNameProtect
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleTeams
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleESP
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.entity.getActualHealth
import net.ccbluex.liquidbounce.utils.entity.ping
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import kotlin.math.roundToInt

class NametagTextFormatter(private val entity: Entity) {
    fun format(): String {
        val outputBuilder = StringBuilder()

        if (ModuleNametags.distance) {
            outputBuilder.append(this.distanceText).append(" ")
        }
        if (ModuleNametags.ping) {
            outputBuilder.append(this.pingText).append(" ")
        }

        outputBuilder.append("${this.nameColor}${ModuleNameProtect.replace(entity.displayName!!.string)}")

        if (ModuleNametags.Health.enabled) {
            outputBuilder.append(" ").append(this.healthText)
        }

        if (this.isBot) {
            outputBuilder.append(" §c§lBot")
        }

        return outputBuilder.toString()
    }

    private val isBot = ModuleAntiBot.isBot(entity)

    private val nameColor: String
        get() {
            val teamColor = if (ModuleTeams.enabled) {
                ModuleESP.getTeamColor(this.entity)
            } else {
                null
            }

            return when {
                isBot -> "§3"
                entity.isInvisible -> "§6"
                entity.isSneaking -> "§4"
                teamColor != null -> "§${teamColor.closestFormattingCode()}"
                else -> "§7"
            }
        }

    private val distanceText: String
        get() {
            val playerDistanceRounded = mc.player!!.distanceTo(entity).roundToInt()

            return "§7${playerDistanceRounded}m"
        }

    private fun getPing(entity: Entity): Int? {
        return (entity as? PlayerEntity)?.ping
    }

    private val pingText: String
        get() {
            val playerPing = getPing(entity) ?: return ""

            val coloringBasedOnPing = when {
                playerPing > 200 -> "§c"
                playerPing > 100 -> "§e"
                else -> "§a"
            }

            return " §7[" + coloringBasedOnPing + playerPing + "ms§7]"
        }

    private val healthText: String
        get() {
            if (entity !is LivingEntity) {
                return ""
            }

            val actualHealth = entity.getActualHealth(ModuleNametags.Health.fromScoreboard).toInt()

            val healthColor = when {
                // Perhaps you should modify the values here
                actualHealth >= 14 -> "§a"
                actualHealth >= 8 -> "§e"
                else -> "§c"
            }

            return "$healthColor$actualHealth HP"

        }
}
