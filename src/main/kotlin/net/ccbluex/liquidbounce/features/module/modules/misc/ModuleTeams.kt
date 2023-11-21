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
import net.ccbluex.liquidbounce.utils.client.stripMinecraftColorCodes
import net.minecraft.entity.LivingEntity

/**
 * Teams module
 *
 * Prevents KillAura from attacking teammates.
 */

object ModuleTeams : Module("Teams", Category.MISC) {

    private val scoreboard by boolean("ScoreboardTeam", true)
    private val color by boolean("Color", true)
    private val prefix by boolean("Prefix", false)

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

        val clientDisplayName = player.displayName
        val targetDisplayName = entity.displayName

        if (clientDisplayName == null || targetDisplayName == null) {
            return false
        }

        // Checks if both names have the same color
        if (color) {
            val targetColor = clientDisplayName.style.color
            val clientColor = targetDisplayName.style.color

            if (targetColor != null && clientColor != null && targetColor == clientColor) {
                return true
            }
        }

        // Prefix check - this works on Hypixel BedWars, GommeHD Skywars and many other servers
        if (prefix) {
            val targetName = targetDisplayName.string
                .stripMinecraftColorCodes()
            val clientName = clientDisplayName.string
                .stripMinecraftColorCodes()
            val targetSplit = targetName.split(" ")
            val clientSplit = clientName.split(" ")

            println(clientSplit)

            // Check if both names have a prefix
            if (targetSplit.size > 1 && clientSplit.size > 1 && targetSplit[0] == clientSplit[0]) {
                return true
            }
        }

        return false
    }

}
