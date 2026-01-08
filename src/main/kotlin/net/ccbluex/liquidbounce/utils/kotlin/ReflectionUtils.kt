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

package net.ccbluex.liquidbounce.utils.kotlin

import net.ccbluex.liquidbounce.utils.mappings.EnvironmentRemapper
import java.lang.reflect.AnnotatedType
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Check if the class is not the root class.
 *
 * The root class is the class [Record] or [Object].
 */
@OptIn(ExperimentalContracts::class)
fun Class<*>?.isNotRoot(): Boolean {
    contract {
        returns(true) implies (this@isNotRoot != null)
    }
    return !(this == null || this === Record::class.java || this.superclass == null)
}

fun Type.toFullString(): String =
    when (this) {
        is AnnotatedType -> this.type.toFullString()
        is ParameterizedType -> {
            val rawType = rawType.toFullString()
            val args = actualTypeArguments
            args.joinToString(", ", prefix = "$rawType<", postfix = ">") { it.toFullString() }
        }

        is WildcardType -> {
            when {
                lowerBounds.isNotEmpty() -> "? super ${lowerBounds.first().toFullString()}"
                upperBounds.isNotEmpty() && upperBounds.first() !== Object::class.java ->
                    upperBounds.joinToString(" & ", prefix = "? extends ") { it.toFullString() }

                else -> "?"
            }
        }

        is TypeVariable<*> -> when {
            bounds.size == 1 && bounds[0] === Object::class.java -> name
            else -> bounds.joinToString(" & ", prefix = "$name extends ") { it.toFullString() }
        }

        is GenericArrayType -> "${genericComponentType.toFullString()}[]"
        is Class<*> -> EnvironmentRemapper.remapClass(this).substringAfterLast('.')
        else -> this.toString()
    }
