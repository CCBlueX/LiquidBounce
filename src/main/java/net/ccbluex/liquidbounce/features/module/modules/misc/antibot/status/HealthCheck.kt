package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.status

import net.ccbluex.liquidbounce.api.minecraft.client.entity.Entity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.EntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.WorldClient
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck

class HealthCheck : BotCheck("misc.health.over20")
{
    override val isActive: Boolean
        get() = AntiBot.healthValue.get()

    override fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer) = target.health > 20F
}
