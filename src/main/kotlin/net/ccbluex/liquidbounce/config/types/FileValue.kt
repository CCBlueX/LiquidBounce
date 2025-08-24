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

import com.google.gson.Gson
import com.google.gson.JsonElement
import net.ccbluex.liquidbounce.config.AutoConfig
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.FileDialogMode.*
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.tinyfd.TinyFileDialogs
import java.io.File

/**
 * A value file input that supports different file dialog modes and optional file type filtering.
 * It will be treated as a relative path if it starts with [ConfigSystem.rootFolder].
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

    override fun deserializeFrom(gson: Gson, element: JsonElement) {
        // File value is not allowed to be deserialized from AutoConfig.
        if (!AutoConfig.loadingNow) {
            super.deserializeFrom(gson, element)
        }
    }

    /**
     * The absolute file path.
     *
     * If the file is not absolute, it is resolved relative to the [ConfigSystem.rootFolder].
     */
    val absoluteFile: File get() = if (inner.isAbsolute) inner else ConfigSystem.rootFolder.resolve(inner)

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
 * TODO: i18n
 *
 * This controls how the file chooser behaves in the UI (e.g., ClickGUI or similar):
 *
 * - [OPEN_FILE]: Opens a dialog to select an existing file.
 * - [SAVE_FILE]: Opens a dialog to choose a file path for saving.
 * - [OPEN_DIRECTORY]: Opens a dialog to select an existing directory. File extension filters are ignored in this mode.
 */
enum class FileDialogMode(val title: String) {
    OPEN_FILE("Open File") {
        override fun selectFilesRaw(extensions: Iterable<String>?) = TinyFileDialogs.tinyfd_openFileDialog(
            title,
            null,
            getFilterPatterns(extensions),
            null,
            false
        )
    },
    SAVE_FILE("Save File As") {
        override fun selectFilesRaw(extensions: Iterable<String>?) = TinyFileDialogs.tinyfd_saveFileDialog(
            title,
            null,
            getFilterPatterns(extensions),
            null
        )
    },
    OPEN_DIRECTORY("Select Folder") {
        override fun selectFilesRaw(extensions: Iterable<String>?) = TinyFileDialogs.tinyfd_selectFolderDialog(
            title,
            ConfigSystem.rootFolder.path,
        )
    };

    protected abstract fun selectFilesRaw(extensions: Iterable<String>?): String?

    fun selectFiles(extensions: Iterable<String>? = null): List<String> {
        // using `|` as a separator because tinyfd separate multiple file selection
        return selectFilesRaw(extensions)?.split('|') ?: emptyList()
    }

    companion object {
        @JvmStatic
        private fun getFilterPatterns(extensions: Iterable<String>?): PointerBuffer? {
            extensions ?: return null
            return MemoryStack.stackPush().use { stack ->
                val patternList = extensions.map { ext -> "*.$ext" }
                val buffer = stack.mallocPointer(patternList.size)
                patternList.forEach { pattern ->
                    buffer.put(stack.ASCII(pattern))
                }
                buffer.flip()
            }
        }
    }
}
