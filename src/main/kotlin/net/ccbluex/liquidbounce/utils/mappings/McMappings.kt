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

    fun remapClass(clazz: String): String {
        val className = clazz.replace('.', '/')

        return mappings?.classEntries?.find {
            it?.get("named") == className
        }?.get("intermediary") ?: className
    }

    fun remapField(clazz: Class<*>, name: String, superClasses: Boolean): String {
        val classNames = mutableSetOf(clazz.name.replace('.', '/'))

        if (superClasses) {
            var current = clazz
            while (current.name != "java.lang.Object") {
                current = current.superclass
                classNames.add(current.name.replace('.', '/'))
            }
        }

        return mappings?.fieldEntries?.find {
            val intern = it?.get("intermediary") ?: return@find false
            val named = it.get("named") ?: return@find false

            classNames.contains(intern.owner) && named.name == name
        }?.get("intermediary")?.name ?: name
    }

    fun remapMethod(clazz: Class<*>, name: String, superClasses: Boolean): String {
        val classNames = mutableSetOf(clazz.name.replace('.', '/'))

        if (superClasses) {
            var current = clazz
            while (current.name != "java.lang.Object") {
                current = current.superclass
                classNames.add(current.name.replace('.', '/'))
            }
        }

        return mappings?.methodEntries?.find {
            val intern = it?.get("intermediary") ?: return@find false
            val named = it.get("named") ?: return@find false

            classNames.contains(intern.owner) && named.name == name
        }?.get("intermediary")?.name ?: name
    }

}
