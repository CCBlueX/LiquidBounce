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

package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.CleanupPlanTemplate.CleanupPlanRestrictions
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.CleanupPlanTemplate.CleanupPlanSlotContent
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.CleanupPlanTemplate.SlotContentPreference
import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.test.Test
import kotlin.test.assertEquals

class WishOrganizerTest {

    companion object {
        init { MinecraftBootstrap.ensureInitialized() }
    }

    @Test
    fun `fulfils the preferred wish of a slot before its alternatives`() {
        val sword = SlotContentPreference(GenericItemType.SWORD)
        val food = SlotContentPreference(GenericItemType.FOOD)

        val organizer = WishOrganizer(
            template(
                HotbarItemSlot.SLOT_0 to listOf(sword, food),
                HotbarItemSlot.SLOT_1 to listOf(food),
            )
        )

        assertEquals(
            listOf(
                HotbarItemSlot.SLOT_0 to sword,
                HotbarItemSlot.SLOT_1 to food,
                HotbarItemSlot.SLOT_0 to food,
            ),
            organizer.organizedWishes.map { it.targetSlot to it.wish }
        )
    }

    @Test
    fun `fulfils the wish for a specific item before a category wish`() {
        val specificItem = SlotContentPreference(
            GenericItemType.ANY_ITEM,
            setOf(ItemSubtype.SpecificItem(Items.DIAMOND_SWORD))
        )
        val category = SlotContentPreference(GenericItemType.SWORD)

        val organizer = WishOrganizer(
            template(
                HotbarItemSlot.SLOT_0 to listOf(category),
                HotbarItemSlot.SLOT_1 to listOf(specificItem),
            )
        )

        assertEquals(
            listOf(
                HotbarItemSlot.SLOT_1 to specificItem,
                HotbarItemSlot.SLOT_0 to category,
            ),
            organizer.organizedWishes.map { it.targetSlot to it.wish }
        )
    }

    @Test
    fun `shares one wish group between equal wishes of different slots`() {
        val block = SlotContentPreference(GenericItemType.BLOCK)

        val organizer = WishOrganizer(
            template(
                HotbarItemSlot.SLOT_0 to listOf(block),
                HotbarItemSlot.SLOT_1 to listOf(block),
            )
        )

        val (first, second) = organizer.organizedWishes
        assertEquals(first.id, second.id)
        assertEquals(
            setOf(ItemCategory(GenericItemType.BLOCK)),
            organizer.itemCategoryWishGroupMap.keys,
        )
    }

    private fun template(vararg targets: Pair<ItemSlot, List<SlotContentPreference>>) = CleanupPlanTemplate(
        targets.associate { (slot, wishes) -> slot to CleanupPlanSlotContent(wishes) },
        itemAmountConstraintProvider = AmountItemAmountConstraintProvider(),
        restrictions = CleanupPlanRestrictions(emptyMap()),
    )

}
