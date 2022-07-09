package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.rotation

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import net.ccbluex.liquidbounce.utils.RotationUtils

class PitchMovementCheck : BotCheck("rotation.pitch")
{
    override val isActive: Boolean
        get() = AntiBot.rotationPitchEnabledValue.get()

    private val previousPitchMap = mutableMapOf<Int, Float>()
    private val pitchMovement = mutableSetOf<Int>()

    override fun isBot(theWorld: IWorldClient, thePlayer: IEntity, target: IEntityPlayer): Boolean = target.entityId !in pitchMovement

    override fun onEntityMove(theWorld: IWorldClient, thePlayer: IEntityPlayer, target: IEntityPlayer, isTeleport: Boolean, newPos: WVec3, rotating: Boolean, newYaw: Float, newPitch: Float, onGround: Boolean)
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
