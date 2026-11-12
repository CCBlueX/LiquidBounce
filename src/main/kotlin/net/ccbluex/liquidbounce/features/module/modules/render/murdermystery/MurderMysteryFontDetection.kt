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

package net.ccbluex.liquidbounce.features.module.modules.render.murdermystery

import net.ccbluex.liquidbounce.config.gson.util.readJson
import net.ccbluex.liquidbounce.utils.item.MAP_SIZE
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.level.saveddata.maps.MapItemSavedData
import java.util.BitSet

object MurderMysteryFontDetection {

    private const val FILE_NAME = "hypixel_mm_letters.json"

    /**
     * Row of the map image the contract line is rendered on.
     */
    private const val CONTRACT_LINE_ROW = 105

    private const val CONTRACT_LINE_HEIGHT = 7

    private val LETTER_MAP: Map<String, BooleanArray> = run {
        val stream =
            ModuleMurderMystery.javaClass.getResourceAsStream("/resources/liquidbounce/data/$FILE_NAME")

        checkNotNull(stream) { "Unable to find $FILE_NAME!" }

        // We should not use interface here
        stream.readJson<HashMap<String, BooleanArray>>()
    }

    /**
     * Whether the given packed map color is the background of the contract paper.
     *
     * The paper is drawn in [MapColor.WOOD] at various brightness levels, so only the color id is
     * compared and the brightness bits are ignored.
     *
     * @see net.minecraft.world.level.material.MapColor.getPackedId
     */
    private fun isContractLineColor(packedColor: Byte): Boolean =
        (packedColor.toInt() and 0xFF) shr 2 == MapColor.WOOD.id

    @Suppress("all")
    fun readContractLine(mapData: MapItemSavedData): String {
        val contractLine = filterContractLine(mapData)

        val output = StringBuilder()

        var lastNonEmptyScanline = -1
        var emptyScanlines = 0

        for (x in 0 until MAP_SIZE) {
            var isEmpty = true

            for (y in 0 until CONTRACT_LINE_HEIGHT) {
                if (!contractLine[MAP_SIZE * y + x]) {
                    isEmpty = false
                    break
                }
            }

            if (isEmpty) {
                if (emptyScanlines++ > 3) {
                    output.append(' ')
                    emptyScanlines = 0
                }
            }

            if (lastNonEmptyScanline != -1 && isEmpty) {
                var yOff = lastNonEmptyScanline
                var off: Int

                val w = x - lastNonEmptyScanline
                val h = CONTRACT_LINE_HEIGHT

                val fingerPrint = BooleanArray(w * h)

                var y1 = 0

                while (y1 < h) {
                    off = yOff

                    for (x1 in 0 until w) {
                        fingerPrint[y1 * w + x1] = !contractLine[off++]
                    }

                    y1++
                    yOff += MAP_SIZE
                }

                val letter = LETTER_MAP.entries.firstOrNull { (_, value1) ->
                    value1.contentEquals(fingerPrint)
                }?.key ?: "?"

                output.append(letter)

                lastNonEmptyScanline = -1
            }

            if (!isEmpty && lastNonEmptyScanline == -1) {
                lastNonEmptyScanline = x
                emptyScanlines = 0
            }
        }

        val outs = output.trim { it <= ' ' }.toString()
        return outs
    }

    private fun filterContractLine(mapData: MapItemSavedData): BitSet {
        val contractLine = BitSet(MAP_SIZE * CONTRACT_LINE_HEIGHT)

        for (y in 0 until CONTRACT_LINE_HEIGHT) {
            val rowOffset = (CONTRACT_LINE_ROW + y) * MAP_SIZE

            for (x in 0 until MAP_SIZE) {
                contractLine[MAP_SIZE * y + x] = isContractLineColor(mapData.colors[rowOffset + x])
            }
        }
        return contractLine
    }
}
