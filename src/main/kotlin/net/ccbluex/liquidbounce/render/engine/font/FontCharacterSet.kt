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
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import net.ccbluex.fastutil.mapToIntArray
import net.ccbluex.liquidbounce.render.FontFace
import java.awt.Font

internal object FontCharacterSet {

    private const val COMMON_HAN_RESOURCE = "/resources/liquidbounce/font/common_zh_cn.txt"
    private const val COMMON_HAN_COUNT = 3500

    val eagerCodepoints: IntArray = IntOpenHashSet(2048).apply {
        // Common western scripts and their precomposed/combining variants.
        addRange(0x0020..0x007E) // Basic Latin, printable characters
        addRange(0x00A0..0x024F) // Latin-1 Supplement and Latin Extended-A/B
        addRange(0x0300..0x036F) // Combining Diacritical Marks
        addRange(0x0370..0x03FF) // Greek and Coptic
        addRange(0x0400..0x052F) // Cyrillic and Cyrillic Supplement
        addRange(0x1E00..0x1EFF) // Latin Extended Additional
        addRange(0x2000..0x206F) // General Punctuation
        addRange(0x20A0..0x20CF) // Currency Symbols

        // Japanese syllabaries and punctuation. Han glyphs are warmed separately.
        addRange(0x3000..0x303F) // CJK Symbols and Punctuation
        addRange(0x3040..0x30FF) // Hiragana and Katakana
        addRange(0x31F0..0x31FF) // Katakana Phonetic Extensions
        addRange(0xFF65..0xFF9F) // Halfwidth Katakana

        add(0xFFFD) // Replacement Character
    }.toIntArray().apply(IntArray::sort)

    /**
     * First-level characters from the 2013 Table of General Standard Chinese Characters.
     *
     * Source data: https://github.com/jaywcjlove/table-of-general-standard-chinese-characters
     */
    val commonHanCodepoints: IntArray by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val text = requireNotNull(FontCharacterSet::class.java.getResourceAsStream(COMMON_HAN_RESOURCE)) {
            "Missing common Han character resource $COMMON_HAN_RESOURCE"
        }.bufferedReader(Charsets.UTF_8).use { it.readText().trim() }

        val codepoints = text.codePoints().toArray()
        require(codepoints.size == COMMON_HAN_COUNT) {
            "Expected $COMMON_HAN_COUNT common Han characters, got ${codepoints.size}"
        }
        require(IntOpenHashSet(codepoints).size == codepoints.size) {
            "Common Han character resource contains duplicates"
        }
        require(codepoints.all(Character::isIdeographic)) {
            "Common Han character resource contains non-ideographic codepoints"
        }

        codepoints
    }

    fun createEagerGlyphs(
        registeredFaces: Collection<FontFace>,
        primaryFace: FontFace,
        fallbackFaces: List<FontFace>,
    ): List<FontGlyph> {
        val result = ObjectOpenHashSet<FontGlyph>()

        resolveGlyphs(
            destination = result,
            requestedFace = primaryFace,
            fallbackFaces = fallbackFaces,
            styles = primaryFace.filledStyles.mapToIntArray { it.style },
            codepoints = eagerCodepoints,
        )

        // A renderer for a registered but inactive face can still be requested later.
        // Its remaining glyphs stay dynamic, while '?' must always be immediately available.
        registeredFaces.forEach { face ->
            face.filledStyles.forEach { font -> result.add(FontGlyph('?'.code, font)) }
        }

        return result.sortedWith(FONT_GLYPH_COMPARATOR)
    }

    fun createCommonHanGlyphs(
        primaryFace: FontFace,
        fallbackFaces: List<FontFace>,
    ): List<FontGlyph> = ObjectOpenHashSet<FontGlyph>().also { result ->
        resolveGlyphs(
            destination = result,
            requestedFace = primaryFace,
            fallbackFaces = fallbackFaces,
            styles = intArrayOf(Font.PLAIN),
            codepoints = commonHanCodepoints,
        )
    }.sortedWith(FONT_GLYPH_COMPARATOR)

    private fun resolveGlyphs(
        destination: MutableSet<FontGlyph>,
        requestedFace: FontFace,
        fallbackFaces: List<FontFace>,
        styles: IntArray,
        codepoints: IntArray,
    ) {
        styles.forEach { style ->
            codepoints.forEach { codepoint ->
                resolveStaticFont(requestedFace, fallbackFaces, style, codepoint)?.let { font ->
                    destination.add(FontGlyph(codepoint, font))
                }
            }
        }
    }

    private fun IntOpenHashSet.addRange(range: IntRange) {
        range.forEach(this::add)
    }

    private fun resolveStaticFont(
        requestedFace: FontFace,
        fallbackFaces: List<FontFace>,
        style: @FontStyle Int,
        codepoint: Int,
    ): FontId? = synchronized(GlyphPage.fontRasterizationLock) {
        val requestedFont = requestedFace.style(style) ?: requestedFace.plainStyle
        if (requestedFont.awtFont.canDisplay(codepoint)) {
            return@synchronized requestedFont
        }

        fallbackFaces.asSequence()
            .filter { it !== requestedFace }
            .map(FontFace::plainStyle)
            .firstOrNull { it.awtFont.canDisplay(codepoint) }
    }

    private val FONT_GLYPH_COMPARATOR = Comparator.comparing<FontGlyph, String> { it.font.awtFont.fontName }
        .thenComparingInt { it.font.style }
        .thenComparingInt { it.codepoint }
}
