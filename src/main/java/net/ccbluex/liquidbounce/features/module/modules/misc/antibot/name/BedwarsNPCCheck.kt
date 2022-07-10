package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.name

import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer

class BedwarsNPCCheck : BotCheck("name.bwNPC")
{
    override val isActive: Boolean
        get() = AntiBot.bedWarsNPCValue.get()

    override fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer): Boolean
    {
        val displayName = target.displayName.formattedText
        return (displayName.isEmpty() || displayName[0] != '\u00A7') && displayName.endsWith("\u00A7r")
    }
}
