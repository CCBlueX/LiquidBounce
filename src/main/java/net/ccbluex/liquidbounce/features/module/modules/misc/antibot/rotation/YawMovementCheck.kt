package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.rotation

import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.Vec3

class YawMovementCheck : BotCheck("rotation.yaw")
{
    override val isActive: Boolean
        get() = AntiBot.rotationYawEnabledValue.get()

    private val previousYawMap = mutableMapOf<Int, Float>()
    private val yawMovement = mutableSetOf<Int>()

    override fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer): Boolean = target.entityId !in yawMovement

    override fun onEntityMove(theWorld: WorldClient, thePlayer: EntityPlayer, target: EntityPlayer, isTeleport: Boolean, newPos: Vec3, rotating: Boolean, newYaw: Float, newPitch: Float, onGround: Boolean)
    {
        if (rotating)
        {
            val entityId = target.entityId

            val prevPitch = previousYawMap.computeIfAbsent(entityId) { newYaw }

            if (RotationUtils.getAngleDifference(newYaw, prevPitch) > AntiBot.rotationYawThresholdValue.get() && entityId !in yawMovement) yawMovement.add(entityId)

            previousYawMap[entityId] = newYaw
        }
    }

    override fun clear()
    {
        previousYawMap.clear()
        yawMovement.clear()
    }
}
