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
package net.ccbluex.liquidbounce.features.addon

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.tree.LiteralCommandNode
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.Config
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.RefreshArrayListEvent
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.CommandRegistrar
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.utils.client.clientLogger
import net.fabricmc.loader.api.ModContainer

@AddonApi
enum class AddonState {
    DISCOVERED,
    LOADED,
    ERRORED,

    /** Skipped by `-Dliquidbounce.disableAddons`. */
    DISABLED,
}

/**
 * Declared under the `liquidbounce` entrypoint in `fabric.mod.json`:
 *
 * ```json
 * "entrypoints": { "liquidbounce": ["com.example.addon.ExampleAddon"] }
 * ```
 *
 * Register through the `register*` helpers, not [ModuleManager] directly, so a failing add-on can
 * be rolled back.
 */
@AddonApi
@Suppress("TooManyFunctions")
abstract class LiquidBounceAddon : EventListener, MinecraftShortcuts {

    internal lateinit var container: ModContainer

    val metadata: AddonMetadata by lazy { AddonMetadata(container) }

    val id: String get() = metadata.id
    val version: String get() = metadata.version
    val authors: List<String> get() = metadata.authors
    val description: String get() = metadata.description
    val color get() = metadata.color

    open val displayName: String get() = metadata.name

    var state: AddonState = AddonState.DISCOVERED
        internal set

    override val running: Boolean
        get() = super.running && state == AddonState.LOADED

    val logger by lazy { clientLogger("Addon/$id") }

    internal val registeredListeners = mutableListOf<EventListener>()
    internal val registeredModules = mutableListOf<ClientModule>()
    internal val registeredNodes = mutableListOf<LiteralCommandNode<ClientCommandSource>>()
    internal val registeredCategories = mutableListOf<ModuleCategory>()
    internal val registeredModes = mutableListOf<Pair<ModeValueGroup<*>, Mode>>()
    internal val registeredConfigs = mutableListOf<Config>()

    /**
     * Registered for all add-ons before any [onInitialize] runs.
     */
    open val categories: List<ModuleCategory> get() = emptyList()

    /**
     * Runs before configs are loaded, so only what is registered here gets its settings restored.
     */
    abstract fun onInitialize()

    /**
     * Runs once configs are loaded.
     */
    open fun onStarted() {}

    /**
     * Runs before configs are written back to disk.
     */
    open fun onStopping() {}

    /**
     * Only tracks [listeners] for rollback; modules and modes are tracked already.
     */
    fun registerListeners(vararg listeners: EventListener) {
        registeredListeners += listeners
    }

    /**
     * For categories that are only known once the add-on runs. Declare the rest in [categories].
     */
    fun registerCategory(category: ModuleCategory): ModuleCategory {
        ModuleCategories.register(category)
        registeredCategories += category
        return category
    }

    fun registerModules(vararg modules: ClientModule) {
        for (module in modules) {
            check(ModuleCategories.byName(module.category.tag) === module.category) {
                "Category '${module.category.tag}' of module '${module.name}' is not registered, " +
                    "declare it in categories"
            }
            ModuleManager.addModule(module)
            registeredModules += module
            // As in registerInbuilt; without it the module has no translation key.
            module.walkKeyPath()
            module.verifyFallbackDescription()
        }
        refreshModuleList()
    }

    fun unregisterModules(vararg modules: ClientModule) {
        for (module in modules) {
            if (registeredModules.remove(module)) {
                ModuleManager.removeModule(module)
            }
        }
        refreshModuleList()
    }

    private fun refreshModuleList() {
        EventManager.callEvent(RefreshArrayListEvent)
        ModuleClickGui.sync()
    }

    fun registerCommand(registrar: CommandRegistrar) {
        // Not CommandManager.register: Brigadier would silently merge a clashing root literal.
        val scratch = CommandDispatcher<ClientCommandSource>()
        registrar.register(scratch)
        registerCommandNodes(scratch.root.children.filterIsInstance<LiteralCommandNode<ClientCommandSource>>())
    }

    fun registerCommandNodes(nodes: Collection<LiteralCommandNode<ClientCommandSource>>) {
        CommandManager.registerNodes(nodes)
        registeredNodes += nodes
    }

    fun <T : Mode> registerMode(parent: ModeValueGroup<T>, mode: T) {
        parent.addMode(mode)
        registeredModes += parent to mode
    }

    @JvmOverloads
    fun config(
        name: String = id,
        tree: MutableCollection<out ValueGroup> = mutableListOf(),
    ): Config = ConfigSystem.root(name, tree).also { registeredConfigs += it }

    override fun toString(): String = "Addon[$id]"

}
