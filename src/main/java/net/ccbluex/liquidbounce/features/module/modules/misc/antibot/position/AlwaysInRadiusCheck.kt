package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.position

import net.ccbluex.liquidbounce.api.minecraft.client.entity.Entity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.EntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.WorldClient
import net.ccbluex.liquidbounce.api.minecraft.util.Vec3
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck

class AlwaysInRadiusCheck : BotCheck("position.alwaysInRadius")
{
    override val isActive: Boolean
        get() = AntiBot.alwaysInRadiusEnabledValue.get()

    private val outOfRadius = mutableSetOf<Int>()

    override fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer): Boolean = target.entityId !in outOfRadius

    override fun onEntityMove(theWorld: WorldClient, thePlayer: EntityPlayer, target: EntityPlayer, isTeleport: Boolean, newPos: Vec3, rotating: Boolean, newYaw: Float, newPitch: Float, onGround: Boolean)
    {
        val entityId = target.entityId
        if (entityId !in outOfRadius && thePlayer.getDistanceToEntity(target) > AntiBot.alwaysInRadiusRadiusValue.get()) outOfRadius.add(entityId)
    }

    override fun clear()
    {
        outOfRadius.clear()
    }
}
