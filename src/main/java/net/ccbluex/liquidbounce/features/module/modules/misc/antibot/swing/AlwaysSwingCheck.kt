package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.swing

import net.ccbluex.liquidbounce.api.minecraft.client.entity.Entity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.EntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.WorldClient
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import net.ccbluex.liquidbounce.utils.timer.MSTimer

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

        if (packet is SPacketAnimation)
        {
            val swingPacket = packet.asSPacketAnimation()
            val entityId = swingPacket.entityID
            val entity = theWorld.getEntityByID(entityId)
            if (entity != null && entity is EntityLivingBase && swingPacket.animationType == 0)
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
