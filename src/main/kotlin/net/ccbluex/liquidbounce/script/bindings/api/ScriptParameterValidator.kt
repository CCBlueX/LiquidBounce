package net.ccbluex.liquidbounce.script.bindings.api

import net.ccbluex.liquidbounce.features.command.ParameterValidationResult
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyObject

@Suppress("unused")
class ScriptParameterValidator(val bindings: Value) {

    private fun map(param: String, validator: ParameterVerificator<*>): Value {
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
