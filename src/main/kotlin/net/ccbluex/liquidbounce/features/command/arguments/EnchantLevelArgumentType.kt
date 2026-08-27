/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, either version 3 of
 * the License, or (at your option) any later version.
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
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import java.util.concurrent.CompletableFuture

/**
 * A parsed enchantment level: either an explicit positive [Explicit.level] or the `max`
 * keyword ([Max]), which resolves to the enchantment's highest level.
 */
sealed interface EnchantLevel {
    /** The enchantment's highest level; always resolves through the holder at use time. */
    data object Max : EnchantLevel

    /** An explicit positive level, used as-is (may exceed the enchantment's max). */
    @JvmRecord
    data class Explicit(val level: Int) : EnchantLevel
}

/**
 * Positive enchantment level or the keyword `max` (the enchantment's highest level),
 * mirroring the legacy string-based level parameter of the `.enchant` command.
 */
object EnchantLevelArgumentType : ArgumentType<EnchantLevel> {

    private const val MAX_KEYWORD = "max"

    private val EXAMPLES = listOf(MAX_KEYWORD, "1", "2", "3", "4", "5")

    @Throws(CommandSyntaxException::class)
    override fun parse(reader: StringReader): EnchantLevel {
        val sourceText = reader.readClientString()

        if (sourceText.equals(MAX_KEYWORD, ignoreCase = true)) {
            return EnchantLevel.Max
        }

        val level = sourceText.toIntOrNull()
        if (level == null || level < 1) {
            throw CommandErrors.INVALID_ENCHANT_LEVEL.createWithContext(reader, listOf(sourceText, MAX_KEYWORD))
        }

        return EnchantLevel.Explicit(level)
    }

    override fun getExamples(): Collection<String> = EXAMPLES

    override fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        builder.suggest(MAX_KEYWORD)
        for (level in 1..5) {
            builder.suggest(level.toString())
        }
        return builder.buildFuture()
    }

}

/**
 * Resolves this parsed level against an enchantment holder: [EnchantLevel.Max] becomes
 * the enchantment's own maximum, an explicit level passes through unchanged.
 */
inline fun EnchantLevel.resolve(maxLevelOf: () -> Int): Int? = when (this) {
    is EnchantLevel.Max -> maxLevelOf()
    is EnchantLevel.Explicit -> level
}

/**
 * Renders this parsed level for the command result message: `max` stays the keyword,
 * an explicit level prints as its number.
 */
fun EnchantLevel.render(): String = when (this) {
    is EnchantLevel.Max -> "max"
    is EnchantLevel.Explicit -> level.toString()
}
