package net.ccbluex.liquidbounce.render.engine.font

import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.engine.font.dynamic.DynamicFontCacheManager
import net.ccbluex.liquidbounce.render.engine.font.dynamic.DynamicGlyphPage
import java.awt.Dimension
import kotlin.math.ceil

private val BASIC_CHARS = '\u0000'..'\u0200'

class FontGlyphPageManager(
    baseFonts: Set<FontManager.FontFace>,
    additionalFonts: Set<FontManager.FontFace> = emptySet()
): EventListener {

    private val staticPage: List<StaticGlyphPage> = StaticGlyphPage.createGlyphPages(baseFonts.flatMap { loadedFont ->
        loadedFont.styles.filterNotNull().flatMap { font -> BASIC_CHARS.map { ch -> FontGlyph(ch, font) } }
    })
    private val dynamicPage: DynamicGlyphPage = DynamicGlyphPage(
        Dimension(1024, 1024),
        ceil(baseFonts.elementAt(0).styles[0]!!.height * 2.0F).toInt()
    )
    private val dynamicFontManager: DynamicFontCacheManager = DynamicFontCacheManager(
        this.dynamicPage,
        baseFonts + additionalFonts
    )

    private val availableFonts: Map<FontManager.FontFace, FontGlyphRegistry>
    private val dynamicallyLoadedGlyphs = Long2ObjectOpenHashMap<GlyphDescriptor>()

    init {
        this.dynamicFontManager.startThread()

        this.availableFonts = createGlyphRegistries(baseFonts, this.staticPage)
    }

    private fun packIntCharKey(intValue: Int, charValue: Char): Long {
        return (intValue.toLong() shl 32) or charValue.code.toLong()
    }

    @Suppress("unused")
    private val renderHandler = handler<GameRenderEvent> {
        this.dynamicFontManager.update().forEach { update ->
            val key = packIntCharKey(update.style, update.descriptor.renderInfo.char)

            if (!update.removed) {
                dynamicallyLoadedGlyphs.put(key, update.descriptor)
            } else {
                dynamicallyLoadedGlyphs.remove(key)
            }
        }
    }

    private fun createGlyphRegistries(
        baseFonts: Set<FontManager.FontFace>,
        glyphPages: List<StaticGlyphPage>
    ): Map<FontManager.FontFace, FontGlyphRegistry> = baseFonts.associateWith { loadedFont ->
        val array = Array(4) { Char2ObjectOpenHashMap<GlyphDescriptor>(512) }

        loadedFont.styles.forEach { fontId ->
            if (fontId == null) {
                return@forEach
            }

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

    private fun getFont(font: FontManager.FontFace): FontGlyphRegistry {
        return availableFonts[font] ?: error("Font $font is not registered")
    }

    fun requestGlyph(font: FontManager.FontFace, style: Int, ch: Char): GlyphDescriptor? {
        val glyph = getFont(font).glyphs[style][ch]

        if (glyph == null) {
            val altGlyph = this.dynamicallyLoadedGlyphs[packIntCharKey(style, ch)]

            if (altGlyph == null) {
                this.dynamicFontManager.requestGlyph(ch, style)
            } else {
                return altGlyph
            }
        }

        return glyph
    }

    fun getFallbackGlyph(font: FontManager.FontFace): GlyphDescriptor {
        return getFont(font).fallbackGlyph
    }

    fun unload() {
        this.dynamicPage.texture.close()
        this.staticPage.forEach { it.texture.close() }
    }

    private class FontGlyphRegistry(
        val glyphs: Array<Char2ObjectOpenHashMap<GlyphDescriptor>>,
        val fallbackGlyph: GlyphDescriptor
    )

}

class GlyphDescriptor(val page: GlyphPage, val renderInfo: GlyphRenderInfo)
