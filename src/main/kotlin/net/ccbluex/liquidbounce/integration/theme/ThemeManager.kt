/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
 */
package net.ccbluex.liquidbounce.integration.theme

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.io.extractZip
import net.ccbluex.liquidbounce.utils.io.resource
import net.ccbluex.liquidbounce.integration.IntegrationHandler
import net.ccbluex.liquidbounce.integration.VirtualScreenType
import net.ccbluex.liquidbounce.integration.theme.component.Component
import net.ccbluex.liquidbounce.integration.theme.component.ComponentFactory
import net.ccbluex.liquidbounce.integration.theme.type.RouteType
import net.ccbluex.liquidbounce.integration.theme.type.Theme
import net.ccbluex.liquidbounce.integration.theme.type.native.NativeTheme
import net.ccbluex.liquidbounce.integration.theme.type.web.WebTheme
import net.ccbluex.liquidbounce.integration.theme.wallpaper.Wallpaper
import java.io.File

const val DEFAULT_THEME = "LiquidBounce"
const val DEFAULT_WALLPAPER = "hills.png"

object ThemeManager {

    val themesFolder = File(ConfigSystem.rootFolder, "themes")

    init {
        extractDefault()
    }

    /**
     * List of available themes, which includes the native theme, the default that is extracted by [extractDefault]
     * and any other themes that have been dropped into the themes folder.
     */
    val availableThemes = arrayOf(
        NativeTheme,
        *themesFolder.listFiles()
            ?.filter(File::isDirectory)
            ?.map(::WebTheme)
            ?.toTypedArray()
            ?: emptyArray()
    )

    /**
     * The preferred active theme which is used as UI of the client.
     */
    var activeTheme: Theme = availableThemes.firstOrNull { it.name == DEFAULT_THEME } ?: NativeTheme
        set(value) {
            field = value

            // Update integration browser
            IntegrationHandler.sync()
            ModuleHud.refresh()
        }

    /**
     * The fallback theme which is used when the active theme does not support a virtual screen type.
     */
    private var fallbackTheme = availableThemes.firstOrNull { it.name == DEFAULT_THEME } ?: NativeTheme

    /**
     * The active wallpaper that is displayed as replacement of the standard Minecraft wallpaper. If set to null,
     * the standard Minecraft wallpaper will be displayed. The wallpaper does not have to match the active theme
     * and can be set independently.
     */
    val activeWallpaper: Wallpaper?
        get() = activeTheme.wallpapers.find { it.name == DEFAULT_WALLPAPER }

    /**
     * A list of all active components that are displayed by the [ComponentOverlay] and is used by [ModuleHud] to
     * display the components.
     *
     * The list can contain components from multiple themes and does not have to match the active theme.
     *
     * It should be populated with the standard components of the default theme, as well as
     *
     */
    var activeComponents: MutableList<Component> = mutableListOf(
        // Weather we support web themes, it might also load native theme defaults instead
        *fallbackTheme.components
            // Check if the component is enabled by default
            .filter { factory -> factory.default }
            // Create a new component instance
            .map { factory -> factory.new(fallbackTheme) }
            .toTypedArray()
    )

    /**
     * Get the route for the given virtual screen type.
     */
    fun route(virtualScreenType: VirtualScreenType? = null): RouteType {
        val theme = when {
            virtualScreenType == null || activeTheme.doesAccept(virtualScreenType) -> activeTheme
            fallbackTheme.doesAccept(virtualScreenType) -> fallbackTheme
            else -> availableThemes.firstOrNull { theme -> theme.doesAccept(virtualScreenType) }
                ?: error("No theme supports the route ${virtualScreenType.routeName}")
        }

        return theme.route(virtualScreenType)
    }

    /**
     * Choose a theme by name.
     */
    fun chooseTheme(name: String) {
        activeTheme = availableThemes.firstOrNull { it.name == name }
            ?: error("Theme $name does not exist")
    }

    /**
     * Extract the default theme from the resources.
     */
    private fun extractDefault() {
        runCatching {
            val folder = themesFolder.resolve("liquidbounce")
            val stream = resource("/assets/liquidbounce/default_theme.zip")

            if (folder.exists()) {
                folder.deleteRecursively()
            }

            extractZip(stream, folder)
            folder.deleteOnExit()

            logger.info("Extracted default theme")
        }.onFailure {
            logger.error("Unable to extract default theme", it)
        }.onSuccess {
            logger.info("Successfully extracted default theme")
        }.getOrThrow()
    }

}
