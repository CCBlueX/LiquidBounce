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
package net.ccbluex.liquidbounce.features.command.commands.ingame

import com.mojang.brigadier.CommandDispatcher
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.features.command.CommandRegistrar
import net.ccbluex.liquidbounce.features.command.arguments.MultiTaggedArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.ServerObserver
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.copyable
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.client.network
import net.ccbluex.liquidbounce.utils.client.onClick
import net.ccbluex.liquidbounce.utils.client.onHover
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.client.warning
import net.ccbluex.liquidbounce.utils.math.roundToDecimalPlaces
import net.ccbluex.liquidbounce.utils.text.hideSensitiveAddress
import net.ccbluex.liquidbounce.utils.text.joinToText
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import kotlin.time.Duration.Companion.seconds

/**
 * ServerInfo Command
 *
 * Displays the current server information, including:
 * - Server Address (Typed In)
 * - Resolved Server Address
 * - Server ID
 * - Server Type (Premium or Cracked)
 * - Server Brand (Brand that the server sent us, F3 menu)
 * - Advertised Version (Version that the server sent us)
 * - Detected Version (Gathers actual server version from known packs packet)
 * - TPS (Same as .tps)
 * - Ping (Same as .ping)
 * - Payload Channels
 * - Transactions (5x ping payloads)
 * - Transaction Differences
 * - Guessed Anti Cheat (Same as AntiCheatDetect)
 * - Hosting Information (Shown when command is being executed with hosting parameter)
 * - Plugins (Same as Plugins Module, requires plugins detect parameter)
 *
 * The command supports active detection modes for more thorough analysis.
 */
object CommandServerInfo : EventListener, CommandRegistrar {
    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("serverinfo") serverinfo@{
            requires { it.isIngame }
            execSuspend {
                this@serverinfo.printInformation(emptySet())
            }
            argument(
                "detect",
                MultiTaggedArgumentType("detect", DetectionType.entries, DetectionType::tag),
            ) { detect ->
                execSuspend { ctx ->
                    val detectionTypes = ctx.get<List<DetectionType>>(detect)
                    this@serverinfo.runActiveDetection(detectionTypes)
                }
            }
        }
    }

    /**
     * Runs active detection for specified detection types
     *
     * @param detectionTypes Collection of detection types to run
     */
    private suspend fun CmdI18n.runActiveDetection(detectionTypes: Collection<DetectionType>) {
        chat(regular(t("detecting")))

        // Run plugin detection if requested
        if (DetectionType.PLUGINS in detectionTypes) {
            if (!ServerObserver.captureCommandSuggestions(10.seconds)) {
                chat(markAsError(t("pluginsDetectionTimeout")))
            }
        }

        // Request hosting information if requested
        if (DetectionType.HOSTING in detectionTypes) {
            ServerObserver.requestHostingInformation()
        }

        printInformation(detectionTypes)
    }

    /**
     * Print all server information to chat
     *
     * @param detections Optional list of active detections that were run
     */
    private fun CmdI18n.printInformation(detections: Collection<DetectionType> = emptyList()) {
        // Gather basic server information
        val serverInfo = network.serverData
        val resolvedServerAddress = ServerObserver.serverAddress?.toString()
        val tps = ServerObserver.tps
        val ping = network.getPlayerInfo(player.uuid)?.latency ?: 0
        val advertisedVersion = "${serverInfo?.version?.string} (${serverInfo?.protocol})"
        val detectedServerVersion = ServerObserver.serverVersion ?: "<= 1.20.4"

        chat(warning(t("header")))
        printStyledText("address", serverInfo?.ip?.hideSensitiveAddress())
        printStyledText("resolvedAddress", resolvedServerAddress?.hideSensitiveAddress())
        printStyledText("serverId", ServerObserver.serverId)
        printStyledText("serverType", ServerObserver.serverType?.tag)
        printStyledText("brand", network.serverBrand())
        printStyledText("advertisedVersion", advertisedVersion)
        printStyledText(
            "detectedVersion",
            detectedServerVersion,
            hover = HoverEvent.ShowText(
                t("detectedVersion.description", variable(detectedServerVersion))
            )
        )

        // Performance metrics
        printStyledText(
            "tps",
            if (tps.isNaN()) t("nan").string else tps.roundToDecimalPlaces(2).toString()
        )
        printStyledText("ping", ping.toString())

        // Server Channels and transactions
        val channelsText = ServerObserver.payloadChannels.map { id ->
            variable(id.toString())
        }.joinToText(regular(", "))
        printStyledComponent("channels", channelsText)
        val transactionsText = ServerObserver.transactions.map { variable(it.toString()) }.joinToText(regular(", "))
        printStyledComponent("transactions", transactionsText)

        val transactionDiffText = ServerObserver.transactions
            .windowed(2) { it[1] - it[0] }
            .map { variable(it.toString()) }
            .joinToText(regular(", "))
        printStyledComponent("transactionDifferences", transactionDiffText)

        // Anti-cheat detection
        val guessedAntiCheat = ServerObserver.guessAntiCheat(serverInfo?.ip ?: "")?.let(::variable)
            ?: markAsError("N/A")
        printStyledComponent(
            "guessedAntiCheat",
            guessedAntiCheat,
            hover = HoverEvent.ShowText(t("guessedAntiCheat.description"))
        )

        printHostingInformation()
        printPluginInformation()

        // Show available detection methods if none were specified
        if (detections.isEmpty()) {
            val detectionList = DetectionType.entries.map { variable(it.tag) }.joinToText(regular(", "))
            printStyledComponent("detectParameter", detectionList, formatting = ::warning)
        }
    }

    private fun CmdI18n.printHostingInformation() {
        val ipData = ServerObserver.hostingInformation ?: return

        printStyledText("hostingIp", ipData.ip)
        printStyledText("hostingHostname", ipData.hostname)
        printStyledText("hostingOrganization", ipData.org)
        printStyledText("hostingCountry", ipData.country)
        printStyledText("hostingCity", ipData.city)
        printStyledText("hostingRegion", ipData.region)
    }

    private fun CmdI18n.printPluginInformation() {
        val plugins = ServerObserver.plugins ?: return

        val pluginCount = plugins.size
        val pluginList = ServerObserver.formattedPluginList?.joinToText(regular(", ")) ?: markAsError("N/A")

        chat(regular(t("plugins", variable(pluginCount.toString()), pluginList)))
    }

    /**
     * Sends a styled command result with copyable content.
     *
     * @param key Translation key suffix (resolved through [CmdI18n.t], e.g. "address")
     * @param data Optional data to be displayed and copied
     * @param formatting Function to apply formatting to the text (default: regular)
     * @param hover Optional hover event (defaults to "Click to copy" tooltip)
     * @param click Optional click action type (defaults to [ClickEvent.CopyToClipboard])
     */
    private fun CmdI18n.printStyledText(
        key: String,
        data: String? = null,
        formatting: (MutableComponent) -> MutableComponent = ::regular,
        hover: HoverEvent? = HoverEvent.ShowText(translation("liquidbounce.tooltip.clickToCopy")),
        click: ClickEvent? = data?.let(ClickEvent::CopyToClipboard),
    ) {
        val content = data?.let(::variable) ?: markAsError("N/A")
        val resultText = formatting(t(key, content))

        chat(resultText.onHover(hover).onClick(click))
    }

    /**
     * Sends a styled command result with copyable content and custom text component.
     *
     * @param key Translation key suffix (resolved through [CmdI18n.t], e.g. "channels")
     * @param textComponent Text component to display
     * @param copyContent Optional content to copy when clicked (defaults to text component's string representation)
     * @param formatting Function to apply formatting to the text (default: regular)
     * @param hover Optional hover event (defaults to "Click to copy" tooltip)
     */
    private fun CmdI18n.printStyledComponent(
        key: String,
        textComponent: Component? = null,
        copyContent: String? = null,
        formatting: (MutableComponent) -> MutableComponent = ::regular,
        hover: HoverEvent? = HoverEvent.ShowText(translation("liquidbounce.tooltip.clickToCopy"))
    ) {
        val displayComponent = textComponent ?: markAsError("N/A")
        val content = copyContent ?: displayComponent.string

        chat(formatting(t(key, displayComponent)).copyable(copyContent = content, hover = hover))
    }

    /**
     * Detection for further server information
     */
    private enum class DetectionType(override val tag: String) : Tagged {
        PLUGINS("Plugins"),
        HOSTING("Hosting");
    }
}
