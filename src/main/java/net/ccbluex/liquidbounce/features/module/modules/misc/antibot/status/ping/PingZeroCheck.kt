package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.status.ping

import net.ccbluex.liquidbounce.api.minecraft.client.entity.Entity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.EntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.WorldClient
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck

class PingZeroCheck : BotCheck("status.ping.zero")
{
    override val isActive: Boolean
        get() = AntiBot.pingZeroValue.get()

    override fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer) = mc.netHandler.getPlayerInfo(target.uniqueID)?.responseTime == 0
}
