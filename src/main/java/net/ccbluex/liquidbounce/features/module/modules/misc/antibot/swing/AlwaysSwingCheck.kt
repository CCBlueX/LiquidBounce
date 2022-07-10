package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.swing

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.S0BPacketAnimation

class AlwaysSwingCheck : BotCheck("swing.always")
{
    override val isActive: Boolean
        get() = AntiBot.alwaysSwingEnabledValue.get()

    private val checked = mutableSetOf<Int>()
    private val delayMap = mutableMapOf<Int, MSTimer>()

    override fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer): Boolean = target.entityId in checked

    override fun onPacket(event: PacketEvent)
    {
        val theWorld = mc.theWorld ?: return

        val packet = event.packet

        if (packet is S0BPacketAnimation)
        {
            val entityId = packet.entityID
            val entity = theWorld.getEntityByID(entityId)
            if (entity != null && entity is EntityLivingBase && packet.animationType == 0)
            {
                val timer = delayMap.computeIfAbsent(entityId) { MSTimer().apply(MSTimer::reset) }

                if (entityId !in checked && timer.hasTimePassed(AntiBot.alwaysSwingThresholdTimeValue.get().toLong())) checked += entityId

                timer.reset()
                delayMap[entityId] = timer
            }
        }
    }

    override fun clear()
    {
        delayMap.clear()
    }
}
