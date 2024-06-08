/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import kotlinx.coroutines.*
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_CLOUD
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.modules.misc.PacketDebugger.selectedPackets
import net.ccbluex.liquidbounce.features.module.modules.misc.PacketDebugger.packetType
import net.ccbluex.liquidbounce.script.api.global.Chat
import net.ccbluex.liquidbounce.utils.misc.HttpUtils

object PacketDebuggerCommand : Command("packetdebugger", "debug") {

    private var packetList: Map<String, Set<String>?> = emptyMap()

    init {
        runBlocking {
            launch { packetList = loadPacketList("$CLIENT_CLOUD/packets-list/packets") }
        }.isCompleted
    }

    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        if (packetType != "Custom") {
            chat("§cPlease select 'Custom' mode in PacketDebugger first.")
            return
        }

        val usedAlias = args[0].lowercase()

        if (args.size < 2) {
            chatSyntax("$usedAlias <add/remove/list>")
            return
        }

        when (args[1].lowercase()) {
            "add" -> {
                if (args.size < 3) {
                    chatSyntax("$usedAlias add <packet>")
                    return
                }

                val packetName = args[2]
                if (selectedPackets.contains(packetName)) {
                    chat("§cPacket §b$packetName §cis already in the debug list.")
                } else {
                    selectedPackets.add(packetName)
                    chat("§b$packetName §ahas been added to the list.")
                }
            }
            "remove" -> {
                if (args.size < 3) {
                    chatSyntax("$usedAlias remove <packet>")
                    return
                }

                val packetName = args[2]
                if (selectedPackets.contains(packetName)) {
                    selectedPackets.remove(packetName)
                    chat("§b$packetName §6has been removed from the list.")
                } else {
                    chat("§b$packetName §cis not in the list.")
                }
            }
            "list" -> {
                val packets = selectedPackets
                chat("Packets List:")
                packets.forEach { chat("§b$it") }
            }
            else -> chatSyntax("$usedAlias <add/remove/list>")
        }
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) {
            return emptyList()
        }

        return when (args.size) {
            1 -> listOf("add", "remove", "list").filter { it.startsWith(args[0], true) }
            2 -> {
                when (args[0].lowercase()) {
                    "add" -> {
                        val input = args[1].lowercase()
                        packetList.flatMap { it.value.orEmpty() }
                            .filter { it.lowercase().startsWith(input) }
                    }
                    "remove" -> {
                        val input = args[1].lowercase()
                        selectedPackets.filter { it.lowercase().startsWith(input) }
                    }
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }
    }

    private suspend fun loadPacketList(url: String): Map<String, Set<String>> {
        return try {
            val (response, code) = fetchDataAsync(url)
            if (code == 200) {
                val packetList = response.split("\n").filter { it.isNotBlank() && it.isNotEmpty() }.map { it.trim() }.toSet()
                Chat.print("§aSuccessfully loaded §9${packetList.size} §apackets.")
                mapOf(url to packetList)
            } else {
                Chat.print("§cFailed to load packet list. §9(ERROR CODE: $code)")
                emptyMap()
            }
        } catch (e: Exception) {
            Chat.print("§cFailed to load packet list. §9(${e.message})")
            e.printStackTrace()
            emptyMap()
        }
    }

    private suspend fun fetchDataAsync(url: String): Pair<String, Int> {
        return withContext(Dispatchers.IO) {
            HttpUtils.request(url, "GET").let { Pair(it.first, it.second) }
        }
    }
}