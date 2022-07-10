package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.misc

import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer

class NoColorCheck : BotCheck("misc.noColor")
{
    override val isActive: Boolean
        get() = AntiBot.noColorValue.get()

    override fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer) = "\u00A7" !in target.displayName.formattedText.replace("\u00A7r", "")
}
