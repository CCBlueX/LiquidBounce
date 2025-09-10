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

import net.ccbluex.liquidbounce.features.command.AutoCompletionProvider
import net.ccbluex.liquidbounce.features.command.Parameter
import net.ccbluex.liquidbounce.features.command.Parameter.Verificator.Result
import net.ccbluex.liquidbounce.features.command.dsl.CommandBuilderDsl
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager

@CommandBuilderDsl
class ParameterBuilder<T: Any> private constructor(val name: String) {

    private var verifier: Parameter.Verificator<T>? = null
    private var required: Boolean? = null
    private var default: T? = null
    private var vararg: Boolean = false
    private var autocompletionHandler: AutoCompletionProvider? = null

    companion object {
        @JvmField
        val STRING_VALIDATOR: Parameter.Verificator<String> = Parameter.Verificator { sourceText ->
            Result.Ok(sourceText)
        }
        @JvmField
        val MODULE_VALIDATOR: Parameter.Verificator<ClientModule> = Parameter.Verificator { sourceText ->
            Result.ofNullable(
                ModuleManager.find { it.name.equals(sourceText, true) }
            ) { "Module '$sourceText' not found" }
        }
        @JvmField
        val INTEGER_VALIDATOR: Parameter.Verificator<Int> = Parameter.Verificator { sourceText ->
            Result.ofNullable(
                sourceText.toIntOrNull()
            ) { "'$sourceText' is not a valid integer" }
        }
        @JvmField
        val POSITIVE_INTEGER_VALIDATOR: Parameter.Verificator<Int> = Parameter.Verificator { sourceText ->
            val integer = sourceText.toIntOrNull()
            when {
                integer == null -> Result.Error("'$sourceText' is not a valid integer")
                integer >= 0 -> Result.Ok(integer)
                else -> Result.Error("The integer must be positive")
            }
        }
        @JvmField
        val BOOLEAN_VALIDATOR: Parameter.Verificator<Boolean> = Parameter.Verificator { sourceText ->
            when (sourceText.lowercase()) {
                "yes", "on", "true" -> Result.Ok(true)
                "no", "off", "false" -> Result.Ok(false)
                else -> Result.Error("'$sourceText' is not a valid boolean")
            }
        }

        @JvmStatic
        fun <T : Any> begin(name: String): ParameterBuilder<T> = ParameterBuilder(name)
    }

    fun verifiedBy(verifier: Parameter.Verificator<T>): ParameterBuilder<T> = apply {
        this.verifier = verifier
    }

    @JvmOverloads
    fun optional(default: T? = null): ParameterBuilder<T> = apply {
        this.required = false
        this.default = default
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

    /**
     * Filter from given strings provided by [placeholdersProvider].
     */
    inline fun autocompletedFrom(
        ignoreCase: Boolean = true,
        crossinline placeholdersProvider: () -> Iterable<String>?,
    ) = autocompletedWith { begin, _ ->
        val placeholders = placeholdersProvider()
        if (placeholders == null || placeholders.none()) {
            emptyList()
        } else {
            placeholders.filter { it.startsWith(begin, ignoreCase) }
        }
    }

    fun build(): Parameter<T> {
        return Parameter(
            this.name,
            this.required
                ?: throw IllegalArgumentException("The parameter was neither marked as required nor as optional."),
            this.default,
            this.vararg,
            this.verifier,
            autocompletionHandler
        )
    }

}
