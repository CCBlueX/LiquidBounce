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
package net.ccbluex.liquidbounce.features.command

import net.ccbluex.liquidbounce.lang.translation
import net.minecraft.text.MutableText

/**
 * Provides autocompletion for one specific parameter
 */
fun interface AutoCompletionProvider {
    /**
     * Autocompletion for a parameter
     *
     * For example for `.value Scaffold Mode G`, this function would be called with
     * - `begin = "G"`
     * - `args = ["Scaffold", "Mode", "G"]`
     *
     * @param begin the current text of the autocompleted parameter
     * @param args all current arguments of the command
     *
     * @return suggestions for the full parameter name
     */
    fun autocomplete(begin: String, args: List<String>): List<String>
}

class Parameter<T : Any>(
    /** Name of the parameter. */
    val name: String,
    /** Whether the parameter is required. */
    val required: Boolean,
    /** Default value if optional. */
    val default: T?,
    /** Whether the parameter is a vararg. Parsed result will be an Array if true. */
    val vararg: Boolean,
    val verifier: Verificator<T>?,
    val autocompletionHandler: AutoCompletionProvider?,
) {
    var command: Command? = null
        internal set
    var index: Int = -1
        internal set

    private val translationBaseKey: String
        get() = "${command?.translationBaseKey}.parameter.$name"

    val description: MutableText
        get() = translation("$translationBaseKey.description")

    fun interface Verificator<T : Any> {
        /**
         * Verifies and parses parameter.
         *
         * This function must not have any side effects since this function may be called
         * while the command is still being written!
         *
         * @return the text is not valid, this function returns [Result.Error], otherwise
         * [Result.Ok] with the parsed content is returned.
         */
        fun verifyAndParse(sourceText: String): Result<out T>

        sealed interface Result<T : Any> {

            companion object {
                internal inline fun <T : Any> ofNullable(value: T?, errorMessage: () -> String) =
                    value?.let(::Ok) ?: Error(errorMessage())
            }

            class Ok<T : Any>(val mappedResult: T) : Result<T>

            class Error(val errorMessage: String) : Result<Nothing>

        }

    }
}
