package net.ccbluex.liquidbounce.utils

import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.Vec3
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

object LookUtils : MinecraftInstance() {

    fun isLookingOnEntities(target: Entity, lookThresholdValue: Double): Boolean {
        val player = mc.thePlayer ?: return false
        val playerRotation = player.rotationYawHead

        if (target !is EntityLivingBase) return false

        val lookVec = Vec3(
            -sin(playerRotation * (Math.PI.toFloat() / 180f)).toDouble(),
            0.0,
            cos(playerRotation * (Math.PI.toFloat() / 180f)).toDouble()
        ).normalize()

        val playerPos = player.positionVector.addVector(0.0, player.eyeHeight.toDouble(), 0.0)
        val entityPos = target.positionVector.addVector(0.0, target.eyeHeight.toDouble(), 0.0)

        val directionToEntity = entityPos.subtract(playerPos).normalize()
        val dotProductThreshold = lookVec.dotProduct(directionToEntity)

        val angle = if (lookVec.crossProduct(directionToEntity).yCoord >= 0) {
            Math.toDegrees(acos(dotProductThreshold))
        } else {
            360 - Math.toDegrees(acos(dotProductThreshold))
        }

        return angle < lookThresholdValue
    }

}