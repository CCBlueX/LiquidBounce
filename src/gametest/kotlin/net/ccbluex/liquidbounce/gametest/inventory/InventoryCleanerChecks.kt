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

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ModuleInventoryCleaner
import net.minecraft.world.item.Items

private const val GIVEN_TOTEMS = 5
private const val KEPT_TOTEMS = 2
private const val CLEAN_TICKS = 200
private const val HOLD_TICKS = 40

/**
 * InventoryCleaner keeps the configured number of totems, throws the rest and leaves the other items alone.
 */
fun InventoryBench.checkInventoryCleaner() {
    command("item replace entity @a hotbar.0 with minecraft:diamond_sword")
    command("give @a minecraft:totem_of_undying $GIVEN_TOTEMS")
    withModule(ModuleInventoryCleaner, {
        ModuleInventoryCleaner.set("MaximumTotems", value = "$KEPT_TOTEMS")
    }) {
        waitUntil(CLEAN_TICKS, { "kept ${count(Items.TOTEM_OF_UNDYING)} of $GIVEN_TOTEMS totems" }) {
            count(Items.TOTEM_OF_UNDYING) == KEPT_TOTEMS
        }

        // Thrown totems must not be picked up again while checking that the cleaner stops at the limit
        command("kill @e[type=item]")
        context.waitTicks(HOLD_TICKS)
        check(count(Items.TOTEM_OF_UNDYING) == KEPT_TOTEMS) {
            "kept ${count(Items.TOTEM_OF_UNDYING)} totems instead of $KEPT_TOTEMS"
        }
        check(count(Items.DIAMOND_SWORD) == 1) { "the sword was thrown" }
    }
}
