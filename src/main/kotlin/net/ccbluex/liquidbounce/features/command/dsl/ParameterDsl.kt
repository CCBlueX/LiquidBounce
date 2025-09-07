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

package net.ccbluex.liquidbounce.features.command.dsl

import net.ccbluex.liquidbounce.features.command.Parameter
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder

inline fun <T : Any> CommandBuilder.addParam(
    block: ParameterBuilder.Companion.() -> ParameterBuilder<T>
): Parameter<T> = ParameterBuilder.block().build().also { parameter(it) }

inline fun <T : Any> CommandBuilder.addParam(
    name: String,
    block: ParameterBuilder<T>.() -> ParameterBuilder<T>
): Parameter<T> = ParameterBuilder.begin<T>(name).block().build().also { parameter(it) }
