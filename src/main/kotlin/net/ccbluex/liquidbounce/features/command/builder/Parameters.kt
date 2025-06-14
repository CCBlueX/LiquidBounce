@file:Suppress("TooManyFunctions")
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

package net.ccbluex.liquidbounce.features.command.builder

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.VALUE_NAME_ORDER
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.features.command.ParameterValidationResult
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.kotlin.emptyEnumSet
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKeys
import kotlin.text.startsWith

object Parameters {

    private fun <V : Value<*>> value(
        paramName: String,
        typeName: String,
        all: Iterable<V>,
        predicate: (V) -> Boolean,
    ) = ParameterBuilder.begin<V>(paramName)
        .verifiedBy { sourceText ->
            val value = all.firstOrNull { v -> v.name.equals(sourceText, true) && predicate(v) }

            if (value == null) {
                ParameterValidationResult.error("'$sourceText' is not a valid $typeName")
            } else {
                ParameterValidationResult.ok(value)
            }
        }
        .autocompletedWith { begin, _ ->
            all.filter {
                it.name.startsWith(begin, true) && predicate(it)
            }.map {
                it.name
            }
        }

    private fun <V : Value<*>> values(
        paramName: String,
        typeName: String,
        all: Iterable<V>,
        predicate: (V) -> Boolean,
    ) = ParameterBuilder.begin<Set<V>>(paramName)
        .verifiedBy { sourceText ->
            val values = sourceText.split(',').mapNotNullTo(sortedSetOf(VALUE_NAME_ORDER)) {
                all.firstOrNull { v -> v.name.equals(it, true) && predicate(v) }
            }
            if (values.isEmpty()) {
                ParameterValidationResult.error("'$sourceText' contains no valid $typeName")
            } else {
                ParameterValidationResult.ok(values)
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

    fun module(
        name: String = "module",
        predicate: (ClientModule) -> Boolean = { true }
    ) = value<ClientModule>(
        paramName = name, typeName = "Module", all = ModuleManager, predicate = predicate
    )

    fun modules(
        name: String = "modules",
        predicate: (ClientModule) -> Boolean = { true }
    ) = values<ClientModule>(
        paramName = name, typeName = "Module", all = ModuleManager, predicate = predicate
    )

    fun configurables(
        name: String = "configurables",
        predicate: (Configurable) -> Boolean = { true }
    ) = values<Configurable>(
        paramName = name, typeName = "Configurable", all = ConfigSystem.configurables, predicate = predicate
    )

    inline fun <reified T> enumChoices(
        name: String = "enums"
    ) where T : Enum<T>, T : NamedChoice = ParameterBuilder.begin<Set<T>>(name)
        .verifiedBy { sourceText ->
            val values = enumValues<T>()
            val choices = sourceText.split(',').mapNotNullTo(emptyEnumSet<T>()) {
                values.firstOrNull { v -> v.choiceName.equals(it, ignoreCase = true) }
            }
            if (choices.isEmpty()) {
                ParameterValidationResult.error("$sourceText contains no valid choice")
            } else {
                ParameterValidationResult.ok(choices)
            }
        }
        .autocompletedWith { begin, _ ->
            val splitAt = begin.lastIndexOf(',') + 1
            val prefix = begin.substring(0, splitAt)
            val modulePrefix = begin.substring(splitAt)
            enumValues<T>().filter {
                it.choiceName.startsWith(modulePrefix, true)
            }.map {
                prefix + it.choiceName
            }
        }

    fun enchantment(
        name: String = "enchantment",
    ) = ParameterBuilder.begin<String>(name)
        .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
        .autocompletedWith { begin, _ ->
            world.registryManager.getOrThrow(RegistryKeys.ENCHANTMENT).indexedEntries.map {
                it.idAsString
            }.filter { it.startsWith(begin, ignoreCase = true) }
        }

    fun block(
        name: String = "block",
    ) = ParameterBuilder.begin<String>(name)
        .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
        .autocompletedWith { _, _ ->
            Registries.BLOCK.map {
                it.translationKey
                    .removePrefix("block.")
                    .replace('.', ':')
            }
        }

    fun item(
        name: String = "item",
    ) = ParameterBuilder.begin<String>(name)
        .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
        .autocompletedWith { _, _ ->
            Registries.ITEM.map {
                it.translationKey
                    .removePrefix("item.")
                    .removePrefix("block.")
                    .replace('.', ':')
            }
        }

    fun playerName(
        name: String = "playerName",
    ) = ParameterBuilder.begin<String>(name)
        .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
        .autocompletedWith { begin, _ ->
            mc.networkHandler?.playerList?.map { it.profile.name }?.filter { it.startsWith(begin, true) } ?: emptyList()
        }

    fun page(
        name: String = "page",
    ) = ParameterBuilder.begin<Int>(name)
        .verifiedBy(ParameterBuilder.POSITIVE_INTEGER_VALIDATOR)

    fun valueName(
        name: String = "valueName",
    ) = ParameterBuilder.begin<String>(name)
        .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
        .autocompletedWith { begin, args ->
            val moduleName = args[2]
            val module = ModuleManager.find { module -> module.name.equals(moduleName, true) }
                ?: return@autocompletedWith emptyList()

            module.getContainedValuesRecursively()
                .filter { !it.name.equals("Bind", true) }
                .map { it.name }
                .filter { it.startsWith(begin, true) }
        }

    fun valueType(
        name: String = "value",
    ) = ParameterBuilder.begin<String>(name)
        .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
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
}
