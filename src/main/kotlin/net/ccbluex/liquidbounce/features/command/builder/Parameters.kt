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
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.world
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKeys

fun blockParameter(name: String = "block"): ParameterBuilder<String> {
    return ParameterBuilder
        .begin<String>(name)
        .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
        .autocompletedWith { _, _ ->
            Registries.BLOCK.map {
                it.translationKey
                    .removePrefix("block.")
                    .replace('.', ':')
            }
        }
}

fun itemParameter(name: String = "item"): ParameterBuilder<String> {
    return ParameterBuilder
        .begin<String>(name)
        .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
        .autocompletedWith { _, _ ->
            Registries.ITEM.map {
                it.translationKey
                    .removePrefix("item.")
                    .removePrefix("block.")
                    .replace('.', ':')
            }
        }
}

fun enchantmentParameter(name: String = "enchantment"): ParameterBuilder<String> {
    return ParameterBuilder
        .begin<String>(name)
        .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
        .autocompletedWith { _, _ ->
            world.registryManager.getOrThrow(RegistryKeys.ENCHANTMENT).indexedEntries.map {
                it.idAsString
            }
        }
}

fun pageParameter(name: String = "page"): ParameterBuilder<Int> {
    return ParameterBuilder
        .begin<Int>(name)
        .verifiedBy(ParameterBuilder.POSITIVE_INTEGER_VALIDATOR)
}

fun moduleParameter(
    name: String = "module",
    validator: (ClientModule) -> Boolean = { true }
): ParameterBuilder<String> {
    return ParameterBuilder
        .begin<String>(name)
        .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
        .autocompletedWith { begin, _ -> ModuleManager.autoComplete(begin, validator = validator) }
}

fun valueNameParameter(name: String = "valueName") = ParameterBuilder
    .begin<String>(name)
    .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
    .autocompletedWith { begin, args ->
        val moduleName = args[2]
        val module = ModuleManager.find { module -> module.name.equals(moduleName, true) }
            ?: return@autocompletedWith emptyList()

        val values = module.getContainedValuesRecursively()
            .filter { !it.name.equals("Bind", true) }
            .map { it.name }
        values.filter { it.startsWith(begin, true) }
    }

fun valueTypeParameter(name: String = "value") = ParameterBuilder
    .begin<String>(name)
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

fun configurableParameter(
    name: String = "configurable",
    validator: (Configurable) -> Boolean = { true }
): ParameterBuilder<String> {
    return ParameterBuilder
        .begin<String>(name)
        .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
        .autocompletedWith { begin, _ -> ConfigSystem.autoComplete(begin, validator = validator) }
}

fun playerParameter(name: String = "playerName"): ParameterBuilder<String> {
    return ParameterBuilder
        .begin<String>(name)
        .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
        .useMinecraftAutoCompletion()
}

inline fun <reified T> enumsParameter(
    name: String = "enum"
): ParameterBuilder<String> where T : Enum<*>, T : NamedChoice {
    return ParameterBuilder
        .begin<String>(name)
        .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
        .autocompletedWith { begin, _ ->
            val parts = begin.split(",")
            val matchingPrefix = parts.last().trim()
            val resultPrefix = if (parts.size > 1) parts.subList(0, parts.size - 1).joinToString(",") + "," else ""

            val suggestions = T::class.java.enumConstants
                .toList()
                .map { (it as NamedChoice).choiceName }
                .filter { it.startsWith(matchingPrefix, ignoreCase = true) }

            suggestions.map {
                if (resultPrefix.isNotEmpty()) {
                    resultPrefix + it
                } else {
                    it
                }
            }
        }
}

inline fun <reified T> parseEnumsFromParameter(
    name: String?,
): List<T> where T : Enum<T>, T : NamedChoice {
    if (name == null) return emptyList()
    return name.split(",").mapNotNull { enumName ->
        enumValues<T>().firstOrNull { it.choiceName.equals(enumName.trim(), ignoreCase = true) }
    }
}
