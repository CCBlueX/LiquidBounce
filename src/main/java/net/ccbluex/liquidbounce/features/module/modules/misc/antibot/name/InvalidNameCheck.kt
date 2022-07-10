package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.name

import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer

class InvalidNameCheck : BotCheck("name.invalidName")
{
    override val isActive: Boolean
        get() = AntiBot.invalidProfileNameValue.get()

    private val invalidProfileNameRegex = Regex("[^a-zA-Z0-9_]*")

    override fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer) = invalidProfileNameRegex.containsMatchIn(target.gameProfile.name)
}
