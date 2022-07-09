package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.name

import net.ccbluex.liquidbounce.api.minecraft.client.entity.Entity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.EntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.WorldClient
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import net.ccbluex.liquidbounce.utils.render.ColorUtils

class DuplicateInWorldAdditionCheck : BotCheck("name.duplicateInWorld.addition")
{
    override val isActive: Boolean
        get() = AntiBot.duplicateInWorldAdditionEnabledValue.get()

    override fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer): Boolean = false

    override fun onPacket(event: PacketEvent)
    {
        val theWorld = mc.theWorld ?: return

        val packet = event.packet

        if (packet is SPacketSpawnPlayer)
        {
            val playerSpawnPacket = packet.asSPacketSpawnPlayer()

            val entityId = playerSpawnPacket.entityID
            val uuid = playerSpawnPacket.uuid
            val playerInfo = mc.netHandler.playerInfoMap.firstOrNull { it.gameProfile.id == uuid }
            if (playerInfo != null)
            {
                val stripColors = AntiBot.duplicateInWorldAdditionStripColorsValue.get()
                val tryStripColors = { string: String -> if (stripColors) ColorUtils.stripColor(string) else string }

                val useDisplayName = AntiBot.duplicateInWorldAdditionModeValue.get().equals("DisplayName", ignoreCase = true)

                val profileName = playerInfo.gameProfile.name
                val displayName = playerInfo.displayName?.formattedText
                val playerName = tryStripColors((if (useDisplayName) displayName else profileName) ?: "")

                if (theWorld.loadedEntityList.filter(classProvider::isEntityPlayer).map(Entity::asEntityPlayer).any { playerName == tryStripColors((if (useDisplayName) it.displayName.formattedText else it.gameProfile.name) ?: return@any false) })
                {
                    event.cancelEvent()
                    remove(theWorld, entityId, profileName, displayName)
                }
            }
        }
    }
}
