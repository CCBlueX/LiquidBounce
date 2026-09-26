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

package net.ccbluex.liquidbounce.utils.item

import com.google.common.base.Predicates
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.MaceItem
import net.minecraft.world.item.enchantment.Enchantments
import java.util.function.Predicate

enum class WeaponType(
    override val tag: String,
    private val predicate: Predicate<ItemStack>,
): Tagged, Predicate<ItemStack> by predicate {
    ANY("Any", Predicates.alwaysTrue()),

    SWORD("Sword", { it.isSword }),
    AXE("Axe", { it.isAxe }),
    MACE("Mace", { it.item is MaceItem }),
    SPEAR("Spear", { it.isSpear }),

    PICKAXE("Pickaxe", { it.isPickaxe }),
    SHOVEL("Shovel", { it.isShovel }),
    HOE("Hoe", { it.isHoe }),

    KNOCKBACK("Knockback", { it.getEnchantment(Enchantments.KNOCKBACK) > 0 }),
    FIRE_ASPECT("FireAspect", { it.getEnchantment(Enchantments.FIRE_ASPECT) > 0 }),
}
