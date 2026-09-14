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
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.RefreshArrayListEvent
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.lang.LanguageManager
import net.ccbluex.liquidbounce.utils.client.clientLogger
import net.fabricmc.loader.api.FabricLoader
import kotlin.io.path.inputStream
import kotlin.io.path.isRegularFile

object AddonManager {

    private const val ENTRYPOINT = "liquidbounce"

    /**
     * Comma-separated add-on ids to skip, or `all`.
     */
    private const val DISABLE_PROPERTY = "liquidbounce.disableAddons"

    private val logger = clientLogger("AddonManager")

    private val loadedAddons = mutableListOf<LiquidBounceAddon>()

    val addons: List<LiquidBounceAddon> get() = loadedAddons

    private val pendingRestarts = LinkedHashMap<Int, String>()

    val pendingRestart: Boolean get() = pendingRestarts.isNotEmpty()

    val restartReasons: Collection<String> get() = pendingRestarts.values

    operator fun get(id: String): LiquidBounceAddon? = loadedAddons.find { it.id.equals(id, true) }

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

        // Stable order, so a name clash always fails the same add-on.
        loadedAddons.sortBy { it.id }

        if (loadedAddons.isNotEmpty()) {
            logger.info("Discovered ${loadedAddons.size} add-on(s): ${loadedAddons.joinToString { it.id }}")
        }
    }

    fun registerCategories() = forEachEnabled("category registration") { addon ->
        for (category in addon.categories) {
            addon.registeredCategories += ModuleCategories.register(category)
        }
    }

    fun initializeAddons() = forEachEnabled("initialization") { addon ->
        addon.onInitialize()
        addon.state = AddonState.LOADED
    }

    fun notifyStarted() = forEachEnabled("startup") { it.onStarted() }

    // No rollback: configs are stored right after, and withdrawing them would lose the settings.
    fun notifyStopping() = forEachEnabled("shutdown", rollbackOnFailure = false) {
        it.onStopping()
    }

    fun markRestartRequired(itemId: Int, reason: String) {
        pendingRestarts[itemId] = reason
    }

    fun clearRestartRequired(itemId: Int) {
        pendingRestarts.remove(itemId)
    }

    private inline fun forEachEnabled(
        phase: String,
        rollbackOnFailure: Boolean = true,
        action: (LiquidBounceAddon) -> Unit,
    ) {
        for (addon in loadedAddons) {
            if (addon.state == AddonState.DISABLED || addon.state == AddonState.ERRORED) {
                continue
            }

            runCatching { action(addon) }.onFailure { error ->
                logger.error("Add-on '${addon.id}' failed during $phase", error)
                addon.state = AddonState.ERRORED
                if (rollbackOnFailure) {
                    rollback(addon)
                }
            }
        }
    }

    private fun rollback(addon: LiquidBounceAddon) {
        fun step(what: String, block: () -> Unit) = runCatching(block)
            .onFailure { logger.error("Failed to withdraw $what of add-on '${addon.id}'", it) }

        addon.registeredListeners.forEach { listener ->
            step("listener ${listener.javaClass.simpleName}") { listener.unregister() }
        }
        addon.registeredListeners.clear()

        addon.registeredNodes.takeIf { it.isNotEmpty() }?.let { nodes ->
            step("command nodes") {
                CommandManager.unregisterNodes(nodes.mapTo(hashSetOf()) { it.name })
            }
        }
        addon.registeredNodes.clear()

        addon.registeredModes.forEach { (parent, mode) ->
            step("mode ${mode.name}") { parent.removeMode(mode) }
        }
        addon.registeredModes.clear()

        addon.registeredModules.forEach { module ->
            step("module ${module.name}") { ModuleManager.removeModule(module) }
        }
        addon.registeredModules.clear()

        addon.registeredCategories.forEach { category ->
            if (ModuleManager.any { it.category === category }) {
                logger.info("Keeping category '${category.tag}' of add-on '${addon.id}', other modules use it")
            } else {
                step("category ${category.tag}") { ModuleCategories.unregister(category) }
            }
        }
        addon.registeredCategories.clear()

        addon.registeredConfigs.forEach { config ->
            step("config ${config.name}") { ConfigSystem.remove(config) }
        }
        addon.registeredConfigs.clear()

        step("event hooks") { addon.unregister() }

        EventManager.callEvent(RefreshArrayListEvent)
        ModuleClickGui.sync()
    }

}
