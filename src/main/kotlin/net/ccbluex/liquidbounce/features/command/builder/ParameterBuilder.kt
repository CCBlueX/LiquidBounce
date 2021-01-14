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

package net.ccbluex.liquidbounce.features.command.builder

import net.ccbluex.liquidbounce.features.command.AutoCompletionHandler
import net.ccbluex.liquidbounce.features.command.Parameter
import net.ccbluex.liquidbounce.features.command.ParameterValidationResult
import net.ccbluex.liquidbounce.features.command.ParameterVerifier

class ParameterBuilder<T> private constructor(val name: String) {

    private var description: String? = null
    private var verifier: ParameterVerifier<T>? = null
    private var required: Boolean? = null
    private var vararg: Boolean = false
    private var autocompletionHandler: AutoCompletionHandler? = null
    private var useMinecraftAutoCompletion: Boolean = false

    companion object {
        val STRING_VALIDATOR: ParameterVerifier<String> = { ParameterValidationResult.ok(it) }
        val INTEGER_VALIDATOR: ParameterVerifier<Int> = {
            try {
                ParameterValidationResult.ok(it.toInt())
            } catch (e: NumberFormatException) {
                ParameterValidationResult.error("'${it}' is not a valid integer")
            }
        }
        val POSITIVE_INTEGER_VALIDATOR: ParameterVerifier<Int> = {
            try {
                val integer = it.toInt()

                if (integer >= 0)
                    ParameterValidationResult.ok(integer)
                else
                    ParameterValidationResult.error("The integer must be positive")
            } catch (e: NumberFormatException) {
                ParameterValidationResult.error("'${it}' is not a valid integer")
            }
        }

        fun <T> begin(name: String): ParameterBuilder<T> = ParameterBuilder(name)

        fun autocompleteWithList(supplier: () -> Iterable<String>): AutoCompletionHandler =
            { start -> supplier().filter { it.startsWith(start, true) } }
    }

    fun verifiedBy(verifier: ParameterVerifier<T>): ParameterBuilder<T> {
        this.verifier = verifier

        return this
    }

    fun description(description: String): ParameterBuilder<T> {
        this.description = description

        return this
    }

    fun optional(): ParameterBuilder<T> {
        this.required = false

        return this
    }

    /**
     * Marks this parameter as a vararg.
     *
     * The values are stored in an array
     *
     * Only allowed at the end.
     */
    fun vararg(): ParameterBuilder<T> {
        this.vararg = true

        return this
    }

    fun required(): ParameterBuilder<T> {
        this.required = true

        return this
    }

    fun autocompletedWith(autocompletionHandler: AutoCompletionHandler): ParameterBuilder<T> {
        this.autocompletionHandler = autocompletionHandler

        return this
    }

    fun useMinecraftAutoCompletion(): ParameterBuilder<T> {
        this.useMinecraftAutoCompletion = true

        return this
    }

    fun build(): Parameter<T> {
        if (this.useMinecraftAutoCompletion && autocompletionHandler == null) {
            throw IllegalArgumentException("Standard Minecraft autocompletion was enabled and an autocompletion handler was set")
        }

        if (required == true && vararg) {
            throw IllegalArgumentException("Varargs cannot be required")
        }

        return Parameter(
            this.name,
            this.description,
            this.required
                ?: throw IllegalArgumentException("The parameter was neither marked as required nor as optional."),
            this.vararg,
            this.verifier,
            autocompletionHandler,
            useMinecraftAutoCompletion
        )
    }

}
