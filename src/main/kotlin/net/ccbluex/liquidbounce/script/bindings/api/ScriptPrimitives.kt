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
package net.ccbluex.liquidbounce.script.bindings.api

/**
 * Because the languages used in scripts usually
 * have very broad requirements for numeric types.
 * For example, all numbers in JS are [Double],
 * which can cause problems on interoperations.
 *
 * The functions provided by this util can explicitly
 * convert script numbers or strings into primitives of the JVM.
 */
@Suppress("unused", "TooManyFunctions")
object ScriptPrimitives {

    fun boolean(boolean: Boolean): Boolean = boolean

    fun boolean(string: String?): Boolean = string.toBoolean()

    fun byte(byte: Byte): Byte = byte

    fun byte(long: Long): Byte = long.toByte()

    fun byte(string: String?): Byte = string?.toByte() ?: 0.toByte()

    fun short(short: Short): Short = short

    fun short(long: Long): Short = long.toShort()

    fun short(string: String?): Short = string?.toShort() ?: 0.toShort()

    fun char(char: Char): Char = char

    fun char(long: Long): Char = long.toInt().toChar()

    fun char(string: String?): Char = string?.firstOrNull() ?: 0.toChar()

    fun int(int: Int): Int = int

    fun int(long: Long): Int = long.toInt()

    fun int(string: String?): Int = string?.toInt() ?: 0

    fun long(long: Long): Long = long

    fun long(string: String?): Long = string?.toLong() ?: 0L

    fun float(float: Float): Float = float

    fun float(double: Double): Float = double.toFloat()

    fun float(string: String?): Float = string?.toFloat() ?: 0F

    fun double(double: Double): Double = double

    fun double(string: String?): Double = string?.toDouble() ?: 0.0

}
