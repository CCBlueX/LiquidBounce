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

import net.ccbluex.liquidbounce.features.module.modules.player.ModuleFastExp
import net.minecraft.world.item.Items

private const val BOTTLES = 64
private const val INVENTORY_SLOT = 20
private const val IDLE_TICKS = 40
private const val START_TICKS = 100

/**
 * FastExp only reaches for bottles in the main inventory with FromInventory enabled.
 */
fun InventoryBench.checkFastExp() {
    command("item replace entity @a container.$INVENTORY_SLOT with minecraft:experience_bottle $BOTTLES")

    withModule(ModuleFastExp, {
        ModuleFastExp.set("NoWaste", "Enabled", value = "false")
        ModuleFastExp.set("FromInventory", "Enabled", value = "false")
    }) {
        context.waitTicks(IDLE_TICKS)
        check(count(Items.EXPERIENCE_BOTTLE) == BOTTLES) { "bottles were thrown from the main inventory" }
    }

    command("item replace entity @a container.$INVENTORY_SLOT with minecraft:experience_bottle $BOTTLES")

    withModule(ModuleFastExp, {
        ModuleFastExp.set("NoWaste", "Enabled", value = "false")
        ModuleFastExp.set("FromInventory", "Enabled", value = "true")
    }) {
        waitUntil(START_TICKS, { "no bottle was taken from the main inventory and thrown" }) {
            count(Items.EXPERIENCE_BOTTLE) < BOTTLES
        }
        check(stackAt(INVENTORY_SLOT).isEmpty) { "the bottles stayed in the main inventory" }
    }
}
