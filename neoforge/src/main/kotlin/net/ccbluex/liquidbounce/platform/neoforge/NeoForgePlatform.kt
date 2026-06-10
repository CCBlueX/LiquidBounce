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
package net.ccbluex.liquidbounce.platform.neoforge

import net.ccbluex.liquidbounce.platform.Platform
import net.minecraft.network.chat.Component
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.neoforged.fml.ModList
import java.util.function.Supplier

/**
 * [Platform] implementation for the NeoForge loader.
 */
class NeoForgePlatform : Platform {

    override val loaderName = "neoforge"

    override fun isModLoaded(id: String) = ModList.get().isLoaded(id)

    /**
     * NeoForge has no ModMenu equivalent whose mod list could be manipulated at
     * runtime, so hiding from the mod list is not supported.
     */
    override fun hideModsFromModList(ids: Collection<String>) = false

    override fun restoreModsInModList() = false

    /**
     * NeoForge's module-layer based loading has no way to remove a loaded mod at
     * runtime, so this is limited to a best-effort deletion of the jar file(s).
     */
    override fun removeModAndDeleteJars(id: String): Boolean {
        val modFile = ModList.get().getModFileById(id)?.file ?: return false

        runCatching {
            modFile.filePath.toFile().delete()
        }

        return true
    }

    override fun buildCreativeTab(
        title: Component,
        icon: Supplier<ItemStack>,
        displayItems: CreativeModeTab.DisplayItemsGenerator
    ): CreativeModeTab? = CreativeModeTab.builder()
        .title(title)
        .icon(icon)
        .displayItems(displayItems)
        .build()

}
