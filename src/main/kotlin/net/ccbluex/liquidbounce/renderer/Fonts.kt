/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

package net.ccbluex.liquidbounce.renderer

import com.google.gson.*
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.renderer.engine.font.FontRenderer
import net.ccbluex.liquidbounce.utils.HttpUtils.download
import java.awt.Font
import java.io.*
import java.util.*
import java.util.zip.ZipInputStream


object Fonts {
    val fontsFolder = File(LiquidBounce.configSystem.rootFolder.path, "fonts")

    @FontDetails(fontName = "Minecraft Font")
    val minecraftFont: AbstractFontRenderer? = null // TODO Handle minecraft font
    private val CUSTOM_FONT_RENDERERS: TreeMap<FontInfo, AbstractFontRenderer> =
        TreeMap<FontInfo, AbstractFontRenderer>()

    @FontDetails(fontName = "Roboto Medium", fontSize = 8)
    lateinit var font35: AbstractFontRenderer

    @FontDetails(fontName = "Roboto Medium", fontSize = 40)
    lateinit var font40: AbstractFontRenderer

    @FontDetails(fontName = "Roboto Bold", fontSize = 45)
    lateinit var fontBold180: AbstractFontRenderer

    fun loadFonts() {
        val l = System.currentTimeMillis()
        LiquidBounce.logger.info("Loading Fonts.")

        fontsFolder.mkdirs()

        downloadFonts()
        font35 = FontRenderer.createFontRenderer(getFont("Roboto-Medium.ttf", 35 / 2))
        font40 = FontRenderer.createFontRenderer(getFont("Roboto-Medium.ttf", 25))
        fontBold180 = FontRenderer.createFontRenderer(getFont("Roboto-Bold.ttf", 45))

        try {
            CUSTOM_FONT_RENDERERS.clear()
            val fontsFile = File(fontsFolder, "fonts.json")

            if (fontsFile.exists()) {
                val jsonElement = JsonParser().parse(BufferedReader(FileReader(fontsFile)))

                if (jsonElement is JsonNull)
                    return

                val jsonArray = jsonElement as JsonArray

                for (element in jsonArray) {
                    if (element is JsonNull)
                        return

                    val fontObject = element as JsonObject

                    val font = getFont(fontObject["fontFile"].asString, fontObject["fontSize"].asInt)

                    CUSTOM_FONT_RENDERERS[FontInfo(font)] = FontRenderer.createFontRenderer(font)
                }
            } else {
                fontsFile.createNewFile()

                val printWriter = PrintWriter(FileWriter(fontsFile))

                printWriter.println(GsonBuilder().setPrettyPrinting().create().toJson(JsonArray()))
                printWriter.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        LiquidBounce.logger.info("Loaded Fonts. (" + (System.currentTimeMillis() - l) + "ms)")
    }

    private fun downloadFonts() {
        try {
            val outputFile = File(fontsFolder, "roboto.zip")
            if (!outputFile.exists()) {
                LiquidBounce.logger.info("Downloading fonts...")
                download(LiquidBounce.CLIENT_CLOUD + "/fonts/Roboto.zip", outputFile)
                LiquidBounce.logger.info("Extract fonts...")
                extractZip(outputFile.path, fontsFolder.path)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getFontRenderer(name: String?, size: Int): AbstractFontRenderer? {
        for (field in Fonts::class.java.declaredFields) {
            try {
                field.isAccessible = true

                val o = field[null]

                if (o is AbstractFontRenderer) {
                    val fontDetails: FontDetails = field.getAnnotation(FontDetails::class.java)

                    if (fontDetails.fontName == name && fontDetails.fontSize == size)
                        return o
                }
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
        }
        return CUSTOM_FONT_RENDERERS.getOrDefault(FontInfo(name, size), minecraftFont)
    }

    fun getFontDetails(fontRenderer: AbstractFontRenderer): FontInfo? {
        for (field in Fonts::class.java.declaredFields) {
            try {
                field.isAccessible = true
                val o = field[null]
                if (o === fontRenderer) {
                    val fontDetails: FontDetails = field.getAnnotation(FontDetails::class.java)
                    return FontInfo(fontDetails.fontName, fontDetails.fontSize)
                }
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
        }
        for ((key, value) in CUSTOM_FONT_RENDERERS) {
            if (value === fontRenderer)
                return key
        }
        return null
    }

    fun getFonts(): List<AbstractFontRenderer> {
        val fonts: ArrayList<AbstractFontRenderer> = ArrayList<AbstractFontRenderer>()

        for (fontField in Fonts::class.java.declaredFields) {
            try {
                fontField.isAccessible = true

                val fontObj = fontField[null]

                if (fontObj is AbstractFontRenderer)
                    fonts.add(fontObj)
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
        }

        fonts.addAll(CUSTOM_FONT_RENDERERS.values)

        return fonts
    }

    private fun getFont(fontName: String, size: Int): Font {
        return try {
            val inputStream: InputStream = FileInputStream(File(fontsFolder, fontName))
            var awtClientFont = Font.createFont(Font.TRUETYPE_FONT, inputStream)
            awtClientFont = awtClientFont.deriveFont(Font.PLAIN, size.toFloat())
            inputStream.close()
            awtClientFont
        } catch (e: Exception) {
            e.printStackTrace()
            Font("default", Font.PLAIN, size)
        }
    }

    private fun extractZip(zipFile: String, outputFolder: String) {
        val buffer = ByteArray(1024)
        try {
            val folder = File(outputFolder)
            if (!folder.exists()) folder.mkdir()
            val zipInputStream = ZipInputStream(FileInputStream(zipFile))
            var zipEntry = zipInputStream.nextEntry
            while (zipEntry != null) {
                val newFile = File(outputFolder + File.separator.toString() + zipEntry.name)
                File(newFile.parent).mkdirs()
                val fileOutputStream = FileOutputStream(newFile)
                var i: Int
                while (zipInputStream.read(buffer).also { i = it } > 0) fileOutputStream.write(buffer, 0, i)
                fileOutputStream.close()
                zipEntry = zipInputStream.nextEntry
            }
            zipInputStream.closeEntry()
            zipInputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    data class FontInfo(val name: String?, val fontSize: Int) {
        constructor(font: Font) : this(font.name, font.size)
    }

    @Retention(AnnotationRetention.RUNTIME)
    annotation class FontDetails(val fontName: String, val fontSize: Int = -1)
}
