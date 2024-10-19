/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015-2024 CCBlueX
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

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.jagrosh.discordipc.IPCClient
import com.jagrosh.discordipc.entities.RichPresence
import com.jagrosh.discordipc.entities.pipe.PipeStatus
import com.jagrosh.discordipc.exceptions.NoDiscordClientException
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_AUTHOR
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_CLOUD
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.LiquidBounce.clientBranch
import net.ccbluex.liquidbounce.LiquidBounce.clientCommit
import net.ccbluex.liquidbounce.LiquidBounce.clientVersion
import net.ccbluex.liquidbounce.config.util.decode
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.ServerConnectEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.hideSensitiveAddress
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.protocolVersion
import net.ccbluex.liquidbounce.utils.io.HttpClient
import net.ccbluex.liquidbounce.utils.kotlin.virtualThread

data class IpcConfiguration(
    val appID: Long,
    val assets: Map<String, String>
)

val ipcConfiguration by lazy {
    logger.info("Loading Discord IPC configuration...")
    decode<IpcConfiguration>(HttpClient.get("$CLIENT_CLOUD/discord.json"))
}

object ModuleRichPresence : Module(
    "RichPresence", Category.CLIENT, state = true, hide = true,
    aliases = arrayOf("DiscordPresence")
) {

    private val detailsText by text("Details", "Nextgen v%clientVersion% by %clientAuthor%")
    private val stateText by text("State", "%enabledModules% of %totalModules% modules enabled")

    private val largeImageText by text("LargeImage", "Online with %protocol%")
    private val smallImageText by text("SmallImage", "%clientBranch% (%clientCommit%)")

    private var ipcClient: IPCClient? = null
    @Volatile
    private var timestamp = System.currentTimeMillis()

    private var doNotTryToConnect = false

    init {
        doNotIncludeAlways()

        virtualThread("RichPresence-Loop") {
            while (true) {
                Thread.sleep(1000L)

                if (enabled) {
                    connectIpc()
                } else {
                    shutdownIpc()
                }

                // Check ipc client is connected and send rpc
                if (ipcClient?.status != PipeStatus.CONNECTED) {
                    continue
                }

                ipcClient?.sendRichPresence {
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

                    setButtons(JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("label", "Download")
                            addProperty("url", "https://liquidbounce.net/")
                        })

                        add(JsonObject().apply {
                            addProperty("label", "GitHub")
                            addProperty("url", "https://github.com/CCBlueX/LiquidBounce")
                        })
                    })
                }
            }
        }
    }

    override fun enable() {
        doNotTryToConnect = false
    }

    private fun connectIpc() {
        if (doNotTryToConnect || ipcClient?.status == PipeStatus.CONNECTED) {
            return
        }

        runCatching {
            ipcClient = IPCClient(ipcConfiguration.appID)
            ipcClient?.connect()
        }.onFailure {
            logger.error("Failed to connect to Discord RPC.", it)

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
        super.enable()
    }

    private fun shutdownIpc() {
        if (ipcClient == null || ipcClient?.status != PipeStatus.CONNECTED) {
            return
        }

        runCatching {
            ipcClient?.close()
        }.onFailure {
            logger.error("Failed to close Discord RPC.", it)
        }.onSuccess {
            logger.info("Successfully closed Discord RPC.")
        }
        super.disable()
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
        .replace("%enabledModules%", ModuleManager.count { it.enabled }.toString())
        .replace("%totalModules%", ModuleManager.count().toString())
        .replace("%protocol%", protocolVersion.let { "${it.name} (${it.version})" })
        .replace("%server%", hideSensitiveAddress(mc.currentServerEntry?.address ?: "none"))

    override fun handleEvents() = true

    private inline fun IPCClient.sendRichPresence(action: RichPresence.Builder.() -> Unit) =
        sendRichPresence(RichPresence.Builder().apply(action).build())

}
