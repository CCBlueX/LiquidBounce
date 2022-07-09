package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.status

import net.ccbluex.liquidbounce.api.minecraft.client.entity.Entity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.EntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.WorldClient
import net.ccbluex.liquidbounce.api.minecraft.util.Vec3
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck

class AirCheck : BotCheck("status.air")
{
    override val isActive: Boolean
        get() = AntiBot.airValue.get()

    private val air = mutableSetOf<Int>()

    override fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer): Boolean = target.entityId !in air

    override fun onEntityMove(theWorld: WorldClient, thePlayer: EntityPlayer, target: EntityPlayer, isTeleport: Boolean, newPos: Vec3, rotating: Boolean, newYaw: Float, newPitch: Float, onGround: Boolean)
    {
        val entityId = target.entityId
        if (!onGround && entityId !in air) air.add(entityId)
    }

    override fun clear()
    {
        air.clear()
    }
}
