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

package net.ccbluex.liquidbounce.script

import org.graalvm.polyglot.Value

inline fun <reified T> Value.asArray(): Array<T> = this.asType<Array<T>>()

fun Value.asBooleanArray(): BooleanArray = this.asType<BooleanArray>()

fun Value.asByteArray(): ByteArray = this.asType<ByteArray>()

fun Value.asCharArray(): CharArray = this.asType<CharArray>()

fun Value.asShortArray(): ShortArray = this.asType<ShortArray>()

fun Value.asIntArray(): IntArray = this.asType<IntArray>()

fun Value.asFloatArray(): FloatArray = this.asType<FloatArray>()

fun Value.asLongArray(): LongArray = this.asType<LongArray>()

fun Value.asDoubleArray(): DoubleArray = this.asType<DoubleArray>()

inline fun <reified T> Value.asType(): T = this.`as`(T::class.java)
