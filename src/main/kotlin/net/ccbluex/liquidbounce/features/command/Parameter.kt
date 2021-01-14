/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

package net.ccbluex.liquidbounce.features.command


class ParameterValidationResult<T> private constructor(
    val errorMessage: String?,
    val mappedResult: T?
) {

    companion object {
        fun <T> ok(value: T): ParameterValidationResult<T> = ParameterValidationResult(null, value)
        fun <T> error(errorMessage: String): ParameterValidationResult<T> =
            ParameterValidationResult(errorMessage, null)
    }

}

typealias ParameterVerifier<T> = (String) -> ParameterValidationResult<T>
typealias AutoCompletionHandler = (String) -> List<String>

class Parameter<T>(
    val name: String,
    val description: String?,
    val required: Boolean,
    val vararg: Boolean,
    val verifier: ParameterVerifier<T>?,
    val autocompletionHandler: AutoCompletionHandler?,
    val useMinecraftAutoCompletion: Boolean
)
