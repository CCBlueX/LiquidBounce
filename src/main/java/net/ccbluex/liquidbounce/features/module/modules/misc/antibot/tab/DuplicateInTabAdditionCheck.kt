package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.tab

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.S38PacketPlayerListItem

class DuplicateInTabAdditionCheck : BotCheck("tab.duplicateInTab.addition")
{
    override val isActive: Boolean
        get() = AntiBot.duplicateInTabAdditionEnabledValue.get()

    override fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer): Boolean = false

    override fun onPacket(event: PacketEvent)
    {
        val packet = event.packet

        // DuplicateInTab - Addition
        if (packet is S38PacketPlayerListItem)
        {
            if (packet.action == S38PacketPlayerListItem.Action.ADD_PLAYER)
            {
                val players = packet.entries

                val stripColors = AntiBot.duplicateInTabAdditionStripColorsValue.get()
                val tryStripColors = { name: String -> if (stripColors) ColorUtils.stripColor(name) else name }

                val useDisplayName = AntiBot.duplicateInTabAdditionNameModeValue.get().equals("DisplayName", ignoreCase = true)
                val queryNameFromInfo = { playerInfo: NetworkPlayerInfo -> (if (useDisplayName) playerInfo.displayName?.formattedText else playerInfo.gameProfile.name)?.let(tryStripColors) }
                val queryNameFromData = { playerData: S38PacketPlayerListItem.AddPlayerData -> (if (useDisplayName) playerData.displayName?.formattedText else playerData.profile.name)?.let(tryStripColors) }

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
