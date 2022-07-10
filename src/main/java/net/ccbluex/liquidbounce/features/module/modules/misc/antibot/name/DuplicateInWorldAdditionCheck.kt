package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.name

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.S0CPacketSpawnPlayer

class DuplicateInWorldAdditionCheck : BotCheck("name.duplicateInWorld.addition")
{
    override val isActive: Boolean
        get() = AntiBot.duplicateInWorldAdditionEnabledValue.get()

    override fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer): Boolean = false

    override fun onPacket(event: PacketEvent)
    {
        val theWorld = mc.theWorld ?: return

        val packet = event.packet

        if (packet is S0CPacketSpawnPlayer)
        {

            val entityId = packet.entityID
            val uuid = packet.player
            val playerInfo = mc.netHandler.playerInfoMap.firstOrNull { it.gameProfile.id == uuid }
            if (playerInfo != null)
            {
                val stripColors = AntiBot.duplicateInWorldAdditionStripColorsValue.get()
                val tryStripColors = { string: String -> if (stripColors) ColorUtils.stripColor(string) else string }

                val useDisplayName = AntiBot.duplicateInWorldAdditionModeValue.get().equals("DisplayName", ignoreCase = true)

                val profileName = playerInfo.gameProfile.name
                val displayName = playerInfo.displayName?.formattedText
                val playerName = tryStripColors((if (useDisplayName) displayName else profileName) ?: "")

                if (theWorld.loadedEntityList.filterIsInstance<EntityPlayer>().any { playerName == tryStripColors((if (useDisplayName) it.displayName.formattedText else it.gameProfile.name) ?: return@any false) })
                {
                    event.cancelEvent()
                    remove(theWorld, entityId, profileName, displayName)
                }
            }
        }
    }
}
