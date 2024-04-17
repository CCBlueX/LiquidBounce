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
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.events.TagEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.client.stripMinecraftColorCodes
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import java.awt.Color

/**
 * Teams module
 *
 * Prevents KillAura from attacking teammates.
 */

object ModuleTeams : Module("Teams", Category.MISC) {

    private val scoreboard by boolean("ScoreboardTeam", true)
    private val color by boolean("Color", true)
    private val prefix by boolean("Prefix", false)

    val entityTagEvent = handler<TagEntityEvent> {
        val entity = it.entity

        if (entity is LivingEntity && isInClientPlayersTeam(entity)) {
            it.dontTarget()
        }

        getTeamColor(entity)?.let { color ->
            it.color(color, Priority.IMPORTANT_FOR_USAGE_1)
        }
    }

    /**
     * Check if [entity] is in your own team using scoreboard, name color or team prefix
     */
    private fun isInClientPlayersTeam(entity: LivingEntity): Boolean {
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

            // Check if both names have a prefix
            if (targetSplit.size > 1 && clientSplit.size > 1 && targetSplit[0] == clientSplit[0]) {
                return true
            }
        }

        return false
    }

    /**
     * Returns the team color of the [entity] or null if the entity is not in a team.
     */
    fun getTeamColor(entity: Entity)
        = entity.displayName?.style?.color?.rgb?.let { Color4b(Color(it)) }
}
