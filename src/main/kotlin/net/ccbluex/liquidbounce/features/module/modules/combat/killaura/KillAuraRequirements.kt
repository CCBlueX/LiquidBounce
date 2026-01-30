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
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEqual1_8
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.input.InputTracker.isPressedOnAny
import net.ccbluex.liquidbounce.utils.input.InputTracker.wasPressedRecently
import net.ccbluex.liquidbounce.utils.item.getEnchantment
import net.ccbluex.liquidbounce.utils.item.isAxe
import net.ccbluex.liquidbounce.utils.item.isSword
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.MaceItem
import net.minecraft.world.item.enchantment.Enchantments
import java.util.function.BooleanSupplier

@Suppress("unused")
enum class KillAuraRequirements(
    override val tag: String,
) : Tagged, BooleanSupplier {
    CLICK("Click"),
    WEAPON("Weapon"),
    VANILLA_NAME("VanillaName"),
    NOT_BREAKING("NotBreaking");

    override fun getAsBoolean(): Boolean =
        when (this) {
            CLICK -> mc.options.keyAttack.isPressedOnAny || mc.options.keyAttack.wasPressedRecently(250)
            WEAPON -> player.mainHandItem.isWeapon()
            VANILLA_NAME -> player.mainHandItem.customName == null
            NOT_BREAKING -> mc.gameMode?.isDestroying == false
        }
}

/**
 * Check if the item is a weapon.
 */
private fun ItemStack.isWeapon() = this.isSword || !isOlderThanOrEqual1_8 && this.isAxe
    || this.item is MaceItem || this.getEnchantment(Enchantments.KNOCKBACK) > 0
