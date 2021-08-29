/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.special

import io.netty.buffer.Unpooled
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.utils.ClientUtils.displayChatMessage
import net.ccbluex.liquidbounce.utils.ClientUtils.logger
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * @author UnstoppableLeaks @ blackspigot.com
 */
class AntiModDisable : MinecraftInstance(), Listenable
{
	@EventTarget
	fun onPacket(event: PacketEvent)
	{
		val packet = event.packet

		if (enabled && !mc.isIntegratedServerRunning) try
		{
			if (blockFMLProxyPackets && packet.javaClass.name.equals("net.minecraftforge.fml.common.network.internal.FMLProxyPacket", ignoreCase = true)) action(event, "FMLProxyPacket")

			val provider = classProvider

			if (provider.isCPacketCustomPayload(packet))
			{
				val customPayload = packet.asCPacketCustomPayload()
				val channelName = customPayload.channelName

				// Block ClientBrandRetriever packets
				if (blockClientBrandRetrieverPackets && customPayload.channelName.equals("MC|Brand", ignoreCase = true))
				{
					customPayload.data = provider.createPacketBuffer(Unpooled.buffer()).writeString("vanilla")
					action(event, "ClientBrandRetriever", cancelEvent = false)
				}

				if (blockFMLPackets)
				{
					var fmlChannelName: String? = null
					if (FML_CHANNEL_NAMES.any { channelName.equals(it, ignoreCase = true) }) fmlChannelName = channelName
					if (fmlChannelName != null) action(event, "FML packet ($fmlChannelName)")
				}

				when
				{
					blockWDLPayloads && channelName.contains("WDL", ignoreCase = true) -> action(event, "World Downloader ($channelName)")
					blockBetterSprintingPayloads && channelName.equals("BSprint", ignoreCase = true) -> action(event, "Better Sprinting mod ($channelName)")
					block5zigsmodPayloads && channelName.startsWith("5ZIG", ignoreCase = true) -> action(event, "The 5zig's mod ($channelName)")
					blockPermissionsReplPayloads && channelName.equals("PERMISSIONSREPL", ignoreCase = true) -> action(event, "(?) ($channelName)")
				}
			}
			else if (provider.isSPacketChat(packet))
			{
				val chat = packet.asSPacketChat()
				val text = chat.chatComponent.unformattedText

				if (blockCrackedVapeSabotages) when // Cracked vapes are responding 'I'm using cracked vape!' when a specified chat is received.
				{
					text.contains("\u00A7c \u00A7r\u00A75 \u00A7r\u00A71 \u00A7r\u00A7f", ignoreCase = true) -> action(event, "CrackedVape Type A(type: \"&C &R &5 &R &1 &R &F\", text: \"$text\")")
					CHATCOLOR_RESET_PATTERN.matcher(text).replaceAll(Matcher.quoteReplacement("")).contains("\u00A73 \u00A76 \u00A73 \u00A76 \u00A73 \u00A76 \u00A7d", ignoreCase = true) -> action(event, "CrackedVape Type B(type: \"&3 &6 &3 &6 &3 &6 &D\", text: \"$text\")")
					text.contains("\u00A70\u00A71") && text.contains("\u00A7f\u00A7f", ignoreCase = true) -> action(event, "CrackedVape Type C(type: \"&0&1\" and \"&F&F\"; text: \"$text\")")
					text.contains("\u00A70\u00A72\u00A70\u00A70\u00A7e\u00A7f", ignoreCase = true) -> action(event, "CrackedVape Type D(type: \"&0&2&0&0&E&F\", text: \"$text\")")
					text.contains("\u00A70\u00A72\u00A71\u00A70\u00A7e\u00A7f", ignoreCase = true) -> action(event, "CrackedVape Type E(type: \"&0&2&1&0&E&F\", text: \"$text\")")
					text.contains("\u00A70\u00A72\u00A71\u00A71\u00A7e\u00A7f", ignoreCase = true) -> action(event, "CrackedVape Type F(type: \"&0&2&1&1&E&F\", text: \"$text\")")
					text.startsWith("\u00A70\u00A70", ignoreCase = true) && text.endsWith("\u00A7e\u00A7f", ignoreCase = true) -> action(event, "CrackedVape Type G(type: startsWith \"&0&0\" and endsWith \"&E&F\", text: \"$text\")")
				}
			}
			else if (provider.isSPacketCustomPayload(packet))
			{
				val customPayload = packet.asSPacketCustomPayload()
				val channelName = customPayload.channelName

				when
				{
					blockWDLPayloads && channelName.contains("WDL", ignoreCase = true) -> action(event, "World Downloader ($channelName)")
					blockBetterSprintingPayloads && channelName.equals("BSM", ignoreCase = true) -> action(event, "Better Sprinting mod ($channelName)")
					block5zigsmodPayloads && channelName.startsWith("5ZIG", ignoreCase = true) -> action(event, "The 5zig's mod ($channelName)")
					blockPermissionsReplPayloads && channelName.equals("PERMISSIONSREPL", ignoreCase = true) -> action(event, "(?) ($channelName)")
					blockDIPermissionsPayloads && channelName.equals("DIPermissions", ignoreCase = true) -> action(event, "(?) ($channelName)")
					blockCrackedVapeSabotages && channelName.contains("LOLIMAHCKER", ignoreCase = true) -> action(event, "CrackedVape Type H(type: Custom payload channel \"$channelName\")")
					blockSchematicaPayloads && channelName.equals("Schematica", ignoreCase = true) -> action(event, "Schematica mod ($channelName)")
				}
			}
		}
		catch (e: Exception)
		{
			logger.error("[AntiModDisable] Unexpected exception while filtering packets", e)
		}
	}

	override fun handleEvents(): Boolean = true

	companion object
	{
		private val CHATCOLOR_RESET_PATTERN = Pattern.compile("\u00A7r", Pattern.LITERAL)
		private val FML_CHANNEL_NAMES = arrayOf("FML", "FORGE", "REGISTER")

		var enabled = true

		var blockFMLPackets = true
		var blockFMLProxyPackets = true
		var blockClientBrandRetrieverPackets = true
		var blockWDLPayloads = true
		var blockBetterSprintingPayloads = true
		var block5zigsmodPayloads = true
		var blockPermissionsReplPayloads = true
		var blockDIPermissionsPayloads = true
		var blockCrackedVapeSabotages = true
		var blockSchematicaPayloads = true
		var debug = false

		@JvmStatic
		fun canBlockForgeChannelPacket(channelName: String): Boolean
		{
			if (enabled && !mc.isIntegratedServerRunning)
			{
				when
				{
					blockWDLPayloads && channelName.contains("WDL", ignoreCase = true) ->
					{
						action(null, "ForgeChannelPacket - World Downloader ($channelName)")
						return true
					}

					blockBetterSprintingPayloads && channelName.equals("BSM", ignoreCase = true) ->
					{
						action(null, "ForgeChannelPacket - Better Sprinting mod ($channelName)")
						return true
					}

					block5zigsmodPayloads && channelName.contains("5ZIG", ignoreCase = true) ->
					{
						action(null, "ForgeChannelPacket - The 5zig's mod ($channelName)")
						return true
					}

					blockPermissionsReplPayloads && channelName.equals("PERMISSIONSREPL", ignoreCase = true) ->
					{
						action(null, "ForgeChannelPacket - (?) ($channelName)")
						return true
					}

					blockDIPermissionsPayloads && channelName.equals("DIPERMISSIONS", ignoreCase = true) ->
					{
						action(null, "ForgeChannelPacket - (?) ($channelName\")")
						return true
					}

					blockCrackedVapeSabotages && channelName.contains("LOLIMAHCKER", ignoreCase = true) ->
					{
						action(null, "CrackedVape Type I(type: ForgeChannelPacket channel \"$channelName\")")
						return true
					}

					blockSchematicaPayloads && channelName.equals("Schematica", ignoreCase = true) ->
					{
						action(null, "ForgeChannelPacket - Schematica mod ($channelName)")
						return true
					}
				}
			}
			return false
		}

		private fun action(event: PacketEvent?, type: String, cancelEvent: Boolean = true)
		{
			if (cancelEvent) event?.cancelEvent()
			if (debug) displayChatMessage(mc.thePlayer, "[AntiModDisable] $type -> ${if (cancelEvent) "Cancelled" else "Spoofed"}")
		}
	}
}
