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
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.Test
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
    fun testRequestedFaceKeepsItsGlyphWhenAFallbackCoversItToo() = runTest {
        val face = FontFace("Test", 43f)
        face.fillStyle(CoverageFont(Font.PLAIN, covered = true), Font.PLAIN)
        val fallback = FontFace("Fallback", 43f)
        fallback.fillStyle(CoverageFont(Font.PLAIN, covered = true), Font.PLAIN)

        val resolved = resolveFont(face, listOf(fallback), Font.PLAIN, 'A'.code)

        assertSame(face.plainStyle, resolved)
    }

    @Test
    fun testFallbackFaceIgnoresTheRequestedStyle() = runTest {
        val face = FontFace("Test", 43f)
        face.fillStyle(CoverageFont(Font.PLAIN, covered = false), Font.PLAIN)
        val fallback = FontFace("Fallback", 43f)
        fallback.fillStyle(CoverageFont(Font.PLAIN, covered = true), Font.PLAIN)
        fallback.fillStyle(CoverageFont(Font.BOLD, covered = true), Font.BOLD)

        val resolved = resolveFont(face, listOf(fallback), Font.BOLD, 'A'.code)

        assertNotNull(fallback.style(Font.BOLD))
        assertSame(fallback.plainStyle, resolved)
    }

    @Test
    fun testFontGlyphIncludesActualFont() {
        val firstFont = createFontId(Font.SANS_SERIF)
        val secondFont = createFontId(Font.SERIF)
        val codepoint = 0x20000

        val first = FontGlyph(codepoint, firstFont)
        val second = FontGlyph(codepoint, secondFont)

        assertEquals(codepoint, first.codepoint)
        assertNotEquals(first, second)
    }

    @Test
    fun testFontIdIdentityIsItsFont() {
        val base = createFontId(Font.SANS_SERIF)
        val sameFont = createFontId(Font.SANS_SERIF)

        assertEquals(base, sameFont)
        assertEquals(base.hashCode(), sameFont.hashCode())

        assertNotEquals(base, createFontId(Font.SERIF))
    }

    @Test
    fun testDerivedStylesAndDecorationMetrics() = runTest {
        val face = FontFace("Test", 43f)
        face.fillDerivedStyles(Font(Font.SANS_SERIF, Font.PLAIN, 43))

        assertTrue(face.style(Font.BOLD)!!.awtFont.isBold)
        assertTrue(face.style(Font.ITALIC)!!.awtFont.isItalic)
        assertNotNull(face.style(Font.BOLD or Font.ITALIC))
        assertTrue(face.plainMetrics.underlineOffset > 0f)
        assertTrue(face.plainMetrics.underlineThickness > 0f)
        assertTrue(face.plainMetrics.strikethroughOffset < 0f)
        assertTrue(face.plainMetrics.strikethroughThickness > 0f)
    }

    private fun createFontId(name: String) = FontId(Font(name, Font.PLAIN, 43))
}

/**
 * A font with a fixed glyph coverage, so the fallback path can be tested without depending on which
 * fonts the machine happens to have installed.
 */
private class CoverageFont(style: Int, private val covered: Boolean) : Font("Dialog", style, 43) {
    override fun canDisplay(codePoint: Int): Boolean = covered
}
