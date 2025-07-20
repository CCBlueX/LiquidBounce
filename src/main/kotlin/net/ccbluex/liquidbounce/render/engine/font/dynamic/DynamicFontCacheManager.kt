package net.ccbluex.liquidbounce.render.engine.font.dynamic

import com.mojang.blaze3d.platform.GlStateManager
import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.withLock
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.engine.font.FontGlyph
import net.ccbluex.liquidbounce.render.engine.font.GlyphDescriptor
import net.ccbluex.liquidbounce.utils.client.logger
import net.minecraft.client.texture.NativeImage
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
    private val availableFonts: Set<FontManager.FontFace>
) {
    private val glyphPageLock = ReentrantLock()
    private val glyphPageDirtyFlag = AtomicBoolean(false)
    private var glyphPageChanges = ArrayList<ChangeOnAtlas>()

    private val cacheData = ConcurrentHashMap<GlyphIdentifier, CharCacheData>()
    private val requests = HashSet<GlyphIdentifier>()

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
            this.dynamicGlyphPage.texture.bindTexture()

            val requiredUpdateCount = this.glyphPageChanges.count { !it.removed }

            if (requiredUpdateCount > 15) {
                this.dynamicGlyphPage.texture.upload()
            } else {
                for (change in this.glyphPageChanges) {
                    if (change.removed) {
                        continue
                    }

                    val bb = change.descriptor.renderInfo.atlasLocation?.pixelBoundingBox ?: continue

                    val width = (bb.xMax - bb.xMin).toInt()
                    val height = (bb.yMax - bb.yMin).toInt()

                    val chunkImage = NativeImage(width, height, false)

                    chunkImage.use {
                        this.dynamicGlyphPage.texture.image!!.copyRect(
                            chunkImage,
                            bb.xMin.toInt(), bb.yMin.toInt(),
                            0, 0,
                            width, height,
                            false, false
                        )

                        chunkImage.upload(
                            0,
                            bb.xMin.toInt(), bb.yMin.toInt(),
                            0, 0,
                            width, height,
                            false
                        )
                    }

                }
            }

            val changes = this.glyphPageChanges

            this.glyphPageChanges = ArrayList()
            this.glyphPageDirtyFlag.set(false)

            changes
        }

        GlStateManager._bindTexture(0)

        return changes
    }

    fun startThread() {
        thread(name = "lb-dynamic-font-manager") {
            while (!Thread.interrupted()) {
                try {
                    threadMainLoop()
                } catch (e: InterruptedException) { // I hate everything about handling thread interrupts in java...
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

            val retrievedRequests = ArrayList(this.requests)

            this.requests.clear()

            retrievedRequests
        }

        val allocationList = createAllocationRequests(requestedChars)

        val unsuccessfullAllocations = this.glyphPageLock.withLock {
            tryAllocations(allocationList)
        }

        if (unsuccessfullAllocations.isEmpty()) {
            return
        }

       freeSpace()


        val stillUnsuccessfulAllocations =
            createAllocationRequests(unsuccessfullAllocations.map { GlyphIdentifier(it.codepoint, it.font.style) })

        // TODO: Optimize the atlas in this situation
        // We weren't able to allocate those chars even after freeing some space. Don't ask us ever again about
        // allocating them >:c
        stillUnsuccessfulAllocations.forEach { dontRetryAllocationOf(GlyphIdentifier(it.codepoint, it.font.style)) }
    }

    private fun dontRetryAllocationOf(it: GlyphIdentifier) {
        this.cacheData[it]!!.cacheState.set(BLOCKED)
    }

    private fun freeSpace() {
        val glyphsToFree = this.cacheData.entries.filter {
            System.currentTimeMillis() - it.value.lastUsage.get() > MAX_CACHE_TIME_MS
        }

        glyphsToFree.forEach { (glyphId, _) ->
            val renderInfo = this.dynamicGlyphPage.free(glyphId.codepoint, glyphId.font)

            if (renderInfo != null) {
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

            this.cacheData[glyphId]!!.cacheState.set(UNCACHED)
        }
    }

    /**
     * Tries the given allocations, returns all allocations that failed.
     */
    private fun tryAllocations(requests: List<FontGlyph>): List<FontGlyph> {
        val unsuccessful = this.dynamicGlyphPage.tryAdd(requests)

        requests.forEach {
            if (it !in unsuccessful) {
                this.cacheData[GlyphIdentifier(it.codepoint, it.font.style)]!!.cacheState.set(CACHED)

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
        return this.availableFonts.firstNotNullOfOrNull {
            val fontInStyle = it.styles.get(ch.font)

            if (fontInStyle != null && fontInStyle.awtFont.canDisplay(ch.codepoint)) {
                fontInStyle
            } else {
                null
            }
        }
    }

    class ChangeOnAtlas(val descriptor: GlyphDescriptor, val style: Int, val removed: Boolean)
}

private data class GlyphIdentifier(val codepoint: Char, val font: Int)

private const val MAX_CACHE_TIME_MS = 30 * 1000

private const val UNCACHED = 0
private const val CACHED = 1
private const val BLOCKED = 2

private class CharCacheData(
    /**
     * Possible values: [UNCACHED], [CACHED] and [BLOCKED]
     */
    var cacheState: AtomicInteger = AtomicInteger(UNCACHED),
    val lastUsage: AtomicLong = AtomicLong(0L)
)
