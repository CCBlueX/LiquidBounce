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
package net.ccbluex.liquidbounce.api.services.auth

import net.ccbluex.liquidbounce.api.core.HttpException
import net.ccbluex.liquidbounce.api.core.HttpMethod
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InvalidGrantTest {

    private fun refused(code: Int, content: String) =
        HttpException(HttpMethod.POST, "https://auth.liquidbounce.net/application/o/token/", code, content)

    @Test
    fun `only a refused grant counts`() {
        assertTrue(refused(400, """{"error": "invalid_grant"}""").isInvalidGrant)
        assertFalse(refused(400, """{"error": "invalid_request"}""").isInvalidGrant)
        assertFalse(refused(502, "<html>Bad Gateway</html>").isInvalidGrant)
    }

}
