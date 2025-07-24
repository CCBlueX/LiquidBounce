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
package net.ccbluex.liquidbounce.config.types

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.FileDialogMode.*
import java.io.File

/**
 * A configurable file input that supports different file dialog modes and optional file type filtering.
 *
 * @param name The name of the configuration option.
 * @param default The default selected file. The default value is [ConfigSystem.rootFolder].
 * @param dialogMode Specifies the type of file dialog to show (e.g., open file, save file, choose folder).
 * @param supportedExtensions A set of allowed file extensions (without the dot), e.g., `setOf("txt", "json")`.
 *        Use `null` to allow any file type. This is ignored if [dialogMode] is set to select directories.
 */
class FileValue(
    name: String,
    default: File?,
    val dialogMode: FileDialogMode,
    val supportedExtensions: Set<String>?,
) : Value<File>(
    name,
    defaultValue = normalizeToClientFolder(default ?: ConfigSystem.rootFolder),
    valueType = ValueType.FILE
) {
    init {
        onChange(::normalizeToClientFolder)
    }

    companion object {
        @JvmStatic
        private fun normalizeToClientFolder(file: File): File {
            return if (file.startsWith(ConfigSystem.rootFolder)) {
                file.relativeTo(ConfigSystem.rootFolder)
            } else {
                file
            }
        }
    }
}

/**
 * Defines the mode of the file dialog used in a [FileValue].
 *
 * This controls how the file chooser behaves in the UI (e.g., ClickGUI or similar):
 *
 * - [OPEN_FILE]: Opens a dialog to select an existing file.
 * - [SAVE_FILE]: Opens a dialog to choose a file path for saving.
 * - [OPEN_DIRECTORY]: Opens a dialog to select an existing directory. File extension filters are ignored in this mode.
 */
enum class FileDialogMode(val title: String) {
    OPEN_FILE("Open File"),
    SAVE_FILE("Save File As"),
    OPEN_DIRECTORY("Select Folder")
}
