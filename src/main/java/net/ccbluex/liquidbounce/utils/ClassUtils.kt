/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.value.Value

object ClassUtils {

    private val cachedClasses = mutableMapOf<String, Boolean>()

    /**
     * Allows you to check for existing classes with the [className]
     */
    fun hasClass(className: String) =
        if (className in cachedClasses)
            cachedClasses[className]!!
        else try {
            Class.forName(className)
            cachedClasses[className] = true

            true
        } catch (e: ClassNotFoundException) {
            cachedClasses[className] = false

            false
        }

    fun findValues(
        element: Any?, configurables: List<Class<*>>, orderedValues: MutableSet<Value<*>>,
    ) {
        if (element == null) return

        val list = mutableSetOf<Value<*>>()

        try {
            if (element::class.java in configurables) {
                /**
                 * For variables that hold a list of Value<*>
                 *
                 * Example: val variable: List<Value<*>>
                 */
                if (element is Collection<*>) {
                    if (element.firstOrNull() is Value<*>) {
                        element.forEach { list += it as Value<*> }
                    }
                }

                element.javaClass.declaredFields.forEach {
                    it.isAccessible = true
                    val fieldValue = it[element] ?: return@forEach

                    if (fieldValue is Value<*>) {
                        list += fieldValue
                    } else {
                        findValues(fieldValue, configurables, list)
                    }
                }
            } else if (element is Value<*>) {
                list += element
            } else {
                /**
                 * For variables that hold a list of a possible class that contains Value<*>
                 *
                 * Example: val variable: List<ColorSettingsInt>
                 */
                if (element is Collection<*>) {
                    element.forEach {
                        findValues(it, configurables, list)
                    }
                }
            }

            orderedValues.addAll(list)
        } catch (e: Exception) {
            LOGGER.error(e)
        }
    }

    fun hasForge() = hasClass("net.minecraftforge.common.MinecraftForge")

}