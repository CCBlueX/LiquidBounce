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
package net.ccbluex.liquidbounce.utils.input

import com.mojang.blaze3d.platform.InputConstants
import com.mojang.brigadier.StringReader
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.item.getOrNull
import net.minecraft.core.Registry
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import java.io.File
import java.util.Locale
import kotlin.jvm.optionals.getOrNull

object HumanInputDeserializer {
    val textDeserializer = StringDeserializer { it }
    val booleanDeserializer = StringDeserializer { str ->
        when (str.lowercase(Locale.ROOT)) {
            "true", "on", "yes" -> true
            "false", "off", "no" -> false
            else -> require(false) { "Unknown boolean value '$str' (allowed are true/on/yes or false/off/no)" }
        }
    }

    val floatDeserializer = StringDeserializer(String::toFloat)
    val floatRangeDeserializer = StringDeserializer { str ->
        parseRange(str, floatDeserializer) { lhs, rhs -> lhs..rhs }
    }

    val intDeserializer = StringDeserializer(String::toInt)
    val intRangeDeserializer = StringDeserializer { str ->
        parseRange(str, intDeserializer) { lhs, rhs -> lhs..rhs }
    }
    val textArrayDeserializer = StringDeserializer { parseArray(it, textDeserializer) }

    val colorDeserializer = StringDeserializer {
        if (it.startsWith('#')) {
            Color4b.fromHex(it)
        } else {
            Color4b(it.toInt())
        }
    }

    fun <T : Any> registryItemDeserializer(key: ResourceKey<Registry<T>>) = StringDeserializer {
        val registry = key.getOrNull() ?: error("No registry '$key'")
        val item = registry.getOptional(Identifier.read(StringReader(it))).getOrNull()

        requireNotNull(item) { "Unknown item '$it'" }
    }

    fun <T : Any> registryItemDeserializer(registry: Registry<T>) = StringDeserializer {
        val item = registry.getOptional(Identifier.read(StringReader(it))).getOrNull()

        requireNotNull(item) { "Unknown item '$it'" }
    }

    val clientModuleDeserializer: StringDeserializer<ClientModule> = StringDeserializer {
        val module = ModuleManager[it]

        requireNotNull(module) { "Unknown module '$it'" }
    }

    val keyDeserializer: StringDeserializer<InputConstants.Key> = StringDeserializer(::inputByName)

    val fileDeserializer: StringDeserializer<File> = StringDeserializer(::File)

    fun <T> parseArray(str: String, componentDeserializer: StringDeserializer<T>): MutableList<T> {
        return str.split(",").mapTo(ArrayList(), componentDeserializer::deserializeThrowing)
    }

    private inline fun <N, R> parseRange(
        str: String,
        numberParser: StringDeserializer<N>,
        rangeSupplier: (N, N) -> R
    ): R {
        val split = str.split("..")

        require(split.size == 2) { "Invalid range '$str', must be in the format 'min..max'" }

        val lhs = numberParser.deserializeThrowing(split[0])
        val rhs = numberParser.deserializeThrowing(split[1])

        return rangeSupplier(lhs, rhs)
    }

    fun interface StringDeserializer<out T> {
        /**
         * Tries to parse the input.
         *
         * @throws IllegalArgumentException if the input is invalid
         */
        @Throws(IllegalArgumentException::class)
        fun deserializeThrowing(str: String): T
    }
}
