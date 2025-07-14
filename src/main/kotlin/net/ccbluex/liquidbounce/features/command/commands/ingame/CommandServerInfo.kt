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
 */
package net.ccbluex.liquidbounce.features.command.commands.ingame

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.Parameters
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.text.HoverEvent

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
object CommandServerInfo : CommandFactory, EventListener {

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("serverinfo")
            .requiresIngame()
            .parameter(
                Parameters.enumChoices<DetectionType>("detect")
                    .optional()
                    .build()
            )
            .handler { command, args ->
                val detectionTypes = args.getOrNull(0) as? Set<DetectionType>

                if (!detectionTypes.isNullOrEmpty()) {
                    runActiveDetection(command, detectionTypes)
                } else {
                    printInformation(command)
                }
            }
            .build()
    }

    /**
     * Runs active detection for specified detection types
     *
     * @param command The command instance
     * @param detectionTypes Collection of detection types to run
     */
    private fun runActiveDetection(command: Command, detectionTypes: Collection<DetectionType>) {
        Sequence(this) {
            chat(regular(command.result("detecting")))

            // Run plugin detection if requested
            if (DetectionType.PLUGINS in detectionTypes) {
                ServerObserver.captureCommandSuggestions()
                // Timeout after 5 seconds
                waitConditional(20 * 5) { ServerObserver.plugins != null }

                if (ServerObserver.plugins == null) {
                    chat(markAsError(command.result("pluginsDetectionTimeout")))
                }
            }

            // Request hosting information if requested
            if (DetectionType.HOSTING in detectionTypes) {
                ServerObserver.requestHostingInformation()
            }

            printInformation(command, detectionTypes)
        }
    }

    /**
     * Print all server information to chat
     *
     * @param command The command instance
     * @param detections Optional list of active detections that were run
     */
    private fun printInformation(command: Command, detections: Collection<DetectionType> = emptyList()) {
        // Gather basic server information
        val serverInfo = network.serverInfo
        val resolvedServerAddress = ServerObserver.serverAddress?.toString()
        val tps = ServerObserver.tps
        val ping = network.getPlayerListEntry(player.uuid)?.latency ?: 0
        val advertisedVersion = "${serverInfo?.version?.convertToString()} (${serverInfo?.protocolVersion})"
        val detectedServerVersion = ServerObserver.serverVersion ?: "<= 1.20.4"

        chat(warning(command.result("header")))
        command.printStyledText("address", serverInfo?.address?.hideSensitiveAddress())
        command.printStyledText("resolvedAddress", resolvedServerAddress?.hideSensitiveAddress())
        command.printStyledText("serverId", ServerObserver.serverId)
        command.printStyledText("serverType", ServerObserver.serverType?.choiceName)
        command.printStyledText("brand", network.brand)
        command.printStyledText("advertisedVersion", advertisedVersion)
        command.printStyledText(
            "detectedVersion",
            detectedServerVersion,
            hover = HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                command.result("detectedVersion.description", variable(detectedServerVersion))
            )
        )

        // Performance metrics
        command.printStyledText(
            "tps",
            if (tps.isNaN()) command.result("nan").string else tps.roundToDecimalPlaces(2).toString()
        )
        command.printStyledText("ping", ping.toString())

        // Server Channels and transactions
        val channelsText = ServerObserver.payloadChannels.map { id ->
            variable(id.toString())
        }.joinToText(regular(", "))
        command.printStyledComponent("channels", channelsText)
        val transactionsText = ServerObserver.transactions.map { variable(it.toString()) }.joinToText(regular(", "))
        command.printStyledComponent("transactions", transactionsText)

        val transactionDiffText = ServerObserver.transactions
            .windowed(2) { it[1] - it[0] }
            .map { variable(it.toString()) }
            .joinToText(regular(", "))
        command.printStyledComponent("transactionDifferences", transactionDiffText)

        // Anti-cheat detection
        val guessedAntiCheat = ServerObserver.guessAntiCheat(serverInfo?.address ?: "")?.let(::variable)
            ?: markAsError("N/A")
        command.printStyledComponent(
            "guessedAntiCheat",
            guessedAntiCheat,
            hover = HoverEvent(HoverEvent.Action.SHOW_TEXT, command.result("guessedAntiCheat.description"))
        )

        printHostingInformation(command)
        printPluginInformation(command)

        // Show available detection methods if none were specified
        if (detections.isEmpty()) {
            val detectionList = DetectionType.entries.map { variable(it.choiceName) }.joinToText(regular(", "))
            command.printStyledComponent("detectParameter", detectionList, formatting = ::warning)
        }
    }

    private fun printHostingInformation(command: Command) {
        val ipData = ServerObserver.hostingInformation ?: return

        command.printStyledText("hostingIp", ipData.ip)
        command.printStyledText("hostingHostname", ipData.hostname)
        command.printStyledText("hostingOrganization", ipData.org)
        command.printStyledText("hostingCountry", ipData.country)
        command.printStyledText("hostingCity", ipData.city)
        command.printStyledText("hostingRegion", ipData.region)
    }

    private fun printPluginInformation(command: Command) {
        val plugins = ServerObserver.plugins ?: return

        val pluginCount = plugins.size
        val pluginList = ServerObserver.formattedPluginList?.joinToText(regular(", ")) ?: markAsError("N/A")

        chat(regular(command.result("plugins", variable(pluginCount.toString()), pluginList)))
    }

    /**
     * Detection for further server information
     */
    private enum class DetectionType(override val choiceName: String) : NamedChoice {
        PLUGINS("Plugins"),
        HOSTING("Hosting");
    }
}
