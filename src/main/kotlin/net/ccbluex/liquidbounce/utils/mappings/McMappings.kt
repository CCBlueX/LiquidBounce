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

package net.ccbluex.liquidbounce.utils.mappings

import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.io.resource
import net.fabricmc.mappings.Mappings
import net.fabricmc.mappings.model.V2MappingsProvider

/**
 * Tiny mappings
 *
 * These are fabric mappings which are being exported when the jar is being built.
 * It allows you to remap obfuscated environments into readable names.
 *
 * This is going to be used for ultralight-databind and our JS script engine.
 */
object McMappings {

    var mappings: Mappings? = null

    fun load() {
        runCatching {
            mappings = V2MappingsProvider.readTinyMappings(resource("/mappings/mappings.tiny").bufferedReader())
        }.onFailure {
            logger.error("Unable to load mappings. Ignore this if you are using a development environment.", it)
        }
    }

    fun mapClass(clazz: Class<*>): String {
        val className = clazz.name.replace('.', '/')

        return mappings?.classEntries?.find {
            it?.get("intermediary") == className
        }?.get("named") ?: className
    }

    fun mapField(clazz: Class<*>, name: String): String {
        val className = clazz.name.replace('.', '/')

        return mappings?.fieldEntries?.find {
            val intern = it?.get("intermediary") ?: return@find false

            // todo: check desc
            intern.owner == className && intern.name == name
        }?.get("named")?.name ?: name
    }

    fun mapMethod(clazz: Class<*>, name: String, desc: String): String {
        val className = clazz.name.replace('.', '/')

        return mappings?.methodEntries?.find {
            val intern = it?.get("intermediary") ?: return@find false

            intern.owner == className && intern.name == name && intern.desc == desc
        }?.get("named")?.name ?: name
    }

}
