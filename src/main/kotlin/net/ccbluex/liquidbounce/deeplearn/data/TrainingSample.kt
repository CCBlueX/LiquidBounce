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

package net.ccbluex.liquidbounce.deeplearn.data

/** A training sample that can write its values directly into flat arrays. */
interface TrainingSample {

    val asInput: FloatArray
        get() = FloatArray(inputSize).also { input ->
            check(fillAsInput(input, 0) == input.size) { "Input size does not match the number of written values" }
        }

    /** Number of input values in this sample. */
    val inputSize: Int

    val asOutput: FloatArray
        get() = FloatArray(outputSize).also { output ->
            check(fillAsOutput(output, 0) == output.size) { "Output size does not match the number of written values" }
        }

    /** Number of output values in this sample. */
    val outputSize: Int

    /** Writes input values at [fromIndex] and returns the exclusive end index. */
    fun fillAsInput(dest: FloatArray, fromIndex: Int): Int

    /** Writes output values at [fromIndex] and returns the exclusive end index. */
    fun fillAsOutput(dest: FloatArray, fromIndex: Int): Int

}
