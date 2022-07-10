package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.rotation

import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.Vec3

class InvalidPitchCheck : BotCheck("rotation.invalidPitch")
{
    override val isActive: Boolean
        get() = AntiBot.rotationInvalidPitchEnabledValue.get()

    private val invalidPitch = mutableSetOf<Int>()

    override fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer): Boolean = if (AntiBot.rotationInvalidPitchKeepVLValue.get()) target.entityId in invalidPitch else (target.rotationPitch !in -90f..90f)

    override fun onEntityMove(theWorld: WorldClient, thePlayer: EntityPlayer, target: EntityPlayer, isTeleport: Boolean, newPos: Vec3, rotating: Boolean, newYaw: Float, newPitch: Float, onGround: Boolean)
    {
        if (rotating)
        {
            val entityId = target.entityId
            if ((newPitch > 90.0F || newPitch < -90.0F) && entityId !in invalidPitch) invalidPitch.add(entityId)
        }
    }

    override fun clear()
    {
        invalidPitch.clear()
    }
}
