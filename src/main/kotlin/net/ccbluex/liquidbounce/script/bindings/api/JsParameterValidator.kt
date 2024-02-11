package net.ccbluex.liquidbounce.script.bindings.api

import net.ccbluex.liquidbounce.features.command.ParameterVerifier
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyObject

class JsParameterValidator(val bindings: Value) {

    private fun map(param: String, validator: ParameterVerifier<*>): Value {
        val result = validator.invoke(param)

        val v = if (result.errorMessage == null) {
            mapOf("accept" to true, "value" to result.mappedResult!!)
        } else {
            mapOf("accept" to false, "error" to result.errorMessage)
        }

        return bindings.context.asValue(ProxyObject.fromMap(v))
    }

    fun string(param: String) = map(param, ParameterBuilder.STRING_VALIDATOR)

    fun module(param: String) = map(param, ParameterBuilder.MODULE_VALIDATOR)

    fun integer(param: String) = map(param, ParameterBuilder.INTEGER_VALIDATOR)

    fun positiveInteger(param: String) = map(param, ParameterBuilder.POSITIVE_INTEGER_VALIDATOR)
}
