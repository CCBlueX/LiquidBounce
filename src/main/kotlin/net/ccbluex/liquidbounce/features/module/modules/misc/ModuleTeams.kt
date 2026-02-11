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
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.fastutil.enumSetOf
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.TagEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.stripMinecraftColorCodes
import net.ccbluex.liquidbounce.utils.inventory.EquipmentSlotChoice
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import java.util.function.Predicate

/**
 * Teams module
 *
 * Prevents KillAura from attacking teammates.
 */
object ModuleTeams : ClientModule("Teams", ModuleCategories.MISC) {

    private val matches by multiEnumChoice(
        "Matches",
        enumSetOf(Matches.SCOREBOARD_TEAM, Matches.NAME_COLOR),
    )

    private val armorColorSlots by multiEnumChoice(
        "ArmorColor",
        enumSetOf(EquipmentSlotChoice.HEAD),
        EquipmentSlotChoice.allHumanoidArmor(),
    )

    private val setColorFromArmor by boolean("SetColorFromArmor", true)

    @Suppress("unused")
    private val entityTagEvent = handler<TagEntityEvent> { event ->
        val entity = event.entity

        if (entity is LivingEntity && isInClientPlayersTeam(entity)) {
            event.dontTarget()
        }

        // Team color
        val color = entity.team?.color?.color
            ?: if (entity is LivingEntity && setColorFromArmor && armorColorSlots.isNotEmpty()) {
                armorColorSlots.firstNotNullOfOrNull { it.getArmorColor(entity) }
            } else {
                null
            }

        event.color(Color4b.fullAlpha(color ?: return@handler), Priority.IMPORTANT_FOR_USAGE_1)
    }

    /**
     * Check if [entity] is in your own team using scoreboard,
     * name color, armor color or team prefix.
     */
    private fun isInClientPlayersTeam(entity: LivingEntity) =
        matches.any { it.testMatches.test(entity) } || checkArmor(entity)

    /**
     * Checks if the color of any armor piece matches.
     */
    private fun checkArmor(entity: LivingEntity) =
        entity is Player && armorColorSlots.any { it.matchesArmorColor(entity) }

    @Suppress("unused")
    private enum class Matches(
        override val tag: String,
        val testMatches: Predicate<LivingEntity>,
    ) : Tagged {
        /**
         * Check if [LivingEntity] is in your own team using scoreboard,
         */
        SCOREBOARD_TEAM("ScoreboardTeam", { suspected ->
            player.isAlliedTo(suspected)
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

    /**
     * Checks if the color of the item in the [EquipmentSlotChoice.slot] of
     * the [player] matches the user's armor color in the same slot.
     */
    private fun EquipmentSlotChoice.matchesArmorColor(suspected: Player): Boolean {
        // returns false if the armor is not dyeable (e.g., iron armor)
        // to avoid a false positive from `null == null`
        val ownColor = getArmorColor(player) ?: return false
        val otherColor = getArmorColor(suspected) ?: return false

        return ownColor == otherColor
    }
}
