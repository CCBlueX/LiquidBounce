/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 *
 */
package net.ccbluex.liquidbounce

import com.mojang.blaze3d.systems.RenderSystem
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import net.ccbluex.liquidbounce.api.core.scope
import net.ccbluex.liquidbounce.api.models.auth.ClientAccount
import net.ccbluex.liquidbounce.api.services.client.ClientUpdate.gitInfo
import net.ccbluex.liquidbounce.api.services.client.ClientUpdate.update
import net.ccbluex.liquidbounce.api.thirdparty.IpInfoApi
import net.ccbluex.liquidbounce.config.AutoConfig.configs
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.ConfigSystem.jsonFile
import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine
import net.ccbluex.liquidbounce.deeplearn.ModelHolster
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ClientShutdownEvent
import net.ccbluex.liquidbounce.event.events.ClientStartEvent
import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.cosmetic.ClientAccountManager
import net.ccbluex.liquidbounce.features.cosmetic.CosmeticService
import net.ccbluex.liquidbounce.features.itemgroup.ClientItemGroups
import net.ccbluex.liquidbounce.features.itemgroup.groups.heads
import net.ccbluex.liquidbounce.features.misc.AccountManager
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.misc.proxy.ProxyManager
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.client.ipcConfiguration
import net.ccbluex.liquidbounce.features.module.modules.combat.backtrack.BacktrackPacketManager
import net.ccbluex.liquidbounce.features.spoofer.SpooferManager
import net.ccbluex.liquidbounce.integration.IntegrationListener
import net.ccbluex.liquidbounce.integration.browser.BrowserManager
import net.ccbluex.liquidbounce.integration.interop.ClientInteropServer
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.ActiveServerList
import net.ccbluex.liquidbounce.integration.task.TaskManager
import net.ccbluex.liquidbounce.integration.task.TaskProgressScreen
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.integration.theme.component.ComponentOverlay
import net.ccbluex.liquidbounce.lang.LanguageManager
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.HAS_AMD_VEGA_APU
import net.ccbluex.liquidbounce.render.ui.ItemImageAtlas
import net.ccbluex.liquidbounce.script.ScriptManager
import net.ccbluex.liquidbounce.utils.aiming.PostRotationExecutor
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.client.InteractionTracker
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager
import net.ccbluex.liquidbounce.utils.client.ServerObserver
import net.ccbluex.liquidbounce.utils.client.error.ErrorHandler
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.entity.RenderedEntities
import net.ccbluex.liquidbounce.utils.input.InputTracker
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.mappings.EnvironmentRemapper
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.resource.ReloadableResourceManagerImpl
import net.minecraft.resource.ResourceManager
import net.minecraft.resource.ResourceReloader
import net.minecraft.resource.SynchronousResourceReloader
import org.apache.logging.log4j.LogManager
import java.io.File
import kotlin.time.measureTime

/**
 * LiquidBounce
 *
 * A free mixin-based injection hacked-client for Minecraft using FabricMC.
 *
 * @author kawaiinekololis (@team CCBlueX)
 */
object LiquidBounce : EventListener {

    /**
     * CLIENT INFORMATION
     *
     * WARNING: Please read the GNU General Public License
     */
    const val CLIENT_NAME = "LiquidBounce"
    const val CLIENT_AUTHOR = "CCBlueX"

    private object Client : Configurable("Client") {
        val version = text("Version", gitInfo["git.build.version"]?.toString() ?: "unknown").immutable()
        val commit = text("Commit", gitInfo["git.commit.id.abbrev"]?.let { "git-$it" } ?: "unknown").immutable()
        val branch = text("Branch", gitInfo["git.branch"]?.toString() ?: "nextgen").immutable()

        init {
            ConfigSystem.root(this)

            version.onChange { previousVersion ->
                runCatching {
                    ConfigSystem.backup("automatic_${previousVersion}-${version.inner}")
                }.onFailure {
                    logger.error("Unable to create backup", it)
                }

                previousVersion
            }
        }
    }

    val clientVersion by Client.version
    val clientCommit by Client.commit
    val clientBranch by Client.branch

    /**
     * Defines if the client is in development mode.
     * This will enable update checking on commit time instead of semantic versioning.
     *
     * TODO: Replace this approach with full semantic versioning.
     */
    const val IN_DEVELOPMENT = true

    /**
     * Client logger to print out console messages
     */
    val logger = LogManager.getLogger(CLIENT_NAME)!!

    var taskManager: TaskManager? = null

    var isInitialized = false
        private set

    /**
     * Initializes the client, called when
     * we reached the last stage of the splash screen.
     *
     * The thread should be the main render thread.
     */
    private fun initializeClient() {
        if (isInitialized) {
            return
        }

        // Ensure we are on the render thread
        RenderSystem.assertOnRenderThread()

        // Initialize managers and features
        Client
        initializeManagers()
        initializeFeatures()
        initializeResources()
        prepareGuiStage()

        // Register shutdown hook in case [ClientShutdownEvent] is not called
        Runtime.getRuntime().addShutdownHook(Thread(::shutdownClient))

        // Check for AMD Vega iGPU
        if (HAS_AMD_VEGA_APU) {
            logger.info("AMD Vega iGPU detected, enabling different line smooth handling. " +
                "If you believe this is a mistake, please create an issue at " +
                "https://github.com/CCBlueX/LiquidBounce/issues.")
        }

        // Do backup before loading configs
        if (!ConfigSystem.isFirstLaunch && !Client.jsonFile.exists()) {
            runCatching {
                ConfigSystem.backup("automatic_${Client.version.inner}")
            }.onFailure {
                logger.error("Unable to create backup", it)
            }
        }

        // Load all configurations
        ConfigSystem.loadAll()

        isInitialized = true
    }

    /**
     * Initializes managers for Event Listener registration.
     */
    private fun initializeManagers() {
        // Config
        ConfigSystem

        // Utility
        RenderedEntities
        ChunkScanner
        InputTracker

        // Feature managers
        ModuleManager
        CommandManager
        ProxyManager
        AccountManager

        // Script system
        EnvironmentRemapper
        runCatching(ScriptManager::initializeEngine).onFailure { error ->
            logger.error("[ScriptAPI] Failed to initialize script engine.", error)
        }

        // Utility managers
        RotationManager
        PacketQueueManager
        BacktrackPacketManager
        InteractionTracker
        CombatManager
        FriendManager
        InventoryManager
        WorldToScreen
        ActiveServerList
        ConfigSystem.root(ClientItemGroups)
        ConfigSystem.root(LanguageManager)
        ConfigSystem.root(ClientAccountManager)
        ConfigSystem.root(SpooferManager)
        PostRotationExecutor
        ServerObserver
        ItemImageAtlas
    }

    /**
     * Initializes in-built and script features.
     */
    private fun initializeFeatures() {
        // Register commands and modules
        CommandManager.registerInbuilt()
        ModuleManager.registerInbuilt()

        // Load user scripts
        runCatching(ScriptManager::loadAll).onFailure { error ->
            logger.error("ScriptManager was unable to load scripts.", error)
        }
    }

    /**
     * Simultaneously initializes resources
     * such as translations, cosmetics, player heads, configs and so on,
     * which do not rely on the main thread.
     */
    private fun initializeResources() = runBlocking {
        listOf(
            scope.async {
                // Load translations
                LanguageManager.loadDefault()
            },
            scope.async {
                val update = update ?: return@async
                logger.info("[Update] Update available: $clientVersion -> ${update.lbVersion}")
            },
            scope.async {
                // Load cosmetics
                CosmeticService.refreshCarriers(force = true) {
                    logger.info("Successfully loaded ${CosmeticService.carriers.size} cosmetics carriers.")
                }
            },
            scope.async {
                // Download player heads
                heads
            },
            scope.async {
                // Load configs
                configs
            },
            scope.async {
                // IPC configuration
                ipcConfiguration
            },
            scope.async {
                IpInfoApi.original
            },
            scope.async {
                if (ClientAccountManager.clientAccount != ClientAccount.EMPTY_ACCOUNT) {
                    runCatching {
                        ClientAccountManager.clientAccount.renew()
                    }.onFailure {
                        logger.error("Failed to renew client account token.", it)
                        ClientAccountManager.clientAccount = ClientAccount.EMPTY_ACCOUNT
                    }.onSuccess {
                        logger.info("Successfully renewed client account token.")
                        ConfigSystem.storeConfigurable(ClientAccountManager)
                    }
                }
            },
            scope.async {
                ThemeManager.themesFolder.listFiles()
                    ?.filter { file -> file.isDirectory }
                    ?.forEach { file ->
                        runCatching {
                            val assetsFolder = File(file, "assets")
                            if (!assetsFolder.exists()) {
                                return@forEach
                            }

                            FontManager.queueFolder(assetsFolder)
                        }.onFailure {
                            logger.error("Failed to queue fonts from theme '${file.name}'.", it)
                        }
                    }
            }
        ).awaitAll()
    }

    /**
     * Prepares the GUI stage of the client.
     * This will load [ThemeManager], as well as the [BrowserManager] and [ClientInteropServer].
     */
    private fun prepareGuiStage() {
        // Load theme and component overlay
        ThemeManager
        BrowserManager

        // Start Interop Server
        ClientInteropServer.start()
        IntegrationListener

        taskManager = TaskManager(scope).apply {
            // Either immediately starts browser or spawns a task to request browser dependencies,
            // and then starts the browser through render thread.
            BrowserManager.makeDependenciesAvailable(this)

            // Initialize deep learning engine as task, because we cannot know if DJL will request
            // resources from the internet.
            launch("Deep Learning") { task ->
                runCatching {
                    DeepLearningEngine.init(task)
                    ModelHolster.load()
                }.onFailure { exception ->
                    task.subTasks.clear()

                    // LiquidBounce can still run without deep learning,
                    // and we don't want to crash the client if it fails.
                    logger.info("Failed to initialize deep learning.", exception)
                }
            }
        }

        // Prepare glyph manager
        val duration = measureTime {
            FontManager.createGlyphManager()
        }
        logger.info("Completed loading fonts in ${duration.inWholeMilliseconds} ms.")
        logger.info("Fonts: [ ${FontManager.fontFaces.joinToString { face -> face.name }} ]")

        // Insert default components on HUD
        ComponentOverlay.insertDefaultComponents()
    }

    /**
     * Shuts down the client. This will save all configurations and stop all running tasks.
     */
    private fun shutdownClient() {
        if (!isInitialized) {
            return
        }
        isInitialized = false
        logger.info("Shutting down client...")

        // Unregister all event listener and stop all running tasks
        ChunkScanner.ChunkScannerThread.stopThread()
        EventManager.unregisterAll()

        // Save all configurations
        ConfigSystem.storeAll()

        // Shutdown browser as last step
        BrowserManager.stopBrowser()
    }

    /**
     * Should be executed to start the client.
     */
    @Suppress("unused")
    private val startHandler = handler<ClientStartEvent> {
        runCatching {
            logger.info("Launching $CLIENT_NAME v$clientVersion by $CLIENT_AUTHOR")
            // Print client information
            logger.info("Client Version: $clientVersion ($clientCommit)")
            logger.info("Client Branch: $clientBranch")
            logger.info("Operating System: ${System.getProperty("os.name")} (${System.getProperty("os.version")})")
            logger.info("Java Version: ${System.getProperty("java.version")}")
            logger.info("Screen Resolution: ${mc.window.width}x${mc.window.height}")
            logger.info("Refresh Rate: ${mc.window.refreshRate} Hz")

            // Initialize event manager
            EventManager

            // Register resource reloader
            val resourceManager = mc.resourceManager
            val clientInitializer = ClientInitializer()
            if (resourceManager is ReloadableResourceManagerImpl) {
                resourceManager.registerReloader(clientInitializer)
            } else {
                logger.warn("Failed to register resource reloader!")

                // Run resource reloader directly as fallback
                clientInitializer.reload(resourceManager)
            }
        }.onFailure {
            ErrorHandler.fatal(it, additionalMessage = "Client start")
        }
    }

    @Suppress("unused")
    private val screenHandler = handler<ScreenEvent>(priority = FIRST_PRIORITY) { event ->
        val taskManager = taskManager ?: return@handler

        if (!taskManager.isCompleted && event.screen !is TaskProgressScreen) {
            event.cancelEvent()
            mc.setScreen(TaskProgressScreen("Loading Required Libraries", taskManager))
        }
    }

    /**
     * Resource reloader which is executed on client start and reload.
     * This is used to run async tasks without blocking the main thread.
     *
     * For now this is only used to check for updates and request additional information from the internet.
     *
     * @see SynchronousResourceReloader
     * @see ResourceReloader
     */
    class ClientInitializer : SynchronousResourceReloader {
        override fun reload(manager: ResourceManager) {
            runCatching(::initializeClient).onSuccess {
                logger.info("$CLIENT_NAME has been successfully initialized.")
            }.onFailure {
                ErrorHandler.fatal(it, additionalMessage = "Client resource reloader")
            }
        }
    }

    /**
     * Should be executed to stop the client.
     */
    @Suppress("unused")
    private val shutdownHandler = handler<ClientShutdownEvent> {
        shutdownClient()
    }


}
