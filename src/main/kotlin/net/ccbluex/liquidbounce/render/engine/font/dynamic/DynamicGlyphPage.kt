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

import it.unimi.dsi.fastutil.ints.IntArrays
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import net.ccbluex.liquidbounce.render.engine.font.FontGlyph
import net.ccbluex.liquidbounce.render.engine.font.GlyphAtlasTexture
import net.ccbluex.liquidbounce.render.engine.font.GlyphPage
import net.ccbluex.liquidbounce.render.engine.font.GlyphPage.Companion
import net.ccbluex.liquidbounce.render.engine.font.RasterizedGlyph
import net.ccbluex.liquidbounce.render.engine.font.copyCoverageToNativeImage
import net.ccbluex.liquidbounce.render.engine.font.toLuminanceNativeImage
import java.awt.Dimension
import kotlin.math.min

class DynamicGlyphPage(val atlasSize: Dimension = DEFAULT_ATLAS_SIZE, fontHeight: Int) : GlyphPage() {
    private val image = createBufferedImageWithDimensions(atlasSize)
    override val texture = GlyphAtlasTexture(
        label = { "DynamicGlyphPage ${atlasSize.width}x${atlasSize.height}" },
        pixels = image.toLuminanceNativeImage(),
        retainPixels = true,
    )
    private val glyphMap = Object2ObjectOpenHashMap<FontGlyph, Pair<RasterizedGlyph, AtlasSliceHandle>>()
    private var copyScratchBuffer = IntArrays.EMPTY_ARRAY

    private val allocator = DynamicAtlasAllocator(
        atlasSize,
        fontHeight + 4,
        Dimension(fontHeight / 3, fontHeight / 3)
    )

    fun getGlyph(fontGlyph: FontGlyph): RasterizedGlyph? {
        return glyphMap[fontGlyph]?.first
    }

    /**
     * Tries to add the given characters to the page.
     *
     * @return A list of characters that could not be added
     */
    fun tryAdd(glyphs: Iterable<FontGlyph>): Set<FontGlyph> {
        val failed = ObjectOpenHashSet<FontGlyph>()

        val changesToDo = glyphs
            .filter { glyphId -> !glyphMap.containsKey(glyphId) }
            .mapNotNull { fontGlyph ->
                createPendingGlyph(fontGlyph) ?: run {
                    failed.add(fontGlyph)
                    null
                }
            }
            .sortedByDescending { pendingGlyph ->
                pendingGlyph.glyphMetrics.bounds2D.run { width * height }
            }
            .mapNotNull { pendingGlyph ->
                val atlasAllocation = allocator.allocate(pendingGlyph.atlasDimension)
                if (atlasAllocation == null) {
                    failed.add(pendingGlyph.fontGlyph)
                    return@mapNotNull null
                }

                pendingGlyph.atlasLocation = atlasAllocation.pos
                pendingGlyph to atlasAllocation
            }

        // Render the characters to the image
        renderGlyphs(this.image, changesToDo.map { it.first })

        changesToDo.forEach { (pendingGlyph, slice) ->
            val glyph = createRasterizedGlyph(pendingGlyph, atlasSize)

            glyphMap[pendingGlyph.fontGlyph] = glyph to slice

            updateNativeTexture(pendingGlyph)
        }

        return failed
    }

    fun free(glyph: FontGlyph): RasterizedGlyph? {
        val (renderInfo, sliceHandle) = this.glyphMap.remove(glyph) ?: return null

        this.allocator.free(sliceHandle)

        return renderInfo
    }

    private fun updateNativeTexture(pending: Companion.PendingGlyph) {
        val location = pending.atlasLocation
        val dimension = pending.atlasDimension
        copyScratchBuffer = image.copyCoverageToNativeImage(
            target = texture.pixels!!,
            sourceX = location.x,
            sourceY = location.y,
            targetX = location.x,
            targetY = location.y,
            width = dimension.width,
            height = dimension.height,
            scratchBuffer = copyScratchBuffer,
        )
    }

    companion object {
        private val DEFAULT_ATLAS_SIZE by lazy(LazyThreadSafetyMode.NONE) {
            val atlasSize = min(2048, maxTextureSize.value)
            Dimension(atlasSize, atlasSize)
        }
    }
}
