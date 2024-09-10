package net.ccbluex.liquidbounce.render.engine.font

import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.GameRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.render.Fonts
import net.ccbluex.liquidbounce.render.Fonts.DEFAULT_FONT_SIZE
import net.ccbluex.liquidbounce.render.engine.font.dynamic.DynamicFontCacheManager
import net.ccbluex.liquidbounce.render.engine.font.dynamic.DynamicGlyphPage
import net.minecraft.util.Identifier
import java.awt.Dimension
import java.awt.Font
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.random.Random

private val BASIC_CHARS = '\u0000'..'\u0200'

/**
 * Hardcoded font. This should be fixed by @1zuna.
 */
private val ALTERNATIVE_FONTS = listOf(
    "Microsoft YaHei"
)

class FontGlyphPageManager(
    baseFonts: List<Fonts.LoadedFont>
): Listenable {
    var staticPage: StaticGlyphPage
    private val dynamicPage: DynamicGlyphPage
    private val dynamicFontManager: DynamicFontCacheManager

    private val availableFonts: Map<Fonts.LoadedFont, FontGlyphRegistry>

    private val dynamicallyLoadedGlyphs = HashMap<Pair<Int, Char>, GlyphDescriptor>()

    private val rng = Random(1337)

    init {
        this.dynamicPage = DynamicGlyphPage(Dimension(1024, 1024), ceil(baseFonts[0].styles[0]!!.height * 2.0F).toInt())
        this.staticPage = StaticGlyphPage.create(baseFonts.flatMap { loadedFont ->
            loadedFont.styles.filterNotNull().flatMap { font -> BASIC_CHARS.map { ch -> FontGlyph(ch, font) } }
        })

        this.dynamicFontManager = DynamicFontCacheManager(this.dynamicPage, baseFonts + ALTERNATIVE_FONTS.map { loadShit(it) })

        this.dynamicFontManager.startThread()

        this.availableFonts = collectShit(baseFonts, this.staticPage.glyphs)
    }

    val renderHandler = handler<GameRenderEvent> {
        this.dynamicFontManager.update().forEach { update ->
            val key = update.style to update.descriptor.renderInfo.char

            if (!update.removed) {
                dynamicallyLoadedGlyphs[key] = update.descriptor
            } else {
                dynamicallyLoadedGlyphs.remove(key)
            }
        }
    }

    private fun loadShit(fontName: String): Fonts.LoadedFont {
        return Fonts.LoadedFont(DEFAULT_FONT_SIZE.toFloat(), (0..3).map { style ->
            val font = Font(fontName, style, DEFAULT_FONT_SIZE)

            val metrics =
                BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics().apply {
                    setFont(font)
                }.fontMetrics

            Fonts.FontId(style, font, metrics.height.toFloat(), metrics.ascent.toFloat())
        }.toTypedArray())
    }

    private fun collectShit(
        baseFonts: List<Fonts.LoadedFont>,
        glyphs: List<Pair<Fonts.FontId, GlyphRenderInfo>>
    ): Map<Fonts.LoadedFont, FontGlyphRegistry> {
        val fontMap =
            baseFonts.associateWith { Array(Fonts.FONT_FORMATS.size) { ConcurrentHashMap<Char, GlyphDescriptor>() } }

        baseFonts.forEach { loadedFont ->
            loadedFont.styles.filterNotNull().forEach { fontId ->
                glyphs
                    .filter { it.first == fontId }
                    .forEach { (font, glyphRenderInfo) ->
                        fontMap[loadedFont]!![font.style][glyphRenderInfo.char] =
                            GlyphDescriptor(staticPage, glyphRenderInfo)
                    }
            }
        }

        return fontMap.entries.associate {
            it.key to FontGlyphRegistry(it.value, it.value[0]['?']!!)
        }
    }

    private fun getFont(font: Fonts.LoadedFont): FontGlyphRegistry {
        return availableFonts[font] ?: error("Font $font is not registered")
    }

    fun requestGlyph(font: Fonts.LoadedFont, style: Int, ch: Char): GlyphDescriptor? {
        val glyph = getFont(font).glyphs[style][ch]

        if (glyph == null) {
            val altGlyph = this.dynamicallyLoadedGlyphs[style to ch]

            if (altGlyph == null) {
                this.dynamicFontManager.requestGlyph(ch, style)
            } else {
                return altGlyph
            }
        }

        return glyph
    }

    fun getFallbackGlyph(font: Fonts.LoadedFont): GlyphDescriptor {
        return getFont(font).fallbackGlyph
    }

    fun unload() {
        this.dynamicPage.texture.close()
        this.staticPage.texture.close()
    }

    private class FontGlyphRegistry(
        val glyphs: Array<ConcurrentHashMap<Char, GlyphDescriptor>>,
        val fallbackGlyph: GlyphDescriptor
    )
}

class GlyphDescriptor(val page: GlyphPage, val renderInfo: GlyphRenderInfo)
