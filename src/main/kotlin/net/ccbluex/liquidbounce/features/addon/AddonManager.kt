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

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.util.readJson
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.lang.LanguageManager
import net.ccbluex.liquidbounce.utils.client.clientLogger
import net.fabricmc.loader.api.FabricLoader
import kotlin.io.path.inputStream
import kotlin.io.path.isRegularFile

/**
 * Discovers and drives the lifecycle of LiquidBounce add-ons.
 *
 * Add-ons are ordinary Fabric mods declaring a [ENTRYPOINT] entrypoint, so they are resolved by the
 * loader before the client starts. That also means they cannot be added or removed while the game
 * is running. A marketplace install stages a jar and asks for a restart.
 */
object AddonManager {

    private const val ENTRYPOINT = "liquidbounce"

    /**
     * Comma-separated add-on ids to skip, or `all`. An escape hatch so a broken add-on cannot leave
     * a user unable to start the client.
     */
    private const val DISABLE_PROPERTY = "liquidbounce.disableAddons"

    private val logger = clientLogger("AddonManager")

    private val loadedAddons = mutableListOf<LiquidBounceAddon>()

    val addons: List<LiquidBounceAddon> get() = loadedAddons

    /**
     * Set when a marketplace add-on was installed or removed, since neither takes effect until the
     * game is restarted.
     */
    var pendingRestart = false
        private set

    private val pendingRestartReasons = mutableListOf<String>()

    val restartReasons: List<String> get() = pendingRestartReasons

    operator fun get(id: String): LiquidBounceAddon? = loadedAddons.find { it.id.equals(id, true) }

    /**
     * Instantiates every add-on entrypoint and registers its translations.
     *
     * Only constructors run here, no add-on logic, so this is safe to call while the client's own
     * managers are still initializing.
     */
    fun discover() {
        if (loadedAddons.isNotEmpty()) {
            return
        }

        val disabled = System.getProperty(DISABLE_PROPERTY).orEmpty()
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val disableAll = disabled.any { it.equals("all", true) }

        val containers = FabricLoader.getInstance()
            .getEntrypointContainers(ENTRYPOINT, LiquidBounceAddon::class.java)

        for (entrypoint in containers) {
            val container = entrypoint.provider
            val id = container.metadata.id

            val addon = runCatching { entrypoint.entrypoint }
                .onFailure { logger.error("Failed to construct add-on '$id'", it) }
                .getOrNull() ?: continue

            addon.container = container

            if (disableAll || disabled.any { it.equals(id, true) }) {
                addon.state = AddonState.DISABLED
                logger.info("Skipping add-on '$id' ($DISABLE_PROPERTY)")
            } else {
                LanguageManager.registerSource { code ->
                    addon.metadata.findPath("resources/$id/lang/$code.json")
                        ?.takeIf { it.isRegularFile() }
                        ?.inputStream()
                        ?.use { it.readJson<HashMap<String, String>>() }
                }
            }

            loadedAddons += addon
        }

        // Deterministic order, so a duplicate name or category always fails on the same add-on.
        loadedAddons.sortBy { it.id }

        if (loadedAddons.isNotEmpty()) {
            logger.info("Discovered ${loadedAddons.size} add-on(s): ${loadedAddons.joinToString { it.id }}")
        }
    }

    /**
     * Runs [LiquidBounceAddon.onRegisterCategories] for every add-on.
     *
     * Separate from [initializeAddons] because constructing a module requires its category to
     * already exist, and an add-on may file modules under another add-on's category.
     */
    fun registerCategories() = forEachEnabled("category registration") { it.onRegisterCategories() }

    fun initializeAddons() = forEachEnabled("initialization") { addon ->
        addon.onInitialize()
        addon.state = AddonState.LOADED
    }

    fun notifyConfigsLoaded() = forEachEnabled("config load callback") { it.onConfigsLoaded() }

    fun shutdown() = forEachEnabled("shutdown") { it.onShutdown() }

    /**
     * Withdraws everything [addon] registered. Its classes stay loaded, since a Fabric mod cannot
     * be unloaded, but none of its features remain active.
     */
    fun disable(addon: LiquidBounceAddon) {
        rollback(addon)
        addon.state = AddonState.DISABLED
    }

    fun markRestartRequired(reason: String) {
        pendingRestart = true
        pendingRestartReasons += reason
    }

    private inline fun forEachEnabled(phase: String, action: (LiquidBounceAddon) -> Unit) {
        for (addon in loadedAddons) {
            if (addon.state == AddonState.DISABLED || addon.state == AddonState.ERRORED) {
                continue
            }

            runCatching { action(addon) }.onFailure { error ->
                // The client's initializer routes any escaping throwable to ErrorHandler.fatal, so
                // an add-on must never be allowed to throw past here.
                logger.error("Add-on '${addon.id}' failed during $phase", error)
                addon.state = AddonState.ERRORED
                rollback(addon)
            }
        }
    }

    /**
     * Reverses an add-on's contributions, in the opposite order they are registered.
     *
     * Each step is guarded on its own: a half-initialized add-on may hold entries that were never
     * fully registered, and one failure must not strand the rest.
     */
    private fun rollback(addon: LiquidBounceAddon) {
        fun step(what: String, block: () -> Unit) = runCatching(block)
            .onFailure { logger.error("Failed to withdraw $what of add-on '${addon.id}'", it) }

        addon.registeredNodes.takeIf { it.isNotEmpty() }?.let { nodes ->
            step("command nodes") {
                CommandManager.unregisterNodes(nodes.mapTo(hashSetOf()) { it.name })
            }
        }
        addon.registeredNodes.clear()

        addon.registeredCommands.forEach { registrar ->
            step("command ${registrar.javaClass.simpleName}") { CommandManager.unregister(registrar) }
        }
        addon.registeredCommands.clear()

        addon.registeredModes.forEach { (parent, mode) ->
            step("mode ${mode.name}") { parent.removeMode(mode) }
        }
        addon.registeredModes.clear()

        addon.registeredModules.forEach { module ->
            step("module ${module.name}") { ModuleManager.removeModule(module) }
        }
        addon.registeredModules.clear()

        addon.registeredCategories.forEach { category ->
            step("category ${category.tag}") { ModuleCategories.unregister(category) }
        }
        addon.registeredCategories.clear()

        addon.registeredConfigs.forEach { config ->
            step("config ${config.name}") { ConfigSystem.remove(config) }
        }
        addon.registeredConfigs.clear()
    }

}
