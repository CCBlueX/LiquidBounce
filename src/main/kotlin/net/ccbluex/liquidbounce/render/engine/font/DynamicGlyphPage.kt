package net.ccbluex.liquidbounce.render.engine.font

import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import java.awt.Dimension
import java.awt.Font
import java.awt.Point
import java.awt.image.BufferedImage

class DynamicGlyphPage(val atlasSize: Dimension, fontHeight: Int) : BaseGlpyhPage() {
    private val image = createBufferedImageWithDimensions(atlasSize)
    override val texture = NativeImageBackedTexture(image.toNativeImage())
    override val fallbackGlyph: Glyph
        get() = getGlyph('?')!!

    val charMap = HashMap<Char, Pair<Glyph, AtlasSliceHandle>>()
    val dirty = ArrayList<Glyph>()

    private val allocator = DynamicAtlasAllocator(
        atlasSize,
        fontHeight + 4,
        Dimension(fontHeight / 3, fontHeight / 3)
    )

    override fun getGlyph(char: Char): Glyph? {
        return charMap[char]?.first
    }

    /**
     * Tries to add the given characters to the page.
     *
     * @return A list of characters that could not be added
     */
    fun tryAdd(c: List<Char>, font: Font): List<Char> {
        val failed = ArrayList<Char>()

        val changesToDo = c
            .filter { !charMap.containsKey(it) }
            .sortedByDescending {
                val dims = font.createGlyphVector(fontRendererContext, it.toString())

                val bounds2D = dims.getGlyphMetrics(0).bounds2D

                bounds2D.width * bounds2D.height
            }
            .mapNotNull {
                val placementPlan = planCharacterPlacement(it, font)

                if (placementPlan != null) {
                    placementPlan
                } else {
                    failed.add(it)

                    null
                }
            }

        // Render the characters to the image
        renderGlyphs(this.image, font, changesToDo.map { it.first })

        changesToDo.forEach { (generationInfo, slice) ->
            val glyph = createGlyphFromGenerationInfo(generationInfo, atlasSize)

            charMap[generationInfo.c] = glyph to slice

            updateNativeTexture(generationInfo, glyph)
        }

        return failed
    }

    private fun updateNativeTexture(generationInfo: Companion.CharacterGenerationInfo, glyph: Glyph) {
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

                toImage.setColor(toLocation.x + i, toLocation.y + j, color)
            }
        }
    }

    private fun planCharacterPlacement(
        char: Char,
        font: Font
    ): Pair<Companion.CharacterGenerationInfo, AtlasSliceHandle>? {
        val characterInfo = createCharacterCreationInfo(char, font) ?: return null
        val atlasAllocation = allocator.allocate(characterInfo.atlasDimension) ?: return null

        characterInfo.atlasLocation = atlasAllocation.pos

        return characterInfo to atlasAllocation
    }


}
