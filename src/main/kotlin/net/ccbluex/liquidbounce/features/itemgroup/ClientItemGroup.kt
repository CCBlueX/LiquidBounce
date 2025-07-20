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
package net.ccbluex.liquidbounce.features.itemgroup

import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.logger
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import java.util.function.Supplier

/**
 * An item group from the client
 */
open class ClientItemGroup(
    val plainName: String,
    val icon: Supplier<ItemStack>,
    val items: (items: ItemGroup.Entries) -> Unit
) {

    // Create item group and assign to minecraft groups
    fun setup(): ItemGroup {
        // Expand array
        val itemGroup = FabricItemGroup.builder()
            .displayName(plainName.asText())
            .icon(icon)
            .entries { displayContext, entries ->
                runCatching {
                    items(entries)
                }.onFailure {
                    logger.error("Unable to create item group $plainName", it)
                }
            }
            .build()

        // Add tab to creative inventory
        Registry.register(Registries.ITEM_GROUP, Identifier.of("liquidbounce", plainName.lowercase()), itemGroup)

        return itemGroup
    }

}
