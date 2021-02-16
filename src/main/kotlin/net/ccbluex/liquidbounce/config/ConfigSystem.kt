/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
import net.ccbluex.liquidbounce.utils.logger
import net.ccbluex.liquidbounce.utils.mc
import java.io.File
import java.lang.reflect.Type

/**
 * A config system which uses configurables
 *
 * @author kawaiinekololis (@team ccbluex)
 */
object ConfigSystem {

    // Config directory folder
    val rootFolder = File(mc.runDirectory, LiquidBounce.CLIENT_NAME).apply {
        // Check if there is already a config folder and if not create new folder (mkdirs not needed - .minecraft should always exist)
        if (!exists())
            mkdir()
    }

    // A mutable list of all root configurable classes (and their sub classes)
    private val configurables: MutableList<Configurable> = mutableListOf()

    // Gson
    private val confType = TypeToken.get(Configurable::class.java).type
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .addSerializationExclusionStrategy(ExcludeStrategy())
        .create()

    /**
     * Create a new root configurable
     */
    fun root(name: String, tree: MutableList<out Configurable> = mutableListOf()) {
        configurables.add(Configurable(name, tree.toMutableList()))
    }

    /**
     * Add a root configurable
     */
    fun root(configurable: Configurable) {
        configurables.add(configurable)
    }

    /**
     * All configurables should load now.
     */
    fun load() {
        for (configurable in configurables) {
            // Make a new .json file to save our root configurable
            File(rootFolder, "${configurable.name}.json").runCatching {
                if (!exists()) {
                    store()
                    return@runCatching
                }

                logger.info("Reading config ${configurable.name} from '$name'")
                configurable.overwrite(gson.fromJson(gson.newJsonReader(reader()), confType))
            }.onFailure {
                logger.error("Unable to load config ${configurable.name}", it)
                store()
            }
        }
    }

    /**
     * All configurables should store now.
     */
    fun store() {
        for (configurable in configurables) {
            // Make a new .json file to save our root configurable
            File(rootFolder, "${configurable.name}.json").runCatching {
                if (!exists()) {
                    createNewFile().let { logger.debug("Created new file (status: $it)") }
                }

                logger.info("Writing config ${configurable.name} to '$name'")
                gson.newJsonWriter(writer()).use {
                    gson.toJson(configurable, confType, it)
                }
            }.onFailure {
                logger.error("Unable to store config ${configurable.name}", it)
            }
        }
    }

}

