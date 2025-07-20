package net.ccbluex.liquidbounce.features.module.modules.render.murdermystery

import net.ccbluex.liquidbounce.config.gson.util.decode
import net.minecraft.block.MapColor
import net.minecraft.item.map.MapState
import java.awt.Color

object MurderMysteryFontDetection {

    private const val FILE_NAME = "hypixel_mm_letters.json"

    private val LETTER_MAP: Map<String, BooleanArray> = run {
        val stream =
            ModuleMurderMystery.javaClass.getResourceAsStream("/resources/liquidbounce/data/$FILE_NAME")

        checkNotNull(stream) { "Unable to find $FILE_NAME!" }

        // We should not use interface here
        decode<HashMap<String, BooleanArray>>(stream)
    }

    @Suppress("all")
    fun readContractLine(mapData: MapState): String {
        val rgb = extractBitmapFromMap(mapData)
        val contractLine = filterContractLine(rgb)

        val output = StringBuilder()

        var lastNonEmptyScanline = -1
        var emptyScanlines = 0

        for (x in 0 until 128) {
            var isEmpty = true

            for (y in 0 until 7) {
                if (contractLine[128 * y + x] == -1) {
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
                val h = 7

                val fingerPrint = BooleanArray(w * h)

                var y1 = 0

                while (y1 < h) {
                    off = yOff

                    for (x1 in 0 until w) {
                        fingerPrint[y1 * w + x1] = contractLine[off++] == -1
                    }

                    y1++
                    yOff += 128
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

    private fun filterContractLine(rgb: IntArray): IntArray {
        val contractLine = IntArray(128 * 7)

        for (y in 0 until 7) {
            for (x in 0 until 128) {
                var newRGB = rgb[128 * 105 + y * 128 + x]

                newRGB =
                    if (newRGB == Color(123, 102, 62).rgb || newRGB == Color(143, 119, 72).rgb) {
                        0
                    } else {
                        -1
                    }

                contractLine[128 * y + x] = newRGB
            }
        }
        return contractLine
    }

    private fun extractBitmapFromMap(mapData: MapState): IntArray {
        val rgb = IntArray(128 * 128)

        for (i in rgb.indices) {
            val color = MapColor.getRenderColor(mapData.colors[i].toInt())

            val r = color and 0xFF
            val g = (color ushr 8) and 0xFF
            val b = (color ushr 16) and 0xFF

            rgb[i] = Color(r, g, b).rgb
        }
        return rgb
    }
}
