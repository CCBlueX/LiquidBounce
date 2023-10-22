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

import net.ccbluex.liquidbounce.config.util.decode
import net.ccbluex.liquidbounce.utils.client.logger
import java.io.File

/**
 * A theme includes a set of pages which can be displayed in the web view. It is being used to load pages from.
 */
class Theme(val name: String, val folder: File) {

    val metadata: ThemeMetadata

    /**
     * JSON format:
     * {
     *  "format": 1,
     *  "displayName": "Default theme",
     *  "authors": ["CCBlueX"],
     *  "description": "Default theme for LiquidBounce"
     *  }
     */
    data class ThemeMetadata(val format: Int, val displayName: String, val authors: List<String>,
                             val description: String)

    init {
        val metadataFile = File(folder, "theme.json")

        if (!metadataFile.exists()) {
            error("Unable to load theme $name: theme.json does not exist")
        }

        metadata = decode<ThemeMetadata>(metadataFile.readText())
        logger.info("Loaded theme $name")
    }

    fun page(name: String): Page? {
        val page = Page(this, name)

        if (page.exists) {
            return page
        }
        return null
    }

}

/**
 * Represents a page of a theme. A page is a folder in the theme folder which contains an index.html file.
 * It can be displayed in the web view.
 */
class Page(theme: Theme, val name: String) {

    // private val pageFolder = File(theme.folder, name)
    private val indexFile = File(theme.folder, "index.html")

    val uri = "file:///${indexFile.absolutePath}"

    val exists: Boolean
        get() = indexFile.exists()

}
