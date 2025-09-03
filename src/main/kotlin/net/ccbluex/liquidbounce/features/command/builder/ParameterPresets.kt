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

@file:Suppress("TooManyFunctions")
package net.ccbluex.liquidbounce.features.command.builder

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.VALUE_NAME_ORDER
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.features.command.Parameter.Verificator.Result
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.kotlin.emptyEnumSet
import net.minecraft.block.Block
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.Identifier
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

fun ParameterBuilder.Companion.rootConfigurables(
    name: String = "configurables",
    predicate: (Configurable) -> Boolean = { true }
) = values<Configurable>(
    paramName = name, typeName = "Configurable", all = ConfigSystem.configurables, predicate = predicate
)

inline fun <reified T> ParameterBuilder.Companion.enumChoice(
    name: String = "enum",
    crossinline predicate: (T) -> Boolean = { true },
) where T : Enum<T>, T : NamedChoice = begin<T>(name)
    .verifiedBy { sourceText ->
        val values = enumValues<T>()
        val choice = values.firstOrNull { v -> v.choiceName.equals(sourceText, true) && predicate(v) }
        if (choice == null) {
            Result.Error("$sourceText is not a valid choice")
        } else {
            Result.Ok(choice)
        }
    }
    .autocompletedWith { begin, _ ->
        enumValues<T>().mapNotNull { v ->
            v.choiceName.takeIf { predicate(v) && it.startsWith(begin, true) }
        }
    }

inline fun <reified T> ParameterBuilder.Companion.enumChoices(
    name: String = "enums",
    crossinline predicate: (T) -> Boolean = { true },
) where T : Enum<T>, T : NamedChoice = begin<Set<T>>(name)
    .verifiedBy { sourceText ->
        val values = enumValues<T>().filterTo(emptyEnumSet(), predicate)
        val choices = sourceText.split(',').mapNotNullTo(emptyEnumSet<T>()) {
            values.firstOrNull { v -> v.choiceName.equals(it, ignoreCase = true) }
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
            predicate(v) && v.choiceName.startsWith(choicePrefix, true)
        }.map {
            prefix + it.choiceName
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
            registry.getOptionalValue(id).getOrNull()
        ) { "$sourceText is not a valid $typeName" }
    }
    .autocompletedFrom {
        registry.ids.map { it.toString() }
    }

fun ParameterBuilder.Companion.enchantment(
    name: String = "enchantment",
) = begin<String>(name)
    .verifiedBy(STRING_VALIDATOR)
    .autocompletedFrom {
        world.registryManager.getOrThrow(RegistryKeys.ENCHANTMENT).indexedEntries.map { it.idAsString }
    }

fun ParameterBuilder.Companion.block(
    name: String = "block",
) = fromRegistry<Block>(name, "Block", Registries.BLOCK)

fun ParameterBuilder.Companion.item(
    name: String = "item",
) = begin<String>(name)
    .verifiedBy(STRING_VALIDATOR)
    .autocompletedFrom {
        Registries.ITEM.ids.map { it.toString() }
    }

fun ParameterBuilder.Companion.playerName(
    name: String = "playerName",
) = begin<String>(name)
    .verifiedBy(STRING_VALIDATOR)
    .autocompletedFrom {
        mc.networkHandler?.playerList?.map { it.profile.name }
    }

fun ParameterBuilder.Companion.valueName(
    name: String = "valueName",
) = begin<String>(name)
    .verifiedBy(STRING_VALIDATOR)
    .autocompletedWith { begin, args ->
        val moduleName = args[2]
        val module = ModuleManager.find { module -> module.name.equals(moduleName, true) }
            ?: return@autocompletedWith emptyList()

        module.getContainedValuesRecursively()
            .filter { !it.name.equals("Bind", true) }
            .map { it.name }
            .filter { it.startsWith(begin, true) }
    }

fun ParameterBuilder.Companion.valueType(
    name: String = "value",
) = begin<String>(name)
    .verifiedBy(STRING_VALIDATOR)
    .autocompletedWith { begin, args ->
        val moduleName = args[2]
        val module = ModuleManager.find {
            it.name.equals(moduleName, true)
        } ?: return@autocompletedWith emptyList()

        val valueName = args[3]

        val value = module.getContainedValuesRecursively().firstOrNull {
            it.name.equals(valueName, true)
        } ?: return@autocompletedWith emptyList()

        val options = value.valueType.completer.possible(value)
        options.filter { it.startsWith(begin, true) }
    }
