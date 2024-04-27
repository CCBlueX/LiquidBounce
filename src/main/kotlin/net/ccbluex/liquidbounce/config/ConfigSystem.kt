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
 */
package net.ccbluex.liquidbounce.config

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.authlib.account.MinecraftAccount
import net.ccbluex.liquidbounce.config.adapter.*
import net.ccbluex.liquidbounce.config.util.ExcludeStrategy
import net.ccbluex.liquidbounce.render.Fonts
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.registry.DynamicRegistryManager
import net.minecraft.text.Text
import java.io.File
import java.io.Reader
import java.io.Writer

/**
 * A config system which uses configurables
 *
 * @author kawaiinekololis (@team ccbluex)
 */
object ConfigSystem {

    /*    init {
            // Delete the config folder if we are integration testing.
            if (LiquidBounce.isIntegrationTesting) {
                File(mc.runDirectory, "${LiquidBounce.CLIENT_NAME}_tenacc_test/configs").deleteRecursively()
            }
        }*/

    private val clientDirectoryName = if (LiquidBounce.isIntegrationTesting) {
        "${LiquidBounce.CLIENT_NAME}_tenacc_test"
    } else {
        LiquidBounce.CLIENT_NAME
    }

    // Config directory folder
    val rootFolder = File(
        mc.runDirectory, clientDirectoryName
    ).apply {
        // Check if there is already a config folder and if not create new folder
        // (mkdirs not needed - .minecraft should always exist)
        if (!exists()) {
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

    // A mutable list of all root configurable classes (and their subclasses)
    private val configurables: MutableList<Configurable> = mutableListOf()

    // Gson
    private val confType = TypeToken.get(Configurable::class.java).type
    private val clientGson: Gson = GsonBuilder()
        .addSerializationExclusionStrategy(ExcludeStrategy())
        .registerCommonTypeAdapters()
        .registerTypeHierarchyAdapter(Configurable::class.javaObjectType, ConfigurableSerializer)
        .create()
    val autoConfigGson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .addSerializationExclusionStrategy(ExcludeStrategy())
        .registerCommonTypeAdapters()
        // A configurable serializer which will not include values with doNotInclude = true
        .registerTypeHierarchyAdapter(Configurable::class.javaObjectType, AutoConfigurableSerializer)
        .create()

    /**
     * Register common type adapters
     * These adapters include anything from Kotlin classes to Minecraft and LiquidBounce types
     * They are safe to use on any GSON instance. (clientGson, autoConfigGson, ...)
     * It does not include any configurable serializers, which means you need to add them yourself if needed!
     *
     * @see GsonBuilder.registerTypeHierarchyAdapter
     * @see GsonBuilder.registerTypeAdapter
     */
    internal fun GsonBuilder.registerCommonTypeAdapters() =
        registerTypeHierarchyAdapter(ClosedRange::class.javaObjectType, RangeSerializer)
            .registerTypeAdapter(IntRange::class.javaObjectType, IntRangeSerializer)
            .registerTypeHierarchyAdapter(Item::class.javaObjectType, ItemValueSerializer)
            .registerTypeAdapter(Color4b::class.javaObjectType, ColorSerializer)
            .registerTypeHierarchyAdapter(Block::class.javaObjectType, BlockValueSerializer)
            .registerTypeAdapter(Fonts.FontInfo::class.javaObjectType, FontDetailSerializer)
            .registerTypeAdapter(ChoiceConfigurable::class.javaObjectType, ChoiceConfigurableSerializer)
            .registerTypeHierarchyAdapter(NamedChoice::class.javaObjectType, EnumChoiceSerializer)
            .registerTypeHierarchyAdapter(MinecraftAccount::class.javaObjectType, MinecraftAccountSerializer)

    /**
     * Create new root configurable
     */
    fun root(name: String, tree: MutableList<out Configurable> = mutableListOf()): Configurable {
        @Suppress("UNCHECKED_CAST")
        return root(Configurable(name, tree as MutableList<Value<*>>))
    }

    /**
     * Add a root configurable
     */
    fun root(configurable: Configurable): Configurable {
        configurable.initConfigurable()
        configurables.add(configurable)
        return configurable
    }

    /**
     * All configurables should load now.
     */
    fun loadAll() {
        for (configurable in configurables) { // Make a new .json file to save our root configurable
            File(rootFolder, "${configurable.loweredName}.json").runCatching {
                if (!exists()) {
                    // Do not try to load a non-existing file
                    return@runCatching
                }

                logger.debug("Reading config ${configurable.loweredName}...")
                deserializeConfigurable(configurable, reader())
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
        File(rootFolder, "${configurable.loweredName}.json").runCatching {
            if (!exists()) {
                createNewFile().let { logger.debug("Created new file (status: $it)") }
            }

            logger.debug("Writing config ${configurable.loweredName}...")
            serializeConfigurable(configurable, writer())
            logger.info("Successfully saved config '${configurable.loweredName}'.")
        }.onFailure {
            logger.error("Unable to store config ${configurable.loweredName}", it)
        }
    }

    /**
     * Serialize a configurable to a writer
     */
    private fun serializeConfigurable(configurable: Configurable, writer: Writer, gson: Gson = this.clientGson) {
        gson.newJsonWriter(writer).use {
            gson.toJson(configurable, confType, it)
        }
    }

    /**
     * Serialize a configurable to a writer
     */
    fun serializeConfigurable(configurable: Configurable, gson: Gson = this.clientGson) =
        gson.toJsonTree(configurable, confType)

    /**
     * Deserialize a configurable from a reader
     */
    fun deserializeConfigurable(configurable: Configurable, reader: Reader, gson: Gson = this.clientGson) {
        JsonParser.parseReader(gson.newJsonReader(reader))?.let {
            deserializeConfigurable(configurable, it)
        }
    }

    /**
     * Deserialize a configurable from a json element
     */
    fun deserializeConfigurable(configurable: Configurable, jsonElement: JsonElement) {
        val jsonObject = jsonElement.asJsonObject

        // Handle auto config
        AutoConfig.handlePossibleAutoConfig(jsonObject)

        // Check if the name is the same as the configurable name
        check(jsonObject.getAsJsonPrimitive("name").asString == configurable.name)

        val values = jsonObject.getAsJsonArray("value").map {
            it.asJsonObject
        }.associateBy { it["name"].asString!! }

        for (value in configurable.inner) {
            val currentElement = values[value.name] ?: continue

            deserializeValue(value, currentElement)
        }
    }

    /**
     * Deserialize a value from a json object
     */
    private fun deserializeValue(value: Value<*>, jsonObject: JsonObject) {
        // In case of a configurable, we need to go deeper and deserialize the configurable itself
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

        // Otherwise we simply deserialize the value
        runCatching {
            value.deserializeFrom(clientGson, jsonObject["value"])
        }.onFailure {
            logger.error("Unable to deserialize value ${value.name}", it)
        }
    }


}
