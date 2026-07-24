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
package net.ccbluex.liquidbounce.integration.interop

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

class ApplicationExtensionsTest {

    @Test
    fun `allows local development origins on arbitrary ports`() {
        assertTrue(isLocalOrigin("http://localhost:5173"))
        assertTrue(isLocalOrigin("https://LOCALHOST:4173"))
        assertTrue(isLocalOrigin("http://127.0.0.1:5173"))
    }

    @Test
    fun `rejects non-local and malformed origins`() {
        assertFalse(isLocalOrigin("http://localhost.example.com:5173"))
        assertFalse(isLocalOrigin("http://127.0.0.2:5173"))
        assertFalse(isLocalOrigin("file://localhost/theme"))
        assertFalse(isLocalOrigin("not an origin"))
    }

}
