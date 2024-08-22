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

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.TagEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.client.stripMinecraftColorCodes
import net.ccbluex.liquidbounce.utils.inventory.getArmorColor
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Text
import java.awt.Color

/**
 * Teams module
 *
 * Prevents KillAura from attacking teammates.
 */
object ModuleTeams : Module("Teams", Category.MISC) {

    private val scoreboard by boolean("ScoreboardTeam", true)
    private val nameColor by boolean("NameColor", true)
    private val prefix by boolean("Prefix", false)

    private object Armor : ToggleableConfigurable(this, "ArmorColor", true) {
        val helmet by boolean("Helmet", true)
        val chestPlate by boolean("Chestplate", false)
        val pants by boolean("Pants", false)
        val boots by boolean("Boots", false)
    }

    init {
        tree(Armor)
    }

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
    private fun isInClientPlayersTeam(entity: LivingEntity): Boolean {
        if (scoreboard && player.isTeammate(entity)) {
            return true
        }

        val clientDisplayName = player.displayName
        val targetDisplayName = entity.displayName

        if (clientDisplayName == null || targetDisplayName == null) {
            return false
        }

        return checkName(clientDisplayName, targetDisplayName) ||
            checkPrefix(targetDisplayName, clientDisplayName) ||
            checkArmor(entity)
    }

    /**
     * Checks if both names have the same color.
     */
    private fun checkName(clientDisplayName: Text, targetDisplayName: Text): Boolean {
        if (!nameColor) {
            return false
        }

        val targetColor = clientDisplayName.style.color
        val clientColor = targetDisplayName.style.color

        return targetColor != null && clientColor != null && targetColor == clientColor
    }

    /**
     * Prefix check - this works on Hypixel BedWars, GommeHD Skywars and many other servers.
     */
    private fun checkPrefix(targetDisplayName: Text, clientDisplayName: Text): Boolean {
        if (!prefix) {
            return false
        }

        val targetName = targetDisplayName.string
            .stripMinecraftColorCodes()
        val clientName = clientDisplayName.string
            .stripMinecraftColorCodes()
        val targetSplit = targetName.split(" ")
        val clientSplit = clientName.split(" ")

        // Check if both names have a prefix
        return targetSplit.size > 1 && clientSplit.size > 1 && targetSplit[0] == clientSplit[0]
    }

    /**
     * Checks if the color of any armor piece matches.
     */
    private fun checkArmor(entity: LivingEntity): Boolean {
        if (!Armor.enabled || entity !is PlayerEntity) {
            return false
        }

        val hasMatchingArmorColor = listOf(
            Armor.helmet to 3,
            Armor.chestPlate to 2,
            Armor.pants to 1,
            Armor.boots to 0
        ).any { (enabled, slot) ->
            enabled && matchesArmorColor(entity, slot)
        }

        return hasMatchingArmorColor
    }

    /**
     * Checks if the color of the item in the [armorSlot] of
     * the [player] matches the user's armor color in the same slot.
     */
    private fun matchesArmorColor(player: PlayerEntity, armorSlot: Int): Boolean {
        val ownStack = this.player.inventory.getArmorStack(armorSlot)
        val otherStack = player.inventory.getArmorStack(armorSlot)

        // returns false if the armor is not dyeable (e.g., iron armor)
        // to avoid a false positive from `null == null`
        val ownColor = ownStack.getArmorColor() ?: return false
        val otherColor = otherStack.getArmorColor() ?: return false

        return ownColor == otherColor
    }

    /**
     * Returns the team color of the [entity] or null if the entity is not in a team.
     */
    fun getTeamColor(entity: Entity)
        = entity.displayName?.style?.color?.rgb?.let { Color4b(Color(it)) }
}
