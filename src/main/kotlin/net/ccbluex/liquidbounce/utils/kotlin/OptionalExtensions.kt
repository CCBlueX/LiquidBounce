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

package net.ccbluex.liquidbounce.utils.kotlin

import java.util.Optional
import java.util.OptionalDouble
import java.util.OptionalInt
import java.util.OptionalLong

inline fun <T : Any> optional(value: T?) = Optional.ofNullable(value)

inline fun <T : Any> optional(block: () -> T?) = Optional.ofNullable(block())

inline fun optional(value: Int): OptionalInt = OptionalInt.of(value)

inline fun optional(value: Long): OptionalLong = OptionalLong.of(value)

inline fun optional(value: Double): OptionalDouble = OptionalDouble.of(value)
