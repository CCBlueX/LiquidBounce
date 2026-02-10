/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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
 */
package net.ccbluex.liquidbounce

import com.mojang.blaze3d.systems.RenderSystem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.future
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.api.core.ApiConfig
import net.ccbluex.liquidbounce.api.core.ioScope
import net.ccbluex.liquidbounce.api.models.auth.ClientAccount
import net.ccbluex.liquidbounce.api.services.client.ClientUpdate.update
import net.ccbluex.liquidbounce.api.thirdparty.IpInfoApi
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.autoconfig.AutoConfig
import net.ccbluex.liquidbounce.config.types.Config
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine
import net.ccbluex.liquidbounce.deeplearn.ModelManager
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ClientShutdownEvent
import net.ccbluex.liquidbounce.event.events.ClientStartEvent
import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.account.AccountManager
import net.ccbluex.liquidbounce.features.blink.BlinkManager
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.cosmetic.ClientAccountManager
import net.ccbluex.liquidbounce.features.cosmetic.CosmeticService
import net.ccbluex.liquidbounce.features.creativetab.tabs.HeadsCreativeModeTab
import net.ccbluex.liquidbounce.features.global.GlobalManager
import net.ccbluex.liquidbounce.features.marketplace.MarketplaceManager
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.misc.proxy.ProxyManager
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.spoofer.SpooferManager
import net.ccbluex.liquidbounce.integration.backend.BrowserBackendManager
import net.ccbluex.liquidbounce.integration.interop.ClientInteropServer
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.ActiveServerList
import net.ccbluex.liquidbounce.integration.screen.ScreenManager
import net.ccbluex.liquidbounce.integration.task.TaskManager
import net.ccbluex.liquidbounce.integration.task.TaskProgressScreen
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.lang.LanguageManager
import net.ccbluex.liquidbounce.render.ClientShaders
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.HAS_AMD_VEGA_APU
import net.ccbluex.liquidbounce.render.engine.BlurEffectRenderer
import net.ccbluex.liquidbounce.render.ui.ItemImageAtlas
import net.ccbluex.liquidbounce.script.ScriptManager
import net.ccbluex.liquidbounce.utils.aiming.PostRotationExecutor
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.client.GitInfo
import net.ccbluex.liquidbounce.utils.client.InteractionTracker
import net.ccbluex.liquidbounce.utils.client.ServerObserver
import net.ccbluex.liquidbounce.utils.client.error.ErrorHandler
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.entity.RenderedEntities
import net.ccbluex.liquidbounce.utils.input.InputTracker
import net.ccbluex.liquidbounce.utils.inventory.EnderChestInventoryTracker
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.kotlin.Minecraft
import net.ccbluex.liquidbounce.utils.mappings.EnvironmentRemapper
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.server.packs.resources.ReloadableResourceManager
import java.io.InputStream
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
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

    private object Client : Config("Client") {
        val version = text("Version", GitInfo.version())
            .immutable()
        val commit = text("Commit", GitInfo.get("git.commit.id.abbrev")?.let { "git-$it" } ?: "unknown")
            .immutable()
        val branch = text("Branch", GitInfo.branch())
            .immutable()

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
    val logger get() = net.ccbluex.liquidbounce.utils.client.logger

    var taskManager: TaskManager? = null

    var isInitialized = false
        private set

    /**
     * Creates an [net.minecraft.resources.Identifier] starts with [CLIENT_NAME].
     */
    @JvmStatic
    fun identifier(path: String): Identifier = Identifier.fromNamespaceAndPath(CLIENT_NAME.lowercase(Locale.ROOT), path)

    /**
     * Gets client resource.
     *
     * @param path prefix `/resources/liquidbounce/`
     * @throws IllegalArgumentException if the resource is not found
     */
    @JvmStatic
    fun resource(path: String): InputStream =
        LiquidBounce::class.java.getResourceAsStream("/resources/liquidbounce/$path")
            ?: throw IllegalArgumentException("Resource $path not found")

    /**
     * Gets client resource as string.
     *
     * @param path prefix `/resources/liquidbounce/`
     * @throws IllegalArgumentException if the resource is not found
     */
    @JvmStatic
    fun resourceToString(path: String): String =
        resource(path).use { it.bufferedReader().readText() }

    /**
     * Initializes the client, called when
     * we reached the last stage of the splash screen.
     *
     * The thread should be the main render thread.
     */
    private fun initializeClient(
        workerDispatcher: CoroutineDispatcher,
        renderThreadDispatcher: CoroutineDispatcher,
    ): CompletableFuture<Unit> = CoroutineScope(
        renderThreadDispatcher + CoroutineName("$CLIENT_NAME Initializer")
    ).future {
        if (isInitialized) {
            return@future
        }

        // Ensure we are on the render thread
        RenderSystem.assertOnRenderThread()

        // Initialize managers and features
        Client
        initializeManagers(workerDispatcher, renderThreadDispatcher)
        initializeFeatures()
        initializeResources(workerDispatcher)
        prepareGuiStage(renderThreadDispatcher)

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
        logger.info("$CLIENT_NAME has been successfully initialized.")
    }.exceptionally { throwable ->
        ErrorHandler.fatal(throwable, additionalMessage = "$CLIENT_NAME initializer")
    }

    /**
     * Initializes managers for Event Listener registration.
     */
    private suspend fun initializeManagers(
        workerDispatcher: CoroutineDispatcher,
        renderThreadDispatcher: CoroutineDispatcher,
    ) = withContext(renderThreadDispatcher) {
        // Script system
        val scriptEngineJob = launch(workerDispatcher) {
            EnvironmentRemapper
            runCatching(ScriptManager::initializeEngine).onFailure { error ->
                logger.error("[ScriptAPI] Failed to initialize script engine.", error)
            }
        }

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

        // Utility managers
        RotationManager
        BlinkManager
        InteractionTracker
        CombatManager
        FriendManager
        InventoryManager
        EnderChestInventoryTracker
        ActiveServerList
        ConfigSystem.root(ClientAccountManager)
        ConfigSystem.root(SpooferManager)
        ConfigSystem.root(GlobalManager)
        ConfigSystem.root(MarketplaceManager)
        PostRotationExecutor
        ServerObserver
        ItemImageAtlas

        scriptEngineJob.join()
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
    private suspend fun initializeResources(
        dispatcher: CoroutineDispatcher,
    ) = withContext(dispatcher) {
        logger.info("Initializing API...")
        // Lookup API config
        ApiConfig.config

        supervisorScope {
            launch {
                // Load translations
                LanguageManager.loadDefault()
            }
            launch {
                val update = update ?: return@launch
                logger.info("[Update] Update available: $clientVersion -> ${update.lbVersion}")
            }
            launch {
                // Load cosmetics
                CosmeticService.refreshCarriers(force = true) {
                    logger.info("Successfully loaded ${CosmeticService.carriers.size} cosmetics carriers.")
                }
            }
            launch {
                // Download player heads
                HeadsCreativeModeTab.heads.getFinalState()
            }
            launch {
                // Load configs
                AutoConfig.reloadConfigs()
            }
            launch {
                IpInfoApi.original
            }
            launch {
                ConfigSystem.load(ClientAccountManager)
                if (ClientAccount.ENV_ACCOUNT != null) {
                    ClientAccountManager.clientAccount = ClientAccount.ENV_ACCOUNT
                }

                if (ClientAccountManager.clientAccount != ClientAccount.EMPTY_ACCOUNT) {
                    runCatching {
                        ClientAccountManager.clientAccount.renew()
                    }.onFailure {
                        logger.error("Failed to renew client account token.", it)
                        ClientAccountManager.clientAccount = ClientAccount.EMPTY_ACCOUNT
                    }.onSuccess {
                        logger.info("Successfully renewed client account token.")
                    }

                    ConfigSystem.store(ClientAccountManager)
                }
            }
        }

        logger.info("API initialization done.")

    }

    /**
     * Prepares the GUI stage of the client.
     * This will load [ThemeManager], as well as the [BrowserBackendManager] and [ClientInteropServer].
     */
    private suspend fun prepareGuiStage(
        dispatcher: CoroutineDispatcher
    ) = withContext(dispatcher) {
        RenderSystem.assertOnRenderThread()

        BrowserBackendManager.init()
        ClientInteropServer.start()
        if (!ClientInteropServer.isSkipping) {
            ThemeManager.init()
            // Preload marketplace items
            ConfigSystem.load(MarketplaceManager)
            ConfigSystem.load(ThemeManager)
            ThemeManager.load()
        }

        BlurEffectRenderer
        ScreenManager

        taskManager = TaskManager(ioScope).apply {
            // Either immediately starts browser or spawns a task to request browser dependencies,
            // and then starts the browser through render thread.
            BrowserBackendManager.makeDependenciesAvailable(this)

            // Initialize deep learning engine as task, because we cannot know if DJL will request
            // resources from the internet.
            launch("Deep Learning") { task ->
                runCatching {
                    DeepLearningEngine.init(task)
                    ModelManager.load()
                }.onFailure { exception ->
                    task.subTasks.clear()

                    // LiquidBounce can still run without deep learning,
                    // and we don't want to crash the client if it fails.
                    logger.info("Failed to initialize deep learning.", exception)
                }
            }

            launch("Marketplace") { task ->
                runCatching {
                    MarketplaceManager.updateAll(task)
                }.onFailure { exception ->
                    logger.error("Failed to update marketplace items.", exception)
                }

                task.isCompleted = true
            }
        }

        // Prepare glyph manager
        val duration = measureTime {
            FontManager.createGlyphManager()
        }
        logger.info("Completed loading fonts in ${duration.inWholeMilliseconds} ms.")
        logger.info("Fonts: [ ${FontManager.fontFaces.keys.joinToString()} ]")
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
        ChunkScanner.stopThread()
        EventManager.unregisterAll()

        // Save all configurations
        ConfigSystem.storeAll()

        // Shutdown browser
        BrowserBackendManager.stop()
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
            logger.info("Screen Resolution: ${mc.window.screenWidth}x${mc.window.screenHeight}")
            logger.info("Refresh Rate: ${mc.window.refreshRate} Hz")

            // Initialize event manager
            EventManager

            // Register resource reloader
            val resourceManager = mc.resourceManager
            if (resourceManager is ReloadableResourceManager) {
                resourceManager.registerReloadListener(ClientResourceReloader)
                resourceManager.registerReloadListener(ThemeManager.reloader)
            } else {
                logger.warn("Failed to register resource reloader!")

                // Run resource reloader directly as fallback
                initializeClient(
                    workerDispatcher = Dispatchers.Default,
                    renderThreadDispatcher = Dispatchers.Minecraft,
                ).thenRun {
                    ThemeManager.reloader.onResourceManagerReload(resourceManager)
                }
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
     * @see net.fabricmc.fabric.api.resource.v1.reloader.SimpleResourceReloader
     * @see PreparableReloadListener
     */
    private object ClientResourceReloader : PreparableReloadListener {
        override fun reload(
            store: PreparableReloadListener.SharedState,
            prepareExecutor: Executor,
            synchronizer: PreparableReloadListener.PreparationBarrier,
            applyExecutor: Executor
        ): CompletableFuture<Void> {
            return synchronizer.wait(net.minecraft.util.Unit.INSTANCE)
                .thenCompose {
                    val prepareDispatcher = prepareExecutor.asCoroutineDispatcher()
                    val applyDispatcher = applyExecutor.asCoroutineDispatcher()
                    @Suppress("UNCHECKED_CAST") // Kotlin Unit to Java Void
                    initializeClient(
                        workerDispatcher = prepareDispatcher,
                        renderThreadDispatcher = applyDispatcher,
                    ) as CompletableFuture<Void>
                }
        }

        override fun getName() = CLIENT_NAME
    }

    /**
     * Should be executed to stop the client.
     */
    @Suppress("unused")
    private val shutdownHandler = handler<ClientShutdownEvent> {
        shutdownClient()
    }

}
