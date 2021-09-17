/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.font

import com.google.gson.*
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IFontRenderer
import net.ccbluex.liquidbounce.utils.ClientUtils.logger
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.misc.HttpUtils.download
import net.ccbluex.liquidbounce.utils.misc.MiscUtils.createBufferedFileReader
import net.ccbluex.liquidbounce.utils.misc.MiscUtils.createBufferedFileWriter
import net.ccbluex.liquidbounce.utils.runAsync
import net.ccbluex.liquidbounce.utils.timer.TimeUtils.nanosecondsToString
import java.awt.Font
import java.io.*
import java.util.zip.ZipInputStream

// TODO: Customizable main font (make can choose between roboto-medium and others)
object Fonts : MinecraftInstance()
{
	@FontDetails(fontName = "Minecraft Font")
	val minecraftFont = mc.fontRendererObj

	@FontDetails(fontName = "Roboto Medium", fontSize = 35)
	lateinit var font35: IFontRenderer

	@FontDetails(fontName = "Roboto Medium", fontSize = 40)
	lateinit var font40: IFontRenderer

	@FontDetails(fontName = "Roboto Bold", fontSize = 180)
	lateinit var fontBold180: IFontRenderer

	private val CUSTOM_FONT_RENDERERS = HashMap<FontInfo, IFontRenderer>()

	fun loadFonts()
	{
		val nanoTime = System.nanoTime()

		logger.info("Loading Fonts.")

		// TODO: Asynchronize

		downloadFonts()

		val provider = classProvider

		font35 = provider.wrapFontRenderer(GameFontRenderer(getFont("Roboto-Medium.ttf", 35)))
		font40 = provider.wrapFontRenderer(GameFontRenderer(getFont("Roboto-Medium.ttf", 40)))
		fontBold180 = provider.wrapFontRenderer(GameFontRenderer(getFont("Roboto-Bold.ttf", 180)))

		try
		{
			CUSTOM_FONT_RENDERERS.clear()
			val fontsFile = File(LiquidBounce.fileManager.fontsDir, "fonts.json")
			if (fontsFile.exists())
			{
				val jsonElement = JsonParser().parse(createBufferedFileReader(fontsFile))

				if (jsonElement is JsonNull) return

				val jsonArray = jsonElement as? Iterable<*> ?: return

				for (element in jsonArray)
				{
					if (element is JsonNull) return

					val fontObject = element as? JsonObject ?: continue

					val font = getFont(fontObject["fontFile"].asString, fontObject["fontSize"].asInt)

					CUSTOM_FONT_RENDERERS[FontInfo(font)] = provider.wrapFontRenderer(GameFontRenderer(font))
				}
			}
			else
			{
				fontsFile.createNewFile()

				val writer = createBufferedFileWriter(fontsFile)
				writer.write(GsonBuilder().setPrettyPrinting().create().toJson(JsonArray()) + System.lineSeparator())
				writer.close()
			}
		}
		catch (e: Exception)
		{
			logger.error("Can't parse fonts.json", e)
		}

		logger.info("Loaded Fonts. Took {}.", nanosecondsToString(System.nanoTime() - nanoTime))
	}

	private fun downloadFonts()
	{
		val outputFile = File(LiquidBounce.fileManager.fontsDir, "roboto.zip")

		if (!outputFile.exists()) runAsync {
			try
			{
				logger.info("Downloading fonts...")

				download(LiquidBounce.CLIENT_CLOUD + "/fonts/Roboto.zip", outputFile)

				logger.info("Extract fonts...")

				extractZip(outputFile.path, LiquidBounce.fileManager.fontsDir.path)
			}
			catch (e: IOException)
			{
				logger.error("Can't download fonts fron cloud", e)
			}
		}
	}

	fun getFontRenderer(name: String, size: Int): IFontRenderer
	{
		for (field in Fonts::class.java.declaredFields) try
		{
			field.isAccessible = true

			val fontRenderer = field[null]

			if (fontRenderer is IFontRenderer)
			{
				val fontDetails = field.getAnnotation(FontDetails::class.java) ?: continue

				if (fontDetails.fontName == name && fontDetails.fontSize == size) return fontRenderer
			}
		}
		catch (e: Exception)
		{
			logger.error("Unexpected exception occurred while reading details about default fonts declared in Fonts.class", e)
		}

		return CUSTOM_FONT_RENDERERS.getOrDefault(FontInfo(name, size), minecraftFont)
	}

	fun getFontDetails(fontRenderer: IFontRenderer): FontInfo?
	{
		for (field in Fonts::class.java.declaredFields) try
		{
			field.isAccessible = true

			val fieldValue = field[null]

			if (fontRenderer == fieldValue)
			{
				val fontDetails = field.getAnnotation(FontDetails::class.java) ?: continue

				return FontInfo(fontDetails.fontName, fontDetails.fontSize)
			}
		}
		catch (e: Exception)
		{
			logger.error("Unexpected exception occurred while reading details about default fonts declared in Fonts.class", e)
		}

		return CUSTOM_FONT_RENDERERS.entries.firstOrNull { fontRenderer == it.value }?.key
	}

	val fonts: List<IFontRenderer>
		get()
		{
			val fonts: MutableList<IFontRenderer> = ArrayList()

			for (fontField in Fonts::class.java.declaredFields) try
			{
				fontField.isAccessible = true

				val fontObj = fontField[null]

				if (fontObj is IFontRenderer) fonts.add(fontObj)
			}
			catch (e: IllegalAccessException)
			{
				logger.error("Unexpected exception occurred while reading details about default fonts declared in Fonts.class", e)
			}

			fonts.addAll(CUSTOM_FONT_RENDERERS.values)

			return fonts
		}

	private fun getFont(fontName: String, size: Int): Font
	{
		return try
		{
			val inputStream: InputStream = FileInputStream(File(LiquidBounce.fileManager.fontsDir, fontName))
			var awtClientFont = Font.createFont(Font.TRUETYPE_FONT, inputStream)

			awtClientFont = awtClientFont.deriveFont(Font.PLAIN, size.toFloat())
			inputStream.close()
			awtClientFont
		}
		catch (e: Exception)
		{
			// noinspection StringConcatenationArgumentToLogCall
			logger.warn("Can't load font named $fontName with size $size", e)
			Font("default", Font.PLAIN, size)
		}
	}

	private fun extractZip(zipFile: String, outputFolder: String)
	{
		val buffer: ByteArray = try
		{
			ByteArray(1024)
		}
		catch (err: OutOfMemoryError)
		{
			logger.error("Failed to extract the fonts: Buffer allocation failed", err)
			throw err
		}

		try
		{
			val folder = File(outputFolder)

			if (!folder.exists()) folder.mkdir()

			val zipInputStream = ZipInputStream(FileInputStream(zipFile))
			var zipEntry = zipInputStream.nextEntry

			while (zipEntry != null)
			{
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
		}
		catch (e: IOException)
		{
			// noinspection StringConcatenationArgumentToLogCall
			logger.error("Failed to extract the fonts: $e", e)
		}
	}

	class FontInfo(val name: String, val fontSize: Int)
	{
		constructor(font: Font) : this(font.name, font.size)

		override fun equals(other: Any?): Boolean
		{
			if (this === other) return true

			if (other == null || javaClass != other.javaClass) return false

			val fontInfo = other as FontInfo
			return fontSize == fontInfo.fontSize && name == fontInfo.name
		}

		override fun hashCode(): Int = 31 * name.hashCode() + fontSize
	}
}
