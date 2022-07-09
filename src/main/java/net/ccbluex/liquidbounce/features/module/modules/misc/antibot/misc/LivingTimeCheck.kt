package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.misc

import net.ccbluex.liquidbounce.api.minecraft.client.entity.Entity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.EntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.WorldClient
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck

class LivingTimeCheck : BotCheck("misc.ticksExisted")
{
    override val isActive: Boolean
        get() = AntiBot.livingTimeEnabledValue.get()

    override fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer) = target.ticksExisted < AntiBot.livingTimeTicksValue.get()
}
