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
package net.ccbluex.liquidbounce.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.authlib.account.MinecraftAccount
import net.ccbluex.liquidbounce.config.adapter.*
import net.ccbluex.liquidbounce.config.util.ExcludeStrategy
import net.ccbluex.liquidbounce.render.Fonts
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.regular
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
        mc.runDirectory, LiquidBounce.CLIENT_NAME
    ).apply { // Check if there is already a config folder and if not create new folder (mkdirs not needed - .minecraft should always exist)
        if (!exists()) {
            mkdir()
        }
    }

    // User config directory folder
    val userConfigsFolder = File(
        rootFolder, "configs"
    ).apply { // Check if there is already a config folder and if not create new folder (mkdirs not needed - .minecraft should always exist)
        if (!exists()) {
            mkdir()
        }
    }

    // A mutable list of all root configurable classes (and their sub-classes)
    private val configurables: MutableList<Configurable> = mutableListOf()

    // Gson
    private val confType = TypeToken.get(Configurable::class.java).type
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .addSerializationExclusionStrategy(ExcludeStrategy())
        .registerTypeHierarchyAdapter(ClosedRange::class.javaObjectType, RangeSerializer)
        .registerTypeHierarchyAdapter(Item::class.javaObjectType, ItemValueSerializer)
        .registerTypeAdapter(Color4b::class.javaObjectType, ColorSerializer)
        .registerTypeHierarchyAdapter(Block::class.javaObjectType, BlockValueSerializer)
        .registerTypeAdapter(Fonts.FontInfo::class.javaObjectType, FontDetailSerializer)
        .registerTypeAdapter(ChoiceConfigurable::class.javaObjectType, ChoiceConfigurableSerializer)
        .registerTypeHierarchyAdapter(NamedChoice::class.javaObjectType, EnumChoiceSerializer)
        .registerTypeAdapter(IntRange::class.javaObjectType, IntRangeSerializer)
        .registerTypeHierarchyAdapter(MinecraftAccount::class.javaObjectType, MinecraftAccountSerializer)
        .registerTypeHierarchyAdapter(Configurable::class.javaObjectType, ConfigurableSerializer).create()

    val autoConfigGson = GsonBuilder()
        .setPrettyPrinting()
        .addSerializationExclusionStrategy(ExcludeStrategy())
        .registerTypeHierarchyAdapter(ClosedRange::class.javaObjectType, RangeSerializer)
        .registerTypeHierarchyAdapter(Item::class.javaObjectType, ItemValueSerializer)
        .registerTypeAdapter(Color4b::class.javaObjectType, ColorSerializer)
        .registerTypeHierarchyAdapter(Block::class.javaObjectType, BlockValueSerializer)
        .registerTypeAdapter(Fonts.FontInfo::class.javaObjectType, FontDetailSerializer)
        .registerTypeAdapter(ChoiceConfigurable::class.javaObjectType, ChoiceConfigurableSerializer)
        .registerTypeHierarchyAdapter(NamedChoice::class.javaObjectType, EnumChoiceSerializer)
        .registerTypeAdapter(IntRange::class.javaObjectType, IntRangeSerializer)
        .registerTypeHierarchyAdapter(MinecraftAccount::class.javaObjectType, MinecraftAccountSerializer)
        .registerTypeHierarchyAdapter(Configurable::class.javaObjectType, AutoConfigurableSerializer)
        .create()

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
    fun serializeConfigurable(configurable: Configurable, writer: Writer, gson: Gson = this.gson) {
        gson.newJsonWriter(writer).use {
            gson.toJson(configurable, confType, it)
        }
    }

    /**
     * Deserialize a configurable from a reader
     */
    fun deserializeConfigurable(configurable: Configurable, reader: Reader, gson: Gson = this.gson) {
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

            val chatMessages = jsonObject.getAsJsonArray("chat")
            if (chatMessages != null) {
                for (messages in chatMessages) {
                    chat(messages.asString)
                }
            }

            val date = jsonObject.getAsJsonPrimitive("date").let { if (it == null) "" else it.asString }
            val time = jsonObject.getAsJsonPrimitive("time").let { if (it == null) "" else it.asString }
            val author = jsonObject.getAsJsonPrimitive("author").let { if (it == null) "" else "by $it" }
            if (date != "" || time != "" || author != "") {
                chat(regular("Config was created ${if (date != "" || time != "") "on $date $time" else ""} $author"))
            }
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
                                        ?: error("Choice ${choice.name} not found")

                                    deserializeConfigurable(choice, choiceElement)
                                }.onFailure {
                                    logger.error("Unable to deserialize choice ${choice.name}", it)
                                }
                            }
                        }
                    }.onFailure {
                        logger.error("Unable to deserialize configurable ${value.name}", it)
                    }

                    deserializeConfigurable(value, currentElement)
                } else {
                    val currentElement = values[value.name] ?: continue

                    runCatching {
                        value.deserializeFrom(gson, currentElement["value"])
                    }.onFailure {
                        logger.error("Unable to deserialize value ${value.name}", it)
                    }
                }

            }
        }.onFailure {
            logger.error("Unable to deserialize configurable ${configurable.name}", it)
        }
    }
}
