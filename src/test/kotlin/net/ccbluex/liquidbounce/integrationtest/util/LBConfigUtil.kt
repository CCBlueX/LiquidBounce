package net.ccbluex.liquidbounce.integrationtest.util

import net.ccbluex.liquidbounce.config.*


fun applySettings(configurable: Configurable, setter: SettingsApplication.() -> Unit) {
    setter(SettingsApplication(configurable, configurable.name))
}

class SettingsApplication(
    private val configurable: Configurable,
    private val path: String
) {
    private fun absolutePath(name: String): String {
        return "${this.path}::$name"
    }

    fun configurable(name: String, setter: SettingsApplication.() -> Unit) {
        val newConfigurable = findValue<Configurable>(name, ValueType.CONFIGURABLE)

        setter(SettingsApplication(newConfigurable, absolutePath(name)))
    }

    fun int(name: String, range: Int) {
        genericRanged(name, range, ValueType.INT)
    }

    fun float(name: String, range: Float) {
        genericRanged(name, range, ValueType.FLOAT_RANGE)
    }

    fun intRange(name: String, range: IntRange) {
        genericRanged(name, range, ValueType.INT_RANGE)
    }

    fun floatRange(name: String, range: ClosedFloatingPointRange<Float>) {
        genericRanged(name, range, ValueType.FLOAT_RANGE)
    }

    private inline fun <reified T, reified V: RangedValue<T>> genericRanged(
        name: String,
        range: T,
        valueType: ValueType
    ) {
        findValue<V>(name, valueType).set(range)
    }

    fun choice(name: String, newMode: String) {
        val foundValue = findValue(name)

        when (foundValue) {
            is ChoiceConfigurable -> foundValue.setFromValueName(newMode)
            is ChooseListValue -> foundValue.setFromValueName(newMode)
            else -> {
                throw IllegalArgumentException(
                    "Expected ${absolutePath(name)} to be a ${ChoiceConfigurable::class.java.simpleName}" +
                        " or a ${ChooseListValue::class.java.simpleName}" +
                        ", but is actually ${foundValue.javaClass.simpleName}")
            }
        }
    }

    private inline fun <reified T: Value<*>> findValue(name: String, valueType: ValueType): T {
        val foundValue = findValue(name)

        val retVal = foundValue as? T
            ?: throw IllegalArgumentException(
                "Expected ${absolutePath(name)} to be ${T::class.java.simpleName}" +
                        ", but is actually ${foundValue.javaClass.simpleName}"
            )

        if (foundValue.type() != valueType) {
            throw IllegalArgumentException(
                "Expected ${absolutePath(name)} to of type ${valueType.name}" +
                        ", but is actually ${foundValue.type()}"
            )
        }

        return retVal
    }

    private fun findValue(name: String): Value<*> {
        val foundValue = this.configurable.containedValues.find { it.name == name }

        requireNotNull(foundValue) { "${absolutePath(name)} was not found" }

        return foundValue
    }


}
