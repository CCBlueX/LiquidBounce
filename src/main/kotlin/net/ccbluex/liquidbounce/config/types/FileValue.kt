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
package net.ccbluex.liquidbounce.config.types

import com.google.gson.Gson
import com.google.gson.JsonElement
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.autoconfig.AutoConfig
import net.ccbluex.liquidbounce.config.gson.stategies.Exclude
import net.ccbluex.liquidbounce.lang.LanguageManager
import org.lwjgl.sdl.SDLError
import org.lwjgl.sdl.SDLDialog.SDL_ShowOpenFileDialog
import org.lwjgl.sdl.SDLDialog.SDL_ShowOpenFolderDialog
import org.lwjgl.sdl.SDLDialog.SDL_ShowSaveFileDialog
import org.lwjgl.sdl.SDL_DialogFileCallback
import org.lwjgl.sdl.SDL_DialogFileFilter
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.Pointer
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

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
    @Exclude val dialogMode: FileDialogMode,
    @Exclude val supportedExtensions: Set<String>?,
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
 * This controls how the file chooser behaves in the UI (e.g., ClickGUI or similar):
 *
 * - [OPEN_FILE]: Opens a dialog to select an existing file.
 * - [SAVE_FILE]: Opens a dialog to choose a file path for saving.
 * - [OPEN_DIRECTORY]: Opens a dialog to select an existing directory. File extension filters are ignored in this mode.
 */
enum class FileDialogMode(
    private val translationKey: String,
    private val fallbackTitle: String
) {
    OPEN_FILE("liquidbounce.fileDialog.mode.openFile", "Open File") {
        override fun selectFiles(extensions: Iterable<String>?) =
            showSdlDialog(extensions, save = false, folder = false)
    },
    SAVE_FILE("liquidbounce.fileDialog.mode.saveFile", "Save File As") {
        override fun selectFiles(extensions: Iterable<String>?) =
            showSdlDialog(extensions, save = true, folder = false)
    },
    OPEN_DIRECTORY("liquidbounce.fileDialog.mode.openDirectory", "Select Folder") {
        override fun selectFiles(extensions: Iterable<String>?) =
            showSdlDialog(extensions, save = false, folder = true)
    };

    val title: String
        get() = LanguageManager.getLanguage()?.getOrDefault(translationKey, fallbackTitle) ?: fallbackTitle

    abstract fun selectFiles(extensions: Iterable<String>?): CompletionStage<List<String>>

    companion object {
        private fun showSdlDialog(
            extensions: Iterable<String>?,
            save: Boolean,
            folder: Boolean,
        ): CompletionStage<List<String>> {
            val session = NativeDialogSession(extensions)

            try {
                when {
                    folder -> SDL_ShowOpenFolderDialog(
                        session,
                        MemoryUtil.NULL,
                        MemoryUtil.NULL,
                        session.defaultLocation,
                        false,
                    )
                    save -> SDL_ShowSaveFileDialog(
                        session,
                        MemoryUtil.NULL,
                        MemoryUtil.NULL,
                        session.filters,
                        session.defaultLocation,
                    )
                    else -> SDL_ShowOpenFileDialog(
                        session,
                        MemoryUtil.NULL,
                        MemoryUtil.NULL,
                        session.filters,
                        session.defaultLocation,
                        false,
                    )
                }
            } catch (t: Throwable) {
                session.close()
                session.future.completeExceptionally(t)
            }

            return session.future
        }

    }
}

private class NativeDialogSession(
    extensions: Iterable<String>?,
) : SDL_DialogFileCallback(), AutoCloseable {

    val future: CompletableFuture<List<String>> = CompletableFuture()

    val defaultLocation: ByteBuffer = MemoryUtil.memUTF8(ConfigSystem.rootFolder.path)
    private var filterNameBuf: ByteBuffer? = null
    private var filterPatternBuf: ByteBuffer? = null
    val filters: SDL_DialogFileFilter.Buffer? = buildFilters(extensions)

    private fun buildFilters(extensions: Iterable<String>?): SDL_DialogFileFilter.Buffer? {
        val exts = extensions
            ?.map { it.trim().removePrefix("*.") }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        if (exts.isEmpty()) return null

        val pattern = exts.joinToString(";")
        val name = exts.joinToString(", ") { "*.$it" }

        val nameBuf = MemoryUtil.memUTF8(name).also { filterNameBuf = it }
        val patternBuf = MemoryUtil.memUTF8(pattern).also { filterPatternBuf = it }

        return SDL_DialogFileFilter.calloc(1).also { buf ->
            buf[0].name(nameBuf).pattern(patternBuf)
        }
    }

    private fun readFileList(filelist: Long): List<String> {
        return buildList {
            var i = 0
            while (true) {
                val ptr = MemoryUtil.memGetAddress(filelist + i.toLong() * Pointer.POINTER_SIZE)
                if (ptr == MemoryUtil.NULL) break
                this += MemoryUtil.memUTF8(ptr)
                i++
            }
        }
    }

    override fun invoke(userdata: Long, filelist: Long, filter: Int) {
        try {
            if (filelist == MemoryUtil.NULL) {
                val error = SDLError.SDL_GetError() ?: "Unknown SDL error"
                future.completeExceptionally(IllegalStateException("SDL dialog error: $error"))
            } else {
                future.complete(readFileList(filelist))
            }
        } catch (t: Throwable) {
            future.completeExceptionally(t)
        } finally {
            this.close()
        }
    }

    override fun close() {
        try {
            filters?.free()
            filterNameBuf?.let(MemoryUtil::memFree)
            filterPatternBuf?.let(MemoryUtil::memFree)
            MemoryUtil.memFree(defaultLocation)
        } finally {
            this.free()
        }
    }
}
