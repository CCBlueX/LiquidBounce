/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.command.Command

class ServerInfoCommand : Command("serverinfo"), Listenable
{
	init
	{
		LiquidBounce.eventManager.registerListener(this)
	}

	private var ip = ""
	private var port = 0

	/**
	 * Execute commands with provided [args]
	 */
	override fun execute(args: Array<String>)
	{
		val thePlayer = mc.thePlayer

		val serverData = mc.currentServerData

		if (serverData == null)
		{
			chat(thePlayer, "This command does not work in single player.")
			return
		}

		chat(thePlayer, "Server infos:")
		chat(thePlayer, "\u00A77Name: \u00A78${serverData.serverName}")
		chat(thePlayer, "\u00A77IP: \u00A78$ip:$port")
		chat(thePlayer, "\u00A77Players: \u00A78${serverData.populationInfo}")
		chat(thePlayer, "\u00A77MOTD: \u00A78${serverData.serverMOTD}")
		chat(thePlayer, "\u00A77ServerVersion: \u00A78${serverData.gameVersion}")
		chat(thePlayer, "\u00A77ProtocolVersion: \u00A78${serverData.version}")
		chat(thePlayer, "\u00A77Ping: \u00A78${serverData.pingToServer}")
	}

	@EventTarget
	fun onPacket(event: PacketEvent)
	{
		val packet = event.packet

		if (classProvider.isCPacketHandshake(packet))
		{
			val handshake = packet.asCPacketHandshake()

			ip = handshake.ip
			port = handshake.port
		}
	}

	override fun handleEvents() = true
}
