/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class IconUtils
{

	public static ByteBuffer[] getFavicon()
	{
		try
		{
			return new ByteBuffer[]
			{
					readImageToBuffer(IconUtils.class.getResourceAsStream("/assets/minecraft/" + LiquidBounce.CLIENT_NAME.toLowerCase() + "/icon_16x16.png")),
					readImageToBuffer(IconUtils.class.getResourceAsStream("/assets/minecraft/" + LiquidBounce.CLIENT_NAME.toLowerCase() + "/icon_32x32.png"))
			};
		}
		catch (final IOException e)
		{
			ClientUtils.getLogger().error("Can't load the favicon from assets", e);
		}
		return null;
	}

	private static ByteBuffer readImageToBuffer(final InputStream imageStream) throws IOException
	{
		if (imageStream == null)
			return null;

		final BufferedImage bufferedImage = ImageIO.read(imageStream);
		final int[] rgb = bufferedImage.getRGB(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), null, 0, bufferedImage.getWidth());
		final ByteBuffer byteBuffer = ByteBuffer.allocate(4 * rgb.length);
		for (final int i : rgb)
			byteBuffer.putInt(i << 8 | i >> 24 & 255);
		byteBuffer.flip();
		return byteBuffer;
	}

	private IconUtils() {
	}
}
