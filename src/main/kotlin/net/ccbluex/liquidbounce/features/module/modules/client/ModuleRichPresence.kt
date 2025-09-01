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
 *
 */
package net.ccbluex.liquidbounce.features.module.modules.client

import com.jagrosh.discordipc.IPCClient
import com.jagrosh.discordipc.entities.RichPresence
import com.jagrosh.discordipc.entities.pipe.PipeStatus
import com.jagrosh.discordipc.exceptions.NoDiscordClientException
import kotlinx.coroutines.Dispatchers
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_AUTHOR
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.LiquidBounce.clientBranch
import net.ccbluex.liquidbounce.LiquidBounce.clientCommit
import net.ccbluex.liquidbounce.LiquidBounce.clientVersion
import net.ccbluex.liquidbounce.api.core.AsyncLazy
import net.ccbluex.liquidbounce.api.services.cdn.ClientCdn
import net.ccbluex.liquidbounce.config.gson.util.json
import net.ccbluex.liquidbounce.config.gson.util.jsonArrayOf
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.ServerConnectEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.hideSensitiveAddress
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.protocolVersion

val ipcConfiguration by AsyncLazy {
    runCatching {
        ClientCdn.requestDiscordConfiguration()
    }.onSuccess {
        LiquidBounce.logger.info("Successfully loaded Discord IPC configuration [${it.appID}].")
    }.onFailure {
        LiquidBounce.logger.error("Failed to load Discord IPC configuration.", it)
    }.getOrNull()
}

object ModuleRichPresence : ClientModule("RichPresence", Category.CLIENT, state = true, hide = true,
    aliases = arrayOf("DiscordPresence")) {

    private val detailsText by text("Details", "Nextgen v%clientVersion% by %clientAuthor%")
    private val stateText by text("State", "%enabledModules% of %totalModules% modules enabled")

    private val largeImageText by text("LargeImage", "Online with %protocol%")
    private val smallImageText by text("SmallImage", "%clientBranch% (%clientCommit%)")

    // IPC Client
    private var ipcClient: IPCClient? = null

    @Volatile
    private var timestamp = System.currentTimeMillis()

    private var doNotTryToConnect = false

    init {
        doNotIncludeAlways()
    }

    override fun onEnabled() {
        doNotTryToConnect = false
    }

    private fun connectIpc() {
        val ipcConfiguration = ipcConfiguration ?: return

        if (doNotTryToConnect || ipcClient?.status == PipeStatus.CONNECTED) {
            return
        }

        runCatching {
            ipcClient = IPCClient(ipcConfiguration.appID)
            ipcClient?.connect()
        }.onFailure {
            logger.info("Failed to connect to Discord RPC.", it)

            if (it is NoDiscordClientException) {
                notification(
                    title = "Discord RPC",
                    message = "Please make sure you have Discord running.",
                    severity = NotificationEvent.Severity.ERROR
                )
            } else {
                notification(
                    title = "Discord RPC",
                    message = "Failed to initialize Discord RPC.",
                    severity = NotificationEvent.Severity.ERROR
                )
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
    val updateCycle = tickHandler {
        waitSeconds(1)

        /**
         * Don't block the render thread
         */
        waitFor(Dispatchers.IO) {
            if (enabled) {
                connectIpc()
            } else {
                shutdownIpc()
            }

            val ipcClient = ipcClient
            // Check ipc client is connected and send rpc
            if (ipcClient == null || ipcClient.status != PipeStatus.CONNECTED) {
                return@waitFor
            }

            val ipcConfiguration = ipcConfiguration ?: return@waitFor

            ipcClient.sendRichPresence {
                // Set playing time
                setStartTimestamp(timestamp)

                // Check assets contains logo and set logo
                if ("logo" in ipcConfiguration.assets) {
                    setLargeImage(ipcConfiguration.assets["logo"], formatText(largeImageText))
                }

                if ("smallLogo" in ipcConfiguration.assets) {
                    setSmallImage(ipcConfiguration.assets["smallLogo"], formatText(smallImageText))
                }

                setDetails(formatText(detailsText))
                setState(formatText(stateText))

                setButtons(jsonArrayOf(
                    json {
                        "label" to "Download"
                        "url" to "https://liquidbounce.net/"
                    },

                    json {
                        "label" to "GitHub"
                        "url" to "https://github.com/CCBlueX/LiquidBounce"
                    },
                ))
            }
        }
    }

    @Suppress("unused")
    val serverConnectHandler = handler<ServerConnectEvent> {
        timestamp = System.currentTimeMillis()
    }

    private fun formatText(text: String) = text.replace("%clientVersion%", clientVersion)
        .replace("%clientAuthor%", CLIENT_AUTHOR)
        .replace("%clientName%", CLIENT_NAME)
        .replace("%clientBranch%", clientBranch)
        .replace("%clientCommit%", clientCommit)
        .replace("%enabledModules%", ModuleManager.count { it.running }.toString())
        .replace("%totalModules%", ModuleManager.count().toString())
        .replace("%protocol%", protocolVersion.let { "${it.name} (${it.version})" })
        .replace("%server%", (mc.currentServerEntry?.address ?: "none").hideSensitiveAddress())

    private inline fun IPCClient.sendRichPresence(builderAction: RichPresence.Builder.() -> Unit) =
        sendRichPresence(RichPresence.Builder().apply(builderAction).build())

    /**
     * Always running
     */
    override val running = true

}
