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

import net.ccbluex.liquidbounce.render.engine.font.*
import net.ccbluex.liquidbounce.utils.render.toNativeImage
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import java.awt.Dimension
import java.awt.Point
import java.awt.image.BufferedImage

class DynamicGlyphPage(val atlasSize: Dimension, fontHeight: Int) : GlyphPage() {
    private val image = createBufferedImageWithDimensions(atlasSize)
    override val texture = NativeImageBackedTexture(image.toNativeImage())
    val glyphMap = HashMap<Pair<Int, Char>, Pair<GlyphRenderInfo, AtlasSliceHandle>>()
    val dirty = ArrayList<GlyphRenderInfo>()

    private val allocator = DynamicAtlasAllocator(
        atlasSize,
        fontHeight + 4,
        Dimension(fontHeight / 3, fontHeight / 3)
    )

    fun getGlyph(char: Char, style: Int): GlyphRenderInfo? {
        return glyphMap[style to char]?.first
    }

    /**
     * Tries to add the given characters to the page.
     *
     * @return A list of characters that could not be added
     */
    fun tryAdd(c: List<FontGlyph>): List<FontGlyph> {
        val failed = ArrayList<FontGlyph>()

        val changesToDo = c
            .filter { glyphId -> !glyphMap.containsKey(glyphId.font.style to glyphId.codepoint) }
            .sortedByDescending { glyphId ->
                val dims = glyphId.font.awtFont.createGlyphVector(fontRendererContext, glyphId.codepoint.toString())

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

            glyphMap[generationInfo.fontGlyph.font.style to generationInfo.fontGlyph.codepoint] = glyph to slice

            updateNativeTexture(generationInfo, glyph)
        }

        return failed
    }

    fun free(ch: Char, style: Int): GlyphRenderInfo? {
        val (renderInfo, sliceHandle) = this.glyphMap.remove(style to ch) ?: return null

        this.allocator.free(sliceHandle)

        return renderInfo
    }

    /**
     * Clears the allocator and uses optimized characters with optimized allocation order to reduce the amount of
     * fragmentation.
     *
     * @return Removed chars
     */
    fun optimizeAtlas(): List<Triple<Int, Char, GlyphRenderInfo>> {
        // Free everything, create a new allocator and use max(largestFontGlyph.height, medianFontGlyphHeight) as
        // minimal vertical slice height and the dimensions of the smallest character is minDimension.

        TODO()
    }

    private fun updateNativeTexture(generationInfo: Companion.CharacterGenerationInfo, glyph: GlyphRenderInfo) {
        copyImageSection(
            fromImage = this.image,
            toImage = texture.image!!,
            fromLocation = generationInfo.atlasLocation,
            toLocation = generationInfo.atlasLocation,
            patchSize = generationInfo.atlasDimension
        )

        this.dirty.add(glyph)
    }

    private fun copyImageSection(
        fromImage: BufferedImage,
        toImage: NativeImage,
        fromLocation: Point,
        toLocation: Point,
        patchSize: Dimension
    ) {
        for (i in 0 until patchSize.width) {
            for (j in 0 until patchSize.height) {
                val color = fromImage.getRGB(fromLocation.x + i, fromLocation.y + j)

                toImage.setColorArgb(toLocation.x + i, toLocation.y + j, color)
            }
        }
    }

    private fun planCharacterPlacement(glyph: FontGlyph): Pair<Companion.CharacterGenerationInfo, AtlasSliceHandle>? {
        val characterInfo = createCharacterCreationInfo(glyph) ?: return null
        val atlasAllocation = allocator.allocate(characterInfo.atlasDimension) ?: return null

        characterInfo.atlasLocation = atlasAllocation.pos

        return characterInfo to atlasAllocation
    }


}
