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
package net.ccbluex.liquidbounce.features.creativetab

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.utils.text.asPlainText
import net.ccbluex.liquidbounce.utils.client.logger
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * An item group from the client
 */
open class CustomCreativeModeTab(
    val plainName: String,
    val icon: Supplier<ItemStack>,
    val items: Consumer<CreativeModeTab.Output>,
) {

    fun init(): CreativeModeTab {
        val creativeTab = FabricCreativeModeTab.builder()
            .title(plainName.asPlainText())
            .icon(icon)
            .displayItems { _, entries ->
                runCatching {
                    items.accept(entries)
                }.onFailure {
                    logger.error("Unable to create creative tab $plainName", it)
                }
            }
            .build()

        // Add a creative tab to creative inventory
        Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            LiquidBounce.identifier(plainName.lowercase()),
            creativeTab
        )

        return creativeTab
    }

}
