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

import net.ccbluex.liquidbounce.bmw.*
import net.ccbluex.liquidbounce.api.ClientUpdate.gitInfo
import net.ccbluex.liquidbounce.api.ClientUpdate.hasUpdate
import net.ccbluex.liquidbounce.api.IpInfoApi
import net.ccbluex.liquidbounce.api.oauth.ClientAccount
import net.ccbluex.liquidbounce.api.oauth.ClientAccountManager
import net.ccbluex.liquidbounce.api.oauth.OAuthClient
import net.ccbluex.liquidbounce.bmw.getTimeFromKey
import net.ccbluex.liquidbounce.config.AutoConfig
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.Reconnect
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.cosmetic.CapeService
import net.ccbluex.liquidbounce.features.itemgroup.ClientItemGroups
import net.ccbluex.liquidbounce.features.itemgroup.groups.headsCollection
import net.ccbluex.liquidbounce.features.misc.AccountManager
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.misc.ProxyManager
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.client.ipcConfiguration
import net.ccbluex.liquidbounce.lang.LanguageManager
import net.ccbluex.liquidbounce.render.Fonts
import net.ccbluex.liquidbounce.render.ui.ItemImageAtlas
import net.ccbluex.liquidbounce.script.ScriptManager
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.block.WorldChangeNotifier
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.combat.globalEnemyConfigurable
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.mappings.Remapper
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.ccbluex.liquidbounce.web.browser.BrowserManager
import net.ccbluex.liquidbounce.web.integration.IntegrationHandler
import net.ccbluex.liquidbounce.web.socket.ClientSocket
import net.ccbluex.liquidbounce.web.theme.ThemeManager
import net.ccbluex.liquidbounce.web.theme.component.ComponentOverlay
import net.minecraft.resource.ReloadableResourceManagerImpl
import net.minecraft.resource.ResourceManager
import net.minecraft.resource.ResourceReloader
import net.minecraft.resource.SynchronousResourceReloader
import org.apache.logging.log4j.LogManager
import java.time.Duration
import java.time.LocalDateTime

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
     * Defines if the client is in development mode.
     * This will enable update checking on commit time instead of semantic versioning.
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
    @Suppress("unused")
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
            MouseStateTracker

            // Features
            ModuleManager
            CommandManager
            ScriptManager
            RotationManager
            InteractionTracker
            CombatManager
            FriendManager
            ProxyManager
            AccountManager
            InventoryManager
            WorldToScreen
            Reconnect
            ConfigSystem.root(ClientItemGroups)
            ConfigSystem.root(LanguageManager)
            ConfigSystem.root(ClientAccountManager)
            BrowserManager
            Fonts

            // Register commands and modules
            CommandManager.registerInbuilt()
            ModuleManager.registerInbuilt()

            // Load user scripts
            ScriptManager.loadScripts()

            // Load theme and component overlay
            ThemeManager
            ComponentOverlay.insertComponents()

            // Load config system from disk
            ConfigSystem.loadAll()

            // Netty WebSocket
            ClientSocket.start()

            // Initialize browser
            logger.info("Refresh Rate: ${mc.window.refreshRate} Hz")

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

            ItemImageAtlas
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

            // Check if client account is available
            if (ClientAccountManager.account != ClientAccount.EMPTY_ACCOUNT) {
                OAuthClient.runWithScope {
                    runCatching {
                        ClientAccountManager.account = ClientAccountManager.account.renew()
                    }.onFailure {
                        logger.error("Failed to renew client account token.", it)
                        ClientAccountManager.account = ClientAccount.EMPTY_ACCOUNT
                    }.onSuccess {
                        logger.info("Successfully renewed client account token.")
                    }
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
                AutoConfig.configs
            }.onSuccess {
                logger.info("Loaded ${it.size} settings from API.")
            }.onFailure {
                logger.error("Failed to load settings list from API", it)
            }

            // Disable conflicting options
            runCatching {
                disableConflictingVfpOptions()
            }.onSuccess {
                logger.info("Disabled conflicting options.")
            }
        }
    }

    /**
     * Should be executed to stop the client.
     */
    @Suppress("unused")
    val shutdownHandler = handler<ClientShutdownEvent> {
        logger.info("Shutting down client...")

        ConfigSystem.storeAll()
        ChunkScanner.ChunkScannerThread.stopThread()

        // Shutdown browser as last step
        BrowserManager.shutdownBrowser()
    }

    private const val NO_KEY_NOTIFICATION = "未激活！请在聊天框发送“.key 激活码”进行激活，|秒后未激活将自动退出世界"
    var free = true
    private var keyTimer = -1

    private fun haveKey() : Boolean {
        if (free) {
            return true
        }
        if (nowKey == "") {
            return false
        }
        val result = getTimeFromKey(nowKey)
        val now = LocalDateTime.now()
        return Duration.between(result.startTime, now).toDays() <= result.days
    }

    val repeatHandler = repeatable {
        if (inGame && mc.currentScreen != null && keyTimer == -1 && !haveKey()) {
            keyTimer = 60*20
        }
    }

    val worldChangeEventHandler = handler<WorldChangeEvent> {
        if (keyTimer != -1) {
            keyTimer = 60*20
        }
    }

    val gameTickEventHandler = handler<GameTickEvent> {
        if (keyTimer > 0) {
            if (haveKey()) {
                keyTimer = -1
            }
            if (keyTimer % 20 == 0) {
                notifyAsMessageAndNotification(
                    NO_KEY_NOTIFICATION.replace("|", (keyTimer / 20).toString()),
                    NotificationEvent.Severity.ERROR
                )
            }
            keyTimer--
        } else if (keyTimer == 0) {
            mc.world?.disconnect()
            keyTimer = -1
        }
    }

}
