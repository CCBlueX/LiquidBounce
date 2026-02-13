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

import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.minecraft.world.inventory.AbstractCraftingMenu
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.display.SlotDisplayContext

/**
 * AutoCrafter module
 *
 * Automatically crafts items in the specified order using the Recipe Book.
 */
object ModuleAutoCrafter : ClientModule("AutoCrafter", ModuleCategories.PLAYER) {

    private val targetItems by itemList(
        "TargetItems", mutableListOf(
            Items.POLISHED_DEEPSLATE, Items.DEEPSLATE_BRICKS, Items.DEEPSLATE_TILES
        )
    )

    private val craftInStacks by boolean("CraftInStacks", true)
    private val delay by intRange("Delay", 2..3, 1..20, "ticks")
    private val allowInventoryCrafting by boolean("AllowInventoryCrafting", false)
    private val craftSequentially by boolean("CraftSequentially", true)
    private var timer = 0

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        val menu = player.containerMenu as? AbstractCraftingMenu ?: return@handler
        if (menu is InventoryMenu && !allowInventoryCrafting) return@handler
        if (++timer < delay.random()) return@handler

        val context = SlotDisplayContext.fromLevel(mc.level ?: return@handler)
        val collections = player.recipeBook.collections

        for ((index, item) in targetItems.withIndex()) {
            val itemsToCraftLater = targetItems.drop(index + 1)

            val recipe = collections.firstNotNullOfOrNull { collection ->
                collection.recipes.firstOrNull { recipe ->
                    recipe.resultItems(context).any { it.item == item } &&
                        collection.isCraftable(recipe.id) &&
                        // Prevent crafting loops (ingot->block->ingot) by rejecting recipes that use items crafted later
                        recipe.craftingRequirements.map { requirements ->
                            requirements.none { req -> itemsToCraftLater.any { req.test(ItemStack(it)) } }
                        }.orElse(true)
                }
            } ?: continue

            val resultSlot = menu.getSlot(0)
            if (resultSlot.item.isEmpty) {
                interaction.handlePlaceRecipe(menu.containerId, recipe.id(), craftInStacks)
            } else {
                val hasSpace = player.inventory.freeSlot != -1
                val clickType = if (hasSpace) ClickType.QUICK_MOVE else ClickType.THROW
                val mouseButton = if (hasSpace) 0 else 1
                interaction.handleInventoryMouseClick(
                    menu.containerId, 0, mouseButton, clickType, player
                )
            }
            timer = 0
            if (craftSequentially) return@handler
        }
    }
}
