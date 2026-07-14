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
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.ccbluex.liquidbounce.render.engine.font.AtlasSliceHandle
import net.ccbluex.liquidbounce.render.engine.font.DynamicAtlasAllocator
import net.ccbluex.liquidbounce.render.engine.font.FontGlyph
import net.ccbluex.liquidbounce.render.engine.font.GlyphIdentifier
import net.ccbluex.liquidbounce.render.engine.font.GlyphPage
import net.ccbluex.liquidbounce.render.engine.font.GlyphPage.Companion
import net.ccbluex.liquidbounce.render.engine.font.GlyphRenderInfo
import net.ccbluex.liquidbounce.utils.render.asTexture
import net.ccbluex.liquidbounce.utils.render.copyToNativeImage
import net.ccbluex.liquidbounce.utils.render.toNativeImage
import java.awt.Dimension
import kotlin.math.min

class DynamicGlyphPage(val atlasSize: Dimension = DEFAULT_ATLAS_SIZE, fontHeight: Int) : GlyphPage() {
    private val image = createBufferedImageWithDimensions(atlasSize)
    override val texture = image.toNativeImage().asTexture { "DynamicGlyphPage ${atlasSize.width}x${atlasSize.height}" }
    private val glyphMap = Object2ObjectOpenHashMap<GlyphIdentifier, Pair<GlyphRenderInfo, AtlasSliceHandle>>()
    private var copyScratchBuffer = IntArray(0)

    private val allocator = DynamicAtlasAllocator(
        atlasSize,
        fontHeight + 4,
        Dimension(fontHeight / 3, fontHeight / 3)
    )

    fun getGlyph(fontGlyph: FontGlyph): GlyphRenderInfo? {
        return glyphMap[GlyphIdentifier(fontGlyph)]?.first
    }

    /**
     * Tries to add the given characters to the page.
     *
     * @return A list of characters that could not be added
     */
    fun tryAdd(c: Iterable<FontGlyph>): List<FontGlyph> {
        val failed = ObjectArrayList<FontGlyph>()

        val changesToDo = c
            .filter { glyphId -> !glyphMap.containsKey(GlyphIdentifier(glyphId)) }
            .sortedByDescending { glyphId ->
                val text = Character.toString(glyphId.codepoint)
                val dims = glyphId.font.awtFont.createGlyphVector(fontRendererContext, text)

                val bounds2D = dims.getGlyphMetrics(0).bounds2D

                bounds2D.width * bounds2D.height
            }
            .mapNotNull { glyphId ->
                val placementPlan = planCharacterPlacement(glyphId)

                if (placementPlan != null) {
                    placementPlan
                } else {
                    failed.add(glyphId)

                    null
                }
            }

        // Render the characters to the image
        renderGlyphs(this.image, changesToDo.map { it.first })

        changesToDo.forEach { (generationInfo, slice) ->
            val glyph = createGlyphFromGenerationInfo(generationInfo, atlasSize)

            glyphMap.put(GlyphIdentifier(generationInfo.fontGlyph), glyph to slice)

            updateNativeTexture(generationInfo)
        }

        return failed
    }

    fun free(glyphIdentifier: GlyphIdentifier): GlyphRenderInfo? {
        val (renderInfo, sliceHandle) = this.glyphMap.remove(glyphIdentifier) ?: return null

        this.allocator.free(sliceHandle)

        return renderInfo
    }

    /**
     * Clears the allocator and uses optimized characters with optimized allocation order to reduce the amount of
     * fragmentation.
     *
     * @return Removed chars
     */
    fun optimizeAtlas(): List<Pair<GlyphIdentifier, GlyphRenderInfo>> {
        // Free everything, create a new allocator and use max(largestFontGlyph.height, medianFontGlyphHeight) as
        // minimal vertical slice height and the dimensions of the smallest character is minDimension.

        TODO()
    }

    private fun updateNativeTexture(generationInfo: Companion.CharacterGenerationInfo) {
        val location = generationInfo.atlasLocation
        val dimension = generationInfo.atlasDimension
        copyScratchBuffer = image.copyToNativeImage(
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

    private fun planCharacterPlacement(glyph: FontGlyph): Pair<Companion.CharacterGenerationInfo, AtlasSliceHandle>? {
        val characterInfo = createCharacterCreationInfo(glyph) ?: return null
        val atlasAllocation = allocator.allocate(characterInfo.atlasDimension) ?: return null

        characterInfo.atlasLocation = atlasAllocation.pos

        return characterInfo to atlasAllocation
    }


    companion object {
        private val DEFAULT_ATLAS_SIZE by lazy(LazyThreadSafetyMode.NONE) {
            val atlasSize = min(2048, maxTextureSize.value)
            Dimension(atlasSize, atlasSize)
        }
    }
}
