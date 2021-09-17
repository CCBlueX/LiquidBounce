/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import jdk.nashorn.internal.runtime.Version
import net.ccbluex.liquidbounce.api.Wrapper
import net.ccbluex.liquidbounce.api.minecraft.util.IResourceLocation
import net.ccbluex.liquidbounce.cape.CapeAPI.registerCapeService
import net.ccbluex.liquidbounce.event.ClientShutdownEvent
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.special.AntiModDisable
import net.ccbluex.liquidbounce.features.special.BungeeCordSpoof
import net.ccbluex.liquidbounce.features.special.ClientRichPresence
import net.ccbluex.liquidbounce.features.special.DonatorCape
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.injection.backend.Backend
import net.ccbluex.liquidbounce.script.ScriptManager
import net.ccbluex.liquidbounce.script.remapper.Remapper.loadSrg
import net.ccbluex.liquidbounce.tabs.BlocksTab
import net.ccbluex.liquidbounce.tabs.ExploitsTab
import net.ccbluex.liquidbounce.tabs.HeadsTab
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui
import net.ccbluex.liquidbounce.ui.client.hud.HUD
import net.ccbluex.liquidbounce.ui.client.hud.HUD.Companion.createDefault
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.ClassUtils.hasForge
import net.ccbluex.liquidbounce.utils.misc.HttpUtils
import net.minecraftforge.common.ForgeVersion
import org.lwjgl.opengl.Display
import org.spongepowered.asm.mixin.MixinEnvironment
import org.spongepowered.asm.util.VersionNumber

/**
 * <p>
 * ex: Add ValueGroup, ColorValue, RangeSliderValue(see Vape) and 'Show/Hide values by Mode' feature
 * </p>
 */
object LiquidBounce
{
	// Client information
	const val CLIENT_NAME = "LiquidBounce"
	const val CLIENT_VERSION = 73
	const val IN_DEV = true
	const val CLIENT_CREATOR = "CCBlueX"
	const val MINECRAFT_VERSION = Backend.MINECRAFT_VERSION
	const val CLIENT_CLOUD = "https://cloud.liquidbounce.net/LiquidBounce"

	var isStarting = false

	// Managers
	lateinit var moduleManager: ModuleManager
	lateinit var commandManager: CommandManager
	lateinit var eventManager: EventManager
	lateinit var fileManager: FileManager
	lateinit var scriptManager: ScriptManager

	// HUD & ClickGUI
	lateinit var hud: HUD

	lateinit var clickGui: ClickGui

	// Update information
	var latestVersion = 0

	// Menu Background
	var background: IResourceLocation? = null

	var currentProgress: String? = null

	// Discord RPC
	lateinit var clientRichPresence: ClientRichPresence

	lateinit var wrapper: Wrapper

	@JvmStatic
	val title: String = run {
		val arr = mutableListOf("$CLIENT_NAME b$CLIENT_VERSION by $CLIENT_CREATOR", "Kotlin ${KotlinVersion.CURRENT}", "Nashorn ${Version.version()}", "Backend $MINECRAFT_VERSION", "Source https://github.com/hsheric0210/LiquidBounce")
		if (IN_DEV) arr += "DEVELOPMENT BUILD"
		if (currentProgress != null) arr += currentProgress!!
		arr.joinToString(" | ")
	}

	fun updateProgress(progress: String?)
	{
		currentProgress = "$progress"
		Display.setTitle(title)
	}

	/**
	 * Execute if client will be started
	 */
	fun startClient()
	{
		isStarting = true

		ClientUtils.logger.info("Starting $CLIENT_NAME for $MINECRAFT_VERSION b$CLIENT_VERSION, by $CLIENT_CREATOR")
		ClientUtils.logger.info("Java: ${System.getProperty("java.vm.name")} ${System.getProperty("java.vm.version")}")
		ClientUtils.logger.info("Kotlin: ${KotlinVersion.CURRENT}")
		if (hasForge()) ClientUtils.logger.info("MinecraftForge: ${ForgeVersion.getVersion()}")
		ClientUtils.logger.info("Mixin: ${VersionNumber.parse(MixinEnvironment.getCurrentEnvironment().version)}")
		ClientUtils.logger.info("Backend: $MINECRAFT_VERSION")
		ClientUtils.logger.info("Nashorn: ${Version.fullVersion()}")
		for (ver in io.netty.util.Version.identify().values) ClientUtils.logger.info("Netty: $ver")
		ClientUtils.logger.info("Jinput: ${net.java.games.util.Version.getVersion()}")
		ClientUtils.logger.info("Jutils: ${net.java.games.util.Version.getVersion()}")

		updateProgress("Initializing FileManager")

		// Create file manager
		fileManager = FileManager()

		updateProgress("Initializing EventManager")

		// Crate event manager
		eventManager = EventManager()

		updateProgress("Initializing Special Event Listeners")

		// Register listeners
		eventManager.registerListener(RotationUtils())
		eventManager.registerListener(AntiModDisable())
		eventManager.registerListener(BungeeCordSpoof())
		eventManager.registerListener(DonatorCape())
		eventManager.registerListener(InventoryUtils())
		eventManager.registerListener(LocationCache())

		updateProgress("Initializing Discord RPC")

		// Init Discord RPC
		clientRichPresence = ClientRichPresence()

		updateProgress("Initializing CommandManager")

		// Create command manager
		commandManager = CommandManager()

		updateProgress("Loading Fonts")

		// Load client fonts
		Fonts.loadFonts()

		updateProgress("Initializing ModuleManager")

		// Setup module manager and register modules
		moduleManager = ModuleManager()
		moduleManager.registerModules()

		updateProgress("Loading Scripts")

		try
		{
			// Remapper
			loadSrg()

			// ScriptManager
			scriptManager = ScriptManager()
			scriptManager.loadScripts()
			scriptManager.enableScripts()
		}
		catch (throwable: Throwable)
		{
			ClientUtils.logger.error("Failed to load scripts.", throwable)
		}

		updateProgress("Registering Commands")

		// Register commands
		commandManager.registerCommands()

		updateProgress("Loading configs")

		// Load configs
		FileManager.loadConfigs(fileManager.modulesConfig, fileManager.valuesConfig, fileManager.accountsConfig, fileManager.friendsConfig, fileManager.xrayConfig, fileManager.shortcutsConfig)

		updateProgress("Initializing ClickGUI")

		// ClickGUI
		clickGui = ClickGui()
		FileManager.loadConfig(fileManager.clickGuiConfig)

		updateProgress("Registering Tabs")

		// Tabs (Only for Forge!)
		if (hasForge())
		{
			BlocksTab()
			ExploitsTab()
			HeadsTab()
		}

		updateProgress("Registering Cape Service")

		// Register capes service
		try
		{
			registerCapeService()
		}
		catch (throwable: Throwable)
		{
			ClientUtils.logger.error("Failed to register cape service", throwable)
		}

		updateProgress("Creating HUD")

		// Set HUD
		hud = createDefault()
		FileManager.loadConfig(fileManager.hudConfig)

		// Disable optifine fastrender
		// ClientUtils.disableFastRender()

		updateProgress("Checking for updates")

		try
		{

			// Read versions json from cloud
			val jsonObj = JsonParser().parse(HttpUtils["$CLIENT_CLOUD/versions.json"])

			// Check json is valid object and has current minecraft version
			if (jsonObj is JsonObject && jsonObj.has(MINECRAFT_VERSION)) latestVersion = jsonObj[MINECRAFT_VERSION].asInt // Get official latest client version
		}
		catch (exception: Throwable)
		{

			// Print throwable to console
			ClientUtils.logger.error("Failed to check for updates.", exception)
		}

		updateProgress("Loading Alt Generators")

		// Load generators
		GuiAltManager.loadGenerators()

		updateProgress("Establishing Discord RPC")

		// Setup Discord RPC
		if (clientRichPresence.showRichPresenceValue)
		{
			runAsync {
				try
				{
					clientRichPresence.setup()
				}
				catch (throwable: Throwable)
				{
					ClientUtils.logger.error("Failed to setup Discord RPC.", throwable)
				}
			}
		}

		// Set is starting status
		isStarting = false

		updateProgress(null) // Done
	}

	/**
	 * Execute if client will be stopped
	 */
	fun stopClient()
	{
		// Call client shutdown
		eventManager.callEvent(ClientShutdownEvent())

		// Save all available configs
		fileManager.saveAllConfigs()

		// Shutdown discord rpc
		clientRichPresence.shutdown()
	}
}
