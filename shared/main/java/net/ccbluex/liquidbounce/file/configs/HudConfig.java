/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.file.configs;

import java.io.*;
import java.nio.charset.StandardCharsets;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.file.FileConfig;
import net.ccbluex.liquidbounce.ui.client.hud.Config;

import net.ccbluex.liquidbounce.utils.misc.MiscUtils;
import org.apache.commons.io.FileUtils;

public class HudConfig extends FileConfig
{

	/**
	 * Constructor of config
	 *
	 * @param file
	 *             of config
	 */
	public HudConfig(final File file)
	{
		super(file);
	}

	/**
	 * Load config from file
	 *
	 * @throws IOException
	 */
	@Override
	protected void loadConfig() throws IOException
	{
		LiquidBounce.hud.clearElements();
		LiquidBounce.hud = new Config(FileUtils.readFileToString(getFile(), StandardCharsets.UTF_8)).toHUD();
	}

	/**
	 * Save config to file
	 *
	 * @throws IOException
	 */
	@Override
	protected void saveConfig() throws IOException
	{
		final BufferedWriter writer = MiscUtils.createBufferedFileWriter(getFile());
		writer.write(new Config(LiquidBounce.hud).toJson() + System.lineSeparator());
		writer.close();
	}
}
