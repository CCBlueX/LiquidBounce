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

package net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.actions

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.ModuleAutoQueue
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.convertToString
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import java.util.function.Predicate

object AutoQueueActionUseItem : AutoQueueAction("UseItem") {

    private val mode = choices("Mode", 0) {
        arrayOf(Mode.Name, Mode.Item)
    }

    private sealed class Mode(name: String) : Choice(name), Predicate<ItemStack> {
        final override val parent: ChoiceConfigurable<*>
            get() = mode

        object Name : Mode("Name") {
            private val stackName by text("Name", "Paper")
            override fun test(itemStack: ItemStack): Boolean =
                itemStack.name.convertToString().contains(stackName)
        }

        object Item : Mode("Item") {
            private val slotItem by item("Item", Items.PAPER)
            override fun test(itemStack: ItemStack): Boolean =
                itemStack.isOf(slotItem)
        }
    }

    override suspend fun execute(sequence: Sequence) {
        val slot = Slots.OffhandWithHotbar.findSlot(mode.activeChoice::test) ?: return

        SilentHotbar.selectSlotSilently(ModuleAutoQueue, slot, 20)
        sequence.waitTicks(1)
        interaction.interactItem(player, slot.useHand)
    }

}
