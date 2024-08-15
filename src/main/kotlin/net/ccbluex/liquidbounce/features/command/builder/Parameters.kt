/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.world
import net.minecraft.enchantment.Enchantments
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKeys

fun blockParameter(name: String = "block") =
    ParameterBuilder
        .begin<String>(name)
        .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
        .autocompletedWith { begin ->
            Registries.BLOCK.map {
                it.translationKey
                    .removePrefix("block.")
                    .replace('.', ':')
            }
        }

fun itemParameter(name: String = "item") =
    ParameterBuilder
        .begin<String>(name)
        .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
        .autocompletedWith { begin ->
            Registries.ITEM.map {
                it.translationKey
                    .removePrefix("item.")
                    .removePrefix("block.")
                    .replace('.', ':')
            }
        }

fun enchantmentParameter(name: String = "enchantment") =
    ParameterBuilder
        .begin<String>(name)
        .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
        .autocompletedWith { begin ->
            world.registryManager.get(RegistryKeys.ENCHANTMENT).indexedEntries.map {
                it.idAsString
            }
        }
fun pageParameter(name: String = "page") =
    ParameterBuilder
        .begin<Int>(name)
        .verifiedBy(ParameterBuilder.POSITIVE_INTEGER_VALIDATOR)

fun moduleParameter(name: String = "module", validator: (Module) -> Boolean = { true }) =
    ParameterBuilder
        .begin<String>(name)
        .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
        .autocompletedWith { begin, args ->
            ModuleManager.autoComplete(begin, args, validator = validator)
        }


