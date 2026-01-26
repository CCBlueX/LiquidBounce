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
package net.ccbluex.liquidbounce.features.itemgroup.groups

import net.ccbluex.liquidbounce.features.itemgroup.ClientItemGroup
import net.ccbluex.liquidbounce.features.itemgroup.ClientItemGroups
import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks

class ContainerItemGroup : ClientItemGroup(
    "Containers",
    icon = { ItemStack(Blocks.CHEST) },
    items = {
        val stack = ItemStack(Blocks.CHEST)

        stack.set(DataComponents.CUSTOM_NAME, "Empty Chest".asPlainText())

        it.accept(stack)

        // Add all stored containers
        it.acceptAll(ClientItemGroups.containersAsItemStacks())
    }
)
