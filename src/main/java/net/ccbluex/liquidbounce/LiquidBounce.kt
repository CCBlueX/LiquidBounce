/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce

import kotlinx.coroutines.*
import net.ccbluex.liquidbounce.api.ClientUpdate.gitInfo
import net.ccbluex.liquidbounce.api.loadSettings
import net.ccbluex.liquidbounce.api.messageOfTheDay
import net.ccbluex.liquidbounce.cape.CapeService
import net.ccbluex.liquidbounce.event.ClientShutdownEvent
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.EventManager.callEvent
import net.ccbluex.liquidbounce.event.EventManager.registerListener
import net.ccbluex.liquidbounce.event.StartupEvent
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.CommandManager.registerCommands
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.ModuleManager.registerModules
import net.ccbluex.liquidbounce.features.special.BungeeCordSpoof
import net.ccbluex.liquidbounce.features.special.ClientFixes
import net.ccbluex.liquidbounce.features.special.ClientRichPresence
import net.ccbluex.liquidbounce.features.special.ClientRichPresence.showRichPresenceValue
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.file.FileManager.loadAllConfigs
import net.ccbluex.liquidbounce.file.FileManager.saveAllConfigs
import net.ccbluex.liquidbounce.lang.LanguageManager.loadLanguages
import net.ccbluex.liquidbounce.script.ScriptManager
import net.ccbluex.liquidbounce.script.ScriptManager.enableScripts
import net.ccbluex.liquidbounce.script.ScriptManager.loadScripts
import net.ccbluex.liquidbounce.script.remapper.Remapper
import net.ccbluex.liquidbounce.script.remapper.Remapper.loadSrg
import net.ccbluex.liquidbounce.tabs.BlocksTab
import net.ccbluex.liquidbounce.tabs.ExploitsTab
import net.ccbluex.liquidbounce.tabs.HeadsTab
import net.ccbluex.liquidbounce.ui.client.GuiClientConfiguration.Companion.updateClientWindow
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager.Companion.loadActiveGenerators
import net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui
import net.ccbluex.liquidbounce.ui.client.hud.HUD
import net.ccbluex.liquidbounce.ui.font.Fonts.loadFonts
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.ClassUtils.hasForge
import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.ClientUtils.disableFastRender
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.render.MiniMapRegister
import net.ccbluex.liquidbounce.utils.timing.TickedActions
import kotlin.concurrent.thread

object LiquidBounce {

    // Client information
    const val CLIENT_NAME = "LiquidBounce"
    val clientVersionText = gitInfo["git.build.version"]?.toString() ?: "unknown"
    val clientVersionNumber = clientVersionText.substring(1).toIntOrNull() ?: 0 // version format: "b<VERSION>" on legacy
    val clientCommit = gitInfo["git.commit.id.abbrev"]?.let { "git-$it" } ?: "unknown"
    val clientBranch = gitInfo["git.branch"]?.toString() ?: "unknown"
    const val IN_DEV = true
    const val CLIENT_CREATOR = "CCBlueX"
    const val MINECRAFT_VERSION = "1.8.9"
    const val CLIENT_WEBSITE = "liquidbounce.net"
    const val CLIENT_CLOUD = "https://cloud.liquidbounce.net/LiquidBounce"

    val clientTitle = CLIENT_NAME + " " + clientVersionText + " " + clientCommit + "  | " + MINECRAFT_VERSION + if (IN_DEV) " | DEVELOPMENT BUILD" else ""

    var isStarting = true

    // Managers
    val moduleManager = ModuleManager
    val commandManager = CommandManager
    val eventManager = EventManager
    val fileManager = FileManager
    val scriptManager = ScriptManager

    // HUD & ClickGUI
    val hud = HUD

    val clickGui = ClickGui

    // Menu Background
    var background: Background? = null

    // Discord RPC
    val clientRichPresence = ClientRichPresence

    /**
     * Execute if client will be started
     */
    fun startClient() {
        isStarting = true

        LOGGER.info("Starting $CLIENT_NAME $clientVersionText $clientCommit, by $CLIENT_CREATOR")

        runBlocking {
            runCatching {
                async {
                    // Load languages
                    loadLanguages()

                    // Register listeners
                    registerListener(RotationUtils)
                    registerListener(ClientFixes)
                    registerListener(BungeeCordSpoof)
                    registerListener(CapeService)
                    registerListener(InventoryUtils)
                    registerListener(MiniMapRegister)
                    registerListener(TickedActions)
                    registerListener(MovementUtils)
                    registerListener(PacketUtils)
                    registerListener(TimerBalanceUtils)
                    registerListener(BPSUtils)

                    // Load client fonts
                    loadFonts()

                    // Load settings
                    loadSettings(false) {
                        LOGGER.info("Successfully loaded ${it.size} settings.")
                    }

                    // Register commands
                    registerCommands()

                    // Setup module manager and register modules
                    registerModules()

                    runCatching {
                        // Remapper
                        loadSrg()

                        if (!Remapper.mappingsLoaded) {
                            error("Failed to load SRG mappings.")
                        }

                        // ScriptManager
                        loadScripts()
                        enableScripts()
                    }.onFailure {
                        LOGGER.error("Failed to load scripts.", it)
                    }

                    // Load configs
                    loadAllConfigs()

                    // Update client window
                    updateClientWindow()

                    // Tabs (Only for Forge!)
                    if (hasForge()) {
                        BlocksTab()
                        ExploitsTab()
                        HeadsTab()
                    }

                    // Disable optifine fastrender
                    disableFastRender()

                    // Load alt generators
                    loadActiveGenerators()

                    // Load message of the day
                    messageOfTheDay?.message?.let { LOGGER.info("Message of the day: $it") }

                    // Setup Discord RPC
                    if (showRichPresenceValue) {
                        thread {
                            try {
                                clientRichPresence.setup()
                            } catch (throwable: Throwable) {
                                LOGGER.error("Failed to setup Discord RPC.", throwable)
                            }
                        }
                    }

                    // Login into known token if not empty
                    if (CapeService.knownToken.isNotBlank()) {
                        runCatching {
                            CapeService.login(CapeService.knownToken)
                        }.onFailure {
                            LOGGER.error("Failed to login into known cape token.", it)
                        }.onSuccess {
                            LOGGER.info("Successfully logged in into known cape token.")
                        }
                    }

                    // Refresh cape service
                    CapeService.refreshCapeCarriers {
                        LOGGER.info("Successfully loaded ${CapeService.capeCarriers.size} cape carriers.")
                    }
                }.await() // Wait to load

                // Load background
                FileManager.loadBackground()

            }.onFailure {
                LOGGER.error("Failed to start client ${it.message}")
            }.onSuccess {
                // Set is starting status
                isStarting = false

                callEvent(StartupEvent())
                LOGGER.info("Successfully started client")
            }
        }
    }

    /**
     * Execute if client will be stopped
     */
    fun stopClient() {
        // Call client shutdown
        callEvent(ClientShutdownEvent())

        // Save all available configs
        saveAllConfigs()

        // Shutdown discord rpc
        clientRichPresence.shutdown()
    }

}
