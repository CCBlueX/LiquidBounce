/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
 *
 */

package net.ccbluex.liquidbounce.render.engine.font.dynamic

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectImmutableList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.withLock
import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.engine.font.FontGlyph
import net.ccbluex.liquidbounce.render.engine.font.GlyphDescriptor
import net.ccbluex.liquidbounce.render.engine.font.GlyphIdentifier
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.render.uploadRect
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

class DynamicFontCacheManager(
    private val dynamicGlyphPage: DynamicGlyphPage,
    /**
     * Available fonts, sorted by priority
     */
    private val availableFonts: Collection<FontManager.FontFace>
) {

    private val glyphPageLock = ReentrantLock()
    private val glyphPageDirtyFlag = AtomicBoolean(false)
    private val glyphPageChanges = ObjectArrayList<ChangeOnAtlas>()

    private val cacheData = ConcurrentHashMap<GlyphIdentifier, CharCacheData>()
    private val requests = ObjectOpenHashSet<GlyphIdentifier>()

    private val lock = ReentrantLock()
    private val condVar = lock.newCondition()

    fun requestGlyph(ch: Char, font: Int) {
        val glyphIdentifier = GlyphIdentifier(ch, font)
        val cacheObject = this.cacheData.computeIfAbsent(glyphIdentifier) { CharCacheData() }

        cacheObject.lastUsage.set(System.currentTimeMillis())

        if (cacheObject.cacheState.get() == UNCACHED) {
            // Notify font cache manager main thread
            this.lock.withLock {
                requests.add(glyphIdentifier)

                condVar.signal()
            }
        }
    }

    fun update(): List<ChangeOnAtlas> {
        if (!this.glyphPageDirtyFlag.get()) {
            return emptyList()
        }

        val changes = this.glyphPageLock.withLock {
            val requiredUpdateCount = this.glyphPageChanges.count { !it.removed }

            if (requiredUpdateCount > 15) {
                this.dynamicGlyphPage.texture.upload()
            } else {
                for (change in this.glyphPageChanges) {
                    if (change.removed) {
                        continue
                    }

                    val bb = change.descriptor.renderInfo.atlasLocation?.pixelBoundingBox ?: continue

                    this.dynamicGlyphPage.texture.uploadRect(
                        mipLevel = 0,
                        x = bb.xMin.toInt(),
                        y = bb.yMin.toInt(),
                        width = (bb.xMax - bb.xMin).toInt(),
                        height = (bb.yMax - bb.yMin).toInt()
                    )
                }
            }

            val changes = ObjectImmutableList(this.glyphPageChanges)

            this.glyphPageChanges.clear()
            this.glyphPageDirtyFlag.set(false)

            changes
        }

        return changes
    }

    fun startThread() {
        thread(name = "lb-dynamic-font-manager") {
            while (!Thread.interrupted()) {
                try {
                    threadMainLoop()
                } catch (_: InterruptedException) { // I hate everything about handling thread interrupts in java...
                    break
                } catch (e: Throwable) {
                    logger.error("Error on dynamic font manager thread", e)
                }
            }
        }
    }

    private fun threadMainLoop() {
        val requestedChars = this.lock.withLock {
            // Wait for stuff to happen
            this.condVar.await()

            val retrievedRequests = ObjectImmutableList(this.requests)

            this.requests.clear()

            retrievedRequests
        }

        val allocationList = createAllocationRequests(requestedChars)

        val unsuccessfulAllocations = this.glyphPageLock.withLock {
            tryAllocations(allocationList)
        }

        if (unsuccessfulAllocations.isEmpty()) {
            return
        }

        freeSpace()

        val stillUnsuccessfulAllocations =
            createAllocationRequests(unsuccessfulAllocations.mapToArray(::GlyphIdentifier).asList())

        // TODO: Optimize the atlas in this situation
        // We weren't able to allocate those chars even after freeing some space. Don't ask us ever again about
        // allocating them >:c
        stillUnsuccessfulAllocations.forEach { dontRetryAllocationOf(GlyphIdentifier(it)) }
    }

    private fun dontRetryAllocationOf(it: GlyphIdentifier) {
        this.cacheData[it]!!.cacheState.set(BLOCKED)
    }

    private fun freeSpace() {
        for ((glyphId, charCacheData) in this.cacheData) {
            if (System.currentTimeMillis() - charCacheData.lastUsage.get() <= MAX_CACHE_TIME_MS) {
                continue
            }

            val renderInfo = this.dynamicGlyphPage.free(glyphId.codepoint, glyphId.style)

            if (renderInfo != null) {
                this.glyphPageChanges.add(
                    ChangeOnAtlas(
                        GlyphDescriptor(this.dynamicGlyphPage, renderInfo),
                        glyphId.style,
                        removed = true
                    )
                )
            } else {
                logger.warn("Character '${glyphId.codepoint}' was freed twice.")
            }

            charCacheData.cacheState.set(UNCACHED)
        }
    }

    /**
     * Tries the given allocations, returns all allocations that failed.
     */
    private fun tryAllocations(requests: List<FontGlyph>): List<FontGlyph> {
        val unsuccessful = this.dynamicGlyphPage.tryAdd(requests)

        requests.forEach {
            if (it !in unsuccessful) {
                this.cacheData[GlyphIdentifier(it)]!!.cacheState.set(CACHED)

                val addedGlyph = this.dynamicGlyphPage.getGlyph(it.codepoint, it.font.style)!!

                this.glyphPageDirtyFlag.set(true)
                this.glyphPageChanges.add(
                    ChangeOnAtlas(
                        GlyphDescriptor(this.dynamicGlyphPage, addedGlyph),
                        it.font.style,
                        removed = false
                    )
                )
            }
        }

        return unsuccessful
    }

    private fun createAllocationRequests(requestedGlyphs: List<GlyphIdentifier>): List<FontGlyph> {
        val requests = ArrayList<FontGlyph>()

        for (requestedGlyph in requestedGlyphs) {
            val font = findFontForGlyph(requestedGlyph)

            // If we have no font which could draw the requested glyph there is no sense in trying it again.
            if (font == null) {
                dontRetryAllocationOf(requestedGlyph)

                continue
            }

            requests.add(FontGlyph(requestedGlyph.codepoint, font))
        }

        return requests
    }

    private fun findFontForGlyph(ch: GlyphIdentifier): FontManager.FontId? {
        return this.availableFonts.firstNotNullOfOrNull { fontFace ->
            fontFace.styles[ch.style]?.takeIf { it.awtFont.canDisplay(ch.codepoint) }
        }
    }

    class ChangeOnAtlas(val descriptor: GlyphDescriptor, val style: Int, val removed: Boolean)
}

private const val MAX_CACHE_TIME_MS = 30 * 1000

private const val UNCACHED = 0
private const val CACHED = 1
private const val BLOCKED = 2

private class CharCacheData {
    /**
     * Possible values: [UNCACHED], [CACHED] and [BLOCKED]
     */
    val cacheState = AtomicInteger(UNCACHED)
    val lastUsage = AtomicLong(0L)
}
