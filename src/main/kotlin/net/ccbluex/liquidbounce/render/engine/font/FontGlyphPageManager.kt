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

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.render.FontFace
import net.ccbluex.liquidbounce.render.engine.font.dynamic.DynamicFontCacheManager
import net.ccbluex.liquidbounce.render.engine.font.dynamic.DynamicGlyphPage
import kotlin.math.ceil

private val BASIC_CODEPOINTS = 0x0000..0x0200

class FontGlyphPageManager(
    baseFonts: Collection<FontFace>,
    private val fallbackFonts: List<FontFace> = emptyList()
) : EventListener, AutoCloseable {

    private val staticPage: List<StaticGlyphPage> = StaticGlyphPage.createGlyphPages(baseFonts.flatMap { loadedFont ->
        loadedFont.filledStyles.flatMap { font -> BASIC_CODEPOINTS.map { codepoint -> FontGlyph(codepoint, font) } }
    })
    private val dynamicPage: DynamicGlyphPage = DynamicGlyphPage(
        fontHeight = ceil(baseFonts.first().plainStyle.height * 2.0F).toInt()
    )
    private val dynamicFontManager: DynamicFontCacheManager = DynamicFontCacheManager(
        this.dynamicPage
    )

    private val registeredFonts = baseFonts.toSet()
    private val staticGlyphs = createStaticGlyphRegistry(this.staticPage)
    private val fallbackGlyphs = baseFonts.associateWith { font ->
        requireNotNull(staticGlyphs[font.plainStyle]?.get('?'.code)) {
            "Font $font has no fallback glyph"
        }
    }
    private val dynamicallyLoadedGlyphs = Object2ObjectOpenHashMap<GlyphIdentifier, GlyphDescriptor>()
    private var closed = false

    init {
        this.dynamicFontManager.startThread()
    }

    @Suppress("unused")
    private val renderHandler = handler<GameRenderEvent> {
        this.dynamicFontManager.update().forEach { update ->
            val key = GlyphIdentifier(update.descriptor.renderInfo.codepoint, update.font)

            if (!update.removed) {
                dynamicallyLoadedGlyphs.put(key, update.descriptor)
            } else {
                dynamicallyLoadedGlyphs.remove(key)
            }
        }
    }

    private fun createStaticGlyphRegistry(
        glyphPages: List<StaticGlyphPage>
    ): Map<FontId, Int2ObjectOpenHashMap<GlyphDescriptor>> {
        val result = Object2ObjectOpenHashMap<FontId, Int2ObjectOpenHashMap<GlyphDescriptor>>()
        glyphPages.forEach { glyphPage ->
            for ((font, glyphRenderInfo) in glyphPage.glyphs) {
                result.computeIfAbsent(font) { Int2ObjectOpenHashMap(512) }
                    .put(glyphRenderInfo.codepoint, GlyphDescriptor(glyphPage, glyphRenderInfo))
            }
        }
        return result
    }

    fun requestGlyph(font: FontFace, style: @FontStyle Int, codepoint: Int): GlyphDescriptor? {
        check(font in registeredFonts) { "Font $font is not registered" }

        val requestedFont = font.style(style) ?: font.plainStyle
        staticGlyphs[requestedFont]?.get(codepoint)?.let { return it }

        val resolvedFont = resolveFont(font, fallbackFonts, style, codepoint) ?: return null
        staticGlyphs[resolvedFont]?.get(codepoint)?.let { return it }

        val fontGlyph = FontGlyph(codepoint, resolvedFont)
        val glyphIdentifier = GlyphIdentifier(fontGlyph)
        this.dynamicFontManager.requestGlyph(fontGlyph)
        return this.dynamicallyLoadedGlyphs[glyphIdentifier]
    }

    fun getFallbackGlyph(font: FontFace): GlyphDescriptor {
        return fallbackGlyphs[font] ?: error("Font $font is not registered")
    }

    override fun close() {
        if (closed) {
            return
        }
        closed = true
        unregister()
        this.dynamicFontManager.close()
        this.dynamicPage.texture.close()
        this.staticPage.forEach { it.texture.close() }
        this.dynamicallyLoadedGlyphs.clear()
    }

}

class GlyphDescriptor(val page: GlyphPage, val renderInfo: GlyphRenderInfo)

internal fun resolveFont(
    requestedFace: FontFace,
    fallbackFaces: List<FontFace>,
    style: @FontStyle Int,
    codepoint: Int,
): FontId? {
    val requestedFont = requestedFace.style(style) ?: requestedFace.plainStyle
    if (requestedFont.awtFont.canDisplay(codepoint)) {
        return requestedFont
    }

    return fallbackFaces.asSequence()
        .filter { it !== requestedFace }
        .map { it.style(style) ?: it.plainStyle }
        .firstOrNull { it.awtFont.canDisplay(codepoint) }
}
