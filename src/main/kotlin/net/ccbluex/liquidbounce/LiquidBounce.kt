/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import net.ccbluex.liquidbounce.api.ClientUpdate.gitInfo
import net.ccbluex.liquidbounce.api.ClientUpdate.hasUpdate
import net.ccbluex.liquidbounce.api.IpInfoApi
import net.ccbluex.liquidbounce.api.v1.ClientApiV1
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.ClientShutdownEvent
import net.ccbluex.liquidbounce.event.events.ClientStartEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.Reconnect
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.commands.client.CommandConfig
import net.ccbluex.liquidbounce.features.cosmetic.CapeService
import net.ccbluex.liquidbounce.features.misc.AccountManager
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.misc.ProxyManager
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.itemgroup.ClientItemGroups
import net.ccbluex.liquidbounce.features.itemgroup.groups.headsCollection
import net.ccbluex.liquidbounce.lang.LanguageManager
import net.ccbluex.liquidbounce.features.module.modules.client.ipcConfiguration
import net.ccbluex.liquidbounce.render.Fonts
import net.ccbluex.liquidbounce.script.ScriptManager
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.block.WorldChangeNotifier
import net.ccbluex.liquidbounce.utils.client.ErrorHandler
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.combat.globalEnemyConfigurable
import net.ccbluex.liquidbounce.utils.item.InventoryTracker
import net.ccbluex.liquidbounce.utils.mappings.Remapper
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.ccbluex.liquidbounce.web.browser.BrowserManager
import net.ccbluex.liquidbounce.web.integration.AcknowledgementHandler
import net.ccbluex.liquidbounce.web.integration.IntegrationHandler
import net.ccbluex.liquidbounce.web.socket.ClientSocket
import net.ccbluex.liquidbounce.web.theme.ThemeManager
import net.minecraft.resource.ReloadableResourceManagerImpl
import net.minecraft.resource.ResourceManager
import net.minecraft.resource.ResourceReloader
import net.minecraft.resource.SynchronousResourceReloader
import org.apache.logging.log4j.LogManager

/**
 * LiquidBounce
 *
 * A free mixin-based injection hacked-client for Minecraft using FabricMC.
 *
 * @author kawaiinekololis (@team CCBlueX)
 */
object LiquidBounce : Listenable {

    /**
     * CLIENT INFORMATION
     *
     * WARNING: Please read the GNU General Public License
     */
    const val CLIENT_NAME = "LiquidBounce"
    const val CLIENT_AUTHOR = "CCBlueX"
    const val CLIENT_CLOUD = "https://cloud.liquidbounce.net/LiquidBounce"

    val clientVersion = gitInfo["git.build.version"]?.toString() ?: "unknown"
    val clientCommit = gitInfo["git.commit.id.abbrev"]?.let { "git-$it" } ?: "unknown"
    val clientBranch = gitInfo["git.branch"]?.toString() ?: "nextgen"

    /**
     * Defines if the client is in development mode. This will enable update checking on commit time instead of semantic versioning.
     *
     * TODO: Replace this approach with full semantic versioning.
     */
    const val IN_DEVELOPMENT = true

    val isIntegrationTesting = !System.getenv("TENACC_TEST_PROVIDER").isNullOrBlank()

    /**
     * Client logger to print out console messages
     */
    val logger = LogManager.getLogger(CLIENT_NAME)!!

    /**
     * Client update information
     */
    val updateAvailable by lazy { hasUpdate() }

    /**
     * Should be executed to start the client.
     */
    val startHandler = handler<ClientStartEvent> {
        runCatching {
            logger.info("Launching $CLIENT_NAME v$clientVersion by $CLIENT_AUTHOR")
            logger.debug("Loading from cloud: '$CLIENT_CLOUD'")

            // Load mappings
            Remapper.load()

            // Load translations
            LanguageManager.loadLanguages()

            // Initialize client features
            EventManager

            // Config
            ConfigSystem
            globalEnemyConfigurable

            ChunkScanner
            WorldChangeNotifier

            // Features
            ModuleManager
            CommandManager
            ScriptManager
            RotationManager
            CombatManager
            FriendManager
            ProxyManager
            AccountManager
            InventoryTracker
            WorldToScreen
            Reconnect
            ConfigSystem.root(ClientItemGroups)
            ConfigSystem.root(LanguageManager)
            BrowserManager
            Fonts

            // Register commands and modules
            CommandManager.registerInbuilt()
            ModuleManager.registerInbuilt()

            // Load user scripts
            ScriptManager.loadScripts()

            // Load config system from disk
            ConfigSystem.load()

            // Netty WebSocket
            ClientSocket.start()

            // Initialize browser
            ThemeManager
            IntegrationHandler
            BrowserManager.initBrowser()

            // Register resource reloader
            val resourceManager = mc.resourceManager
            val clientResourceReloader = ClientResourceReloader()
            if (resourceManager is ReloadableResourceManagerImpl) {
                resourceManager.registerReloader(clientResourceReloader)
            } else {
                logger.warn("Failed to register resource reloader!")

                // Run resource reloader directly as fallback
                clientResourceReloader.reload(resourceManager)
            }
        }.onSuccess {
            logger.info("Successfully loaded client!")
        }.onFailure(ErrorHandler::fatal)
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
    class ClientResourceReloader : SynchronousResourceReloader {

        override fun reload(manager: ResourceManager) {
            runCatching {
                logger.info("Loading fonts...")
                Fonts.loadQueuedFonts()
            }.onSuccess {
                logger.info("Loaded fonts successfully!")
            }.onFailure(ErrorHandler::fatal)

            // Check for newest version
            if (updateAvailable) {
                logger.info("Update available! Please download the latest version from https://liquidbounce.net/")
            }

            runCatching {
                ipcConfiguration.let {
                    logger.info("Loaded Discord IPC configuration.")
                }
            }.onFailure {
                logger.error("Failed to load Discord IPC configuration.", it)
            }

            // Refresh local IP info
            logger.info("Refreshing local IP info...")
            IpInfoApi.refreshLocalIpInfo()

            // Login into known token if not empty
            if (CapeService.knownToken.isNotBlank()) {
                runCatching {
                    CapeService.login(CapeService.knownToken)
                }.onFailure {
                    logger.error("Failed to login into known cape token.", it)
                }.onSuccess {
                    logger.info("Successfully logged in into known cape token.")
                }
            }

            // Refresh cape service
            CapeService.refreshCapeCarriers {
                logger.info("Successfully loaded ${CapeService.capeCarriers.size} cape carriers.")
            }

            // Load Head collection
            headsCollection

            // Load settings list from API
            runCatching {
                logger.info("Loading settings list from API...")
                CommandConfig.cachedSettingsList = ClientApiV1.requestSettingsList()
            }.onSuccess {
                logger.info("Loaded ${CommandConfig.cachedSettingsList?.size} settings from API.")
            }.onFailure {
                logger.error("Failed to load settings list from API", it)
            }

            // Load acknowledgement handler
            AcknowledgementHandler
        }
    }

    /**
     * Should be executed to stop the client.
     */
    val shutdownHandler = handler<ClientShutdownEvent> {
        logger.info("Shutting down client...")
        BrowserManager.shutdownBrowser()
        ConfigSystem.storeAll()

        ChunkScanner.ChunkScannerThread.stopThread()
    }

}
