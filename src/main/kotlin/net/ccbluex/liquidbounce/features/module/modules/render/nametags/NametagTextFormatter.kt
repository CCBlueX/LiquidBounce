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
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.entity.ping
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.scoreboard.ScoreboardDisplaySlot
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

        outputBuilder.append("${this.nameColor}${entity.displayName!!.string}")

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
        get() = when {
            isBot -> "§3"
            entity.isInvisible -> "§6"
            entity.isSneaking -> "§4"
            else -> "§7"
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

            var health = entity.health.toInt()

            if (ModuleNametags.Health.fromScoreboard) {
                entity.world.scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME)?.let { objective ->
                    // todo: check if this still works after updating to 1.20.4
                    objective.scoreboard.getScore(entity, objective)?.let { scoreboard ->
                        if (scoreboard.score > 0 && objective.displayName?.string == "❤") {
                            health = scoreboard.score
                        }
                    }
                }
            }

            return "§c${health} HP"
        }
}
