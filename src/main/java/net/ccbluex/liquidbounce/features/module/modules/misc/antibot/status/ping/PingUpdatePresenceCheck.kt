package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.status.ping

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.S38PacketPlayerListItem
import java.util.*

class PingUpdatePresenceCheck : BotCheck("status.ping.updatePresence")
{
    override val isActive: Boolean
        get() = AntiBot.pingUpdatePresenceEnabledValue.get()

    private val notUpdated = mutableSetOf<UUID>()

    override fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer) = target.uniqueID in notUpdated

    override fun onPacket(event: PacketEvent)
    {
        val packet = event.packet

        if (packet is S38PacketPlayerListItem)
        {
            val updatedPlayers = packet.entries

            val playerInfoMap = mc.netHandler.playerInfoMap

            @Suppress("NON_EXHAUSTIVE_WHEN") when (packet.action)
            {
                S38PacketPlayerListItem.Action.UPDATE_LATENCY ->
                {
                    val updatesPlayerUUIDs = updatedPlayers.map { it.profile.id }
                    val tabPlayerUUIDs = playerInfoMap.map { it.gameProfile.id }

                    val uuidListToString = { uuidList: Collection<UUID> -> uuidList.joinToString(prefix = "[", postfix = "]") { uuid -> playerInfoMap.firstOrNull { it.gameProfile.id == uuid }?.gameProfile?.name ?: "$uuid" } }

                    if (AntiBot.pingUpdatePresenceValidateEnabledValue.get())
                    {
                        val allMatches = AntiBot.pingUpdatePresenceValidateModeValue.get().equals("AllMatches", ignoreCase = true)
                        val prevPingUpdatedPlayerUUIDList = tabPlayerUUIDs.filterNot(notUpdated::contains)
                        if (if (allMatches) !updatesPlayerUUIDs.all(prevPingUpdatedPlayerUUIDList::contains) else updatesPlayerUUIDs.none(prevPingUpdatedPlayerUUIDList::contains))
                        {
                            notification { arrayOf("reason=(Player omission)", "omitted=${if (allMatches) uuidListToString(updatesPlayerUUIDs.filterNot(prevPingUpdatedPlayerUUIDList::contains)) else "<none matches>"}") }
                            return
                        }
                    }

                    if (notUpdated.isEmpty()) notUpdated.addAll(tabPlayerUUIDs.filterNot(updatesPlayerUUIDs::contains)) else notUpdated.removeAll(tabPlayerUUIDs.filter(updatesPlayerUUIDs::contains).filter(notUpdated::contains))
                    if (notUpdated.isNotEmpty()) notification { arrayOf("reason=(Ping update omission)", "list=${uuidListToString(notUpdated)}") }
                }

                S38PacketPlayerListItem.Action.REMOVE_PLAYER -> notUpdated.removeAll(updatedPlayers.map { it.profile.id })
            }
        }
    }
}
