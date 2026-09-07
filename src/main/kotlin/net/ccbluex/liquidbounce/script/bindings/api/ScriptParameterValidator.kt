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
package net.ccbluex.liquidbounce.script.bindings.api

import net.ccbluex.liquidbounce.features.module.ModuleManager
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyObject

/**
 * Validation helpers exposed to scripts as `ParameterValidator`.
 *
 * Each function returns a `{ accept: boolean, value?: any, error?: string }` object,
 * mirroring the contract expected by the `validate` field of script command parameters
 * (see [net.ccbluex.liquidbounce.script.bindings.features.ScriptCommandBuilder]).
 */
@Suppress("unused")
class ScriptParameterValidator(val bindings: Value) {

    private fun map(param: String, parse: (String) -> Pair<Any?, String?>): Value {
        val (value, error) = parse(param)
        val v = if (error == null) {
            mapOf("accept" to true, "value" to value)
        } else {
            mapOf("accept" to false, "error" to error)
        }

        return bindings.context.asValue(ProxyObject.fromMap(v))
    }

    fun string(param: String) = map(param) { it to null }

    fun module(param: String) = map(param) { sourceText ->
        val module = ModuleManager.find { it.name.equals(sourceText, true) }
        if (module == null) null to "Module '$sourceText' not found" else module to null
    }

    fun integer(param: String) = map(param) { sourceText ->
        val integer = sourceText.toIntOrNull()
        if (integer == null) null to "'$sourceText' is not a valid integer" else integer to null
    }

    fun positiveInteger(param: String) = map(param) { sourceText ->
        val integer = sourceText.toIntOrNull()
        when {
            integer == null -> null to "'$sourceText' is not a valid integer"
            integer > 0 -> integer to null
            else -> null to "The integer must be positive"
        }
    }
}
