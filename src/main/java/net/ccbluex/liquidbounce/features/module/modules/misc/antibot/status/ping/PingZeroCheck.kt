package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.status.ping

import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer

class PingZeroCheck : BotCheck("status.ping.zero")
{
    override val isActive: Boolean
        get() = AntiBot.pingZeroValue.get()

    override fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer) = mc.netHandler.getPlayerInfo(target.uniqueID)?.responseTime == 0
}
