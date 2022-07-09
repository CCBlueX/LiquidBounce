package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.misc

import net.ccbluex.liquidbounce.api.minecraft.client.entity.Entity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.EntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.WorldClient
import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck

class NeedHitCheck : BotCheck("misc.needHit")
{
    override val isActive: Boolean
        get() = AntiBot.needHitValue.get()

    private val hitted = mutableSetOf<Int>()

    override fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer): Boolean = target.entityId !in hitted

    override fun onAttack(event: AttackEvent)
    {
        val entity = event.targetEntity
        if (entity != null && entity is EntityLivingBase && entity.entityId !in hitted) hitted.add(entity.entityId)
    }

    override fun clear()
    {
        hitted.clear()
    }
}
