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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.api.core.ioScope
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.render.FontFace
import net.ccbluex.liquidbounce.render.engine.font.dynamic.DynamicFontCacheManager
import net.ccbluex.liquidbounce.render.engine.font.dynamic.DynamicGlyphPage
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.kotlin.Minecraft
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.ceil

class FontGlyphPageManager(
    registeredFaces: Collection<FontFace>,
    private val primaryFace: FontFace,
    private val fallbackFonts: List<FontFace> = emptyList()
) : EventListener, AutoCloseable {

    private val registeredFonts = registeredFaces.toSet()
    private val staticPage = ArrayList<StaticGlyphPage>()
    private val staticGlyphs = Object2ObjectOpenHashMap<FontId, Int2ObjectOpenHashMap<GlyphDescriptor>>()
    private val dynamicPage: DynamicGlyphPage = DynamicGlyphPage(
        fontHeight = ceil(primaryFace.plainStyle.height * 2.0F).toInt()
    )
    private val dynamicFontManager: DynamicFontCacheManager = DynamicFontCacheManager(
        this.dynamicPage
    )

    private val fallbackGlyphs: Map<FontFace, GlyphDescriptor>
    private val dynamicallyLoadedGlyphs = Object2ObjectOpenHashMap<GlyphIdentifier, GlyphDescriptor>()
    private val closed = AtomicBoolean(false)
    private val staticPagesLock = Any()
    private var commonHanWarmupJob: Job? = null

    init {
        require(primaryFace in registeredFonts) { "Primary font $primaryFace is not registered" }

        val eagerGlyphs = FontCharacterSet.createEagerGlyphs(registeredFonts, primaryFace, fallbackFonts)
        registerStaticPages(StaticGlyphPage.createGlyphPages(eagerGlyphs))

        val primaryFallback = requireNotNull(staticGlyphs[primaryFace.plainStyle]?.get('?'.code)) {
            "Primary font $primaryFace has no fallback glyph"
        }
        fallbackGlyphs = registeredFonts.associateWith { font ->
            staticGlyphs[font.plainStyle]?.get('?'.code) ?: primaryFallback
        }

        this.dynamicFontManager.startThread()
        startCommonHanWarmup()
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

    private fun registerStaticPages(glyphPages: List<StaticGlyphPage>): Boolean = synchronized(staticPagesLock) {
        if (closed.get()) {
            return@synchronized false
        }

        glyphPages.forEach { glyphPage ->
            for ((font, glyphRenderInfo) in glyphPage.glyphs) {
                staticGlyphs.computeIfAbsent(font) { Int2ObjectOpenHashMap(512) }
                    .put(glyphRenderInfo.codepoint, GlyphDescriptor(glyphPage, glyphRenderInfo))
            }
        }
        staticPage.addAll(glyphPages)
        true
    }

    private fun startCommonHanWarmup() {
        commonHanWarmupJob = ioScope.launch(Dispatchers.Default) {
            try {
                warmCommonHanGlyphs()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                logger.error("Failed to warm common Han glyphs", exception)
            }
        }
    }

    private suspend fun warmCommonHanGlyphs() {
        val glyphs = FontCharacterSet.createCommonHanGlyphs(primaryFace, fallbackFonts)
        if (glyphs.isEmpty()) {
            logger.info("Skipping common Han warmup because no configured font can display it")
            return
        }

        val preparedPages = StaticGlyphPage.prepareGlyphPages(glyphs)
        currentCoroutineContext().ensureActive()
        if (closed.get()) {
            return
        }

        val registered = withContext(Dispatchers.Minecraft) {
            materializeAndRegister(preparedPages)
        }
        if (registered) {
            logger.info("Finished warming ${glyphs.size} common Han glyphs")
        }
    }

    private fun materializeAndRegister(preparedPages: List<PreparedStaticGlyphPage>): Boolean {
        if (closed.get()) {
            return false
        }

        val pages = preparedPages.map(PreparedStaticGlyphPage::materialize)
        if (registerStaticPages(pages)) {
            return true
        }

        pages.forEach { it.texture.close() }
        return false
    }

    fun requestGlyph(font: FontFace, style: @FontStyle Int, codepoint: Int): GlyphDescriptor? {
        check(font in registeredFonts) { "Font $font is not registered" }

        val requestedFont = font.style(style) ?: font.plainStyle
        staticGlyphs[requestedFont]?.get(codepoint)?.let { return it }

        val resolvedFont = resolveFont(font, fallbackFonts, style, codepoint) ?: return null
        findResolvedStaticGlyph(font, resolvedFont, codepoint)?.let { return it }

        val fontGlyph = FontGlyph(codepoint, resolvedFont)
        val glyphIdentifier = GlyphIdentifier(fontGlyph)
        this.dynamicFontManager.requestGlyph(fontGlyph)
        return this.dynamicallyLoadedGlyphs[glyphIdentifier]
    }

    private fun findResolvedStaticGlyph(
        requestedFace: FontFace,
        resolvedFont: FontId,
        codepoint: Int,
    ): GlyphDescriptor? = staticGlyphs[resolvedFont]?.get(codepoint) ?: fallbackFonts.asSequence()
        .filter { it !== requestedFace }
        .map(FontFace::plainStyle)
        .firstNotNullOfOrNull { staticGlyphs[it]?.get(codepoint) }

    fun getFallbackGlyph(font: FontFace): GlyphDescriptor {
        return fallbackGlyphs[font] ?: error("Font $font is not registered")
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) {
            return
        }
        commonHanWarmupJob?.cancel()
        commonHanWarmupJob = null
        unregister()
        this.dynamicFontManager.close()
        this.dynamicPage.texture.close()
        synchronized(staticPagesLock) {
            this.staticPage.forEach { it.texture.close() }
            this.staticPage.clear()
        }
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
    return synchronized(GlyphPage.fontRasterizationLock) {
        val requestedFont = requestedFace.style(style) ?: requestedFace.plainStyle
        if (requestedFont.awtFont.canDisplay(codepoint)) {
            return@synchronized requestedFont
        }

        fallbackFaces.asSequence()
            .filter { it !== requestedFace }
            .map { it.style(style) ?: it.plainStyle }
            .firstOrNull { it.awtFont.canDisplay(codepoint) }
    }
}
