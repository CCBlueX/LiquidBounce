/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.font

import com.google.gson.*
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_CLOUD
import net.ccbluex.liquidbounce.file.FileManager.fontsDir
import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.misc.HttpUtils.download
import net.minecraft.client.gui.FontRenderer
import java.awt.Font
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipInputStream

object Fonts : MinecraftInstance() {
    @FontDetails(fontName = "Minecraft Font")
    @JvmStatic
    val minecraftFont: FontRenderer = mc.fontRendererObj

    @FontDetails(fontName = "Roboto Medium", fontSize = 35)
    @JvmStatic
    lateinit var font35: GameFontRenderer

    @FontDetails(fontName = "Roboto Medium", fontSize = 40)
    @JvmStatic
    lateinit var font40: GameFontRenderer

    @FontDetails(fontName = "Roboto Bold", fontSize = 180)
    @JvmStatic
    lateinit var fontBold180: GameFontRenderer

    private val CUSTOM_FONT_RENDERERS = HashMap<FontInfo, FontRenderer>()

    @JvmStatic
    fun loadFonts() {
        val l = System.currentTimeMillis()
        LOGGER.info("Loading Fonts.")

        downloadFonts()
        font35 = GameFontRenderer(getFont("Roboto-Medium.ttf", 35))
        font40 = GameFontRenderer(getFont("Roboto-Medium.ttf", 40))
        fontBold180 = GameFontRenderer(getFont("Roboto-Bold.ttf", 180))

        try {
            CUSTOM_FONT_RENDERERS.clear()
            val fontsFile = File(fontsDir, "fonts.json")
            if (fontsFile.exists()) {
                val jsonElement = JsonParser().parse(fontsFile.bufferedReader())
                if (jsonElement is JsonNull) return
                val jsonArray = jsonElement as JsonArray
                for (element in jsonArray) {
                    if (element is JsonNull) return
                    val fontObject = element as JsonObject
                    val font = getFont(fontObject["fontFile"].asString, fontObject["fontSize"].asInt)
                    CUSTOM_FONT_RENDERERS[FontInfo(font)] = GameFontRenderer(font)
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

        LOGGER.info("Loaded Fonts. (" + (System.currentTimeMillis() - l) + "ms)")
    }

    private fun downloadFonts() {
        try {
            val outputFile = File(fontsDir, "roboto.zip")
            if (!outputFile.exists()) {
                LOGGER.info("Downloading fonts...")
                download("$CLIENT_CLOUD/fonts/Roboto.zip", outputFile)
                LOGGER.info("Extract fonts...")
                extractZip(outputFile.path, fontsDir.path)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun getFontRenderer(name: String, size: Int): FontRenderer {
        for (field in Fonts::class.java.declaredFields) {
            try {
                field.isAccessible = true
                val obj = field[null]
                if (obj is FontRenderer) {
                    val fontDetails = field.getAnnotation(FontDetails::class.java)
                    if (fontDetails.fontName == name && fontDetails.fontSize == size) return obj
                }
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
        }
        return CUSTOM_FONT_RENDERERS.getOrDefault(FontInfo(name, size), minecraftFont)
    }

    @JvmStatic
    fun getFontDetails(fontRenderer: FontRenderer): FontInfo? {
        for (field in Fonts::class.java.declaredFields) {
            try {
                field.isAccessible = true
                val obj = field[null]
                if (obj == fontRenderer) {
                    val fontDetails = field.getAnnotation(FontDetails::class.java)
                    return FontInfo(fontDetails.fontName, fontDetails.fontSize)
                }
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
        }
        for ((key, value) in CUSTOM_FONT_RENDERERS) {
            if (value === fontRenderer) return key
        }
        return null
    }

    @JvmStatic
    val fonts: List<FontRenderer>
        get() {
            val fonts: MutableList<FontRenderer> = ArrayList()
            for (fontField in Fonts::class.java.declaredFields) {
                try {
                    fontField.isAccessible = true
                    val fontObj = fontField[null]
                    if (fontObj is FontRenderer) fonts.add(fontObj)
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                }
            }
            fonts.addAll(CUSTOM_FONT_RENDERERS.values)
            return fonts
        }

    private fun getFont(fontName: String, size: Int): Font {
        return try {
            val inputStream = Files.newInputStream(File(fontsDir, fontName).toPath())
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
            val zipInputStream = ZipInputStream(Files.newInputStream(Paths.get(zipFile)))
            var zipEntry = zipInputStream.nextEntry
            while (zipEntry != null) {
                val newFile = File(outputFolder + File.separator + zipEntry.name)
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

    class FontInfo(val name: String?, val fontSize: Int) {

        constructor(font: Font) : this(font.name, font.size)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false

            val fontInfo = other as FontInfo

            return fontSize == fontInfo.fontSize && name == fontInfo.name
        }

        override fun hashCode(): Int {
            var result = name?.hashCode() ?: 0
            result = 31 * result + fontSize
            return result
        }
    }
}