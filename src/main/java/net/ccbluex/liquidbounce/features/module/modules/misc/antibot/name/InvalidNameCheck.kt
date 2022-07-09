package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.name

import net.ccbluex.liquidbounce.api.minecraft.client.entity.Entity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.EntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.WorldClient
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck

class InvalidNameCheck : BotCheck("name.invalidName")
{
    override val isActive: Boolean
        get() = AntiBot.invalidProfileNameValue.get()

    private val invalidProfileNameRegex = Regex("[^a-zA-Z0-9_]*")

    override fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer) = invalidProfileNameRegex.containsMatchIn(target.gameProfile.name)
}
