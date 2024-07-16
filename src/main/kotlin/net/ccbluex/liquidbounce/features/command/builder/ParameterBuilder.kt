/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleManager

class ParameterBuilder<T> private constructor(val name: String) {

    private var verifier: ParameterVerifier<T>? = null
    private var required: Boolean? = null
    private var vararg: Boolean = false
    private var autocompletionHandler: AutoCompletionHandler? = null
    private var useMinecraftAutoCompletion: Boolean = false

    companion object {
        val STRING_VALIDATOR: ParameterVerifier<String> = { ParameterValidationResult.ok(it) }
        val MODULE_VALIDATOR: ParameterVerifier<Module> = { name ->
            val mod = ModuleManager.find { it.name.equals(name, true) }

            if (mod == null) {
                ParameterValidationResult.error("Module '$name' not found")
            } else {
                ParameterValidationResult.ok(mod)
            }
        }
        val INTEGER_VALIDATOR: ParameterVerifier<Int> = {
            try {
                ParameterValidationResult.ok(it.toInt())
            } catch (e: NumberFormatException) {
                ParameterValidationResult.error("'$it' is not a valid integer")
            }
        }
        val POSITIVE_INTEGER_VALIDATOR: ParameterVerifier<Int> = {
            try {
                val integer = it.toInt()

                if (integer >= 0) {
                    ParameterValidationResult.ok(integer)
                } else {
                    ParameterValidationResult.error("The integer must be positive")
                }
            } catch (e: NumberFormatException) {
                ParameterValidationResult.error("'$it' is not a valid integer")
            }
        }
        val BOOLEAN_VALIDATOR: ParameterVerifier<Boolean> = {
            when (it.lowercase()) {
                "yes" -> ParameterValidationResult.ok(true)
                "no" -> ParameterValidationResult.ok(false)
                "true" -> ParameterValidationResult.ok(true)
                "false" -> ParameterValidationResult.ok(false)
                "on" -> ParameterValidationResult.ok(true)
                "off" -> ParameterValidationResult.ok(false)
                else -> ParameterValidationResult.error("'$it' is not a valid boolean")
            }
        }

        fun <T> begin(name: String): ParameterBuilder<T> = ParameterBuilder(name)

    }

    fun verifiedBy(verifier: ParameterVerifier<T>): ParameterBuilder<T> {
        this.verifier = verifier

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

    fun autocompletedWith(autocompletionHandler: (String) -> List<String>): ParameterBuilder<T> {
        this.autocompletionHandler = { begin, _ -> autocompletionHandler(begin) }

        return this
    }

    fun useMinecraftAutoCompletion(): ParameterBuilder<T> {
        this.useMinecraftAutoCompletion = true

        return this
    }

    fun build(): Parameter<T> {
        require(!this.useMinecraftAutoCompletion || autocompletionHandler == null) {
            "Standard Minecraft autocompletion was enabled and an autocompletion handler was set"
        }

        return Parameter(
            this.name,
            this.required
                ?: throw IllegalArgumentException("The parameter was neither marked as required nor as optional."),
            this.vararg,
            this.verifier,
            autocompletionHandler,
            useMinecraftAutoCompletion
        )
    }

}
