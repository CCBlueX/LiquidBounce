/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
package net.ccbluex.liquidbounce.web.theme

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.io.extractZip
import net.ccbluex.liquidbounce.utils.io.resource
import java.io.File

/**
 * Keeps track of themes and provides access to them.
 */
object ThemeManager {

    private val themesFolder = File(ConfigSystem.rootFolder, "themes")
    val themes = mutableMapOf<String, Theme>()
    private val defaultTheme = initDefault()

    var activeTheme = defaultTheme
        private set

    fun load() {
        if (!themesFolder.exists()) {
            themesFolder.mkdirs()
        }

        themesFolder.listFiles()?.forEach {
            if (it.isDirectory) {
                loadTheme(it)
            }
        }
    }

    /**
     * Loads a theme from a folder.
     */
    private fun loadTheme(folder: File) = runCatching {
        // Skip if theme is already loaded.
        if (themes.containsKey(folder.name)) {
            return@runCatching themes[folder.name]
        }

        // Otherwise load theme.
        val theme = Theme(folder.name, folder)
        themes[folder.name] = theme

        theme
    }.onFailure {
        logger.error("Unable to load theme ${folder.name}", it)
    }.getOrNull()

    /**
     * Init default theme.
     *
     * Loads default theme from resources and extracts it to the themes folder.
     */
    private fun initDefault(): Theme {
        if (!themesFolder.exists()) {
            themesFolder.mkdirs()
        }

        val defaultFolder = File(themesFolder, "default")

        runCatching {
            // Delete default theme folder if it exists, it might be outdated.
            if (defaultFolder.exists()) {
                defaultFolder.deleteRecursively()
            }

            // Extract default theme from resources.
            val stream = resource("/assets/liquidbounce/default_theme.zip")
            extractZip(stream, defaultFolder)

            // Delete zip file when process exits, but this is not guaranteed to work.
            // That's why we delete the folder above.
            defaultFolder.deleteOnExit()
        }.onFailure {
            logger.error("Unable to extract default theme", it)
        }.onSuccess {
            logger.info("Successfully extracted default theme")
        }

        // An error should never happen.
        // If it does, the client is unable to function properly. Crash if this happens or notify the user.
        return loadTheme(defaultFolder) ?: error("Unable to load default theme")
    }

    /**
     * Returns page by name from the active theme or the default theme if the page does not exist in the active theme.
     */
    fun page(name: String) = activeTheme.page(name) ?: defaultTheme.page(name)

}
