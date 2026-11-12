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
package net.ccbluex.liquidbounce.api.thirdparty.translator

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TranslationResultTest {

    private fun success(origin: String, translation: String, from: String, to: String) =
        TranslationResult.Success(origin, translation, TranslateLanguage.of(from), TranslateLanguage.of(to))

    @Test
    fun `translation into another language is valid`() {
        assertTrue(success("Hallo Welt", "Hello world", "de", "en").isValid)
    }

    @Test
    fun `unchanged text is not valid`() {
        assertFalse(success("Hello world", "Hello world", "de", "en").isValid)
    }

    @Test
    fun `same source and target language is not valid`() {
        assertFalse(success("hello world", "Hello world.", "en", "en").isValid)
    }

    @Test
    fun `language codes are compared ignoring case`() {
        assertFalse(success("hello world", "Hello world.", "zh-CN", "zh-cn").isValid)
    }

}
