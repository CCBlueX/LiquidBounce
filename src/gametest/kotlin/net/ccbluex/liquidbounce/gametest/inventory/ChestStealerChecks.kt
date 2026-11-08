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

import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.ModuleChestStealer
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.collection.itemSortedSetOf
import net.minecraft.world.Container
import net.minecraft.world.MenuProvider
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import java.util.SequencedSet

private const val STEAL_TICKS = 100

/**
 * With a whitelist ChestStealer takes only the listed items and closes the chest once only other items are left.
 */
fun InventoryBench.checkChestStealerItemFilter() {
    val chest = origin.offset(2, 0, 0)
    command("setblock ${chest.x} ${chest.y} ${chest.z} minecraft:chest")
    command("item replace block ${chest.x} ${chest.y} ${chest.z} container.0 with minecraft:diamond 3")
    command("item replace block ${chest.x} ${chest.y} ${chest.z} container.1 with minecraft:dirt 10")

    withModule(ModuleChestStealer, {
        ModuleChestStealer.setting<Filter>("Filter").set(Filter.WHITELIST)
        ModuleChestStealer.setting<SequencedSet<Item>>("Items").set(itemSortedSetOf(Items.DIAMOND))
    }) {
        onServer { player -> player.openMenu(player.level().getBlockEntity(chest) as MenuProvider) }

        waitUntil(STEAL_TICKS, { "the chest was not emptied of diamonds and closed" }) {
            onServer { player -> player.containerMenu === player.inventoryMenu } && count(Items.DIAMOND) == 3
        }
        check(count(Items.DIRT) == 0) { "dirt was taken although it is not whitelisted" }
        val dirtLeft = onServer { player -> (player.level().getBlockEntity(chest) as Container).countItem(Items.DIRT) }
        check(dirtLeft == 10) { "the chest holds $dirtLeft dirt instead of 10" }
    }
    command("setblock ${chest.x} ${chest.y} ${chest.z} minecraft:air")
}
