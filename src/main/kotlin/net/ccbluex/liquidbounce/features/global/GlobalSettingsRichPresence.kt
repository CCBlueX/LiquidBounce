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
package net.ccbluex.liquidbounce.features.global

import com.jagrosh.discordipc.IPCClient
import com.jagrosh.discordipc.entities.ActivityType
import com.jagrosh.discordipc.entities.RichPresence
import com.jagrosh.discordipc.entities.StatusDisplayType
import com.jagrosh.discordipc.entities.pipe.PipeStatus
import com.jagrosh.discordipc.exceptions.NoDiscordClientException
import kotlinx.coroutines.Dispatchers
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.LiquidBounce.clientBranch
import net.ccbluex.liquidbounce.LiquidBounce.clientCommit
import net.ccbluex.liquidbounce.LiquidBounce.clientVersion
import net.ccbluex.liquidbounce.config.gson.util.jsonArrayOf
import net.ccbluex.liquidbounce.config.gson.util.jsonObject
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.ClientShutdownEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.hideSensitiveAddress
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.protocolVersion

/**
 * Discord Rich Presence
 *
 * todo: use ordered multi choose (https://github.com/CCBlueX/LiquidBounce/pull/7350), which allows
 *   custom ordering of parts.
 */
object GlobalSettingsRichPresence : ToggleableValueGroup(
    name = "RichPresence",
    enabled = true,
    aliases = listOf("DiscordPresence")
) {

    private const val IPC_APP_ID = 443472046031110144L

    private val activityType by enumChoice("ActivityType", PresenceActivityType.COMPETING)
    private val statusDisplayType by enumChoice("StatusDisplayType", PresenceStatusDisplayType.NAME)

    private val separatorText by text("Separator", " - ")

    private val detailsParts by multiEnumChoice(
        "DetailsParts",
        RichPresencePart.CLIENT_NAME,
        RichPresencePart.CLIENT_VERSION,
        RichPresencePart.CLIENT_AUTHOR
    )
    private val stateParts by multiEnumChoice(
        "StateParts",
        RichPresencePart.MODULES_SUMMARY,
        RichPresencePart.PROTOCOL,
    )

    private object LargeImageConfig : ToggleableValueGroup(
        parent = this,
        name = "LargeImage",
        enabled = true,
    ) {
        val asset by enumChoice("Asset", PresenceAsset.LOGO)
        val parts by multiEnumChoice(
            "Parts",
            RichPresencePart.PROTOCOL,
        )
    }

    private object SmallImageConfig : ToggleableValueGroup(
        parent = this,
        name = "SmallImage",
        enabled = false,
    ) {
        val asset by enumChoice("Asset", PresenceAsset.LOGO)
        val parts by multiEnumChoice(
            "Parts",
            RichPresencePart.BRANCH,
            RichPresencePart.COMMIT
        )
    }

    private val largeImage = tree(LargeImageConfig)
    private val smallImage = tree(SmallImageConfig)

    private val buttons = jsonArrayOf(
        jsonObject {
            "label"("Website")
            "url"("https://liquidbounce.net")
        },
        jsonObject {
            "label"("LiquidProxy")
            "url"("https://liquidproxy.net")
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
        if (doNotTryToConnect || ipcClient?.status == PipeStatus.CONNECTED) {
            return
        }

        runCatching {
            ipcClient = IPCClient(IPC_APP_ID).also { it.connect() }
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

        ipcClient.sendRichPresence {
            setActivityType(activityType.activityType)
            setStatusDisplayType(statusDisplayType.statusDisplayType)
            setStartTimestamp(timestamp)

            if (largeImage.enabled) {
                largeImage.asset.assetValue?.let { assetValue ->
                    setLargeImageWithTooltip(assetValue, buildText(largeImage.parts))
                }
            }
            if (smallImage.enabled) {
                smallImage.asset.assetValue?.let { assetValue ->
                    setSmallImageWithTooltip(assetValue, buildText(smallImage.parts))
                }
            }

            setDetails(buildText(detailsParts))
            setState(buildText(stateParts))

            setButtons(buttons)
        }
    }

    @Suppress("unused")
    private val shutdownHandler = handler<ClientShutdownEvent> {
        shutdownIpc()
    }

    private fun buildText(parts: Set<RichPresencePart>): String {
        val pieces = RichPresencePart.entries
            .filter { it in parts }
            .mapNotNull { it.getText() }
            .filter { it.isNotBlank() }

        if (pieces.isEmpty()) {
            return ""
        }

        return pieces.joinToString(separatorText)
    }

    private inline fun IPCClient.sendRichPresence(builderAction: RichPresence.Builder.() -> Unit) =
        sendRichPresence(RichPresence.Builder().apply(builderAction).build())

    /**
     * Always running after initialized
     */
    override val running get() = LiquidBounce.isInitialized

    private enum class RichPresencePart(override val tag: String) : Tagged {
        CLIENT_NAME("Client Name"),
        CLIENT_VERSION("Client Version"),
        CLIENT_AUTHOR("Client Author"),
        MODULES_SUMMARY("Modules"),
        PROTOCOL("Protocol"),
        SERVER("Server"),
        BRANCH("Branch"),
        COMMIT("Commit");

        fun getText(): String? = when (this) {
            CLIENT_NAME -> LiquidBounce.CLIENT_NAME
            CLIENT_VERSION -> clientVersion
            CLIENT_AUTHOR -> LiquidBounce.CLIENT_AUTHOR
            MODULES_SUMMARY -> "${ModuleManager.count { it.running }}/${ModuleManager.count()} modules"
            PROTOCOL -> protocolVersion.let { "${it.name} (${it.version})" }
            SERVER -> (mc.currentServer?.ip ?: "none").hideSensitiveAddress()
            BRANCH -> clientBranch
            COMMIT -> clientCommit
        }

    }

    @Suppress("unused")
    private enum class PresenceActivityType(
        override val tag: String,
        val activityType: ActivityType,
    ) : Tagged {
        PLAYING("Playing", ActivityType.Playing),
        LISTENING("Listening", ActivityType.Listening),
        WATCHING("Watching", ActivityType.Watching),
        COMPETING("Competing", ActivityType.Competing),
    }

    @Suppress("unused")
    private enum class PresenceStatusDisplayType(
        override val tag: String,
        val statusDisplayType: StatusDisplayType,
    ) : Tagged {
        NAME("Name", StatusDisplayType.Name),
        STATE("State", StatusDisplayType.State),
        DETAILS("Details", StatusDisplayType.Details),
    }

    private enum class PresenceAsset(
        override val tag: String,
        val assetValue: String?,
    ) : Tagged {
        LOGO("Logo", "liquidbounce"),
    }

}
