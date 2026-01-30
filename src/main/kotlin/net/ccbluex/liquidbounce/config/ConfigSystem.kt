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
package net.ccbluex.liquidbounce.config

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.config.gson.fileGson
import net.ccbluex.liquidbounce.config.gson.util.parseTree
import net.ccbluex.liquidbounce.config.types.Config
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.io.createZipArchive
import net.ccbluex.liquidbounce.utils.io.extractZip
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.File
import java.io.Reader
import java.io.Writer

/**
 * A hierarchy config system
 */
@Suppress("TooManyFunctions")
object ConfigSystem {

    const val KEY_PREFIX = "liquidbounce"

    private val logger: Logger = LogManager.getLogger("$CLIENT_NAME/ConfigSystem")

    var isFirstLaunch: Boolean = false
        private set

    // Config directory folder
    val rootFolder = File(
        mc.gameDirectory, LiquidBounce.CLIENT_NAME
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

    val configs = ArrayList<Config>()

    fun findValueByKey(key: String): Value<*>? {
        ensureRootKeys()
        val normalizedKey = normalizeKeyInput(key)
        return configs.asSequence()
            .flatMap { it.collectValuesRecursively().asSequence() }
            .firstOrNull { it.key?.equals(normalizedKey, true) == true }
    }

    fun findValueGroupByKey(key: String): ValueGroup? {
        ensureRootKeys()
        val normalizedKey = normalizeKeyInput(key)
        return configs.asSequence()
            .flatMap { it.collectValueGroupsRecursively().asSequence() }
            .firstOrNull { it.key?.equals(normalizedKey, true) == true }
    }

    fun valueKeySequence(prefix: String): Sequence<String> = sequence {
        ensureRootKeys()
        for (valueGroup in configs) {
            for (value in valueGroup.collectValuesRecursively(prefix)) {
                value.key?.let { yield(it) }
            }
        }
    }

    fun valueGroupsKeySequence(prefix: String): Sequence<String> = sequence {
        ensureRootKeys()
        for (valueGroup in configs) {
            for (child in valueGroup.collectValueGroupsRecursively(prefix)) {
                child.key?.let { yield(it) }
            }
        }
    }

    /**
     * Create an config based on an existing tree
     */
    fun root(name: String, tree: MutableCollection<out ValueGroup> = mutableListOf()): Config {
        @Suppress("UNCHECKED_CAST")
        return root(Config(name, value = tree as MutableCollection<Value<*>>))
    }

    /**
     * Add an existing config instance
     */
    fun root(config: Config): Config {
        config.walkInit()
        configs.add(config)
        return config
    }

    /**
     * Create a ZIP file backup of configs
     */
    fun backup(fileName: String, groups: Iterable<Config> = this.configs) {
        var zipFile = File(backupFolder, "$fileName.zip")
        var suffix = 1
        while (zipFile.exists()) {
            zipFile = File(backupFolder, "${fileName}_${suffix++}.zip")
        }

        groups.map { valueGroup -> valueGroup.jsonFile }.createZipArchive(zipFile)
    }

    /**
     * Restore a backup from a ZIP file to the configs
     */
    fun restore(fileName: String) {
        val zipFile = File(backupFolder, "$fileName.zip")
        check(zipFile.exists()) { "Backup file does not exist" }

        // Store all configs to make sure they are up to date,
        // before we overwrite some of them through [extractZip]
        storeAll()
        extractZip(zipFile, rootFolder)
        loadAll()
    }

    /**
     * Loads all registered configs.
     */
    fun loadAll() {
        for (valueGroup in configs) { // Make a new .json file to save our root config
            load(valueGroup)
        }
    }

    fun load(config: Config) {
        config.jsonFile.runCatching {
            if (!exists()) {
                // Do not try to load a non-existing file
                return@runCatching
            }

            logger.debug("Reading config ${config.loweredName}...")
            deserializeValueGroup(config, bufferedReader())
        }.onSuccess {
            logger.info("Successfully loaded config '${config.loweredName}'.")
        }.onFailure {
            logger.error("Unable to load config ${config.loweredName}", it)
        }

        // After loading the config, we need to store it again to make sure all values are up to date
        store(config)
    }

    /**
     * All configs known to the config system should be stored now.
     * This will overwrite all existing files with the new values.
     *
     * These configs are root configs, which always create a new file with their name.
     */
    fun storeAll() {
        configs.forEach(::store)
    }

    /**
     * Store config to a file (will be created if not exists).
     *
     * The config should be known to the config system.
     */
    fun store(config: Config) { // Make a new .json file to save our root config
        config.jsonFile.runCatching {
            if (!exists()) {
                createNewFile().let { logger.debug("Created new file (status: $it)") }
            }

            logger.debug("Writing config ${config.loweredName}...")
            serializeValueGroup(config, bufferedWriter())
            logger.info("Successfully saved config '${config.loweredName}'.")
        }.onFailure {
            logger.error("Unable to store config ${config.loweredName}", it)
        }
    }

    /**
     * Serialize a config to a writer and close it
     */
    private fun serializeValueGroup(valueGroup: ValueGroup, writer: Writer, gson: Gson = fileGson) {
        gson.newJsonWriter(writer).use {
            gson.toJson(valueGroup, ValueGroup::class.javaObjectType, it)
        }
    }

    /**
     * Serialize a config to a [JsonObject].
     */
    fun serializeValueGroup(valueGroup: ValueGroup, gson: Gson = fileGson): JsonObject =
        gson.toJsonTree(valueGroup, ValueGroup::class.javaObjectType) as JsonObject

    /**
     * Deserialize a config from a reader, and close it
     */
    fun deserializeValueGroup(valueGroup: ValueGroup, reader: Reader, gson: Gson = fileGson) {
        gson.newJsonReader(reader).use { reader ->
            deserializeValueGroup(valueGroup, reader.parseTree())
        }
    }

    /**
     * Deserialize a config from a [JsonElement]. It should be [JsonObject].
     */
    fun deserializeValueGroup(valueGroup: ValueGroup, jsonElement: JsonElement) {
        val jsonObject = jsonElement.asJsonObject

        // Check if the name is the same as the config name
        val name = jsonObject.getAsJsonPrimitive("name").asString
        check(name == valueGroup.name || valueGroup.aliases.contains(name)) {
            "config name does not match the name in the json object"
        }

        val values = jsonObject.getAsJsonArray("value")
            .map { valueElement -> valueElement.asJsonObject }
            .associateBy { valueObj -> valueObj["name"].asString!! }

        // Migration Code for KillAura's Range Values
        if (valueGroup is ModuleKillAura) {
            valueGroup.range.migrateFromValues(values)
        }

        for (value in valueGroup.inner) {
            val currentElement = values[value.name]
            // Alias support
                ?: values.entries.firstOrNull { entry -> entry.key in value.aliases }?.value
                ?: continue

            deserializeValue(value, currentElement)
        }
    }

    /**
     * Deserialize a value from a json object
     */
    fun deserializeValue(value: Value<*>, jsonObject: JsonObject) {
        // In the case of a config, we need to go deeper and deserialize the config itself
        if (value is ValueGroup) {
            runCatching {
                if (value is ModeValueGroup<*>) {
                    // Set current active choice
                    runCatching {
                        value.setByString(jsonObject["active"].asString)
                    }.onFailure {
                        logger.error("Unable to deserialize active choice for ${value.name}", it)
                    }

                    // Deserialize each choice
                    val choices = jsonObject["choices"].asJsonObject

                    for (choice in value.modes) {
                        runCatching {
                            val choiceElement = choices[choice.name]
                                // Alias support
                                ?: choice.aliases.firstNotNullOfOrNull { alias -> choices[alias] }
                                ?: error("Choice ${choice.name} not found")

                            deserializeValueGroup(choice, choiceElement)
                        }.onFailure {
                            logger.error("Unable to deserialize choice ${choice.name}", it)
                        }
                    }
                }

                // Deserialize the rest of the config
                deserializeValueGroup(value, jsonObject)
            }.onFailure {
                logger.error("Unable to deserialize config ${value.name}", it)
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

    private fun ensureRootKeys() {
        for (valueGroup in configs) {
            if (valueGroup.key == null) {
                valueGroup.walkKeyPath()
            }
        }
    }

    private fun normalizeKeyInput(key: String): String {
        val trimmed = key.trim()
        if (trimmed.isBlank()) {
            return trimmed
        }
        val prefix = "$KEY_PREFIX."
        return if (trimmed.startsWith(prefix, ignoreCase = true)) {
            trimmed
        } else {
            prefix + trimmed
        }
    }

}
