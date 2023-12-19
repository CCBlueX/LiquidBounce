package net.ccbluex.liquidbounce.utils

import net.minecraft.entity.Entity
import net.minecraft.util.MathHelper.cos
import net.minecraft.util.MathHelper.sin
import net.minecraft.util.Vec3

object LookUtils : MinecraftInstance() {

    fun isLookingOnEntities(target: Entity, lookThresholdValue: Double): Boolean {
        val player = mc.thePlayer ?: return false
        val playerRotation = player.rotationYawHead

        val lookVec = Vec3(
            -sin(playerRotation * (Math.PI.toFloat() / 180f)).toDouble(),
            0.0,
            cos(playerRotation * (Math.PI.toFloat() / 180f)).toDouble()
        ).normalize()

        val playerPos = player.positionVector.addVector(0.0, player.eyeHeight.toDouble(), 0.0)
        val entityPos = target.positionVector.addVector(0.0, target.eyeHeight.toDouble(), 0.0)

        val directionToEntity = entityPos.subtract(playerPos).normalize()
        val dotProductThreshold = lookVec.dotProduct(directionToEntity)

        return dotProductThreshold > lookThresholdValue
    }

}