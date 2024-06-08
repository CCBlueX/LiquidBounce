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
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.stripMinecraftColorCodes
import net.ccbluex.liquidbounce.utils.inventory.getArmorColor
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Text

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

        if (
            Armor.helmet && matchesArmorColor(entity, 3) ||
            Armor.chestPlate && matchesArmorColor(entity, 2) ||
            Armor.pants && matchesArmorColor(entity, 1) ||
            Armor.boots && matchesArmorColor(entity, 0)
        ) {
            return true
        }

        return false
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

}
