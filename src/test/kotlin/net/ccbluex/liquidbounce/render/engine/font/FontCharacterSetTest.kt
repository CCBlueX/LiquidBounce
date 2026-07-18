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

package net.ccbluex.liquidbounce.render.engine.font

import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import kotlinx.coroutines.test.runTest
import net.ccbluex.liquidbounce.render.FontFace
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.Font

class FontCharacterSetTest {

    @Test
    fun testEagerCharacterSetCoverage() {
        val codepoints = IntOpenHashSet(FontCharacterSet.eagerCodepoints)

        assertEquals(1835, codepoints.size)
        intArrayOf(
            'A'.code,
            'é'.code,
            'ạ'.code,
            0x0301, // Combining acute accent
            'Ω'.code,
            'Ж'.code,
            'Ґ'.code,
            'あ'.code,
            'ア'.code,
            'ㇰ'.code,
            'ｱ'.code,
        ).forEach { assertTrue(codepoints.contains(it), "Missing U+${it.toString(16).uppercase()}") }

        assertFalse(codepoints.contains('你'.code))
        assertFalse(codepoints.contains('한'.code))
        assertFalse(codepoints.contains(0x1F600))
    }

    @Test
    fun testCommonHanResourceIntegrity() {
        val codepoints = FontCharacterSet.commonHanCodepoints

        assertEquals(3500, codepoints.size)
        assertEquals(codepoints.size, IntOpenHashSet(codepoints).size)
        assertEquals('一'.code, codepoints.first())
        assertEquals('矗'.code, codepoints.last())
        assertTrue(codepoints.all(Character::isIdeographic))
    }

    @Test
    fun testOnlyPrimaryFaceIsEagerlyPopulated() = runTest {
        val primary = FontFace("Primary", 43f).apply {
            fillStyle(Font(Font.SERIF, Font.PLAIN, 43), Font.PLAIN)
        }
        val inactive = FontFace("Inactive", 43f).apply {
            fillStyle(Font(Font.MONOSPACED, Font.PLAIN, 43), Font.PLAIN)
        }

        val glyphs = FontCharacterSet.createEagerGlyphs(
            registeredFaces = listOf(primary, inactive),
            primaryFace = primary,
            fallbackFaces = emptyList(),
        )

        assertSame(primary.plainStyle, glyphs.single { it.codepoint == 'A'.code }.font)
        assertTrue(glyphs.any { it.codepoint == '?'.code && it.font === inactive.plainStyle })
        assertFalse(glyphs.any { it.codepoint == 'A'.code && it.font === inactive.plainStyle })
    }
}
