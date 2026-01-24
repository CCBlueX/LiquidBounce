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

import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.render.FontFace
import net.ccbluex.liquidbounce.render.engine.font.dynamic.DynamicFontCacheManager
import net.ccbluex.liquidbounce.render.engine.font.dynamic.DynamicGlyphPage
import kotlin.math.ceil

private val BASIC_CHARS = '\u0000'..'\u0200'

class FontGlyphPageManager(
    baseFonts: Collection<FontFace>,
    additionalFonts: Collection<FontFace> = emptySet()
): EventListener {

    private val staticPage: List<StaticGlyphPage> = StaticGlyphPage.createGlyphPages(baseFonts.flatMap { loadedFont ->
        loadedFont.filledStyles.flatMap { font -> BASIC_CHARS.map { ch -> FontGlyph(ch, font) } }
    })
    private val dynamicPage: DynamicGlyphPage = DynamicGlyphPage(
        fontHeight = ceil(baseFonts.first().plainStyle.height * 2.0F).toInt()
    )
    private val dynamicFontManager: DynamicFontCacheManager = DynamicFontCacheManager(
        this.dynamicPage,
        ObjectOpenHashSet<FontFace>(baseFonts.size + staticPage.size).apply {
            addAll(baseFonts)
            addAll(additionalFonts)
        }
    )

    private val availableFonts: Map<FontFace, FontGlyphRegistry>
    private val dynamicallyLoadedGlyphs = Long2ObjectOpenHashMap<GlyphDescriptor>()

    init {
        this.dynamicFontManager.startThread()

        this.availableFonts = createGlyphRegistries(baseFonts, this.staticPage)
    }

    @Suppress("unused")
    private val renderHandler = handler<GameRenderEvent> {
        this.dynamicFontManager.update().forEach { update ->
            val key = GlyphIdentifier.asLong(update.descriptor.renderInfo.char, update.style)

            if (!update.removed) {
                dynamicallyLoadedGlyphs.put(key, update.descriptor)
            } else {
                dynamicallyLoadedGlyphs.remove(key)
            }
        }
    }

    private fun createGlyphRegistries(
        baseFonts: Collection<FontFace>,
        glyphPages: List<StaticGlyphPage>
    ): Map<FontFace, FontGlyphRegistry> = baseFonts.associateWith { loadedFont ->
        val array = Array(4) { Char2ObjectOpenHashMap<GlyphDescriptor>(512) }

        loadedFont.filledStyles.forEach { fontId ->
            glyphPages.forEach { glyphPage ->
                for ((font, glyphRenderInfo) in glyphPage.glyphs) {
                    if (font != fontId) {
                        continue
                    }

                    array[font.style].put(glyphRenderInfo.char, GlyphDescriptor(glyphPage, glyphRenderInfo))
                }
            }
        }

        FontGlyphRegistry(array, array[0]['?']!!)
    }

    private fun getFont(font: FontFace): FontGlyphRegistry {
        return availableFonts[font] ?: error("Font $font is not registered")
    }

    fun requestGlyph(font: FontFace, style: Int, ch: Char): GlyphDescriptor? {
        val glyph = getFont(font).glyphs[style][ch]

        if (glyph == null) {
            val altGlyph = this.dynamicallyLoadedGlyphs[GlyphIdentifier.asLong(ch, style)]

            if (altGlyph == null) {
                this.dynamicFontManager.requestGlyph(ch, style)
            } else {
                return altGlyph
            }
        }

        return glyph
    }

    fun getFallbackGlyph(font: FontFace): GlyphDescriptor {
        return getFont(font).fallbackGlyph
    }

    fun unload() {
        this.dynamicPage.texture.close()
        this.staticPage.forEach { it.texture.close() }
    }

    private class FontGlyphRegistry(
        @JvmField val glyphs: Array<Char2ObjectOpenHashMap<GlyphDescriptor>>,
        @JvmField val fallbackGlyph: GlyphDescriptor,
    )

}

class GlyphDescriptor(val page: GlyphPage, val renderInfo: GlyphRenderInfo)
