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

@file:Suppress("TooManyFunctions")
package net.ccbluex.liquidbounce.features.command.builder

import net.ccbluex.fastutil.enumSetOf
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.Config
import net.ccbluex.liquidbounce.config.types.VALUE_NAME_ORDER
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.features.command.Parameter.Verificator.Result
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.world
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.Block
import kotlin.jvm.optionals.getOrNull

private fun <V : Value<*>> ParameterBuilder.Companion.value(
    paramName: String,
    typeName: String,
    all: Iterable<V>,
    predicate: (V) -> Boolean,
) = begin<V>(paramName)
    .verifiedBy { sourceText ->
        Result.ofNullable(
            all.firstOrNull { v -> v.name.equals(sourceText, true) && predicate(v) }
        ) { "'$sourceText' is not a valid $typeName" }
    }
    .autocompletedWith { begin, _ ->
        all.filter {
            it.name.startsWith(begin, true) && predicate(it)
        }.map {
            it.name
        }
    }

private fun <V : Value<*>> ParameterBuilder.Companion.values(
    paramName: String,
    typeName: String,
    all: Iterable<V>,
    predicate: (V) -> Boolean,
) = begin<Set<V>>(paramName)
    .verifiedBy { sourceText ->
        val values = sourceText.split(',').mapNotNullTo(sortedSetOf(VALUE_NAME_ORDER)) {
            all.firstOrNull { v -> v.name.equals(it, true) && predicate(v) }
        }
        if (values.isEmpty()) {
            Result.Error("'$sourceText' contains no valid $typeName")
        } else {
            Result.Ok(values)
        }
    }
    .autocompletedWith { begin, _ ->
        val splitAt = begin.lastIndexOf(',') + 1
        val prefix = begin.substring(0, splitAt)
        val modulePrefix = begin.substring(splitAt)
        all.filter {
            it.name.startsWith(modulePrefix, true) && predicate(it)
        }.map {
            prefix + it.name
        }
    }

fun ParameterBuilder.Companion.module(
    name: String = "module",
    all: Iterable<ClientModule> = ModuleManager,
    predicate: (ClientModule) -> Boolean = { true }
) = value<ClientModule>(
    paramName = name, typeName = "Module", all = all, predicate = predicate
)

fun ParameterBuilder.Companion.modules(
    name: String = "modules",
    all: Iterable<ClientModule> = ModuleManager,
    predicate: (ClientModule) -> Boolean = { true }
) = values<ClientModule>(
    paramName = name, typeName = "Module", all = all, predicate = predicate
)

fun ParameterBuilder.Companion.configs(
    name: String = "configs",
    predicate: (Config) -> Boolean = { true }
) = values<Config>(
    paramName = name, typeName = "Config", all = ConfigSystem.configs, predicate = predicate
)

fun ParameterBuilder.Companion.valueGroupKeyPath(
    name: String = "valueGroupPath",
) = begin<String>(name)
    .verifiedBy(STRING_VALIDATOR)
    .autocompletedWith { begin, _ ->
        suggestKeySegments(begin) { prefix -> ConfigSystem.valueGroupsKeySequence(prefix) }
    }

fun ParameterBuilder.Companion.valueKeyPath(
    name: String = "path",
) = begin<String>(name)
    .verifiedBy(STRING_VALIDATOR)
    .autocompletedWith { begin, _ ->
        suggestKeySegments(begin) { prefix -> ConfigSystem.valueKeySequence(prefix) }
    }

inline fun <reified T> ParameterBuilder.Companion.enumChoice(
    name: String = "enum",
    crossinline predicate: (T) -> Boolean = { true },
) where T : Enum<T>, T : Tagged = begin<T>(name)
    .verifiedBy { sourceText ->
        val values = enumValues<T>()
        val choice = values.firstOrNull { v -> v.tag.equals(sourceText, true) && predicate(v) }
        if (choice == null) {
            Result.Error("$sourceText is not a valid choice")
        } else {
            Result.Ok(choice)
        }
    }
    .autocompletedWith { begin, _ ->
        enumValues<T>().mapNotNull { v ->
            v.tag.takeIf { predicate(v) && it.startsWith(begin, true) }
        }
    }

inline fun <reified T> ParameterBuilder.Companion.enumChoices(
    name: String = "enums",
    crossinline predicate: (T) -> Boolean = { true },
) where T : Enum<T>, T : Tagged = begin<Set<T>>(name)
    .verifiedBy { sourceText ->
        val values = enumValues<T>().filterTo(enumSetOf(), predicate)
        val choices = sourceText.split(',').mapNotNullTo(enumSetOf<T>()) {
            values.firstOrNull { v -> v.tag.equals(it, ignoreCase = true) }
        }
        if (choices.isEmpty()) {
            Result.Error("$sourceText contains no valid choice")
        } else {
            Result.Ok(choices)
        }
    }
    .autocompletedWith { begin, _ ->
        val splitAt = begin.lastIndexOf(',') + 1
        val prefix = begin.substring(0, splitAt)
        val choicePrefix = begin.substring(splitAt)
        enumValues<T>().filter { v ->
            predicate(v) && v.tag.startsWith(choicePrefix, true)
        }.map {
            prefix + it.tag
        }
    }

private fun <T : Any> ParameterBuilder.Companion.fromRegistry(
    paramName: String,
    typeName: String,
    registry: Registry<T>,
) = begin<T>(paramName)
    .verifiedBy { sourceText ->
        val id = Identifier.tryParse(sourceText)
            ?: return@verifiedBy Result.Error("'$paramName' is not a valid Identifier")

        Result.ofNullable(
            registry.getOptional(id).getOrNull()
        ) { "$sourceText is not a valid $typeName" }
    }
    .autocompletedFrom(minecraftPlaceholders = true) {
        registry.keySet().map { it.toString() }
    }

fun ParameterBuilder.Companion.enchantment(
    name: String = "enchantment",
) = begin<String>(name)
    .verifiedBy(STRING_VALIDATOR)
    .autocompletedFrom(minecraftPlaceholders = true) {
        world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap().map { it.registeredName }
    }

fun ParameterBuilder.Companion.block(
    name: String = "block",
) = fromRegistry<Block>(name, "Block", BuiltInRegistries.BLOCK)

fun ParameterBuilder.Companion.item(
    name: String = "item",
) = begin<String>(name)
    .verifiedBy(STRING_VALIDATOR)
    .autocompletedFrom(minecraftPlaceholders = true) {
        BuiltInRegistries.ITEM.keySet().map { it.toString() }
    }

fun ParameterBuilder.Companion.boolean(
    name: String,
) = begin<Boolean>(name)
    .verifiedBy(BOOLEAN_VALIDATOR)
    .autocompletedFrom { listOf("true", "false") }

fun ParameterBuilder.Companion.playerName(
    name: String = "playerName",
) = begin<String>(name)
    .verifiedBy(STRING_VALIDATOR)
    .autocompletedFrom {
        mc.connection?.onlinePlayers?.map { it.profile.name }
    }

fun ParameterBuilder.Companion.valueType(
    name: String = "value",
) = begin<String>(name)
    .verifiedBy(STRING_VALIDATOR)
    .autocompletedWith { begin, args ->
        val value = ConfigSystem.findValueByKey(args[2]) ?: return@autocompletedWith emptyList()

        val options = value.valueType.completer.possible(value)
        options.filter { it.startsWith(begin, true) }
    }

private data class KeySegmentQuery(
    val prefix: String,
    val typed: String,
    val depth: Int,
)

private fun suggestKeySegments(begin: String, keyProvider: (String) -> Sequence<String>): List<String> {
    val query = buildKeySegmentQuery(begin)
    return keyProvider(query.prefix)
        .map { it.lowercase() }
        .filter { query.prefix.isBlank() || it.startsWith(query.prefix) }
        .map { it.split('.') }
        .filter { it.size > query.depth }
        .map { it[query.depth] }
        .filter { it.startsWith(query.typed, true) }
        .map { formatSuggestion(query.prefix, it) }
        .distinct()
        .sorted()
        .toList()
}

private fun buildKeySegmentQuery(begin: String): KeySegmentQuery {
    val normalizedBegin = begin.lowercase()
    val effectiveBegin = addDefaultPrefixIfMissing(normalizedBegin)
    val (prefix, typed) = splitKeyPrefix(effectiveBegin)
    val depth = countSegments(prefix)
    return KeySegmentQuery(prefix, typed, depth)
}

private fun splitKeyPrefix(input: String): Pair<String, String> {
    val endsWithDot = input.endsWith(".")
    val lastDot = input.lastIndexOf('.')
    val prefix = if (lastDot >= 0) input.substring(0, lastDot + 1) else ""
    val typed = if (endsWithDot || lastDot < 0) input.substring(prefix.length) else input.substring(lastDot + 1)
    return prefix to typed
}

private fun countSegments(prefix: String): Int {
    return if (prefix.isBlank()) {
        0
    } else {
        prefix.dropLast(1).count { it == '.' } + 1
    }
}

private fun formatSuggestion(prefix: String, segment: String): String {
    val suggestion = "$prefix$segment"
    val defaultPrefix = "${ConfigSystem.KEY_PREFIX}."
    return suggestion.removePrefix(defaultPrefix)
}

private fun addDefaultPrefixIfMissing(input: String): String {
    val prefix = "${ConfigSystem.KEY_PREFIX}."
    return if (input.startsWith(prefix) || input == ConfigSystem.KEY_PREFIX) {
        input
    } else {
        prefix + input
    }
}
