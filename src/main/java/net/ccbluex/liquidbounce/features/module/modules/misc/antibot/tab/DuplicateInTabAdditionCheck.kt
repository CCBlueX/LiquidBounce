package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.tab

import net.ccbluex.liquidbounce.api.minecraft.client.entity.Entity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.EntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.WorldClient
import net.ccbluex.liquidbounce.api.minecraft.client.network.NetworkPlayerInfo
import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketPlayerListItem
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import net.ccbluex.liquidbounce.utils.render.ColorUtils

class DuplicateInTabAdditionCheck : BotCheck("tab.duplicateInTab.addition")
{
    override val isActive: Boolean
        get() = AntiBot.duplicateInTabAdditionEnabledValue.get()

    override fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer): Boolean = false

    override fun onPacket(event: PacketEvent)
    {
        val packet = event.packet

        // DuplicateInTab - Addition
        if (packet is SPacketPlayerListItem)
        {
            val playerListItem = packet.asSPacketPlayerListItem()
            if (playerListItem.action == ISPacketPlayerListItem.WAction.ADD_PLAYER)
            {
                val players = playerListItem.players

                val stripColors = AntiBot.duplicateInTabAdditionStripColorsValue.get()
                val tryStripColors = { name: String -> if (stripColors) ColorUtils.stripColor(name) else name }

                val useDisplayName = AntiBot.duplicateInTabAdditionNameModeValue.get().equals("DisplayName", ignoreCase = true)
                val queryNameFromInfo = { playerInfo: NetworkPlayerInfo -> (if (useDisplayName) playerInfo.displayName?.formattedText else playerInfo.gameProfile.name)?.let(tryStripColors) }
                val queryNameFromData = { playerData: ISPacketPlayerListItem.WAddPlayerData -> (if (useDisplayName) playerData.displayName?.formattedText else playerData.profile.name)?.let(tryStripColors) }

                val currentPlayerList = mc.netHandler.playerInfoMap.map { queryNameFromInfo(it) ?: "" }

                val itr = players.listIterator()
                while (itr.hasNext())
                {
                    val playerData = itr.next()
                    if ((queryNameFromData(playerData) ?: continue) in currentPlayerList)
                    {
                        itr.remove()
                        notification { arrayOf("profileName=${playerData.profile.name}", "displayName=${playerData.displayName?.formattedText}\u00A7r") }
                    }
                }

                if (players.isEmpty()) event.cancelEvent()
            }
        }
    }
}
