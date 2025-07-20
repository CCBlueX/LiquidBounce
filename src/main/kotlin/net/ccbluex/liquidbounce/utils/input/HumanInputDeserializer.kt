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
package net.ccbluex.liquidbounce.utils.input

import com.mojang.brigadier.StringReader
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.minecraft.block.Block
import net.minecraft.client.util.InputUtil
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.sound.SoundEvent
import net.minecraft.util.Identifier
import java.awt.Color
import java.util.*
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
            Color4b(Color(it.toInt()))
        }
    }

    val blockDeserializer: StringDeserializer<Block> = StringDeserializer {
        val block = Registries.BLOCK.getOptionalValue(Identifier.fromCommandInput(StringReader(it))).getOrNull()

        requireNotNull(block) { "Unknown block '$it'" }
    }

    val itemDeserializer: StringDeserializer<Item> = StringDeserializer {
        val block = Registries.ITEM.getOptionalValue(Identifier.fromCommandInput(StringReader(it))).getOrNull()

        requireNotNull(block) { "Unknown item '$it'" }
    }

    val soundDeserializer: StringDeserializer<SoundEvent> = StringDeserializer {
        val sound = Registries.SOUND_EVENT.getOptionalValue(Identifier.fromCommandInput(StringReader(it))).getOrNull()

        requireNotNull(sound) { "Unknown sound '$it'" }
    }

    val statusEffectDeserializer: StringDeserializer<StatusEffect> = StringDeserializer {
        val effect = Registries.STATUS_EFFECT.getOptionalValue(Identifier.fromCommandInput(StringReader(it)))
            .getOrNull()

        requireNotNull(effect) { "Unknown status effect '$it'" }
    }

    val keyDeserializer: StringDeserializer<InputUtil.Key> = StringDeserializer(::inputByName)

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

    private fun fail(s: String): Boolean {
        throw IllegalArgumentException(s)
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
