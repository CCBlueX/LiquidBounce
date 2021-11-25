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
import net.ccbluex.liquidbounce.utils.misc.HttpUtils
import net.minecraftforge.common.ForgeVersion
import org.lwjgl.Sys
import org.lwjgl.opengl.Display
import org.spongepowered.asm.mixin.MixinEnvironment
import org.spongepowered.asm.util.VersionNumber

// Mixin TODO: 목표 메서드 내의 *두 개 이상의* 로컬 변수를 가져올 수 있는 ModifyVariable 비슷한 역할 하는 injector 만들 수 있으면 만들어보기 (ModifyArgs 처럼)
// ㄴ 여려 개의 @ModifyVariable 인젝션과, 지역 변수 대신 전역 변수를 사용하는 방안으로 해결 가능하다.
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
	val title: String
		get()
		{
			val arr = mutableListOf("$CLIENT_NAME b$CLIENT_VERSION by $CLIENT_CREATOR", "Kotlin ${KotlinVersion.CURRENT}", "Nashorn ${Version.version()}", "Backend $MINECRAFT_VERSION", "Source https://github.com/hsheric0210/LiquidBounce")
			if (IN_DEV) arr += "DEVELOPMENT BUILD"
			currentProgress?.let { arr += it }
			return arr.joinToString(" | ")
		}

	fun updateProgress(progress: String?)
	{
		currentProgress = progress
		Display.setTitle(title)
	}

	/**
	 * Execute if client will be started
	 */
	fun startClient()
	{
		isStarting = true

		ClientUtils.logger.info("Starting $CLIENT_NAME for $MINECRAFT_VERSION b$CLIENT_VERSION, by $CLIENT_CREATOR")

		ClientUtils.logger.info("* Java ${System.getProperty("java.vm.name")} ${System.getProperty("java.vm.version")}")
		ClientUtils.logger.info("* Kotlin ${KotlinVersion.CURRENT}")
		if (hasForge())
		{
			ClientUtils.logger.info("* MinecraftForge ${ForgeVersion.getVersion()}")
			ClientUtils.logger.info("* MCP ${ForgeVersion.mcpVersion}")
		}
		ClientUtils.logger.info("* Mixin ${VersionNumber.parse(MixinEnvironment.getCurrentEnvironment().version)}")
		ClientUtils.logger.info("* Nashorn ${Version.fullVersion()}")
		for (ver in io.netty.util.Version.identify().values) ClientUtils.logger.info("* Netty $ver")
		ClientUtils.logger.info("* LWJGL ${Sys.getVersion()}")

		// Initialize file manager
		updateProgress("Initializing File Manager")
		fileManager = FileManager()

		// Initialize event manager
		updateProgress("Initializing Event Manager")
		eventManager = EventManager()

		// Register special event listeners
		updateProgress("Initializing Special Event Listeners")
		eventManager.registerListener(RotationUtils())
		eventManager.registerListener(AntiModDisable())
		eventManager.registerListener(BungeeCordSpoof())
		eventManager.registerListener(DonatorCape())
		eventManager.registerListener(InventoryUtils())
		eventManager.registerListener(LocationCache())

		// Init Discord RPC
		updateProgress("Initializing Discord RPC")
		clientRichPresence = ClientRichPresence()

		// Create command manager
		updateProgress("Initializing Command Manager")
		commandManager = CommandManager()

		// Load client fonts
		updateProgress("Loading Fonts")
		Fonts.loadFonts()

		// Setup module manager and register modules
		updateProgress("Initializing Module Manager")
		moduleManager = ModuleManager()
		moduleManager.registerModules()

		try
		{
			// Remapper
			updateProgress("Loading Remapper")
			loadSrg()

			// ScriptManager
			updateProgress("Initializing Script Manager")
			scriptManager = ScriptManager()

			updateProgress("Loading Scripts")
			scriptManager.loadScripts()

			updateProgress("Enabling Scripts")
			scriptManager.enableScripts()
		}
		catch (throwable: Throwable)
		{
			ClientUtils.logger.error("Failed to load scripts.", throwable)
		}

		updateProgress("Registering Commands")

		// Register commands
		commandManager.registerCommands()

		// Load configs
		updateProgress("Loading Configurations")
		FileManager.loadConfigs(fileManager.modulesConfig, fileManager.valuesConfig, fileManager.accountsConfig, fileManager.friendsConfig, fileManager.xrayConfig, fileManager.shortcutsConfig)

		// ClickGUI
		updateProgress("Initializing ClickGUI")
		clickGui = ClickGui()

		updateProgress("Loading ClickGUI Configuration")
		FileManager.loadConfig(fileManager.clickGuiConfig)

		// Tabs (Only for Forge!)
		if (hasForge())
		{
			updateProgress("Registering Tabs")

			BlocksTab()
			ExploitsTab()
			HeadsTab()
		}

		// Register capes service
		updateProgress("Registering Cape Service")
		try
		{
			registerCapeService()
		}
		catch (throwable: Throwable)
		{
			ClientUtils.logger.error("Failed to register cape service", throwable)
		}

		// Set HUD
		updateProgress("Creating HUD")
		hud = createDefault()

		updateProgress("Loading HUD Configuration")
		FileManager.loadConfig(fileManager.hudConfig)

		// Disable optifine fastrender
		// updateProgress("Disabling OptiFine Fast-Render Feature")
		// ClientUtils.disableFastRender()

		try
		{
			updateProgress("Checking for updates")

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
		GuiAltManager.loadGenerators()

		// Setup Discord RPC
		if (clientRichPresence.showRichPresenceValue)
		{
			updateProgress("Establishing Discord RPC")
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

		updateProgress("Done") // Done

		runAsyncDelayed(5000L) {
			runSync { updateProgress(null) }
		}
	}

	/**
	 * Execute if client will be stopped
	 */
	fun stopClient()
	{
		// Call client shutdown
		updateProgress("Handling Shutdown Event")
		eventManager.callEvent(ClientShutdownEvent())

		// Save all available configs
		updateProgress("Saving All Configurations")
		fileManager.saveAllConfigs()

		// Shutdown discord rpc
		updateProgress("Shutting Down Discord RPC")
		clientRichPresence.shutdown()

		updateProgress(null)
	}
}
