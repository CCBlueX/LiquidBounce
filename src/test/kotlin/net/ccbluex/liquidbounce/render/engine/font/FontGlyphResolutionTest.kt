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

import kotlinx.coroutines.test.runTest
import net.ccbluex.liquidbounce.render.FontFace
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.awt.Font

class FontGlyphResolutionTest {

    @Test
    fun testMissingStyleFallsBackToPlainStyleOfRequestedFace() = runTest {
        val face = FontFace("Test", 43f)
        face.fillStyle(Font(Font.SANS_SERIF, Font.PLAIN, 43), Font.PLAIN)

        val resolved = resolveFont(face, emptyList(), Font.BOLD, 'A'.code)

        assertSame(face.plainStyle, resolved)
    }

    @Test
    fun testGlyphIdentifierIncludesActualFont() {
        val firstFont = FontId(Font.PLAIN, Font(Font.SANS_SERIF, Font.PLAIN, 43), 51f, 40f)
        val secondFont = FontId(Font.PLAIN, Font(Font.SERIF, Font.PLAIN, 43), 51f, 40f)
        val codepoint = 0x20000

        val first = GlyphIdentifier(codepoint, firstFont)
        val second = GlyphIdentifier(codepoint, secondFont)

        assertEquals(codepoint, first.codepoint)
        assertNotEquals(first, second)
    }
}
