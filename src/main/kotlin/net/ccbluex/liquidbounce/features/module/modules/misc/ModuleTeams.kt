/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.entity.LivingEntity

/**
 * Teams module
 *
 * Prevents KillAura from attacking teammates.
 */

object ModuleTeams : Module("Teams", Category.MISC) {

    private val scoreboard by boolean("ScoreboardTeam", true)
    private val color by boolean("Color", true)
    private val gommeSW by boolean("GommeSW", false)

    /**
     * Check if [entity] is in your own team using scoreboard, name color or team prefix
     */
    fun isInClientPlayersTeam(entity: LivingEntity): Boolean {
        if (!enabled) {
            return false
        }

        if (scoreboard && player.isTeammate(entity)) {
            return true
        }

        val displayName = player.displayName
        val entityDisplayName = entity.displayName

        if (displayName == null || entityDisplayName == null) {
            return false
        }

        val targetName = entityDisplayName.string.replace("§r", "")
        val clientName = displayName.string.replace("§r", "")

        if (targetName.length < 2 || clientName.length < 2) {
            return false
        }

        if (gommeSW) {
            if (targetName.startsWith("T") && clientName.startsWith("T")) {
                if (targetName[1].isDigit() && clientName[1].isDigit()) {
                    return targetName[1] == clientName[1]
                }
            }
        }

        if (color && entity.displayName != null) {
            return targetName.startsWith("§${clientName[1]}")
        }

        return false
    }

}
