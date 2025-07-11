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

import net.ccbluex.liquidbounce.features.command.*
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager

class ParameterBuilder<T: Any> private constructor(val name: String) {

    private var verifier: Parameter.Verificator<T>? = null
    private var required: Boolean? = null
    private var vararg: Boolean = false
    private var autocompletionHandler: AutoCompletionProvider? = null

    companion object {
        val STRING_VALIDATOR: Parameter.Verificator<String> = Parameter.Verificator { sourceText ->
            ParameterValidationResult.ok(sourceText)
        }
        val MODULE_VALIDATOR: Parameter.Verificator<ClientModule> = Parameter.Verificator { sourceText ->
            ParameterValidationResult.ofNullable(
                ModuleManager.find { it.name.equals(sourceText, true) }
            ) {
                "Module '$sourceText' not found"
            }
        }
        val INTEGER_VALIDATOR: Parameter.Verificator<Int> = Parameter.Verificator { sourceText ->
            ParameterValidationResult.ofNullable(
                sourceText.toIntOrNull()
            ) {
                "'$sourceText' is not a valid integer"
            }
        }
        val POSITIVE_INTEGER_VALIDATOR: Parameter.Verificator<Int> = Parameter.Verificator { sourceText ->
            val integer = sourceText.toIntOrNull() ?:
                return@Verificator ParameterValidationResult.error("'$sourceText' is not a valid integer")

            if (integer >= 0) {
                ParameterValidationResult.ok(integer)
            } else {
                ParameterValidationResult.error("The integer must be positive")
            }
        }
        val BOOLEAN_VALIDATOR: Parameter.Verificator<Boolean> = Parameter.Verificator { sourceText ->
            when (sourceText.lowercase()) {
                "yes", "on", "true" -> ParameterValidationResult.ok(true)
                "no", "off", "false" -> ParameterValidationResult.ok(false)
                else -> ParameterValidationResult.error("'$sourceText' is not a valid boolean")
            }
        }

        @JvmStatic
        fun <T: Any> begin(name: String): ParameterBuilder<T> = ParameterBuilder(name)
    }

    fun verifiedBy(verifier: Parameter.Verificator<T>): ParameterBuilder<T> = apply {
        this.verifier = verifier
    }

    fun optional(): ParameterBuilder<T> = apply {
        this.required = false
    }

    /**
     * Marks this parameter as a vararg.
     *
     * The values are stored in an array
     *
     * Only allowed at the end.
     */
    fun vararg(): ParameterBuilder<T> = apply {
        this.vararg = true
    }

    fun required(): ParameterBuilder<T> = apply {
        this.required = true
    }

    fun autocompletedWith(autocompletionHandler: AutoCompletionProvider) = apply {
        this.autocompletionHandler = autocompletionHandler
    }

    fun build(): Parameter<T> {
        return Parameter(
            this.name,
            this.required
                ?: throw IllegalArgumentException("The parameter was neither marked as required nor as optional."),
            this.vararg,
            this.verifier,
            autocompletionHandler
        )
    }

}
