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
package net.ccbluex.liquidbounce.features.module.modules.client

import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.config.AutoConfig
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ClickGuiValueChangeEvent
import net.ccbluex.liquidbounce.event.eventListenerScope
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.notification

object LocalConfig : ClientModule("LocalConfig", Category.CLIENT) {

    class ConfigChoice(override val choiceName: String) : NamedChoice

    private val configChoices = linkedSetOf<ConfigChoice>()

    init {
        refreshConfigList()
    }

    private val configName = enumChoice(
        "Config",
        configChoices.firstOrNull() ?: ConfigChoice("None"),
        configChoices
    ).onChanged { selectedChoice ->
        if (enabled) {
            loadConfig(selectedChoice.choiceName)
        }
    }

    private val refreshButton = boolean("Refresh", false)

    init {
        refreshButton.onChanged {
            if (it) {
                refreshConfigList()

                refreshButton.set(false)
                notification("LocalConfig", message("refreshed"), NotificationEvent.Severity.INFO)
            }
        }
    }

    fun refreshConfigList() {
        val files = ConfigSystem.userConfigsFolder.listFiles { _, name ->
            name.endsWith(".json", ignoreCase = true)
        } ?: emptyArray()

        configChoices.clear()

        if (files.isEmpty()) {
            configChoices.add(ConfigChoice("None"))
        } else {
            // Sort alphabetically
            files.sortedBy { it.name }.forEach {
                configChoices.add(ConfigChoice(it.nameWithoutExtension))
            }
        }

        EventManager.callEvent(ClickGuiValueChangeEvent(this))
    }

    private fun loadConfig(configName: String) {
        if (configName.equals("None", true)) return

        val file = ConfigSystem.userConfigsFolder.resolve("$configName.json")

        if (!file.exists()) {
            notification("LocalConfig", message("notFound", configName), NotificationEvent.Severity.ERROR)
            // Auto refresh if file is missing
            refreshConfigList()
            return
        }

        // Run loading
        eventListenerScope.launch {
            try {
                file.bufferedReader().use { r ->
                    AutoConfig.withLoading {
                        AutoConfig.loadAutoConfig(r)
                    }
                }
                notification("LocalConfig", message("loaded", configName), NotificationEvent.Severity.SUCCESS)
            } catch (e: Exception) {
                logger.error("Failed to load config $configName", e)
                notification("LocalConfig", message("failed"), NotificationEvent.Severity.ERROR)
            }
        }
    }

    override suspend fun enabledEffect() {
        // Load the currently selected config when the module is toggled ON
        loadConfig(configName.get().choiceName)
    }
}
