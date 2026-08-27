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
package net.ccbluex.liquidbounce.features.command.arguments

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.exceptions.CommandSyntaxException

/**
 * Reads the next unquoted token: every character up to the next space (or end of input).
 *
 * Brigadier's [StringReader.readUnquotedString] only accepts `[0-9A-Za-z_.+-]`, which
 * truncates non-ASCII input such as Chinese module or player names. Client commands
 * historically split on spaces only, so this restores that contract.
 */
fun StringReader.readUnquotedToken(): String {
    val start = cursor
    while (canRead() && peek() != ' ') {
        skip()
    }
    return string.substring(start, cursor)
}

/**
 * Quoted string (same rules as [StringReader.readString]) or, if unquoted, an
 * [readUnquotedToken] that accepts any non-space character.
 */
fun StringReader.readClientString(): String {
    if (!canRead()) {
        return ""
    }

    val next = peek()
    if (next == '"' || next == '\'') {
        skip()
        return readStringUntil(next)
    }

    return readUnquotedToken()
}

/**
 * Client-command string arguments. Prefer these over Brigadier
 * [com.mojang.brigadier.arguments.StringArgumentType.word] / `string()` so unquoted
 * tokens are not limited to the ASCII identifier whitelist.
 *
 * `greedyString()` is unchanged: it already consumes the remainder of the line as-is.
 */
object ClientStringArgumentType {

    /**
     * One unquoted token. Quotes are literal characters, not grouping.
     */
    @JvmStatic
    fun word(): ArgumentType<String> = UnquotedWord

    /**
     * One token: a quoted string (quotes stripped, escapes honored) or an unquoted
     * [readUnquotedToken].
     */
    @JvmStatic
    fun string(): ArgumentType<String> = QuotedOrUnquoted

    private object UnquotedWord : ArgumentType<String> {
        private val examples = listOf("word", "words_with_underscores")

        override fun parse(reader: StringReader): String = reader.readUnquotedToken()

        override fun getExamples(): Collection<String> = examples
    }

    private object QuotedOrUnquoted : ArgumentType<String> {
        private val examples = listOf("word", "\"quoted phrase\"", "\"escaped \\\"phrase\\\"\"")

        @Throws(CommandSyntaxException::class)
        override fun parse(reader: StringReader): String = reader.readClientString()

        override fun getExamples(): Collection<String> = examples
    }
}
