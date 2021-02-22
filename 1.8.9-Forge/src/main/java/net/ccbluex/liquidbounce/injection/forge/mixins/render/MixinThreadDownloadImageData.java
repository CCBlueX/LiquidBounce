package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

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
	private static final ExecutorService downloaders = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("Texture Downloader #%d").setDaemon(true).build());
	@Shadow
	@Final
	private static Logger logger;

	@Shadow
	@Final
	private File cacheFile;

	@Shadow
	@Final
	private String imageUrl;

	@Shadow
	@Final
	private IImageBuffer imageBuffer;

	@SuppressWarnings("unused")
	@Shadow
	private Thread imageThread;

	@Shadow
	public abstract void setBufferedImage(BufferedImage bufferedImageIn);

	/**
	 * @author eric0210
	 * @reason Use ExecutorService instead of thread creation & Make able to support more protocols (as file://)
	 */
	@Overwrite
	protected void loadTextureFromServer()
	{
		downloaders.execute(() ->
		{
			logger.debug("Downloading http texture from url \"{}\" to cache file \"{}\"", imageUrl, cacheFile);

			URLConnection urlconnection = null;

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
		});

		// Create a dummy thread and save imageThread to prevent false operation in default code

		// noinspection InstantiatingAThreadWithDefaultRunMethod
		imageThread = new Thread();
	}
}
