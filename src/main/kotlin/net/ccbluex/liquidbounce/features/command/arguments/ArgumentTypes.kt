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
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager
import java.util.concurrent.CompletableFuture
import kotlin.enums.enumEntries

/**
 * Comma-separated multi-select argument, parsing to a [Set] of validated values.
 *
 * Mirrors the legacy `values`/`enumChoices` parameter presets (e.g. `.hide hide killaura,scaffold`):
 * the whole comma-separated token is validated and every name is matched case-insensitively.
 */
class MultiSelectArgumentType<T : Any>(
    private val typeName: String,
    private val all: Iterable<T>,
    private val predicate: (T) -> Boolean,
    private val nameOf: (T) -> String,
) : ArgumentType<Set<T>> {

    override fun parse(reader: StringReader): Set<T> {
        val sourceText = reader.readClientString()

        val values = buildSet {
            sourceText.split(',')
                .mapNotNullTo(this) { token ->
                    all.firstOrNull { nameOf(it).equals(token, true) && predicate(it) }
                }
        }

        if (values.isEmpty()) {
            throw CommandErrors.INVALID_MULTI_SELECT.createWithContext(reader, listOf(sourceText, typeName))
        }

        return values
    }

    override fun getExamples(): Collection<String> = all.filter(predicate).map(nameOf)

    override fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        val begin = builder.remaining
        val splitAt = begin.lastIndexOf(',') + 1
        val prefix = begin.substring(0, splitAt)
        val choicePrefix = begin.substring(splitAt)

        all.filter { predicate(it) && nameOf(it).startsWith(choicePrefix, true) }
            .forEach { builder.suggest(prefix + nameOf(it)) }

        return builder.buildFuture()
    }

}

/**
 * Fills this builder with the [nameOf] names of [values] that pass [include] and whose name
 * matches the typed prefix through [match], then completes the suggestion future.
 *
 * This is the shared skeleton of the suggestion providers of the argument factories in this
 * file (enum choices, modules, tagged values and registry entries), which all follow the
 * "filter candidates by case-insensitive prefix and suggest their name" pattern.
 */
private fun <T> SuggestionsBuilder.suggestMatching(
    values: Iterable<T>,
    nameOf: (T) -> String,
    match: (value: T, prefix: String) -> Boolean = { value, prefix -> nameOf(value).startsWith(prefix, true) },
    include: (T) -> Boolean = { true },
): CompletableFuture<Suggestions> {
    val prefix = remaining
    values.filter { include(it) && match(it, prefix) }
        .forEach { suggest(nameOf(it)) }
    return buildFuture()
}

/**
 * Single-choice argument over an enum-like set of tagged values: the tag is matched case-insensitively.
 */
class TaggedArgumentType<T : Tagged>(
    private val parameterName: String,
    private val values: Collection<T>,
    private val predicate: (T) -> Boolean = { true },
) : ArgumentType<T> {

    override fun parse(reader: StringReader): T {
        val sourceText = reader.readClientString()

        val choice = values.firstOrNull { it.tag.equals(sourceText, true) && predicate(it) }
            ?: throw CommandErrors.INVALID_CHOICE.createWithContext(reader, listOf(sourceText, parameterName))

        return choice
    }

    override fun getExamples(): Collection<String> = values.filter(predicate).map(Tagged::tag)

    override fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        return builder.suggestMatching(values, Tagged::tag, include = predicate)
    }

    companion object {
        inline operator fun <reified T> invoke(
            parameterName: String,
            noinline predicate: (T) -> Boolean = { true },
        ) where T : Enum<T>, T : Tagged =
            TaggedArgumentType(parameterName, enumEntries<T>(), predicate)
    }
}

/**
 * Single-choice module argument, mirroring the legacy `module()` parameter preset:
 * the module name is matched case-insensitively.
 */
class ModuleArgumentType(
    private val parameterName: String,
    private val predicate: (ClientModule) -> Boolean = { true },
) : ArgumentType<ClientModule> {

    override fun parse(reader: StringReader): ClientModule {
        val sourceText = reader.readClientString()

        return ModuleManager.find { it.name.equals(sourceText, true) && predicate(it) }
            ?: throw CommandErrors.NO_SUCH_MODULE.createWithContext(reader, sourceText)
    }

    override fun getExamples(): Collection<String> = ModuleManager.filter(predicate).map { it.name }

    override fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        return builder.suggestMatching(ModuleManager, ClientModule::name, include = predicate)
    }

}

/**
 * Reads every remaining whitespace-separated token of [reader], translating each one
 * through [parseToken]. The callback validates the token and throws a
 * [com.mojang.brigadier.exceptions.CommandSyntaxException] on invalid input.
 *
 * This is the shared parsing skeleton of the greedy multi-value argument types
 * ([MultiTaggedArgumentType] and the script multi-value validator), which all consume
 * one token per value and report errors per token.
 */
inline fun <T> readGreedyTokens(reader: StringReader, parseToken: (String) -> T): List<T> = buildList {
    while (reader.canRead()) {
        if (reader.peek() == ' ') {
            reader.skip()
            continue
        }

        this.add(parseToken(reader.readClientString()))
    }
}

/**
 * Greedy multi-value argument that parses every remaining token as a tagged value,
 * mirroring the legacy `enumChoice(...).vararg()` parameter (one token per value).
 */
class MultiTaggedArgumentType<T : Any>(
    private val parameterName: String,
    private val values: Collection<T>,
    private val tagOf: (T) -> String,
) : ArgumentType<List<T>> {

    override fun parse(reader: StringReader): List<T> {
        val output = readGreedyTokens(reader) { sourceText ->
            values.firstOrNull { tagOf(it).equals(sourceText, true) }
                ?: throw CommandErrors.INVALID_CHOICE.createWithContext(reader, listOf(sourceText, parameterName))
        }

        if (output.isEmpty()) {
            throw CommandErrors.EMPTY_MULTI_SELECT.createWithContext(reader, parameterName)
        }

        return output
    }

    override fun getExamples(): Collection<String> = values.map(tagOf)

    override fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        return builder.suggestMatching(values, tagOf)
    }

}

/**
 * Boolean argument mirroring the legacy `boolean` parameter preset: accepts
 * `yes`/`on`/`true` and `no`/`off`/`false` (case-insensitive).
 */
class BooleanArgumentType(
    private val parameterName: String,
) : ArgumentType<Boolean> {

    override fun parse(reader: StringReader): Boolean {
        val sourceText = reader.readClientString()

        return when (sourceText.lowercase()) {
            "yes", "on", "true" -> true
            "no", "off", "false" -> false
            else -> throw CommandErrors.INVALID_BOOLEAN.createWithContext(reader, listOf(sourceText, parameterName))
        }
    }

    override fun getExamples(): Collection<String> = listOf("true", "false")

    override fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        builder.suggest("true")
        builder.suggest("false")
        return builder.buildFuture()
    }

}
