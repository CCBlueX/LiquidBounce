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
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.TagEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.stripMinecraftColorCodes
import net.ccbluex.liquidbounce.utils.inventory.getArmorColor
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import java.awt.Color

/**
 * Teams module
 *
 * Prevents KillAura from attacking teammates.
 */
object ModuleTeams : ClientModule("Teams", Category.MISC) {

    private val matches by multiEnumChoice("Matches",
        Matches.SCOREBOARD_TEAM,
        Matches.NAME_COLOR
    )

    private val armorColor by multiEnumChoice("ArmorColor",
        ArmorColor.HELMET
    )

    @Suppress("unused")
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
     * Check if [entity] is in your own team using scoreboard,
     * name color, armor color or team prefix.
     */
    @Suppress("ReturnCount")
    private fun isInClientPlayersTeam(entity: LivingEntity) =
        matches.any { it.testMatches(entity) } || checkArmor(entity)

    /**
     * Checks if the color of any armor piece matches.
     */
    private fun checkArmor(entity: LivingEntity) =
        entity is PlayerEntity && armorColor.any { it.matchesArmorColor(entity) }

    /**
     * Returns the team color of the [entity] or null if the entity is not in a team.
     */
    private fun getTeamColor(entity: Entity)
        = entity.displayName?.style?.color?.rgb?.let { Color4b(Color(it)) }

    @Suppress("unused")
    private enum class Matches(
        override val choiceName: String,
        val testMatches: (suspected: LivingEntity) -> Boolean
    ) : NamedChoice {
        /**
         * Check if [LivingEntity] is in your own team using scoreboard,
         */
        SCOREBOARD_TEAM("ScoreboardTeam", { suspected ->
            player.isTeammate(suspected)
        }),

        /**
         * Checks if both names have the same color.
         */
        NAME_COLOR("NameColor", { suspected ->
            val targetColor = player.displayName?.style?.color
            val clientColor = suspected.displayName?.style?.color

            targetColor != null
                && clientColor != null
                && targetColor == clientColor
        }),

        /**
         * Prefix check - this works on Hypixel BedWars, GommeHD Skywars and many other servers.
         */
        PREFIX("Prefix", { suspected ->
            val targetSplit = suspected.displayName
                ?.string
                ?.stripMinecraftColorCodes()
                ?.split(" ")

            val clientSplit = player.displayName
                ?.string
                ?.stripMinecraftColorCodes()
                ?.split(" ")


            targetSplit != null
                && clientSplit != null
                && targetSplit.size > 1
                && clientSplit.size > 1
                && targetSplit[0] == clientSplit[0]
        })
    }

    @Suppress("unused", "MagicNumber")
    private enum class ArmorColor(
        override val choiceName: String,
        val slot: Int
    ) : NamedChoice {
        HELMET("Helmet", 3),
        CHESTPLATE("Chestplate", 2),
        PANTS("Pants", 1),
        BOOTS("Boots", 0);

        /**
         * Checks if the color of the item in the [slot] of
         * the [player] matches the user's armor color in the same slot.
         */
        @Suppress("ReturnCount")
        fun matchesArmorColor(suspected: PlayerEntity): Boolean {
            val ownStack = player.inventory.getArmorStack(slot)
            val otherStack = suspected.inventory.getArmorStack(slot)

            // returns false if the armor is not dyeable (e.g., iron armor)
            // to avoid a false positive from `null == null`
            val ownColor = ownStack.getArmorColor() ?: return false
            val otherColor = otherStack.getArmorColor() ?: return false

            return ownColor == otherColor
        }
    }
}
