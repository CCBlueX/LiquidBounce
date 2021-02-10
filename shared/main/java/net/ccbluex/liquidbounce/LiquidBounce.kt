/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.api.Wrapper
import net.ccbluex.liquidbounce.api.minecraft.util.IResourceLocation
import net.ccbluex.liquidbounce.cape.CapeAPI.registerCapeService
import net.ccbluex.liquidbounce.event.ClientShutdownEvent
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.special.*
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

	// Discord RPC
	lateinit var clientRichPresence: ClientRichPresence

	lateinit var wrapper: Wrapper

	/**
	 * Execute if client will be started
	 */
	fun startClient()
	{
		isStarting = true

		ClientUtils.logger.info("Starting $CLIENT_NAME for $MINECRAFT_VERSION b$CLIENT_VERSION, by $CLIENT_CREATOR")

		// Create file manager
		fileManager = FileManager()

		// Crate event manager
		eventManager = EventManager()

		// Register listeners
		eventManager.registerListener(RotationUtils())
		eventManager.registerListener(AntiModDisable())
		eventManager.registerListener(BungeeCordSpoof())
		eventManager.registerListener(DonatorCape())
		eventManager.registerListener(InventoryUtils())

		// Init Discord RPC
		clientRichPresence = ClientRichPresence()

		// Create command manager
		commandManager = CommandManager()

		// Load client fonts
		Fonts.loadFonts()

		// Setup module manager and register modules
		moduleManager = ModuleManager()
		moduleManager.registerModules()

		try
		{ // Remapper
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

		// Register commands
		commandManager.registerCommands()

		// Load configs
		FileManager.loadConfigs(fileManager.modulesConfig, fileManager.valuesConfig, fileManager.accountsConfig, fileManager.friendsConfig, fileManager.xrayConfig, fileManager.shortcutsConfig)

		// ClickGUI
		clickGui = ClickGui()
		FileManager.loadConfig(fileManager.clickGuiConfig)

		// Tabs (Only for Forge!)
		if (hasForge())
		{
			BlocksTab()
			ExploitsTab()
			HeadsTab()
		}

		// Register capes service
		try
		{
			registerCapeService()
		}
		catch (throwable: Throwable)
		{
			ClientUtils.logger.error("Failed to register cape service", throwable)
		}

		// Set HUD
		hud = createDefault()
		FileManager.loadConfig(fileManager.hudConfig)

		// Disable optifine fastrender
		ClientUtils.disableFastRender()

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

		// Load generators
		GuiAltManager.loadGenerators()

		// Setup Discord RPC
		if (clientRichPresence.showRichPresenceValue)
		{
			WorkerUtils.workers.submit {
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

	@JvmStatic
	val title: String = "$CLIENT_NAME b$CLIENT_VERSION by $CLIENT_CREATOR | Backend version $MINECRAFT_VERSION${if (IN_DEV) " | DEVELOPMENT BUILD" else ""}" // TODO: Add more details
}
