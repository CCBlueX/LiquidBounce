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
package net.ccbluex.liquidbounce.platform

import net.minecraft.network.chat.Component
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import java.nio.file.Path
import java.util.ServiceLoader
import java.util.function.Supplier

/**
 * Abstraction over the mod loader (Fabric, NeoForge) the client is running on.
 *
 * Implementations are discovered through [ServiceLoader]; exactly one implementation
 * is expected to be present on the classpath of each loader-specific distribution.
 */
interface Platform {

    /**
     * The game (run) directory of the running instance. Unlike
     * `Minecraft.getInstance().gameDirectory`, this is available before the
     * client instance exists.
     */
    val gameDirectory: Path

    /**
     * Whether the mod with the given [id] is currently loaded.
     */
    fun isModLoaded(id: String): Boolean

    /**
     * Hides the given mods from the loader's mod list UI (e.g. ModMenu),
     * remembering the removed entries so [restoreModsInModList] can add them back.
     *
     * @return true if the mod list UI was updated
     */
    fun hideModsFromModList(ids: Collection<String>): Boolean

    /**
     * Restores all mods previously hidden by [hideModsFromModList].
     *
     * @return true if the mod list UI was updated
     */
    fun restoreModsInModList(): Boolean

    /**
     * Best-effort removal of the mod with the given [id] from the running loader,
     * deleting its jar file(s) from disk. Used by the client self-destruct feature.
     *
     * @return true if the mod was found and removal was attempted
     */
    fun removeModAndDeleteJars(id: String): Boolean

    /**
     * Builds an unregistered creative mode tab, using the loader's extended builder
     * when available (e.g. Fabric API pagination support).
     *
     * @return the tab, or null when the loader has no support for additional
     * creative tabs available (e.g. Fabric API is missing)
     */
    fun buildCreativeTab(
        title: Component,
        icon: Supplier<ItemStack>,
        displayItems: CreativeModeTab.DisplayItemsGenerator
    ): CreativeModeTab?

    /**
     * Registers client resource reload listeners with the loader's resource
     * reloading mechanism. Each key is a listener id from
     * [CLIENT_RELOAD_LISTENER_IDS], a path unique within the `liquidbounce`
     * namespace.
     *
     * Registration is all or nothing: either every listener is registered and
     * will run with the (initial) resource reload, or none is and the caller
     * has to reload the listeners itself.
     *
     * @return true if the listeners were registered
     */
    fun registerResourceReloadListeners(listeners: Map<String, PreparableReloadListener>): Boolean

    companion object {

        const val RELOAD_LISTENER_CLIENT_RESOURCES = "client_resources"
        const val RELOAD_LISTENER_THEME = "theme"

        /**
         * Every listener id the client passes to [registerResourceReloadListeners]
         * during startup. Loaders that have to set up their reload integration
         * before the listeners exist (NeoForge) rely on this list being complete.
         */
        val CLIENT_RELOAD_LISTENER_IDS = listOf(
            RELOAD_LISTENER_CLIENT_RESOURCES,
            RELOAD_LISTENER_THEME,
        )

        /**
         * The [Platform] implementation of the mod loader we are running on.
         */
        @JvmStatic
        val current: Platform = run {
            val found = ServiceLoader.load(Platform::class.java, Platform::class.java.classLoader).toList()

            checkNotNull(found.singleOrNull()) {
                if (found.isEmpty()) {
                    "No Platform implementation found on the classpath"
                } else {
                    "Multiple Platform implementations found: ${found.map { it.javaClass.name }}"
                }
            }
        }

    }

}
