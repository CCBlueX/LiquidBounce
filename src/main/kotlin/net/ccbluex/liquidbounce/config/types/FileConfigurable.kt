package net.ccbluex.liquidbounce.config.types

import net.ccbluex.liquidbounce.config.types.FileDialogMode.*
import java.io.File
import java.util.*

/**
 * A configurable file input that supports different file dialog modes and optional file type filtering.
 *
 * @param name The name of the configuration option.
 * @param default The default selected file. Can be null and will be wrapped in [Optional].
 * @param dialogMode Specifies the type of file dialog to show (e.g., open file, save file, choose folder).
 * @param supportedExtensions A set of allowed file extensions (without the dot), e.g., `setOf("txt", "json")`.
 *        Use `null` to allow any file type. This is ignored if [dialogMode] is set to select directories.
 */
class FileConfigurable(
    name: String,
    default: File?,
    val dialogMode: FileDialogMode,
    val supportedExtensions: Set<String>?
) : Value<Optional<File>>(
    name,
    defaultValue = Optional.ofNullable(default),
    valueType = ValueType.FILE
)

/**
 * Defines the mode of the file dialog used in a [FileConfigurable].
 *
 * This controls how the file chooser behaves in the UI (e.g., ClickGUI or similar):
 *
 * - [OPEN_FILE]: Opens a dialog to select an existing file.
 * - [SAVE_FILE]: Opens a dialog to choose a file path for saving.
 * - [OPEN_DIRECTORY]: Opens a dialog to select an existing directory. File extension filters are ignored in this mode.
 */
enum class FileDialogMode {
    OPEN_FILE,
    SAVE_FILE,
    OPEN_DIRECTORY
}
