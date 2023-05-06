/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2023 CCBlueX
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

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.adapter.*
import net.ccbluex.liquidbounce.config.util.ExcludeStrategy
import net.ccbluex.liquidbounce.render.Fonts
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.block.Block
import net.minecraft.item.Item
import java.io.File
import java.io.Reader
import java.io.Writer

/**
 * A config system which uses configurables
 *
 * @author kawaiinekololis (@team ccbluex)
 */
object ConfigSystem {

    // Config directory folder
    val rootFolder = File(
        mc.runDirectory,
        LiquidBounce.CLIENT_NAME
    ).apply { // Check if there is already a config folder and if not create new folder (mkdirs not needed - .minecraft should always exist)
        if (!exists()) {
            mkdir()
        }
    }

    // User config directory folder
    val userConfigsFolder = File(
        rootFolder,
        "configs"
    ).apply { // Check if there is already a config folder and if not create new folder (mkdirs not needed - .minecraft should always exist)
        if (!exists()) {
            mkdir()
        }
    }

    // A mutable list of all root configurable classes (and their sub-classes)
    private val configurables: MutableList<Configurable> = mutableListOf()

    // Gson
    private val confType = TypeToken.get(Configurable::class.java).type
    private val gson = GsonBuilder().setPrettyPrinting().addSerializationExclusionStrategy(ExcludeStrategy(false))
        .registerTypeHierarchyAdapter(ClosedRange::class.javaObjectType, RangeSerializer)
        .registerTypeHierarchyAdapter(Item::class.javaObjectType, ItemValueSerializer)
        .registerTypeAdapter(Color4b::class.javaObjectType, ColorSerializer)
        .registerTypeHierarchyAdapter(Block::class.javaObjectType, BlockValueSerializer)
        .registerTypeAdapter(Fonts.FontDetail::class.javaObjectType, FontDetailSerializer)
        .registerTypeAdapter(ChoiceConfigurable::class.javaObjectType, ChoiceConfigurableSerializer)
        .registerTypeHierarchyAdapter(NamedChoice::class.javaObjectType, EnumChoiceSerializer)
        .registerTypeAdapter(IntRange::class.javaObjectType, IntRangeSerializer)
        .registerTypeHierarchyAdapter(Configurable::class.javaObjectType, ConfigurableSerializer).create()

    /**
     * Create a new root configurable
     */
    fun root(name: String, tree: MutableList<out Configurable> = mutableListOf()): Configurable {
        @Suppress("UNCHECKED_CAST") return root(Configurable(name, tree as MutableList<Value<*>>))
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
    fun load() {
        for (configurable in configurables) { // Make a new .json file to save our root configurable
            File(rootFolder, "${configurable.loweredName}.json").runCatching {
                if (!exists()) {
                    storeAll()
                    return@runCatching
                }

                logger.debug("Reading config ${configurable.loweredName}...")
                deserializeConfigurable(configurable, reader())
                logger.info("Successfully loaded config '${configurable.loweredName}'.")
            }.onFailure {
                logger.error("Unable to load config ${configurable.loweredName}", it)
                storeAll()
            }
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
    fun serializeConfigurable(configurable: Configurable, writer: Writer) {
        gson.newJsonWriter(writer).use {
            gson.toJson(configurable, confType, it)
        }
    }

    /**
     * Deserialize a configurable from a reader
     */
    fun deserializeConfigurable(configurable: Configurable, reader: Reader) {
        JsonParser.parseReader(gson.newJsonReader(reader))?.let {
            deserializeConfigurable(configurable, it)
        }
    }

    /**
     * Deserialize a configurable from a json element
     */
    fun deserializeConfigurable(configurable: Configurable, jsonElement: JsonElement) {
        runCatching {
            val jsonObject = jsonElement.asJsonObject

            if (jsonObject.getAsJsonPrimitive("name").asString != configurable.name) {
                throw IllegalStateException()
            }

            val values =
                jsonObject.getAsJsonArray("value").map { it.asJsonObject }.associateBy { it["name"].asString!! }

            for (value in configurable.value) {
                if (value is Configurable) {
                    val currentElement = values[value.name] ?: continue

                    runCatching {
                        if (value is ChoiceConfigurable) {
                            runCatching {
                                val newActive = currentElement["active"].asString

                                value.setFromValueName(newActive)
                            }.onFailure { it.printStackTrace() }

                            val choices = currentElement["choices"].asJsonObject

                            for (choice in value.choices) {
                                runCatching {
                                    val choiceElement = choices[choice.name]

                                    deserializeConfigurable(choice, choiceElement)
                                }.onFailure { it.printStackTrace() }
                            }
                        }
                    }.onFailure { it.printStackTrace() }

                    deserializeConfigurable(value, currentElement)
                } else {
                    val currentElement = values[value.name] ?: continue

                    runCatching {
                        value.deserializeFrom(gson, currentElement["value"])
                    }.onFailure { it.printStackTrace() }
                }

            }
        }.onFailure { it.printStackTrace() }
    }

}
