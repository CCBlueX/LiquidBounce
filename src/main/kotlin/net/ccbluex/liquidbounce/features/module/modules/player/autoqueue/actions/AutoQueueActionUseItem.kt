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
 *
 *
 */
package net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.actions

import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.ModuleAutoQueue
import net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.actions.AutoQueueActionUseItem.itemName
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.convertToString
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.minecraft.item.ItemStack
import net.minecraft.util.Hand

object AutoQueueActionUseItem : AutoQueueAction("UseItem") {

    /**
     * The [itemName] of the item to click in order to queue.
     */
    private val itemName by text("Name", "Paper")

    override suspend fun execute(sequence: Sequence) {
        val item = Slots.Hotbar.findSlot { itemStack: ItemStack ->
            itemStack.name.convertToString().contains(itemName)
        } ?: return

        SilentHotbar.selectSlotSilently(ModuleAutoQueue, item, 20)
        sequence.waitTicks(1)
        interaction.interactItem(player, Hand.MAIN_HAND)
    }

}
