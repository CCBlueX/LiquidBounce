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
package net.ccbluex.liquidbounce.config

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.gson.fileGson
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.config.types.nesting.DynamicConfigurable
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.io.createZipArchive
import net.ccbluex.liquidbounce.utils.io.extractZip
import java.io.File
import java.io.Reader
import java.io.Writer

/**
 * A config system which uses configurables
 *
 * @author kawaiinekololis (@team ccbluex)
 */
@Suppress("TooManyFunctions")
object ConfigSystem {

    var isFirstLaunch: Boolean = false
        private set

    // Config directory folder
    val rootFolder = File(
        mc.runDirectory, LiquidBounce.CLIENT_NAME
    ).apply {
        // Check if there is already a config folder and if not create new folder
        // (mkdirs not needed - .minecraft should always exist)
        if (!exists()) {
            isFirstLaunch = true
            mkdir()
        }
    }

    // User config directory folder
    val userConfigsFolder = File(
        rootFolder, "configs"
    ).apply {
        // Check if there is already a config folder and if not create new folder
        // (mkdirs not needed - .minecraft should always exist)
        if (!exists()) {
            mkdir()
        }
    }

    internal val backupFolder = File(
        rootFolder, "backups"
    ).apply {
        // Check if there is already a config folder and if not create new folder
        // (mkdirs not needed - .minecraft should always exist)
        if (!exists()) {
            mkdir()
        }
    }

    // A mutable list of all root configurable classes (and their subclasses)
    val configurables = ArrayList<Configurable>()

    /**
     * Create new root configurable
     */
    fun root(name: String, tree: MutableList<out Configurable> = mutableListOf()): Configurable {
        @Suppress("UNCHECKED_CAST")
        return root(Configurable(name, value = tree as MutableList<Value<*>>))
    }

    fun dynamic(
        name: String,
        tree: MutableList<out Configurable> = mutableListOf(),
        factory: (String, JsonObject) -> Value<*>
    ): Configurable {
        @Suppress("UNCHECKED_CAST")
        return root(DynamicConfigurable(name, tree as MutableList<Value<*>>, factory))
    }

    /**
     * Add a root configurable
     */
    fun root(configurable: Configurable): Configurable {
        configurable.initConfigurable()
        configurables.add(configurable)
        return configurable
    }

    val Configurable.jsonFile: File
        get() {
            require(this in configurables) { "${this.name} is not root configurable" }
            return File(rootFolder, "${this.loweredName}.json")
        }

    /**
     * Create a ZIP file of root configurable files
     */
    fun backup(fileName: String, configurables: Collection<Configurable> = this.configurables) {
        val zipFile = File(backupFolder, "$fileName.zip")
        check(!zipFile.exists()) { "Backup file already exists" }

        configurables.map { configurable -> configurable.jsonFile }.createZipArchive(zipFile)
    }

    /**
     * Restore a backup from a ZIP file to the root configurable files
     */
    fun restore(fileName: String) {
        val zipFile = File(backupFolder, "$fileName.zip")
        check(zipFile.exists()) { "Backup file does not exist" }

        // Store all configurables to make sure they are up to date,
        // before we overwrite some of them through [extractZip]
        storeAll()
        extractZip(zipFile, rootFolder)
        loadAll()
    }

    /**
     * All configurables should load now.
     */
    fun loadAll() {
        for (configurable in configurables) { // Make a new .json file to save our root configurable
            configurable.jsonFile.runCatching {
                if (!exists()) {
                    // Do not try to load a non-existing file
                    return@runCatching
                }

                logger.debug("Reading config ${configurable.loweredName}...")
                deserializeConfigurable(configurable, bufferedReader())
            }.onSuccess {
                logger.info("Successfully loaded config '${configurable.loweredName}'.")
            }.onFailure {
                logger.error("Unable to load config ${configurable.loweredName}", it)
            }

            // After loading the config, we need to store it again to make sure all values are up to date
            storeConfigurable(configurable)
        }
    }

    /**
     * All configurables known to the config system should be stored now.
     * This will overwrite all existing files with the new values.
     *
     * These configurables are root configurables, which always create a new file with their name.
     */
    fun storeAll() {
        configurables.forEach(::storeConfigurable)
    }

    /**
     * Store a configurable to a file (will be created if not exists).
     *
     * The configurable should be known to the config system.
     */
    fun storeConfigurable(configurable: Configurable) { // Make a new .json file to save our root configurable
        configurable.jsonFile.runCatching {
            if (!exists()) {
                createNewFile().let { logger.debug("Created new file (status: $it)") }
            }

            logger.debug("Writing config ${configurable.loweredName}...")
            serializeConfigurable(configurable, bufferedWriter())
            logger.info("Successfully saved config '${configurable.loweredName}'.")
        }.onFailure {
            logger.error("Unable to store config ${configurable.loweredName}", it)
        }
    }

    /**
     * Serialize a configurable to a writer
     */
    private fun serializeConfigurable(configurable: Configurable, writer: Writer, gson: Gson = fileGson) {
        gson.newJsonWriter(writer).use {
            gson.toJson(configurable, Configurable::class.javaObjectType, it)
        }
    }

    /**
     * Serialize a configurable to a writer
     */
    fun serializeConfigurable(configurable: Configurable, gson: Gson = fileGson) =
        gson.toJsonTree(configurable, Configurable::class.javaObjectType)

    /**
     * Deserialize a configurable from a reader
     */
    fun deserializeConfigurable(configurable: Configurable, reader: Reader, gson: Gson = fileGson) {
        JsonParser.parseReader(gson.newJsonReader(reader))?.let {
            deserializeConfigurable(configurable, it)
        }
    }

    /**
     * Deserialize a configurable from a json element
     */
    fun deserializeConfigurable(configurable: Configurable, jsonElement: JsonElement) {
        val jsonObject = jsonElement.asJsonObject

        // Check if the name is the same as the configurable name
        val name = jsonObject.getAsJsonPrimitive("name").asString
        check(name == configurable.name || configurable.aliases.contains(name)) {
            "Configurable name does not match the name in the json object"
        }

        val values = jsonObject.getAsJsonArray("value")
            .map { valueElement -> valueElement.asJsonObject }
            .associateBy { valueObj -> valueObj["name"].asString!! }

        when (configurable) {

            // On a dynamic configurable, we first create an instance of the value and then deserialize it
            is DynamicConfigurable -> {
                if (values.isNotEmpty()) {
                    // Clear the current values
                    configurable.inner.clear()
                }

                for ((name, value) in values) {
                    val valueInstance = configurable.factory(name, value)
                    configurable.value(valueInstance)

                    deserializeValue(valueInstance, value)
                }
            }

            // On an ordinary configurable, we simply deserialize the values that are present
            else -> {
                for (value in configurable.inner) {
                    val currentElement = values[value.name]
                        // Alias support
                        ?: values.entries.firstOrNull { entry -> entry.key in value.aliases }?.value
                        ?: continue

                    deserializeValue(value, currentElement)
                }
            }
        }
    }

    /**
     * Deserialize a value from a json object
     */
    private fun deserializeValue(value: Value<*>, jsonObject: JsonObject) {
        // In the case of a configurable, we need to go deeper and deserialize the configurable itself
        if (value is Configurable) {
            runCatching {
                if (value is ChoiceConfigurable<*>) {
                    // Set current active choice
                    runCatching {
                        value.setByString(jsonObject["active"].asString)
                    }.onFailure {
                        logger.error("Unable to deserialize active choice for ${value.name}", it)
                    }

                    // Deserialize each choice
                    val choices = jsonObject["choices"].asJsonObject

                    for (choice in value.choices) {
                        runCatching {
                            val choiceElement = choices[choice.name]
                                // Alias support
                                ?: choice.aliases.firstNotNullOfOrNull { alias -> choices[alias] }
                                ?: error("Choice ${choice.name} not found")

                            deserializeConfigurable(choice, choiceElement)
                        }.onFailure {
                            logger.error("Unable to deserialize choice ${choice.name}", it)
                        }
                    }
                }

                // Deserialize the rest of the configurable
                deserializeConfigurable(value, jsonObject)
            }.onFailure {
                logger.error("Unable to deserialize configurable ${value.name}", it)
            }

            return
        }

        // Otherwise, we simply deserialize the value
        runCatching {
            value.deserializeFrom(fileGson, jsonObject["value"])
        }.onFailure {
            logger.error("Unable to deserialize value ${value.name}", it)
        }
    }

    fun getConfigurableByName(name: String): Configurable? {
        return configurables.firstOrNull { it.name.equals(name, true) }
    }

}
