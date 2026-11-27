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
package net.ccbluex.liquidbounce.lang

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PublishedLanguagesTest {

    @Test
    fun `published file names are accepted`() {
        for (name in listOf("de_de.json", "zh_cn.json", "en_pt.json", "uk_ua.json")) {
            assertTrue(isPlainFileName(name), name)
        }
    }

    @Test
    fun `file names reaching outside the cache folder are rejected`() {
        for (name in listOf("", ".", "..", "../de.json", "lang/de.json", "..\\de.json", "C:de.json", "/etc/passwd")) {
            assertFalse(isPlainFileName(name), name)
        }
    }

}
