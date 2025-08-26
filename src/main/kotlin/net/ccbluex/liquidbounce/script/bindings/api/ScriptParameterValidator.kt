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

import net.ccbluex.liquidbounce.features.command.Parameter
import net.ccbluex.liquidbounce.features.command.ParameterValidationResult
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyObject

@Suppress("unused")
class ScriptParameterValidator(val bindings: Value) {

    private fun map(param: String, validator: Parameter.Verificator<*>): Value {
        val v = when (val result = validator.verifyAndParse(param)) {
            is ParameterValidationResult.Ok -> mapOf("accept" to true, "value" to result.mappedResult)
            is ParameterValidationResult.Error -> mapOf("accept" to false, "error" to result.errorMessage)
        }

        return bindings.context.asValue(ProxyObject.fromMap(v))
    }

    fun string(param: String) = map(param, ParameterBuilder.STRING_VALIDATOR)

    fun module(param: String) = map(param, ParameterBuilder.MODULE_VALIDATOR)

    fun integer(param: String) = map(param, ParameterBuilder.INTEGER_VALIDATOR)

    fun positiveInteger(param: String) = map(param, ParameterBuilder.POSITIVE_INTEGER_VALIDATOR)
}
