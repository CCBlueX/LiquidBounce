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
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.mergeableCapacityFor
import net.minecraft.client.gui.screens.recipebook.SearchRecipeBookCategory
import net.minecraft.world.inventory.AbstractFurnaceMenu
import net.minecraft.world.inventory.BlastFurnaceMenu
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.CraftingMenu
import net.minecraft.world.inventory.FurnaceMenu
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.world.inventory.RecipeBookMenu
import net.minecraft.world.inventory.RecipeBookType
import net.minecraft.world.inventory.SmokerMenu
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.display.SlotDisplayContext

/**
 * AutoCrafter module
 *
 * Automatically crafts items using the Recipe Book.
 */
object ModuleAutoCrafter : ClientModule("AutoCrafter", ModuleCategories.PLAYER) {

    private val itemsToCraft by itemList(
        "ItemsToCraft", mutableListOf(
            Items.POLISHED_DEEPSLATE, Items.DEEPSLATE_BRICKS, Items.DEEPSLATE_TILES
        )
    )

    private val delay by intRange("Delay", 2..3, 1..20, "ticks")
    private val stackCrafting by boolean("StackCrafting", true)
    private val sequentialCrafting by boolean("SequentialCrafting", true)
    private val onFull by enumChoice("OnFull", OnFull.WAIT)
    private val allowedContainers by multiEnumChoice("AllowedContainers", RecipeBookMenuType.CRAFTING_TABLE)

    @Suppress("unused")
    private val tickHandler = tickHandler {
        val menu = player.containerMenu
        if (menu !is RecipeBookMenu) return@tickHandler
        val menuType = RecipeBookMenuType.fromMenu(menu) ?: return@tickHandler
        if (menuType !in allowedContainers) return@tickHandler

        if (!player.recipeBook.bookSettings.isOpen(menuType.recipeBookType)) {
            return@tickHandler
        }

        val context = SlotDisplayContext.fromLevel(mc.level ?: return@tickHandler)
        val collections = player.recipeBook.getCollection(menuType.searchCategory)

        for ((index, targetItem) in itemsToCraft.withIndex()) {
            val currentMenu = player.containerMenu
            if (currentMenu !== menu) break

            val remainingItems = itemsToCraft.subList(index + 1, itemsToCraft.size)

            val recipe = collections.firstNotNullOfOrNull { collection ->
                collection.recipes.firstOrNull { recipe ->
                    recipe.resultItems(context).any { it.item == targetItem } &&
                        collection.isCraftable(recipe.id)

                        // Prevent crafting loops (ingot->block->ingot)
                        // by rejecting recipes that use items that appear later in the list
                        && (recipe.craftingRequirements.isEmpty ||
                        recipe.craftingRequirements.get()
                            .none { req -> remainingItems.any { req.test(it.defaultInstance) } })
                }
            } ?: continue

            val resultSlotId = if (menu is AbstractFurnaceMenu) 2 else 0
            val resultSlot = menu.slots[resultSlotId]
            if (resultSlot.item.isEmpty) {
                interaction.handlePlaceRecipe(menu.containerId, recipe.id, stackCrafting)
            } else {
                if (Slots.HotbarAndInventory.mergeableCapacityFor(resultSlot.item) >= resultSlot.item.count) {
                    interaction.handleContainerInput(
                        menu.containerId, resultSlotId, 0, ContainerInput.QUICK_MOVE, player
                    )
                } else {
                    when (onFull) {
                        OnFull.DISABLE -> {
                            enabled = false
                            return@tickHandler
                        }

                        OnFull.CLOSE_SCREEN -> {
                            player.closeContainer()
                            return@tickHandler
                        }

                        OnFull.WAIT -> {
                            waitTicks(delay.random())
                            return@tickHandler
                        }

                        OnFull.THROW -> {
                            interaction.handleContainerInput(
                                menu.containerId, resultSlotId, 1, ContainerInput.THROW, player
                            )
                        }
                    }
                }
            }
            waitTicks(delay.random())
            if (sequentialCrafting || menu is AbstractFurnaceMenu) return@tickHandler
        }

    }

    private enum class OnFull(override val tag: String) : Tagged {
        DISABLE("Disable"),
        CLOSE_SCREEN("CloseScreen"),
        WAIT("Wait"),
        THROW("Throw"),
    }

    private enum class RecipeBookMenuType(
        override val tag: String,
        val recipeBookType: RecipeBookType,
        val searchCategory: SearchRecipeBookCategory,
    ) : Tagged {
        INVENTORY("Inventory", RecipeBookType.CRAFTING, SearchRecipeBookCategory.CRAFTING),
        CRAFTING_TABLE("CraftingTable", RecipeBookType.CRAFTING, SearchRecipeBookCategory.CRAFTING),
        FURNACE("Furnace", RecipeBookType.FURNACE, SearchRecipeBookCategory.FURNACE),
        BLAST_FURNACE("BlastFurnace", RecipeBookType.BLAST_FURNACE, SearchRecipeBookCategory.BLAST_FURNACE),
        SMOKER("Smoker", RecipeBookType.SMOKER, SearchRecipeBookCategory.SMOKER);

        companion object {
            fun fromMenu(menu: RecipeBookMenu) = when (menu) {
                is InventoryMenu -> INVENTORY
                is CraftingMenu -> CRAFTING_TABLE
                is FurnaceMenu -> FURNACE
                is BlastFurnaceMenu -> BLAST_FURNACE
                is SmokerMenu -> SMOKER
                else -> null
            }
        }
    }
}
