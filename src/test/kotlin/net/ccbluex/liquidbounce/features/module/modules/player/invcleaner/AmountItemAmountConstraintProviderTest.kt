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

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.ItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.PrimitiveItemFacet
import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.inventory.VirtualItemSlot
import net.minecraft.world.item.ItemStack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AmountItemAmountConstraintProviderTest {

    companion object {
        init { MinecraftBootstrap.ensureInitialized() }
    }

    @Test
    fun `a configured rule replaces the fallback limit of the same category`() {
        val provider = AmountItemAmountConstraintProvider(
            configuredItemsInSpecificCategories = listOf(listOf(ItemCategory(GenericItemType.ARROW)) to 32),
            fallbackItemsInSpecificCategories = listOf(listOf(ItemCategory(GenericItemType.ARROW)) to 96),
        )

        val constraint = provider.getConstraints(facet(GenericItemType.ARROW)).single()

        assertEquals(32..Int.MAX_VALUE, constraint.group.acceptableRange)
        assertFalse(constraint.default)
    }

    @Test
    fun `applies the fallback limit of a category without a rule`() {
        val provider = AmountItemAmountConstraintProvider(
            fallbackItemsInSpecificCategories = listOf(listOf(ItemCategory(GenericItemType.ARROW)) to 96),
        )

        val constraint = provider.getConstraints(facet(GenericItemType.ARROW)).single()

        assertEquals(96..Int.MAX_VALUE, constraint.group.acceptableRange)
        assertTrue(constraint.default)
    }

    @Test
    fun `counts the food points a stack provides`() {
        val provider = AmountItemAmountConstraintProvider(
            fallbackAmountPerFunction = mapOf(ItemFunction.FOOD to 64),
        )

        val constraint = provider.getConstraints(foodFacet(foodPoints = 8)).single()

        assertEquals(64..Int.MAX_VALUE, constraint.group.acceptableRange)
        assertEquals(8, constraint.amountAddedByItem)
    }

    @Test
    fun `processes the categories with a rule before the others`() {
        val provider = AmountItemAmountConstraintProvider(
            configuredItemsInSpecificCategories = listOf(
                listOf(ItemCategory(GenericItemType.FOOD)) to 64,
                listOf(ItemCategory(GenericItemType.BLOCK)) to 512,
            ),
        )

        val firstRule = provider.getAllocationPriority(ItemCategory(GenericItemType.FOOD))
        val secondRule = provider.getAllocationPriority(ItemCategory(GenericItemType.BLOCK))
        val withoutRule = provider.getAllocationPriority(ItemCategory(GenericItemType.POTION))

        assertTrue(firstRule < secondRule)
        assertTrue(secondRule < withoutRule)
    }

    private fun facet(type: GenericItemType): ItemFacet =
        PrimitiveItemFacet(EMPTY_SLOT, ItemCategory(type))

    private fun foodFacet(foodPoints: Int): ItemFacet = object : ItemFacet(EMPTY_SLOT) {
        override val providedItemFunctions = listOf(ProvidedFunction(ItemFunction.FOOD, foodPoints))
    }

    private val EMPTY_SLOT: ItemSlot = VirtualItemSlot(ItemStack.EMPTY, ItemSlot.Type.INVENTORY, 0)

}
