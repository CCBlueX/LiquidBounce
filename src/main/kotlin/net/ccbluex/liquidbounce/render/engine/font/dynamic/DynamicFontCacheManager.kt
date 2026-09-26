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

package net.ccbluex.liquidbounce.render.engine.font.dynamic

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectImmutableList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.withLock
import net.ccbluex.liquidbounce.render.engine.font.FontId
import net.ccbluex.liquidbounce.render.engine.font.FontGlyph
import net.ccbluex.liquidbounce.render.engine.font.GlyphDescriptor
import net.ccbluex.liquidbounce.render.engine.font.GlyphIdentifier
import net.ccbluex.liquidbounce.utils.client.logger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

class DynamicFontCacheManager(
    private val dynamicGlyphPage: DynamicGlyphPage,
) : AutoCloseable {

    private val glyphPageLock = ReentrantLock()
    private val glyphPageDirtyFlag = AtomicBoolean(false)
    private val glyphPageChanges = ObjectArrayList<ChangeOnAtlas>()

    private val cacheData = ConcurrentHashMap<GlyphIdentifier, CharCacheData>()

    private val requests = ObjectOpenHashSet<GlyphIdentifier>()
    private val requestsLock = ReentrantLock()
    private val hasRequest = requestsLock.newCondition()
    private val running = AtomicBoolean(false)

    @Volatile
    private var workerThread: Thread? = null

    fun requestGlyph(fontGlyph: FontGlyph) {
        val glyphIdentifier = GlyphIdentifier(fontGlyph)
        val cacheObject = this.cacheData.computeIfAbsent(glyphIdentifier) { CharCacheData() }

        cacheObject.lastUsage.set(System.currentTimeMillis())

        if (cacheObject.cacheState.compareAndSet(UNCACHED, REQUESTED)) {
            this.requestsLock.withLock {
                requests.add(glyphIdentifier)
                hasRequest.signal()
            }
        }
    }

    fun update(): List<ChangeOnAtlas> {
        if (!this.glyphPageDirtyFlag.get()) {
            return emptyList()
        }

        return this.glyphPageLock.withLock {
            val changes = ObjectImmutableList(this.glyphPageChanges)
            this.glyphPageChanges.clear()
            val requiredUpdateCount = changes.count { !it.removed }

            if (requiredUpdateCount > 15) {
                this.dynamicGlyphPage.texture.upload()
            } else {
                for (change in changes) {
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

            this.glyphPageDirtyFlag.set(false)

            changes
        }
    }

    @Synchronized
    fun startThread() {
        check(workerThread == null) { "Dynamic font manager thread already started" }
        running.set(true)
        workerThread = thread(name = "lb-dynamic-font-manager", isDaemon = true) {
            while (running.get()) {
                try {
                    threadMainLoop()
                } catch (_: InterruptedException) {
                    break
                } catch (e: Throwable) {
                    logger.error("Error on dynamic font manager thread", e)
                }
            }
        }
    }

    private fun threadMainLoop() {
        val requestedChars = this.requestsLock.withLock {
            while (requests.isEmpty() && running.get()) {
                this.hasRequest.await()
            }

            if (!running.get()) {
                return
            }

            val retrievedRequests = ObjectImmutableList(this.requests)
            this.requests.clear()
            retrievedRequests
        }

        val allocationList = requestedChars.map { FontGlyph(it.codepoint, it.font) }

        val unsuccessfulAllocations = this.glyphPageLock.withLock {
            tryAllocations(allocationList)
        }

        if (unsuccessfulAllocations.isEmpty()) {
            return
        }

        val stillUnsuccessfulAllocations = this.glyphPageLock.withLock {
            if (freeSpace()) {
                tryAllocations(unsuccessfulAllocations)
            } else {
                unsuccessfulAllocations
            }
        }

        stillUnsuccessfulAllocations.forEach { dontRetryAllocationOf(GlyphIdentifier(it)) }
    }

    private fun dontRetryAllocationOf(it: GlyphIdentifier) {
        this.cacheData[it]!!.cacheState.set(BLOCKED)
    }

    private fun freeSpace(): Boolean {
        var freedAny = false
        val now = System.currentTimeMillis()

        for ((glyphId, charCacheData) in this.cacheData) {
            if (charCacheData.cacheState.get() != CACHED || now - charCacheData.lastUsage.get() <= MAX_CACHE_TIME_MS) {
                continue
            }

            val renderInfo = this.dynamicGlyphPage.free(glyphId)

            if (renderInfo != null) {
                freedAny = true
                this.glyphPageChanges.add(
                    ChangeOnAtlas(
                        GlyphDescriptor(this.dynamicGlyphPage, renderInfo),
                        glyphId.font,
                        removed = true
                    )
                )
            } else {
                logger.warn("Character '${glyphId.codepoint}' was freed twice.")
            }

            charCacheData.cacheState.set(UNCACHED)
        }

        if (freedAny) {
            this.glyphPageDirtyFlag.set(true)
            this.cacheData.values.forEach { cacheData ->
                cacheData.cacheState.compareAndSet(BLOCKED, UNCACHED)
            }
        }

        return freedAny
    }

    /**
     * Tries the given allocations, returns all allocations that failed.
     */
    private fun tryAllocations(requests: Iterable<FontGlyph>): List<FontGlyph> {
        val unsuccessful = this.dynamicGlyphPage.tryAdd(requests)

        requests.forEach {
            if (it !in unsuccessful) {
                this.cacheData[GlyphIdentifier(it)]!!.cacheState.set(CACHED)

                val addedGlyph = this.dynamicGlyphPage.getGlyph(it)!!

                this.glyphPageDirtyFlag.set(true)
                this.glyphPageChanges.add(
                    ChangeOnAtlas(
                        GlyphDescriptor(this.dynamicGlyphPage, addedGlyph),
                        it.font,
                        removed = false
                    )
                )
            }
        }

        return unsuccessful
    }

    @Synchronized
    override fun close() {
        if (!running.getAndSet(false)) {
            return
        }

        requestsLock.withLock {
            hasRequest.signalAll()
        }
        workerThread?.interrupt()
        workerThread?.join()
        workerThread = null
        requestsLock.withLock {
            requests.clear()
        }
    }

    class ChangeOnAtlas(
        @JvmField val descriptor: GlyphDescriptor,
        @JvmField val font: FontId,
        @JvmField val removed: Boolean,
    )
}

private const val MAX_CACHE_TIME_MS = 30 * 1000L

private const val UNCACHED = 0
private const val REQUESTED = 1
private const val CACHED = 2
private const val BLOCKED = 3

private class CharCacheData {
    /**
     * Possible values: [UNCACHED], [REQUESTED], [CACHED] and [BLOCKED]
     */
    val cacheState = AtomicInteger(UNCACHED)
    val lastUsage = AtomicLong(0L)
}
