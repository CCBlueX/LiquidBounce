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

package net.ccbluex.liquidbounce.integration.theme.component

import java.util.concurrent.CopyOnWriteArrayList
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ComponentsUpdateEvent
import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.integration.theme.Theme
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.integration.theme.component.components.NativeHudComponent
import net.ccbluex.liquidbounce.integration.theme.component.components.minimap.MinimapHudComponent

@AddonApi
@Suppress("TooManyFunctions")
object HudComponentManager {

    private val registeredComponents = CopyOnWriteArrayList<NativeHudComponent>()
    private val registeredFactories = CopyOnWriteArrayList<HudComponentFactory.NativeHudComponentFactory>()

    val nativeComponents: List<HudComponent>
        get() = listOf(MinimapHudComponent) + registeredComponents

    /**
     * Not persisted; the caller keeps the component's state.
     */
    @AddonApi
    fun register(component: NativeHudComponent) {
        if (registeredComponents.addIfAbsent(component)) {
            updateComponents()
        }
    }

    @AddonApi
    fun unregister(component: NativeHudComponent) {
        if (registeredComponents.remove(component)) {
            updateComponents()
        }
    }

    @AddonApi
    fun registerFactory(factory: HudComponentFactory.NativeHudComponentFactory) {
        registeredFactories.addIfAbsent(factory)
    }

    @AddonApi
    fun unregisterFactory(factory: HudComponentFactory.NativeHudComponentFactory) {
        registeredFactories.remove(factory)
    }

    val components: List<HudComponent>
        get() = nativeComponents + (ThemeManager.theme?.components ?: emptyList())

    @JvmStatic
    fun isTweakEnabled(tweak: HudComponentTweak) = ModuleHud.running &&
        components.any { component ->
            component.enabled && component.tweaks.contains(tweak)
        }

    @JvmStatic
    fun getComponentWithTweak(tweak: HudComponentTweak): HudComponent? {
        if (!ModuleHud.running) {
            return null
        }

        return components.find { component ->
            component.enabled && component.tweaks.contains(tweak)
        }
    }

    fun getComponents(id: String?): List<HudComponent> {
        if (id == null) {
            return components
        }

        val theme = ThemeManager.themes.find { it.metadata.id == id } ?: return emptyList()
        return theme.components
    }

    fun getComponentCatalog(id: String): List<Theme.ComponentCatalogEntry> {
        val theme = ThemeManager.themes.find { it.metadata.id == id } ?: return emptyList()
        return nativeComponents.map { component ->
            Theme.ComponentCatalogEntry(
                component.name,
                component.componentDescription,
                component.id.toString(),
                singleton = true,
                canAdd = !component.enabled,
            )
        } + registeredFactories.map { factory ->
            Theme.ComponentCatalogEntry(
                factory.name,
                factory.description,
                factory.id.toString(),
                factory.singleton,
                canAdd = !factory.singleton || registeredComponents.none { it.name == factory.name && it.enabled },
            )
        } + theme.componentCatalog()
    }

    fun getComponent(id: String): HudComponent? =
        components.find { it.id.toString() == id }
            ?: ThemeManager.themes.asSequence()
                .flatMap { it.components.asSequence() }
                .find { it.id.toString() == id }

    fun bringComponentToFront(component: HudComponent): Int {
        val zIndex = (components.maxOfOrNull { it.zIndex } ?: 0) + 1
        component.zIndex = zIndex
        return zIndex
    }

    fun getFactory(id: String): HudComponentFactory.NativeHudComponentFactory? =
        registeredFactories.find { it.id.toString() == id }

    fun addComponent(id: String): HudComponent? {
        getFactory(id)?.let { factory ->
            val component = factory.createComponent()
            component.enabled = true
            register(component)
            return component
        }

        nativeComponents.find { it.id.toString() == id }?.let { component ->
            if (component.enabled) {
                return null
            }

            component.enabled = true
            return component
        }

        return ThemeManager.themes
            .find { theme -> theme.components.any { it.id.toString() == id } }
            ?.addComponent(id)
    }

    fun updateComponents() {
        EventManager.callEvent(ComponentsUpdateEvent(
            source = ComponentsUpdateEvent.Source.NATIVE,
            components = nativeComponents,
        ))
        val theme = ThemeManager.theme ?: return
        EventManager.callEvent(ComponentsUpdateEvent(
            source = ComponentsUpdateEvent.Source.THEME,
            components = theme.components,
            themeId = theme.metadata.id,
        ))
    }

}
