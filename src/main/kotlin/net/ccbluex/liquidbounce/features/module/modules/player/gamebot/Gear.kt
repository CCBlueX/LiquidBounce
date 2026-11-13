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
package net.ccbluex.liquidbounce.features.module.modules.player.gamebot

import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.item.attackDamage
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import java.util.Locale

/**
 * How well a player is equipped, from what can be seen of them: armor points, weapon damage, enchanted pieces and
 * health. On 1.8 full iron armor with an iron sword makes [equipment] [GOOD].
 */
internal class Gear private constructor(
    val armor: Double,
    val damage: Double,
    val enchanted: Int,
    /** Health from 0 to 1; 1 where the server sends none. */
    val health: Double,
) {

    val equipment = armor + damage * DAMAGE_WEIGHT + enchanted * ENCHANT_WEIGHT

    /** [equipment] scaled down with missing health, to compare two players. */
    val score = equipment * (HEALTH_FLOOR + (1.0 - HEALTH_FLOOR) * health)

    /** 1 with nothing, down to 0 once [equipment] reaches [GOOD]. */
    val weakness = (1.0 - equipment / GOOD).coerceIn(0.0, 1.0)

    override fun toString() = "%.1f[a%.0f d%.1f e%d hp%.0f%%]".format(
        Locale.ROOT, score, armor, damage, enchanted, health * PERCENT
    )

    companion object {

        /** The bot with its best weapon anywhere in the inventory. */
        fun own(): Gear {
            val self = player
            val weapon = Slots.All.maxByOrNull { it.itemStack.attackDamage }?.itemStack
            val enchanted = Opponents.ARMOR_SLOTS.count { self.getItemBySlot(it).isEnchanted } +
                if (weapon?.isEnchanted == true) 1 else 0
            return Gear(
                Opponents.armorPoints(self), weapon?.attackDamage ?: 0.0, enchanted, healthOf(self)
            )
        }

        /** An opponent with the best weapon it was seen holding. */
        fun of(other: Player): Gear {
            val enchanted = Opponents.ARMOR_SLOTS.count { other.getItemBySlot(it).isEnchanted } +
                if (other.mainHandItem.isEnchanted) 1 else 0
            return Gear(Opponents.armorPoints(other), Opponents.weaponDamage(other), enchanted, healthOf(other))
        }

        private fun healthOf(entity: LivingEntity): Double {
            val health = entity.health
            return if (health > 0f) (health / entity.maxHealth).toDouble().coerceIn(0.0, 1.0) else 1.0
        }

        /** Full iron armor and an iron sword on 1.8: 15 + 6 * [DAMAGE_WEIGHT]. */
        const val GOOD = 27.0

        private const val DAMAGE_WEIGHT = 2.0
        private const val ENCHANT_WEIGHT = 1.5

        /** At no health a player still counts with this share of its equipment. */
        private const val HEALTH_FLOOR = 0.5
        private const val PERCENT = 100
    }

}
