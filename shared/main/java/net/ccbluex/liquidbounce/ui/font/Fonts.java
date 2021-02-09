/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.font;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.gson.*;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IFontRenderer;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.ccbluex.liquidbounce.utils.MinecraftInstance;
import net.ccbluex.liquidbounce.utils.WorkerUtils;
import net.ccbluex.liquidbounce.utils.misc.HttpUtils;
import net.ccbluex.liquidbounce.utils.misc.MiscUtils;
import net.ccbluex.liquidbounce.utils.timer.TimeUtils;

// TODO: Customizable main font (make can choose between roboto-medium and others)
public class Fonts extends MinecraftInstance
{
	@FontDetails(fontName = "Minecraft Font")
	public static final IFontRenderer minecraftFont = mc.getFontRendererObj();

	private static final HashMap<FontInfo, IFontRenderer> CUSTOM_FONT_RENDERERS = new HashMap<>();

	@FontDetails(fontName = "Roboto Medium", fontSize = 35)
	public static IFontRenderer font35;

	@FontDetails(fontName = "Roboto Medium", fontSize = 40)
	public static IFontRenderer font40;

	@FontDetails(fontName = "Roboto Medium", fontSize = 60)
	public static IFontRenderer font60;

	@FontDetails(fontName = "Roboto Bold", fontSize = 180)
	public static IFontRenderer fontBold180;

	@SuppressWarnings("unchecked")
	public static void loadFonts()
	{
		final long nanoTime = System.nanoTime();

		ClientUtils.getLogger().info("Loading Fonts.");

		downloadFonts();

		font35 = classProvider.wrapFontRenderer(new GameFontRenderer(getFont("Roboto-Medium.ttf", 35)));
		font40 = classProvider.wrapFontRenderer(new GameFontRenderer(getFont("Roboto-Medium.ttf", 40)));
		font60 = classProvider.wrapFontRenderer(new GameFontRenderer(getFont("Roboto-Medium.ttf", 60)));
		fontBold180 = classProvider.wrapFontRenderer(new GameFontRenderer(getFont("Roboto-Bold.ttf", 180)));

		try
		{
			CUSTOM_FONT_RENDERERS.clear();

			final File fontsFile = new File(LiquidBounce.fileManager.fontsDir, "fonts.json");

			if (fontsFile.exists())
			{
				final JsonElement jsonElement = new JsonParser().parse(MiscUtils.createBufferedFileReader(fontsFile));

				if (jsonElement instanceof JsonNull)
					return;

				final Iterable<JsonElement> jsonArray = (Iterable<JsonElement>) jsonElement;

				for (final JsonElement element : jsonArray)
				{
					if (element instanceof JsonNull)
						return;

					final JsonObject fontObject = (JsonObject) element;

					final Font font = getFont(fontObject.get("fontFile").getAsString(), fontObject.get("fontSize").getAsInt());

					CUSTOM_FONT_RENDERERS.put(new FontInfo(font), classProvider.wrapFontRenderer(new GameFontRenderer(font)));
				}
			}
			else
			{
				fontsFile.createNewFile();

				final BufferedWriter writer = MiscUtils.createBufferedFileWriter(fontsFile);
				writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(new JsonArray()) + System.lineSeparator());
				writer.close();
			}
		}
		catch (final Exception e)
		{
			ClientUtils.getLogger().error("Can't parse fonts.json", e);
		}

		ClientUtils.getLogger().info("Loaded Fonts. Took {}.", TimeUtils.nanosecondsToString(System.nanoTime() - nanoTime));
	}

	private static void downloadFonts()
	{
		final File outputFile = new File(LiquidBounce.fileManager.fontsDir, "roboto.zip");

		// TEST: Download fonts in a worker thread
		if (!outputFile.exists())
			WorkerUtils.getWorkers().submit(() ->
			{
				try
				{
					ClientUtils.getLogger().info("Downloading fonts...");
					HttpUtils.download(LiquidBounce.CLIENT_CLOUD + "/fonts/Roboto.zip", outputFile);
					ClientUtils.getLogger().info("Extract fonts...");
					extractZip(outputFile.getPath(), LiquidBounce.fileManager.fontsDir.getPath());
				}
				catch (final IOException e)
				{
					ClientUtils.getLogger().error("Can't download fonts fron cloud", e);
				}
			});
	}

	public static IFontRenderer getFontRenderer(final String name, final int size)
	{
		for (final Field field : Fonts.class.getDeclaredFields())
			try
			{
				field.setAccessible(true);

				final Object o = field.get(null);

				if (o instanceof IFontRenderer)
				{
					final FontDetails fontDetails = field.getAnnotation(FontDetails.class);

					if (fontDetails.fontName().equals(name) && fontDetails.fontSize() == size)
						return (IFontRenderer) o;
				}
			}
			catch (final IllegalAccessException e)
			{
				ClientUtils.getLogger().error("Unexpected exception occurred while reading details about default fonts declared in Fonts.class", e);
			}

		return CUSTOM_FONT_RENDERERS.getOrDefault(new FontInfo(name, size), minecraftFont);
	}

	public static FontInfo getFontDetails(final IFontRenderer fontRenderer)
	{
		for (final Field field : Fonts.class.getDeclaredFields())
			try
			{
				field.setAccessible(true);

				final Object o = field.get(null);

				if (o.equals(fontRenderer))
				{
					final FontDetails fontDetails = field.getAnnotation(FontDetails.class);

					return new FontInfo(fontDetails.fontName(), fontDetails.fontSize());
				}
			}
			catch (final IllegalAccessException e)
			{
				ClientUtils.getLogger().error("Unexpected exception occurred while reading details about default fonts declared in Fonts.class", e);
			}

		return CUSTOM_FONT_RENDERERS.entrySet().stream().filter(entry -> entry.getValue() == fontRenderer).findFirst().map(Entry::getKey).orElse(null);
	}

	public static List<IFontRenderer> getFonts()
	{
		final List<IFontRenderer> fonts = new ArrayList<>();

		for (final Field fontField : Fonts.class.getDeclaredFields())
			try
			{
				fontField.setAccessible(true);

				final Object fontObj = fontField.get(null);

				if (fontObj instanceof IFontRenderer)
					fonts.add((IFontRenderer) fontObj);
			}
			catch (final IllegalAccessException e)
			{
				ClientUtils.getLogger().error("Unexpected exception occurred while reading details about default fonts declared in Fonts.class", e);
			}

		fonts.addAll(CUSTOM_FONT_RENDERERS.values());

		return fonts;
	}

	private static Font getFont(final String fontName, final int size)
	{
		try
		{
			final InputStream inputStream = new FileInputStream(new File(LiquidBounce.fileManager.fontsDir, fontName));
			Font awtClientFont = Font.createFont(Font.TRUETYPE_FONT, inputStream);
			awtClientFont = awtClientFont.deriveFont(Font.PLAIN, size);
			inputStream.close();
			return awtClientFont;
		}
		catch (final Exception e)
		{
			// noinspection StringConcatenationArgumentToLogCall
			ClientUtils.getLogger().warn("Can't load font named " + fontName + " with size " + size, e);

			return new Font("default", Font.PLAIN, size);
		}
	}

	private static void extractZip(final String zipFile, final String outputFolder)
	{
		final byte[] buffer;

		try
		{
			buffer = new byte[1024];
		}
		catch (final OutOfMemoryError err)
		{
			ClientUtils.getLogger().error("Failed to extract the fonts: Buffer allocation failed", err);
			throw err;
		}

		try
		{
			final File folder = new File(outputFolder);

			if (!folder.exists())
				folder.mkdir();

			final ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile));

			ZipEntry zipEntry = zipInputStream.getNextEntry();
			while (zipEntry != null)
			{
				final File newFile = new File(outputFolder + File.separator + zipEntry.getName());
				new File(newFile.getParent()).mkdirs();

				final FileOutputStream fileOutputStream = new FileOutputStream(newFile);

				int i;
				while ((i = zipInputStream.read(buffer)) > 0)
					fileOutputStream.write(buffer, 0, i);

				fileOutputStream.close();
				zipEntry = zipInputStream.getNextEntry();
			}

			zipInputStream.closeEntry();
			zipInputStream.close();
		}
		catch (final IOException e)
		{
			// noinspection StringConcatenationArgumentToLogCall
			ClientUtils.getLogger().error("Failed to extract the fonts: " + e, e);
		}
	}

	public static class FontInfo
	{
		private final String name;
		private final int fontSize;

		public FontInfo(final String name, final int fontSize)
		{
			this.name = name;
			this.fontSize = fontSize;
		}

		public FontInfo(final Font font)
		{
			this(font.getName(), font.getSize());
		}

		public String getName()
		{
			return name;
		}

		public int getFontSize()
		{
			return fontSize;
		}

		@Override
		public boolean equals(final Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null || getClass() != obj.getClass())
				return false;

			final FontInfo fontInfo = (FontInfo) obj;

			return fontSize == fontInfo.fontSize && Objects.equals(name, fontInfo.name);
		}

		@Override
		public int hashCode()
		{
			int result = Optional.ofNullable(name).map(String::hashCode).orElse(0);
			result = 31 * result + fontSize;
			return result;
		}
	}

}
