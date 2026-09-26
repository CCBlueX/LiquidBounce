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
package net.ccbluex.liquidbounce.features.command

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [lowercaseCommandPath]: only the leading path tokens (command and
 * subcommand names) are lower-cased while argument values are left untouched.
 */
class LowercaseCommandPathTest {

    @Test
    fun `lowercases only the leading tokens`() {
        assertEquals("cmdtest Hello", lowercaseCommandPath("CMDTEST Hello", 1))
        assertEquals("cmd sub Foo", lowercaseCommandPath("CMD SUB Foo", 2))
    }

    @Test
    fun `leaves argument values untouched`() {
        assertEquals("rename MyItem", lowercaseCommandPath("RENAME MyItem", 1))
        assertEquals("value set scaffold.mode G", lowercaseCommandPath("VALUE SET scaffold.mode G", 2))
    }

    @Test
    fun `handles quoted arguments`() {
        assertEquals("friend add \"Senk Ju\"", lowercaseCommandPath("FRIEND ADD \"Senk Ju\"", 2))
    }

    @Test
    fun `handles multiple and trailing spaces`() {
        assertEquals("cmd  double", lowercaseCommandPath("CMD  double", 1))
        assertEquals("cmd ", lowercaseCommandPath("CMD ", 1))
    }

    @Test
    fun `tokenCount of zero leaves input untouched`() {
        assertEquals("CMDTEST Hello", lowercaseCommandPath("CMDTEST Hello", 0))
    }

    @Test
    fun `tokenCount larger than token count lowercases everything`() {
        assertEquals("cmd", lowercaseCommandPath("CMD", 5))
    }

    @Test
    fun `collapses repeated spaces outside quotes`() {
        assertEquals("cmd a b", normalizeCommandSpaces("cmd  a   b"))
        assertEquals("cmd a b", normalizeCommandSpaces("cmd a   b"))
    }

    @Test
    fun `preserves spaces inside quotes`() {
        assertEquals("cmd \"a  b\" c", normalizeCommandSpaces("cmd  \"a  b\"  c"))
    }

    @Test
    fun `preserves escaped quotes`() {
        assertEquals("cmd \"a \\\" b\"", normalizeCommandSpaces("cmd  \"a \\\" b\""))
    }

    @Test
    fun `trims leading and trailing whitespace via caller`() {
        assertEquals("cmd a", normalizeCommandSpaces("  cmd a  ".trim()))
    }

}
