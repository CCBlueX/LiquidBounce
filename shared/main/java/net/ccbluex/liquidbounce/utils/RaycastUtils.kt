/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import kotlin.math.cos
import kotlin.math.sin


object RaycastUtils : MinecraftInstance() {

    @JvmStatic
    fun raycastEntity(range: Double, entityFilter: EntityFilter) = raycastEntity(range, RotationUtils.serverRotation.yaw, RotationUtils.serverRotation.pitch, entityFilter)

    private fun raycastEntity(range: Double, yaw: Float, pitch: Float, entityFilter: EntityFilter): IEntity? {
        val renderViewEntity = mc.renderViewEntity

        if (renderViewEntity != null && mc.theWorld != null) {
            var blockReachDistance = range
            val eyePosition = renderViewEntity.getPositionEyes(1f)

            val yawCos = cos(-yaw * 0.017453292f - Math.PI.toFloat())
            val yawSin = sin(-yaw * 0.017453292f - Math.PI.toFloat())
            val pitchCos = (-cos(-pitch * 0.017453292f.toDouble())).toFloat()
            val pitchSin = sin(-pitch * 0.017453292f.toDouble()).toFloat()

            val entityLook = WVec3((yawSin * pitchCos).toDouble(), pitchSin.toDouble(), (yawCos * pitchCos).toDouble())
            val vector = eyePosition.addVector(entityLook.xCoord * blockReachDistance, entityLook.yCoord * blockReachDistance, entityLook.zCoord * blockReachDistance)
            val entityList = mc.theWorld!!.getEntitiesInAABBexcluding(renderViewEntity, renderViewEntity.entityBoundingBox.addCoord(entityLook.xCoord * blockReachDistance, entityLook.yCoord * blockReachDistance, entityLook.zCoord * blockReachDistance).expand(1.0, 1.0, 1.0)) {
                it != null && (!classProvider.isEntityPlayer(it) || !it.asEntityPlayer().spectator) && it.canBeCollidedWith()
            }

            var pointedEntity: IEntity? = null

            for (entity in entityList) {
                if (!entityFilter.canRaycast(entity))
                    continue

                val collisionBorderSize = entity.collisionBorderSize.toDouble()
                val axisAlignedBB = entity.entityBoundingBox.expand(collisionBorderSize, collisionBorderSize, collisionBorderSize)

                val movingObjectPosition = axisAlignedBB.calculateIntercept(eyePosition, vector)

                if (axisAlignedBB.isVecInside(eyePosition)) {
                    if (blockReachDistance >= 0.0) {
                        pointedEntity = entity
                        blockReachDistance = 0.0
                    }
                } else if (movingObjectPosition != null) {
                    val eyeDistance = eyePosition.distanceTo(movingObjectPosition.hitVec)

                    if (eyeDistance < blockReachDistance || blockReachDistance == 0.0) {
                        if (entity == renderViewEntity.ridingEntity && !renderViewEntity.canRiderInteract()) {
                            if (blockReachDistance == 0.0)
                                pointedEntity = entity
                        } else {
                            pointedEntity = entity
                            blockReachDistance = eyeDistance
                        }
                    }
                }
            }

            return pointedEntity
        }

        return null
    }

    interface EntityFilter {
        fun canRaycast(entity: IEntity?): Boolean
    }
}