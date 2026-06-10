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
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.neoforged.fml.ModList
import net.neoforged.fml.loading.FMLPaths
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
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
     * NeoForge collects reload listeners through [AddClientReloadListenersEvent], which
     * fires before the client start hook where the listeners are registered (and the
     * resource manager rejects registrations once the event's sorted listener list has
     * been applied). The event therefore registers a lazy wrapper per known listener id,
     * and this method binds the actual listener as the wrapper's delegate. The wrappers
     * are part of the resource manager's listener list from the start, so the delegates
     * take part in the initial resource load and in every manual reload (F3+T).
     *
     * Ids not in [KNOWN_LISTENER_IDS] are rejected, which callers handle with their
     * direct-reload fallback.
     */
    override fun registerResourceReloadListener(id: String, listener: PreparableReloadListener): Boolean {
        if (id !in KNOWN_LISTENER_IDS) {
            return false
        }

        reloadListenerDelegates[id] = listener
        return true
    }

    /**
     * Forwards reloads to the listener registered under [id], or releases the
     * preparation barrier untouched while no listener is bound yet.
     */
    private class LazyReloadListener(private val id: String) : PreparableReloadListener {

        private val delegate: PreparableReloadListener?
            get() = reloadListenerDelegates[id]

        override fun prepareSharedState(state: PreparableReloadListener.SharedState) {
            delegate?.prepareSharedState(state)
        }

        override fun reload(
            state: PreparableReloadListener.SharedState,
            backgroundExecutor: Executor,
            barrier: PreparableReloadListener.PreparationBarrier,
            gameExecutor: Executor
        ): CompletableFuture<Void> {
            val delegate = this.delegate
                ?: return barrier.wait(net.minecraft.util.Unit.INSTANCE).thenAccept { }

            return delegate.reload(state, backgroundExecutor, barrier, gameExecutor)
        }

        override fun getName() = "LiquidBounce/$id"

    }

    companion object {

        /**
         * The reload listeners LiquidBounce registers during client start. New listener
         * ids must be added here to participate in resource reloads on NeoForge.
         */
        private val KNOWN_LISTENER_IDS = listOf("client_resources", "theme")

        private val reloadListenerDelegates = ConcurrentHashMap<String, PreparableReloadListener>()

        @JvmStatic
        fun onAddReloadListeners(event: AddClientReloadListenersEvent) {
            for (id in KNOWN_LISTENER_IDS) {
                event.addListener(LiquidBounce.identifier(id), LazyReloadListener(id))
            }
        }

    }

}
