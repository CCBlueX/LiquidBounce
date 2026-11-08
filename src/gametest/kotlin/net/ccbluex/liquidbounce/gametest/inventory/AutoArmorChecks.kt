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
package net.ccbluex.liquidbounce.gametest.inventory

import com.google.gson.JsonArray
import net.ccbluex.liquidbounce.config.gson.util.GSON_DEFAULT
import net.ccbluex.liquidbounce.features.module.modules.combat.autoarmor.ModuleAutoArmor
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items

private const val EQUIP_TICKS = 100
private const val SETTLE_TICKS = 20

/**
 * AutoArmor passes over diamond armor with a curse picked in SkipCursed and wears the plain iron piece instead.
 */
fun InventoryBench.checkAutoArmor() {
    checkSkipCursed("Binding", helmet = Items.DIAMOND_HELMET)
    checkSkipCursed("Binding", "Vanishing", helmet = Items.IRON_HELMET)
}

private fun InventoryBench.checkSkipCursed(vararg curses: String, helmet: Item) {
    command("item replace entity @a hotbar.0 with minecraft:diamond_chestplate[enchantments={binding_curse:1}]")
    command("item replace entity @a hotbar.1 with minecraft:iron_chestplate")
    command("item replace entity @a hotbar.2 with minecraft:diamond_helmet[enchantments={vanishing_curse:1}]")
    command("item replace entity @a hotbar.3 with minecraft:iron_helmet")

    withModule(ModuleAutoArmor, {
        ModuleAutoArmor.setting<Any>("SkipCursed").deserializeFrom(
            GSON_DEFAULT, JsonArray().apply { curses.forEach { add(it) } }
        )
    }) {
        fun worn() = onServer { player ->
            player.getItemBySlot(EquipmentSlot.CHEST).`is`(Items.IRON_CHESTPLATE) &&
                player.getItemBySlot(EquipmentSlot.HEAD).`is`(helmet)
        }

        val skipped = curses.joinToString()
        waitUntil(EQUIP_TICKS, { "skipping $skipped: the iron chestplate and the $helmet were not put on" }, ::worn)
        context.waitTicks(SETTLE_TICKS)
        check(worn()) { "skipping $skipped: the armor did not stay on" }
    }
}
