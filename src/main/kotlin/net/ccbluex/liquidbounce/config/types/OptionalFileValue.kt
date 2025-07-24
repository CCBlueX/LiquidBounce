package net.ccbluex.liquidbounce.config.types

import com.google.gson.Gson
import com.google.gson.JsonElement
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.FileDialogMode.*
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.jvm.optionals.getOrNull

/**
 * A configurable file input that supports different file dialog modes and optional file type filtering.
 *
 * @param name The name of the configuration option.
 * @param default The default selected file. Can be null and will be wrapped in [Optional].
 * @param dialogMode Specifies the type of file dialog to show (e.g., open file, save file, choose folder).
 * @param supportedExtensions A set of allowed file extensions (without the dot), e.g., `setOf("txt", "json")`.
 *        Use `null` to allow any file type. This is ignored if [dialogMode] is set to select directories.
 */
class OptionalFileValue(
    name: String,
    default: File?,
    val dialogMode: FileDialogMode,
    val supportedExtensions: Set<String>?,
) : Value<Optional<File>>(
    name,
    defaultValue = Optional.ofNullable(default),
    valueType = ValueType.FILE
) {
    override fun deserializeFrom(gson: Gson, element: JsonElement) {
        val file: File? = gson.fromJson(element, File::class.java)
        set(Optional.ofNullable(file))
    }
}

/**
 * A [Configurable] for [File]. Including the path and the [Type].
 */
class FileConfigurable(
    name: String,
    default: File? = null,
    dialogMode: FileDialogMode = OPEN_FILE,
    supportedExtensions: Set<String>? = null,
    defaultType: Type = Type.ABSOLUTE,
) : Configurable(name) {
    private val editing = AtomicBoolean(false)

    private val optionalFile = file("Path", default, dialogMode, supportedExtensions).onChange { new ->
        if (new.isEmpty || !editing.compareAndSet(false, true)) return@onChange new

        val value = new.get()
        if (value.isAbsolute && type === Type.RELATIVE) {
            val relativeValue = value.relativeToOrNull(ConfigSystem.rootFolder)
            if (relativeValue == null) {
                type = Type.ABSOLUTE
                new
            } else {
                Optional.of(relativeValue)
            }
        } else if (!value.isAbsolute && type === Type.ABSOLUTE) {
            Optional.of(ConfigSystem.rootFolder.resolve(value))
        } else {
            new
        }
    }

    val file: File? get() = optionalFile.get().getOrNull()

    var type: Type by enumChoice("Type", defaultType).onChange { new ->
        if (file == null || !editing.compareAndSet(true, false)) return@onChange new

        when (new) {
            Type.ABSOLUTE -> {
                optionalFile.get().getOrNull()?.let {
                    optionalFile.set(Optional.of(it.absoluteFile))
                }
                new
            }
            Type.RELATIVE -> {
                val relativeValue = optionalFile.get().getOrNull()?.let {
                    it.relativeToOrNull(ConfigSystem.rootFolder)
                }

                if (relativeValue == null) {
                    Type.ABSOLUTE
                } else {
                    optionalFile.set(Optional.of(relativeValue))
                    new
                }
            }
        }
    }
        private set

    enum class Type(override val choiceName: String) : NamedChoice {
        ABSOLUTE("Absolute"),
        RELATIVE("Relative"),
    }
}

/**
 * Defines the mode of the file dialog used in a [OptionalFileValue].
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
