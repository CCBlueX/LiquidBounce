package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.misc

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck

class NoColorCheck : BotCheck("misc.noColor")
{
    override val isActive: Boolean
        get() = AntiBot.noColorValue.get()

    override fun isBot(theWorld: IWorldClient, thePlayer: IEntity, target: IEntityPlayer) = "\u00A7" !in target.displayName.formattedText.replace("\u00A7r", "")
}
