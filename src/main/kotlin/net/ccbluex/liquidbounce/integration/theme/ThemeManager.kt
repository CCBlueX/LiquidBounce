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
import net.ccbluex.liquidbounce.integration.theme.type.RouteType
import net.ccbluex.liquidbounce.integration.theme.type.Theme
import net.ccbluex.liquidbounce.integration.theme.type.native.NativeTheme
import net.ccbluex.liquidbounce.integration.theme.type.web.WebTheme
import net.ccbluex.liquidbounce.integration.theme.wallpaper.Wallpaper
import net.ccbluex.liquidbounce.integration.theme.wallpaper.WallpaperManager
import java.io.File

object ThemeManager : Configurable("Theme") {

    val themesFolder = File(ConfigSystem.rootFolder, "themes")

    var activeWallpaper: Wallpaper? = null

    init {
        extractDefault()
    }

    val availableThemes = arrayOf(
        NativeTheme,
        *themesFolder.listFiles()
            ?.filter(File::isDirectory)
            ?.map(::WebTheme)
            ?.toTypedArray()
            ?: emptyArray()
    )

    var activeTheme: Theme = availableThemes.firstOrNull { it.name == "default" } ?: NativeTheme
        set(value) {
//            if (!value.exists) {
//                logger.warn("Unable to set theme to ${value.name}, theme does not exist")
//                return
//            }

            field = value

            // Update integration browser
            IntegrationHandler.sync()
            ModuleHud.refresh()
        }

    init {
        ConfigSystem.root(this)
    }

    val wallpaper = tree(WallpaperManager)

    fun route(virtualScreenType: VirtualScreenType? = null): RouteType {
        val theme = if (virtualScreenType == null || activeTheme.doesAccept(virtualScreenType)) {
            activeTheme
        } else {
            availableThemes.firstOrNull { theme -> theme.doesAccept(virtualScreenType) }
                ?: error("No theme supports the route ${virtualScreenType.routeName}")
        }

        return theme.route(virtualScreenType)
    }

    fun chooseTheme(name: String) {
        activeTheme = availableThemes.firstOrNull { it.name == name }
            ?: error("Theme $name does not exist")
    }

    /**
     * Extract the default theme from the resources.
     */
    private fun extractDefault() {
        runCatching {
            val folder = themesFolder.resolve("default")
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
