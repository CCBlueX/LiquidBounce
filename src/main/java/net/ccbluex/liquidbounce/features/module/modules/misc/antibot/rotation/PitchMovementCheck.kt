package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.rotation

import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.Vec3

class PitchMovementCheck : BotCheck("rotation.pitch")
{
    override val isActive: Boolean
        get() = AntiBot.rotationPitchEnabledValue.get()

    private val previousPitchMap = mutableMapOf<Int, Float>()
    private val pitchMovement = mutableSetOf<Int>()

    override fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer): Boolean = target.entityId !in pitchMovement

    override fun onEntityMove(theWorld: WorldClient, thePlayer: EntityPlayer, target: EntityPlayer, isTeleport: Boolean, newPos: Vec3, rotating: Boolean, newYaw: Float, newPitch: Float, onGround: Boolean)
    {
        if (rotating)
        {
            val entityId = target.entityId

            val prevPitch = previousPitchMap.computeIfAbsent(entityId) { newPitch }

            if (RotationUtils.getAngleDifference(newPitch, prevPitch) > AntiBot.rotationPitchThresholdValue.get() && entityId !in pitchMovement) pitchMovement.add(entityId)

            previousPitchMap[entityId] = newPitch
        }
    }

    override fun clear()
    {
        previousPitchMap.clear()
        pitchMovement.clear()
    }
}
