package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.name

import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer

class NPCCheck : BotCheck("name.npc")
{
    override val isActive: Boolean
        get() = AntiBot.npcValue.get()

    override fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer) = "\u00A78[NPC]" in target.displayName.formattedText
}
