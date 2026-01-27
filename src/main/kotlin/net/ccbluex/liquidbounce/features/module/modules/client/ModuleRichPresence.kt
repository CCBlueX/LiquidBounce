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
package net.ccbluex.liquidbounce.features.module.modules.client

import com.jagrosh.discordipc.IPCClient
import com.jagrosh.discordipc.entities.ActivityType
import com.jagrosh.discordipc.entities.RichPresence
import com.jagrosh.discordipc.entities.StatusDisplayType
import com.jagrosh.discordipc.entities.pipe.PipeStatus
import com.jagrosh.discordipc.exceptions.NoDiscordClientException
import kotlinx.coroutines.Dispatchers
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_AUTHOR
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.LiquidBounce.clientBranch
import net.ccbluex.liquidbounce.LiquidBounce.clientCommit
import net.ccbluex.liquidbounce.LiquidBounce.clientVersion
import net.ccbluex.liquidbounce.api.core.ioScope
import net.ccbluex.liquidbounce.api.core.retrying
import net.ccbluex.liquidbounce.api.services.cdn.ClientCdn
import net.ccbluex.liquidbounce.config.gson.util.jsonArrayOf
import net.ccbluex.liquidbounce.config.gson.util.jsonObject
import net.ccbluex.liquidbounce.event.events.ClientShutdownEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.hideSensitiveAddress
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.protocolVersion
import kotlin.time.Duration.Companion.seconds

object ModuleRichPresence : ClientModule("RichPresence", ModuleCategories.CLIENT, state = true, hide = true,
    aliases = listOf("DiscordPresence")
) {

    private val ipcConfiguration = ioScope.retrying(
        interval = 5.seconds,
        name = "IPC-Configuration",
        maxRetries = 5,
    ) {
        ClientCdn.requestDiscordConfiguration().also {
            LiquidBounce.logger.info("Successfully loaded Discord IPC configuration [${it.appID}].")
        }
    }

    private val detailsText by text("Details", "Nextgen v%clientVersion% by %clientAuthor%")
    private val stateText by text("State", "%enabledModules% of %totalModules% modules enabled")

    private val largeImageText by text("LargeImage", "Online with %protocol%")
    private val smallImageText by text("SmallImage", "%clientBranch% (%clientCommit%)")

    private val buttons = jsonArrayOf(
        jsonObject {
            "label"("Download")
            "url"("https://liquidbounce.net/")
        },

        jsonObject {
            "label"("GitHub")
            "url"("https://github.com/CCBlueX/LiquidBounce")
        },
    )

    // IPC Client
    private var ipcClient: IPCClient? = null

    @Volatile
    private var timestamp = System.currentTimeMillis()

    private var doNotTryToConnect = false

    init {
        doNotIncludeAlways()
    }

    override fun onEnabled() {
        timestamp = System.currentTimeMillis()
        doNotTryToConnect = false
    }

    private fun connectIpc() {
        val ipcConfiguration = ipcConfiguration.getNow() ?: return

        if (doNotTryToConnect || ipcClient?.status == PipeStatus.CONNECTED) {
            return
        }

        runCatching {
            ipcClient = IPCClient(ipcConfiguration.appID).also { it.connect() }
        }.onFailure {
            if (it is NoDiscordClientException) {
                notification(
                    title = "Discord RPC",
                    message = "Please make sure you have Discord running.",
                    severity = NotificationEvent.Severity.ERROR
                )
                logger.warn("No Discord client for RichPresence.")
            } else {
                notification(
                    title = "Discord RPC",
                    message = "Failed to initialize Discord RPC.",
                    severity = NotificationEvent.Severity.ERROR
                )
                logger.error("Failed to connect to Discord RPC.", it)
            }

            doNotTryToConnect = true
        }.onSuccess {
            logger.info("Successfully connected to Discord RPC.")
        }
    }

    private fun shutdownIpc() {
        val ipcClient = ipcClient
        if (ipcClient == null || ipcClient.status != PipeStatus.CONNECTED) {
            return
        }

        runCatching {
            ipcClient.close()
        }.onFailure {
            logger.error("Failed to close Discord RPC.", it)
        }.onSuccess {
            logger.info("Successfully closed Discord RPC.")
        }
        super.onDisabled()
    }

    @Suppress("unused")
    private val updateCycle = tickHandler(Dispatchers.IO) {
        waitTicks(20)

        if (enabled) {
            connectIpc()
        } else {
            shutdownIpc()
        }

        val ipcClient = ipcClient
        // Check ipc client is connected and send rpc
        if (ipcClient == null || ipcClient.status != PipeStatus.CONNECTED) {
            return@tickHandler
        }

        val ipcConfiguration = ipcConfiguration.getNow() ?: return@tickHandler

        ipcClient.sendRichPresence {
            setActivityType(ActivityType.Playing)
            setStatusDisplayType(StatusDisplayType.Name)
            setStartTimestamp(timestamp)

            // Check assets contains logo and set logo
            ipcConfiguration.assets["logo"]?.let { value ->
                setLargeImageWithTooltip(value, formatText(largeImageText))
            }

            ipcConfiguration.assets["smallLogo"]?.let { value ->
                setLargeImageWithTooltip(value, formatText(smallImageText))
            }

            setDetails(formatText(detailsText))
            setState(formatText(stateText))

            setButtons(buttons)
        }
    }

    @Suppress("unused")
    private val shutdownHandler = handler<ClientShutdownEvent> {
        shutdownIpc()
    }

    private fun formatText(text: String) = text.replace("%clientVersion%", clientVersion)
        .replace("%clientAuthor%", CLIENT_AUTHOR)
        .replace("%clientName%", CLIENT_NAME)
        .replace("%clientBranch%", clientBranch)
        .replace("%clientCommit%", clientCommit)
        .replace("%enabledModules%", ModuleManager.count { it.running }.toString())
        .replace("%totalModules%", ModuleManager.count().toString())
        .replace("%protocol%", protocolVersion.let { "${it.name} (${it.version})" })
        .replace("%server%", (mc.currentServer?.ip ?: "none").hideSensitiveAddress())

    private inline fun IPCClient.sendRichPresence(builderAction: RichPresence.Builder.() -> Unit) =
        sendRichPresence(RichPresence.Builder().apply(builderAction).build())

    /**
     * Always running after initialized
     */
    override val running get() = LiquidBounce.isInitialized

}
