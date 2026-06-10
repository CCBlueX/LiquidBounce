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

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.platform.Platform
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.neoforged.fml.ModList
import net.neoforged.fml.loading.FMLPaths
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent
import java.nio.file.Path
import java.util.function.Supplier

/**
 * [Platform] implementation for the NeoForge loader.
 */
class NeoForgePlatform : Platform {

    override val loaderName = "neoforge"

    override val gameDirectory: Path
        get() = FMLPaths.GAMEDIR.get()

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

    /**
     * The tab is built with the first free column of the top row, because
     * `CreativeModeTabs.validate()` requires a unique position for every tab in
     * the registry. The actual placement is handled by NeoForge's tab paging.
     */
    override fun buildCreativeTab(
        title: Component,
        icon: Supplier<ItemStack>,
        displayItems: CreativeModeTab.DisplayItemsGenerator
    ): CreativeModeTab? {
        val freeColumn = BuiltInRegistries.CREATIVE_MODE_TAB
            .filter { tab -> tab.row() == CreativeModeTab.Row.TOP }
            .maxOf(CreativeModeTab::column) + 1

        return CreativeModeTab.builder(CreativeModeTab.Row.TOP, freeColumn)
            .title(title)
            .icon(icon)
            .displayItems(displayItems)
            .build()
    }

    /**
     * NeoForge collects reload listeners through [AddClientReloadListenersEvent] on the
     * mod event bus, which fires after the client start hook. Listeners registered
     * before that are buffered until [LiquidBounceNeoForge] forwards the event here.
     */
    override fun registerResourceReloadListener(id: String, listener: PreparableReloadListener): Boolean {
        synchronized(bufferedReloadListeners) {
            if (reloadListenersCollected) {
                return false
            }

            bufferedReloadListeners[LiquidBounce.identifier(id)] = listener
        }

        return true
    }

    companion object {

        private val bufferedReloadListeners = LinkedHashMap<Identifier, PreparableReloadListener>()
        private var reloadListenersCollected = false

        @JvmStatic
        fun onAddReloadListeners(event: AddClientReloadListenersEvent) {
            synchronized(bufferedReloadListeners) {
                reloadListenersCollected = true
                bufferedReloadListeners.forEach(event::addListener)
                bufferedReloadListeners.clear()
            }
        }

    }

}
