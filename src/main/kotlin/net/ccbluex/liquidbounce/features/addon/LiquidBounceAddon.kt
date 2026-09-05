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

import com.mojang.brigadier.tree.LiteralCommandNode
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.OptionalInclusion
import net.ccbluex.liquidbounce.config.types.Config
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.CommandRegistrar
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.clientLogger
import net.fabricmc.loader.api.ModContainer

/**
 * Where an add-on is in its lifecycle. Reported by `.addon list`.
 */
enum class AddonState {
    /** Discovered by Fabric, not initialized yet. */
    DISCOVERED,

    /** [LiquidBounceAddon.onInitialize] completed; the add-on's features are registered. */
    LOADED,

    /** A lifecycle hook threw; the add-on's contributions were rolled back. */
    ERRORED,

    /** Withdrawn via `.addon disable`, or skipped by `-Dliquidbounce.disableAddons`. */
    DISABLED,
}

/**
 * Base class for a LiquidBounce add-on.
 *
 * An add-on is an ordinary Fabric mod that names its implementation under the `liquidbounce`
 * entrypoint in its `fabric.mod.json`:
 *
 * ```json
 * "entrypoints": { "liquidbounce": ["com.example.addon.ExampleAddon"] }
 * ```
 *
 * Identity is read from the providing mod's metadata rather than declared twice in code.
 *
 * Register features through the `register*` helpers rather than calling [ModuleManager] and friends
 * directly. They record what the add-on contributed so [AddonManager] can withdraw it again when
 * the add-on is disabled or a lifecycle hook throws.
 */
abstract class LiquidBounceAddon {

    /**
     * Set by [AddonManager] right after Fabric instantiates the entrypoint.
     */
    internal lateinit var container: ModContainer

    val metadata: AddonMetadata by lazy { AddonMetadata(container) }

    val id: String get() = metadata.id
    val version: String get() = metadata.version
    val authors: List<String> get() = metadata.authors
    val description: String get() = metadata.description
    val color get() = metadata.color

    /**
     * Defaults to the mod's name; override to present something else in `.addon list`.
     */
    open val displayName: String get() = metadata.name

    var state: AddonState = AddonState.DISCOVERED
        internal set

    val logger by lazy { clientLogger("Addon/$id") }

    internal val registeredModules = mutableListOf<ClientModule>()
    internal val registeredCommands = mutableListOf<CommandRegistrar>()
    internal val registeredNodes = mutableListOf<LiteralCommandNode<ClientCommandSource>>()
    internal val registeredCategories = mutableListOf<ModuleCategory>()
    internal val registeredModes = mutableListOf<Pair<ModeValueGroup<*>, Mode>>()
    internal val registeredConfigs = mutableListOf<Config>()

    /**
     * Registers module categories.
     *
     * Runs for every add-on before any add-on's [onInitialize], because constructing a
     * [ClientModule] requires its [ModuleCategory] to already exist.
     */
    open fun onRegisterCategories() {}

    /**
     * Registers the add-on's modules, commands, configs and event listeners.
     *
     * Runs before [ConfigSystem.loadAll], so anything registered here has its persisted settings
     * restored; anything registered later does not.
     */
    abstract fun onInitialize()

    /**
     * Runs once every config has been read from disk, so settings hold their stored values.
     */
    open fun onConfigsLoaded() {}

    /**
     * Runs on client shutdown, before configs are written back to disk.
     */
    open fun onShutdown() {}

    fun registerCategory(
        name: String,
        inclusionGroup: OptionalInclusion? = null,
    ): ModuleCategory = ModuleCategories.register(ModuleCategory(name, inclusionGroup)).also {
        registeredCategories += it
    }

    fun registerModules(vararg modules: ClientModule) {
        for (module in modules) {
            ModuleManager.addModule(module)
            // Matches ModuleManager.registerInbuilt: without walkKeyPath the module has no
            // translation key and the ClickGUI falls back to raw names.
            module.walkKeyPath()
            module.verifyFallbackDescription()
            registeredModules += module
        }
    }

    fun registerCommand(registrar: CommandRegistrar) {
        CommandManager.register(registrar)
        registeredCommands += registrar
    }

    /**
     * Registers command nodes built at runtime, for add-ons that generate commands rather than
     * writing them against the Brigadier DSL.
     */
    fun registerCommandNodes(nodes: Collection<LiteralCommandNode<ClientCommandSource>>) {
        CommandManager.registerNodes(nodes)
        registeredNodes += nodes
    }

    /**
     * Adds a mode to an existing [ModeValueGroup], e.g. a new target-sorting mode.
     */
    fun registerMode(parent: ModeValueGroup<*>, mode: Mode) {
        parent.addMode(mode)
        registeredModes += parent to mode
    }

    /**
     * Creates the add-on's own config file at `LiquidBounce/<name>.json`, defaulting to its id.
     */
    fun config(
        name: String = id,
        tree: MutableCollection<out ValueGroup> = mutableListOf(),
    ): Config = ConfigSystem.root(name, tree).also { registeredConfigs += it }

    override fun toString(): String = "Addon[$id]"

}
