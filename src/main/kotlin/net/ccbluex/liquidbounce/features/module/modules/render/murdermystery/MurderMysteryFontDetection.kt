package net.ccbluex.liquidbounce.features.module.modules.render.murdermystery

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import net.minecraft.block.MapColor
import net.minecraft.item.map.MapState
import java.awt.Color
import java.io.InputStreamReader

object MurderMysteryFontDetection {
    private val LETTER_MAP: Map<String, BooleanArray>

    init {
        val gson = Gson()

        val stream =
            ModuleMurderMystery.javaClass.getResourceAsStream("/assets/liquidbounce/data/hypixel_mm_letters.json")

        check(stream != null) { "Unable to find hypixel_mm_letters.json!" }

        val map =
            stream.use {
                gson.fromJson(
                    JsonReader(InputStreamReader(it)),
                    TypeToken.getParameterized(Map::class.java, String::class.java, BooleanArray::class.java),
                )
            }

        LETTER_MAP = map as Map<String, BooleanArray>
    }

    @Suppress("all")
    fun readContractLine(mapData: MapState): String {
        val rgb = extractBitmapFromMap(mapData)
        val contractLine = filterContractLine(rgb)

        val output = StringBuilder()

        var lastNonEmptyScanline = -1
        var emptyScanlines = 0

        for (x in 0..128) {
            var isEmpty = true

            for (y in 0 until 7) {
                if (contractLine[128 * y + x] == -1) {
                    isEmpty = false
                    break
                }
            }

            if (isEmpty) {
                if (emptyScanlines++ > 3) {
                    output.append(" ")
                    emptyScanlines = 0
                }
            }

            if (lastNonEmptyScanline != -1 && isEmpty) {
                var yoff = lastNonEmptyScanline
                var off: Int

                val w = x - lastNonEmptyScanline
                val h = 7

                val fingerPrint = BooleanArray(w * h)

                var y1 = 0

                while (y1 < h) {
                    off = yoff

                    for (x1 in 0 until w) {
                        fingerPrint[y1 * w + x1] = contractLine[off++] == -1
                    }

                    y1++
                    yoff += 128
                }

                var letter: String? = null

                for ((key, value1) in LETTER_MAP.entries) {
                    if (value1.contentEquals(fingerPrint)) {
                        letter = key
                        break
                    }
                }

                output.append(letter ?: "?")

                lastNonEmptyScanline = -1
            }

            if (!isEmpty && lastNonEmptyScanline == -1) {
                lastNonEmptyScanline = x
                emptyScanlines = 0
            }
        }

        val outs = output.toString().trim { it <= ' ' }
        return outs
    }

    private fun filterContractLine(rgb: IntArray): IntArray {
        val contractLine = IntArray(128 * 7)

        for (y in 0..7) {
            for (x in 0..128) {
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

        for (i in 0..rgb.size) {
            val color = MapColor.getRenderColor(mapData.colors[i].toInt())

            val r = color and 0xFF
            val g = (color ushr 8) and 0xFF
            val b = (color ushr 16) and 0xFF

            rgb[i] = Color(r, g, b).rgb
        }
        return rgb
    }
}
