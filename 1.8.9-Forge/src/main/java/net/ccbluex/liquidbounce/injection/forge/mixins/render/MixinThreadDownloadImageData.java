package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IImageBuffer;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.texture.TextureUtil;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ThreadDownloadImageData.class)
public abstract class MixinThreadDownloadImageData
{
	@Shadow
	@Final
	private static Logger logger;

	@Shadow
	@Final
	private static AtomicInteger threadDownloadCounter;

	@Shadow
	@Final
	private File cacheFile;

	@Shadow
	@Final
	private String imageUrl;

	@Shadow
	@Final
	private IImageBuffer imageBuffer;

	@Shadow
	private Thread imageThread;

	@Shadow
	public abstract void setBufferedImage(BufferedImage bufferedImageIn);

	/**
	 * @author eric0210
	 * @reason Upgrade thread performance & Make able to load offline cape files
	 */
	@Overwrite
	protected void loadTextureFromServer()
	{
		imageThread = new Thread(() ->
		{
			logger.debug("Downloading http texture from {} to {}", imageUrl, cacheFile);

			URLConnection urlconnection = null;

			// noinspection OverlyBroadCatchBlock
			try
			{
				// Establish the URL connection
				urlconnection = new URL(imageUrl).openConnection(Minecraft.getMinecraft().getProxy());
				urlconnection.connect();

				// Check the HTTP URL connection responce code
				if (urlconnection instanceof HttpURLConnection && ((HttpURLConnection) urlconnection).getResponseCode() / 100 != 2)
					return;

				BufferedImage image;
				if (cacheFile != null)
				{
					// Read from the connection and save to the cache file
					FileUtils.copyInputStreamToFile(urlconnection.getInputStream(), cacheFile);

					// Read image from the cache file
					image = ImageIO.read(cacheFile);
				}
				else
					image = TextureUtil.readBufferedImage(urlconnection.getInputStream()); // Read image directly from the connection

				// Parse the user skin
				if (imageBuffer != null)
					image = imageBuffer.parseUserSkin(image);

				// Update the image
				setBufferedImage(image);
			}
			catch (final IOException e)
			{
				// noinspection StringConcatenationArgumentToLogCall
				logger.error("Couldn't download texture from " + imageUrl, e);
			}
			finally
			{
				// If the connection is HTTP URL connection, disconnect it
				if (urlconnection instanceof HttpURLConnection)
					((HttpURLConnection) urlconnection).disconnect();
			}
		}, "Texture Downloader #" + threadDownloadCounter.incrementAndGet());
		imageThread.setDaemon(true);
		imageThread.start();
	}
}
