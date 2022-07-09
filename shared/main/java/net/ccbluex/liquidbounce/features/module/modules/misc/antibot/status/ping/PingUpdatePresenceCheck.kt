package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.status.ping

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketPlayerListItem
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import java.util.*

class PingUpdatePresenceCheck : BotCheck("status.ping.updatePresence")
{
    override val isActive: Boolean
        get() = AntiBot.pingUpdatePresenceEnabledValue.get()

    private val notUpdated = mutableSetOf<UUID>()

    override fun isBot(theWorld: IWorldClient, thePlayer: IEntity, target: IEntityPlayer) = target.uniqueID in notUpdated

    override fun onPacket(event: PacketEvent)
    {
        val packet = event.packet

        if (classProvider.isSPacketPlayerListItem(packet))
        {
            val playerListItemPacket = packet.asSPacketPlayerListItem()
            val updatedPlayers = playerListItemPacket.players

            val playerInfoMap = mc.netHandler.playerInfoMap

            @Suppress("NON_EXHAUSTIVE_WHEN") when (playerListItemPacket.action)
            {
                ISPacketPlayerListItem.WAction.UPDATE_LATENCY ->
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

                ISPacketPlayerListItem.WAction.REMOVE_PLAYER -> notUpdated.removeAll(updatedPlayers.map { it.profile.id })
            }
        }
    }
}
