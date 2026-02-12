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

package net.ccbluex.liquidbounce.utils.text

import net.ccbluex.liquidbounce.config.types.list.Tagged
import java.util.function.BiPredicate
import kotlin.text.equals

/**
 * A mode for matching strings.
 *
 * The first argument is the source string, and the second argument is the target string to match against.
 */
enum class StringMatchMode(override val tag: String) : Tagged, BiPredicate<String, String> {
    EQUALS("Equals") {
        override fun test(t: String, u: String): Boolean = t.equals(u, ignoreCase = false)
    },
    EQUALS_IGNORE_CASE("EqualsIgnoreCase") {
        override fun test(t: String, u: String): Boolean = t.equals(u, ignoreCase = true)
    },
    CONTAINS("Contains") {
        override fun test(t: String, u: String): Boolean = t.contains(u, ignoreCase = false)
    },
    CONTAINS_IGNORE_CASE("ContainsIgnoreCase") {
        override fun test(t: String, u: String): Boolean = t.contains(u, ignoreCase = true)
    },
    STARTS_WITH("StartsWith") {
        override fun test(t: String, u: String): Boolean = t.startsWith(u, ignoreCase = false)
    },
    STARTS_WITH_IGNORE_CASE("StartsWithIgnoreCase") {
        override fun test(t: String, u: String): Boolean = t.startsWith(u, ignoreCase = true)
    },
    ENDS_WITH("EndsWith") {
        override fun test(t: String, u: String): Boolean = t.endsWith(u, ignoreCase = false)
    },
    ENDS_WITH_IGNORE_CASE("EndsWithIgnoreCase") {
        override fun test(t: String, u: String): Boolean = t.endsWith(u, ignoreCase = true)
    }
}
