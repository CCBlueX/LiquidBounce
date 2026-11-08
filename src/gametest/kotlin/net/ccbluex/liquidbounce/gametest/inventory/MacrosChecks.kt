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

import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleMacros
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleMiddleClickAction
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.utils.collection.itemSortedSetOf
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import java.util.SequencedSet

private const val ITEM_SLOT = 3
private const val SWITCH_TICKS = 40

/**
 * The visible switch of Macros and MiddleClickAction selects the item's slot, uses it and selects the old slot again.
 */
fun InventoryBench.checkMacros() {
    command("item replace entity @a hotbar.$ITEM_SLOT with minecraft:snowball 16")
    withModule(ModuleMacros, {
        ModuleMacros.setting<InputConstants.Key>("Item-1", "Trigger").set(testKey)
        ModuleMacros.set("Item-1", "PickMode", value = "Item")
        // The release of the key would trigger the macro a second time
        ModuleMacros.set("Item-1", "TriggerDelay", value = "5000..5000")
        ModuleMacros.setting<SequencedSet<Item>>("Item-1", "PickMode", "Item", "Items")
            .set(itemSortedSetOf(Items.SNOWBALL))
        ModuleMacros.set("Item-1", "VisibleSwitch", "Enabled", value = "true")
    }) {
        context.input.pressKey(testKey)
        expectVisibleSwitch(Items.SNOWBALL, 16)
    }

    command("item replace entity @a hotbar.$ITEM_SLOT with minecraft:ender_pearl 4")
    withModule(ModuleMiddleClickAction, {
        ModuleMiddleClickAction.set("Mode", value = "Pearl")
        ModuleMiddleClickAction.set("Mode", "Pearl", "VisibleSwitch", "Enabled", value = "true")
    }) {
        context.input.holdKey { options -> options.keyPickItem }
        context.waitTicks(2)
        context.input.releaseKey { options -> options.keyPickItem }
        expectVisibleSwitch(Items.ENDER_PEARL, 4)
    }
}

private fun InventoryBench.expectVisibleSwitch(item: Item, initial: Int) {
    waitUntil(SWITCH_TICKS, { "the hotbar did not visibly switch to the ${item.descriptionId}" }) {
        context.fromClient { client -> checkNotNull(client.player).inventory.selectedSlot } == ITEM_SLOT
    }
    waitUntil(SWITCH_TICKS, { "the ${item.descriptionId} was not used" }) { count(item) < initial }
    waitUntil(SWITCH_TICKS, { "the hotbar did not switch back" }) {
        serverSlot == 0 && context.fromClient { client -> checkNotNull(client.player).inventory.selectedSlot } == 0
    }
}
