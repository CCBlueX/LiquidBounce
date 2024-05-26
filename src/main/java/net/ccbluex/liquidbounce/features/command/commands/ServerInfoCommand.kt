/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.event.EventManager.registerListener
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.ServerUtils.serverData
import net.minecraft.client.multiplayer.ServerAddress

object ServerInfoCommand : Command("serverinfo"), Listenable {
    init {
        registerListener(this)
    }

    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        if (mc.currentServerData == null || mc.isSingleplayer) {
            chat("This command does not work in single player.")
            return
        }

        val serverAddress = ServerAddress.fromString(serverData?.serverIP) ?: return
        val data = mc.currentServerData ?: return

        chat("Server info:")
        chat("§7Name: §8${data.serverName}")
        chat("§7IP: §8${serverAddress.ip}:${serverAddress.port}")
        chat("§7Players: §8${data.populationInfo}")
        chat("§7MOTD: §8${data.serverMOTD}")
        chat("§7ServerVersion: §8${data.gameVersion}")
        chat("§7ProtocolVersion: §8${data.version}")
        chat("§7Ping: §8${data.pingToServer}")
    }

    override fun handleEvents() = true
}