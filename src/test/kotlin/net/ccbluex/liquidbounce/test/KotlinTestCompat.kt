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

package net.ccbluex.liquidbounce.test

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.function.Executable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun <T : Any> assertNotNull(actual: T?, message: String? = null): T {
    contract {
        returns() implies (actual != null)
    }

    if (message == null) {
        Assertions.assertNotNull(actual)
    } else {
        Assertions.assertNotNull(actual, message)
    }

    return actual!!
}

@OptIn(ExperimentalContracts::class)
inline fun <reified T : Any> assertIs(actual: Any?, message: String? = null): T {
    contract {
        returns() implies (actual is T)
    }

    return if (message == null) {
        Assertions.assertInstanceOf(T::class.java, actual)
    } else {
        Assertions.assertInstanceOf(T::class.java, actual, message)
    }
}

inline fun <reified T : Throwable> assertFailsWith(message: String? = null, block: Executable): T =
    if (message == null) {
        Assertions.assertThrows(T::class.java, block)
    } else {
        Assertions.assertThrows(T::class.java, block, message)
    }
