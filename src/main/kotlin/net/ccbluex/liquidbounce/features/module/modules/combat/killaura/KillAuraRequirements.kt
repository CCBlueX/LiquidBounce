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
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEqual1_8
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.input.InputTracker.isPressedOnAny
import net.minecraft.item.AxeItem
import net.minecraft.item.Item
import net.minecraft.item.MaceItem
import net.minecraft.item.SwordItem

@Suppress("unused")
enum class KillAuraRequirements(
    override val choiceName: String,
    val meets: () -> Boolean
) : NamedChoice {
    CLICK("Click", {
        mc.options.attackKey.isPressedOnAny
    }),
    WEAPON("Weapon", {
        player.inventory.mainHandStack.item.isWeapon()
    });
}

/**
 * Check if the item is a weapon.
 */
private fun Item.isWeapon() = this is SwordItem || !isOlderThanOrEqual1_8 && this is AxeItem
    || this is MaceItem
