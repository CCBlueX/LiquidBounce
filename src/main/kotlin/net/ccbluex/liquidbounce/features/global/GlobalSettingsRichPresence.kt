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

import kotlinx.coroutines.Dispatchers
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.LiquidBounce.clientBranch
import net.ccbluex.liquidbounce.LiquidBounce.clientCommit
import net.ccbluex.liquidbounce.LiquidBounce.clientVersion
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.ClientShutdownEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.protocolVersion
import net.ccbluex.liquidbounce.utils.text.hideSensitiveAddress
import net.ccbluex.discordipc.DiscordActivity
import net.ccbluex.discordipc.DiscordIpcClient
import net.ccbluex.discordipc.DiscordIpcPlatform
import net.ccbluex.discordipc.NoDiscordClientException
import net.minecraft.SharedConstants
import net.minecraft.util.Util
import java.time.Instant
import java.util.concurrent.Executors

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
        RichPresencePart.CLIENT_VERSION
    )
    private val stateParts by multiEnumChoice(
        "StateParts",
        RichPresencePart.MODULES_SUMMARY,
        RichPresencePart.CLIENT_COMMIT,
    )

    private object LargeImageConfig : ToggleableValueGroup(
        parent = this,
        name = "LargeImage",
        enabled = true,
    ) {
        val asset by enumChoice("Asset", PresenceAsset.LOGO)
        val parts by multiEnumChoice(
            "Parts",
            RichPresencePart.PROTOCOL_VERSION,
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
            RichPresencePart.CLIENT_BRANCH,
            RichPresencePart.CLIENT_COMMIT
        )
    }

    private val largeImage = tree(LargeImageConfig)
    private val smallImage = tree(SmallImageConfig)

    private val buttons = listOf(
        DiscordActivity.Button("Website", "https://liquidbounce.net"),
        DiscordActivity.Button("LiquidProxy", "https://liquidproxy.net"),
    )

    // IPC Client
    private var ipcClient: DiscordIpcClient? = null

    @Volatile
    private var timestamp = Instant.now()

    private var doNotTryToConnect = false

    init {
        doNotIncludeAlways()
    }

    override fun onEnabled() {
        timestamp = Instant.now()
        doNotTryToConnect = false
    }

    private fun connectIpc() {
        if (doNotTryToConnect || ipcClient?.state == DiscordIpcClient.State.CONNECTED) {
            return
        }

        runCatching {
            ipcClient?.close()
            ipcClient = DiscordIpcClient(
                applicationId = IPC_APP_ID,
                platform = Util.getPlatform().toDiscordIpcPlatform(),
                Executors.newVirtualThreadPerTaskExecutor(),
            ).also { it.connect() }
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
        val ipcClient = ipcClient ?: return
        this.ipcClient = null

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
        if (ipcClient == null || ipcClient.state != DiscordIpcClient.State.CONNECTED) {
            return@tickHandler
        }

        val activity = DiscordActivity(
            type = activityType.activityType,
            statusDisplayType = statusDisplayType.statusDisplayType,
            startTimestamp = timestamp,
            details = buildText(detailsParts),
            state = buildText(stateParts),
            largeImage = largeImage.takeIf { it.enabled }?.asset?.assetValue?.let { assetValue ->
                DiscordActivity.Image(assetValue, buildText(largeImage.parts))
            },
            smallImage = smallImage.takeIf { it.enabled }?.asset?.assetValue?.let { assetValue ->
                DiscordActivity.Image(assetValue, buildText(smallImage.parts))
            },
            buttons = buttons,
        )

        runCatching {
            ipcClient.sendActivity(activity)
        }.onFailure {
            logger.warn("Failed to update Discord Rich Presence.", it)
        }
    }

    @Suppress("unused")
    private val shutdownHandler = handler<ClientShutdownEvent> {
        shutdownIpc()
    }

    private fun buildText(parts: Set<RichPresencePart>): String {
        val pieces = parts.mapNotNull { it.getText()?.takeIf(String::isNotBlank) }

        if (pieces.isEmpty()) {
            return ""
        }

        return pieces.joinToString(separatorText)
    }

    /**
     * Always running after initialized
     */
    override val running get() = LiquidBounce.isInitialized

    private enum class RichPresencePart(override val tag: String) : Tagged {
        CLIENT_NAME("ClientName"),
        CLIENT_VERSION("ClientVersion"),
        CLIENT_AUTHOR("ClientAuthor"),
        CLIENT_BRANCH("ClientBranch"),
        CLIENT_COMMIT("ClientCommit"),
        MODULES_SUMMARY("Modules"),
        MINECRAFT_VERSION("MinecraftVersion"),
        PROTOCOL_VERSION("ProtocolVersion"),
        SERVER("Server");

        fun getText(): String? = when (this) {
            CLIENT_NAME -> LiquidBounce.CLIENT_NAME
            CLIENT_VERSION -> clientVersion
            CLIENT_AUTHOR -> LiquidBounce.CLIENT_AUTHOR
            MODULES_SUMMARY -> "${ModuleManager.count { it.running }}/${ModuleManager.count()} modules"
            MINECRAFT_VERSION -> SharedConstants.getCurrentVersion().name().let { "Minecraft $it" }
            PROTOCOL_VERSION -> protocolVersion.let { "Joined with Minecraft ${it.name}" }
            SERVER -> (mc.currentServer?.ip ?: "none").hideSensitiveAddress()
            CLIENT_BRANCH -> clientBranch
            CLIENT_COMMIT -> clientCommit
        }

    }

    @Suppress("unused")
    private enum class PresenceActivityType(
        override val tag: String,
        val activityType: DiscordActivity.Type,
    ) : Tagged {
        PLAYING("Playing", DiscordActivity.Type.PLAYING),
        LISTENING("Listening", DiscordActivity.Type.LISTENING),
        WATCHING("Watching", DiscordActivity.Type.WATCHING),
        COMPETING("Competing", DiscordActivity.Type.COMPETING),
    }

    @Suppress("unused")
    private enum class PresenceStatusDisplayType(
        override val tag: String,
        val statusDisplayType: DiscordActivity.StatusDisplayType,
    ) : Tagged {
        NAME("Name", DiscordActivity.StatusDisplayType.NAME),
        STATE("State", DiscordActivity.StatusDisplayType.STATE),
        DETAILS("Details", DiscordActivity.StatusDisplayType.DETAILS),
    }

    private enum class PresenceAsset(
        override val tag: String,
        val assetValue: String?,
    ) : Tagged {
        LOGO("Logo", "liquidbounce"),
    }

}

private fun Util.OS.toDiscordIpcPlatform(): DiscordIpcPlatform = when (this) {
    Util.OS.WINDOWS -> DiscordIpcPlatform.WINDOWS
    Util.OS.LINUX, Util.OS.OSX -> DiscordIpcPlatform.UNIX
    Util.OS.SOLARIS, Util.OS.UNKNOWN -> DiscordIpcPlatform.UNSUPPORTED
}
