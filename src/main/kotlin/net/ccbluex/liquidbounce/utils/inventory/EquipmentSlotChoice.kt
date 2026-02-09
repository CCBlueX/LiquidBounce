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

package net.ccbluex.liquidbounce.utils.inventory

import net.ccbluex.fastutil.enumSetOf
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.minecraft.core.component.DataComponents
import net.minecraft.util.ARGB.opaque
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity

enum class EquipmentSlotChoice(
    override val tag: String,
    val slot: EquipmentSlot,
    override val tagAliases: List<String> = emptyList(),
) : Tagged {
    MAINHAND("Mainhand", EquipmentSlot.MAINHAND),
    OFFHAND("Offhand", EquipmentSlot.OFFHAND),
    FEET("Feet", EquipmentSlot.FEET, listOf("Boots")),
    LEGS("Legs", EquipmentSlot.LEGS, listOf("Pants")),
    CHEST("Chest", EquipmentSlot.CHEST, listOf("Chestplate")),
    HEAD("Head", EquipmentSlot.HEAD, listOf("Helmet")),
    BODY("Body", EquipmentSlot.BODY),
    SADDLE("Saddle", EquipmentSlot.SADDLE);

    fun getArmorColor(entity: LivingEntity): Int? {
        val itemStack = entity.getItemBySlot(this.slot)
        return itemStack[DataComponents.DYED_COLOR]?.rgb?.let { opaque(it) }
    }

    companion object {
        @JvmStatic
        fun allHumanoidArmor() = enumSetOf(FEET, LEGS, CHEST, HEAD)
    }
}
