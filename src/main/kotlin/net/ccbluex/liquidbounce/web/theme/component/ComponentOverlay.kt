/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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
 *
 *
 */

package net.ccbluex.liquidbounce.web.theme.component

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.Configurable

import net.ccbluex.liquidbounce.config.Value
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.ComponentsUpdate
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.web.theme.ThemeManager
import net.ccbluex.liquidbounce.web.theme.component.types.IntegratedComponent

var components: MutableList<Component> = mutableListOf()

object ComponentOverlay : Configurable("Components", components as MutableList<Value<*>>), Listenable {

    @JvmStatic
    fun isTweakEnabled(tweak: FeatureTweak) = handleEvents() && components.filterIsInstance<IntegratedComponent>()
        .any { it.enabled && it.tweaks.contains(tweak) }

    fun parseComponents() {
        val theme = ThemeManager.activeTheme
        val rawComponents = theme.metadata.rawComponents
        val themeComponent = rawComponents
            .map { it.asJsonObject }
            .associateBy { it["name"].asString!! }

        val componentList = mutableListOf<Component>()

        for ((name, obj) in themeComponent) {
            // Check if component already exists in components, allows for seamless switch between themes/
            val existingComponent = components.find { it.name == name }
            if (existingComponent != null) {
                componentList.add(existingComponent)
                continue
            }

            runCatching {
                val componentType = ComponentType.byName(name) ?: error("Unknown component type: $name")
                val component = componentType.createComponent()

                runCatching {
                    ConfigSystem.deserializeConfigurable(component, obj)
                }.onFailure {
                    logger.error("Failed to deserialize component $name", it)
                }

                componentList.add(component)
            }.onFailure {
                logger.error("Failed to create component $name", it)
            }
        }

        // Clear and fill the components list
        components.clear()
        components.addAll(componentList)
    }

    fun fireComponentsUpdate() = EventManager.callEvent(ComponentsUpdate(components))

    override fun parent() = ModuleHud

}
