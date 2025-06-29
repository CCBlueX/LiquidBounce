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

import net.ccbluex.liquidbounce.utils.kotlin.mapArray
import net.ccbluex.liquidbounce.utils.mappings.EnvironmentRemapper
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

@Suppress("SpreadOperator", "unused")
class ScriptReflectionUtil {

    // we assume that a single script will only
    // call a finite (small) number of methods not likely shared by other scripts
    // therefore we don't put a size limit on the cache
    private val methodCache = ConcurrentHashMap<Triple<Class<*>, String, List<Class<*>>>, Method>()
    private val fieldCache = ConcurrentHashMap<Pair<Class<*>, String>, Field>()

    /**
     * Invalidate the cache.
     * Allows user to invalidate the cache for whatever reason, like dynamic class loading.
     */
    fun invalidateCache() {
        methodCache.clear()
        fieldCache.clear()
    }


    @JvmName("classByName")
    fun classByName(name: String): Class<*> = Class.forName(
        EnvironmentRemapper.remapClassName(name).replace('/', '.')
    )

    @JvmName("newInstance")
    fun newInstance(clazz: Class<*>, vararg args: Any?): Any? =
        clazz.getDeclaredConstructor(*args.mapArray { it!!::class.java }).apply {
            isAccessible = true
        }.newInstance(*args)

    @JvmName("newInstanceByName")
    fun newInstanceByName(name: String, vararg args: Any?): Any? =
        Class.forName(EnvironmentRemapper.remapClassName(name).replace('/', '.'))
            .getDeclaredConstructor(*args.mapArray { it!!::class.java }).apply {
                isAccessible = true
            }.newInstance(*args)

    @JvmName("newInstanceByObject")
    fun newInstanceByObject(obj: Any, vararg args: Any?): Any? =
        obj::class.java.getDeclaredConstructor(*args.mapArray { it!!::class.java }).apply {
            isAccessible = true
        }.newInstance(*args)

    /**
     * Get the value of a declared field from an object
     *
     * @return - the value stored in field
     * @param obj - object from which to extract
     * @param name - method name in yarn mapping
     */

    @JvmName("getField")
    fun getField(obj: Any, name: String): Any? {
        return fieldCache.computeIfAbsent(Pair(obj::class.java, name)) {
            obj::class.java.fields
                .find { field ->
                    name == EnvironmentRemapper.remapField(obj::class.java, field.name)
                }?.apply {
                    isAccessible = true
                } ?: throw NoSuchFieldException("Field '$name' not found in ${obj::class.java.name}")
        }.get(obj)
    }

    /**
     * Get the value of a declared field from class
     *
     * @return - the value stored in field
     * @param clazz - class for which to search
     * @param name - method name in yarn mapping
     */

    @JvmName("getDeclaredField")
    fun getDeclaredField(clazz: Class<*>, name: String): Any? {
        return fieldCache.computeIfAbsent(Pair(clazz, name)) {
            clazz.declaredFields
                .find { field ->
                    name == EnvironmentRemapper.remapField(clazz, field.name)
                }?.apply {
                    isAccessible = true
                } ?: throw NoSuchFieldException("Field '$name' not found in ${clazz.name}")
        }.get(null)
    }

    private val primitiveTypeMap = mapOf(
        java.lang.Integer::class.java to Int::class.javaPrimitiveType,
        java.lang.Long::class.java to Long::class.javaPrimitiveType,
        java.lang.Double::class.java to Double::class.javaPrimitiveType,
        java.lang.Float::class.java to Float::class.javaPrimitiveType,
        java.lang.Boolean::class.java to Boolean::class.javaPrimitiveType,
        java.lang.Byte::class.java to Byte::class.javaPrimitiveType,
        java.lang.Short::class.java to Short::class.javaPrimitiveType,
        java.lang.Character::class.java to Char::class.javaPrimitiveType
        // not removing the redundant qualifier name for consistency
    )


    /**
     * Invoke method(**PUBLIC ONLY**) based on method name on an object,
     * match overloaded methods based on number and type of arguments,
     * does **NOT** handle null arguments
     *
     * @return - result of invoking method
     * @param obj - object to be invoked
     * @param name - method name in yarn mapping
     * @param args - arguments of method
     *
     * @exception - throw IllegalArgumentException when an argument is null
     *
     * Example when used in js:
     * ```javascript
     *   mod.on("overlayRender", (event) => {
     *     if (!mc.player || !mc.world)
     *       return;
     *
     *     ReflectionUtil.invokeMethod(event.context, "fill", 100, 100, 200, 200, -1);
     *   })
     * ```
     */

    @JvmName("invokeMethod")
    fun invokeMethod(obj: Any, name: String, vararg args: Any?): Any? {
        return findMethodInternal(obj::class.java, name, args) { it.methods }.invoke(obj, *args)
    }


    /**
     * Invoke method(**ONLY OF THIS CLASS WITHOUT INHERITED METHODS**) with no access restrictions based on method name,
     * match overloaded methods based on number and type of arguments
     * does **NOT** handle null arguments
     *
     * @return - result of invoking method
     * @param clazz - class with declared methods
     * @param name - method name in yarn mapping
     * @param args - arguments of method
     *
     */

    @JvmName("invokeDeclaredMethod")
    fun invokeDeclaredMethod(clazz: Class<*>, name: String, vararg args: Any?): Any? {
        return findMethodInternal(clazz, name, args) { it.declaredMethods }.invoke(null, args)
    }

    private fun findMethodInternal(
        clazz: Class<*>,
        name: String,
        args: Array<out Any?>,
        methodProvider: (Class<*>) -> Array<Method>
    ): Method {
        if (args.any { it == null }) {
            throw IllegalArgumentException(
                "Null argument is not support by this api, please use " +
                    "reflection api with EnvironmentRemapper manually"
            )
        }

        // Pre-compute argument types
        val argTypes = args.map { arg ->
            primitiveTypeMap[arg!!.javaClass] ?: arg.javaClass
        }

        val cacheKey = Triple(clazz, name, argTypes)

        // Try to get from cache first
        return methodCache.computeIfAbsent(cacheKey) {
            val potentialMatches = methodProvider(clazz).filter { method ->
                method.parameterTypes.size == args.size &&
                    method.parameterTypes.mapArray { arg -> primitiveTypeMap[arg] ?: arg }
                        .zip(argTypes).all { (paramType, argType) ->
                            paramType == argType || (!paramType.isPrimitive && paramType.isAssignableFrom(argType))
                        }
            }

            // Find and return the matching method
            potentialMatches.find { method ->
                EnvironmentRemapper.remapMethod(clazz, method.name) == name
            }?.apply {
                isAccessible = true
            } ?: throw NoSuchMethodException("Could not find method $name with matching argument types")
        }
    }

}
