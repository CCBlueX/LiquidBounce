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
package net.ccbluex.liquidbounce.script.bindings.api

import net.ccbluex.liquidbounce.utils.mappings.Remapper

object JsReflectionUtil {

    @JvmName("classByName")
    fun classByName(name: String): Class<*> = Class.forName(
        Remapper.remapClassName(name).replace('/', '.')
    )

    @JvmName("classByObject")
    fun classByObject(obj: Any): Class<*> = obj::class.java

    @JvmName("newInstance")
    fun newInstance(clazz: Class<*>, vararg args: Any?): Any? =
        clazz.getDeclaredConstructor(*args.map { it!!::class.java }.toTypedArray()).apply {
            isAccessible = true
        }.newInstance(*args)

    @JvmName("newInstanceByName")
    fun newInstanceByName(name: String, vararg args: Any?): Any? =
        Class.forName(Remapper.remapClassName(name).replace('/', '.'))
            .getDeclaredConstructor(*args.map { it!!::class.java }.toTypedArray()).apply {
                isAccessible = true
            }.newInstance(*args)

    @JvmName("newInstanceByObject")
    fun newInstanceByObject(obj: Any, vararg args: Any?): Any? =
        obj::class.java.getDeclaredConstructor(*args.map { it!!::class.java }.toTypedArray()).apply {
            isAccessible = true
        }.newInstance(*args)

    @JvmName("getField")
    fun getField(obj: Any, name: String): Any? = obj::class.java.getDeclaredField(
        Remapper.remapField(obj::class.java, name, true)
    ).apply {
        isAccessible = true
    }.get(obj)

    @JvmName("getStaticField")
    fun getStaticField(clazz: Class<*>, name: String): Any? = clazz.getDeclaredField(
        Remapper.remapField(clazz, name, true)
    ).apply {
        isAccessible = true
    }.get(null)

    @JvmName("invokeMethod")
    fun invokeMethod(obj: Any, name: String, vararg args: Any?): Any? =
        obj::class.java.getDeclaredMethod(
            Remapper.remapField(obj::class.java, name, true),
            *args.map { it!!::class.java }.toTypedArray()
        ).apply {
            isAccessible = true
        }.invoke(obj, *args)

    @JvmName("invokeStaticMethod")
    fun invokeStaticMethod(clazz: Class<*>, name: String, vararg args: Any?): Any? =
        clazz.getDeclaredMethod(
            Remapper.remapField(clazz, name, true),
            *args.map { it!!::class.java }.toTypedArray()
        ).apply {
            isAccessible = true
        }.invoke(null, *args)

}
