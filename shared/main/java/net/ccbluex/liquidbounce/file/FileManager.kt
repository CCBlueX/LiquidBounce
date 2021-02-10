/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.file

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.LiquidBounce.background
import net.ccbluex.liquidbounce.LiquidBounce.isStarting
import net.ccbluex.liquidbounce.file.configs.*
import net.ccbluex.liquidbounce.injection.backend.Backend
import net.ccbluex.liquidbounce.utils.ClientUtils.logger
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.WorkerUtils
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.io.File
import java.io.FileInputStream
import javax.imageio.ImageIO

@SideOnly(Side.CLIENT)
class FileManager : MinecraftInstance()
{
	val dir = File(mc.dataDir, LiquidBounce.CLIENT_NAME + "-" + Backend.MINECRAFT_VERSION_MAJOR + "." + Backend.MINECRAFT_VERSION_MINOR)

	@JvmField
	val fontsDir = File(dir, "fonts")
	val settingsDir = File(dir, "settings")

	val modulesConfig: FileConfig = ModulesConfig(File(dir, "modules.json"))

	@JvmField
	val valuesConfig: FileConfig = ValuesConfig(File(dir, "values.json"))

	val clickGuiConfig: FileConfig = ClickGuiConfig(File(dir, "clickgui.json"))

	@JvmField
	val accountsConfig = AccountsConfig(File(dir, "accounts.json"))

	@JvmField
	val friendsConfig = FriendsConfig(File(dir, "friends.json"))

	val xrayConfig: FileConfig = XRayConfig(File(dir, "xray-blocks.json"))
	val hudConfig: FileConfig = HudConfig(File(dir, "hud.json"))
	val shortcutsConfig: FileConfig = ShortcutsConfig(File(dir, "shortcuts.json"))
	val backgroundFile = File(dir, "userbackground.png")

	@JvmField
	var firstStart = false

	/**
	 * Setup folder
	 */
	private fun setupFolder()
	{
		if (!dir.exists())
		{
			dir.mkdir()
			firstStart = true
		}
		if (!fontsDir.exists()) fontsDir.mkdir()
		if (!settingsDir.exists()) settingsDir.mkdir()
	}

	// /**
	//  * Load all configs in file manager
	//  */
	// fun loadAllConfigs()
	// {
	// 	javaClass.declaredFields.asSequence().filter { it.type == FileConfig::class.java }.forEach {
	// 		try
	// 		{
	// 			if (!it.isAccessible) it.isAccessible = true
	// 			val fileConfig = it[this] as FileConfig
	// 			loadConfig(fileConfig)
	// 		}
	// 		catch (e: IllegalAccessException)
	// 		{
	// 			logger.error("Failed to load config file of field {}.", it.name, e)
	// 		}
	// 	}
	// }

	/**
	 * Save all configs in file manager
	 */
	fun saveAllConfigs()
	{
		javaClass.declaredFields.asSequence().filter { it.type == FileConfig::class.java }.forEach {
			try
			{
				if (!it.isAccessible) it.isAccessible = true
				val fileConfig = it[this] as FileConfig
				saveConfig(fileConfig)
			}
			catch (e: IllegalAccessException)
			{
				logger.error("[FileManager] Failed to save config file of field {}.", it.name, e)
			}
		}
	}

	/**
	 * Load background for background
	 */
	private fun loadBackground()
	{
		if (backgroundFile.exists()) try
		{
			val bufferedImage = ImageIO.read(FileInputStream(backgroundFile)) ?: return
			background = classProvider.createResourceLocation(LiquidBounce.CLIENT_NAME.toLowerCase() + "/background.png")
			mc.textureManager.loadTexture(background!!, classProvider.createDynamicTexture(bufferedImage))
			logger.info("[FileManager] Loaded background.")
		}
		catch (e: Exception)
		{
			logger.error("[FileManager] Failed to load background.", e)
		}
	}

	companion object
	{
		val PRETTY_GSON: Gson = GsonBuilder().setPrettyPrinting().create()

		/**
		 * Load a list of configs
		 *
		 * @param configs
		 * list
		 */
		fun loadConfigs(vararg configs: FileConfig)
		{
			for (fileConfig in configs) loadConfig(fileConfig)
		}

		/**
		 * Load one config
		 *
		 * @param config
		 * to load
		 */
		fun loadConfig(config: FileConfig)
		{

			if (!config.hasConfig())
			{
				logger.info("[FileManager] Skipped loading config: {}.", config.file.name)
				saveConfig(config, true)
				return
			}

			// To minimize overheads caused by saving config, use workers instead of directly saving it
			WorkerUtils.workers.submit {
				try
				{
					val nanoTime = System.nanoTime()

					config.loadConfig()

					logger.info("[FileManager] Loaded config: {}. Took {}.", config.file.name, TimeUtils.nanosecondsToString(System.nanoTime() - nanoTime))
				}
				catch (t: Throwable)
				{
					logger.error("[FileManager] Failed to load config file: {}.", config.file.name, t)
				}
			}
		}

		//	/**
		//	 * Save a list of configs
		//	 *
		//	 * @param configs
		//	 *                list
		//	 */
		//	public static final void saveConfigs(final FileConfig... configs)
		//	{
		//		for (final FileConfig fileConfig : configs)
		//			saveConfig(fileConfig);
		//	}

		/**
		 * Save one config
		 *
		 * @param config
		 * to save
		 */
		fun saveConfig(config: FileConfig)
		{
			saveConfig(config, false)
		}

		/**
		 * Save one config
		 *
		 * @param config
		 * to save
		 * @param ignoreStarting
		 * check starting
		 */
		private fun saveConfig(config: FileConfig, ignoreStarting: Boolean)
		{
			if (!ignoreStarting && isStarting) return

			// To minimize overheads caused by saving config, use workers instead of directly saving it
			WorkerUtils.workers.submit {
				try
				{
					val nanoTime = System.nanoTime()

					if (!config.hasConfig()) config.createConfig()

					config.saveConfig()

					logger.info("[FileManager] Saved config: \"{}\". Took {}.", config.file.name, TimeUtils.nanosecondsToString(System.nanoTime() - nanoTime))
				}
				catch (t: Throwable)
				{
					// TODO: Create Back-up
					logger.error("[FileManager] Failed to save config file: \"{}\". A back-up created with name \"{}\".", config.file.name, t)
				}
			}
		}
	}

	/**
	 * Constructor of file manager Setup everything important
	 */
	init
	{
		setupFolder()
		loadBackground()
	}
}
