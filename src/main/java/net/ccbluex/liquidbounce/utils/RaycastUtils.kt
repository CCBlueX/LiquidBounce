/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.features.module.modules.combat.Backtrack
import net.ccbluex.liquidbounce.utils.extensions.hitBox
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer


object RaycastUtils : MinecraftInstance() {

    @JvmStatic
    fun raycastEntity(range: Double, entityFilter: EntityFilter) = raycastEntity(range, RotationUtils.serverRotation.yaw, RotationUtils.serverRotation.pitch, entityFilter)

    @JvmStatic
    fun raycastEntity(range: Double, yaw: Float, pitch: Float, entityFilter: EntityFilter): Entity? {
        val renderViewEntity = mc.renderViewEntity

        if (renderViewEntity != null && mc.theWorld != null) {
            var blockReachDistance = range
            val eyePosition = renderViewEntity.getPositionEyes(1f)
            val entityLook = RotationUtils.getVectorForRotation(Rotation(yaw, pitch))
            val vector = eyePosition.addVector(entityLook.xCoord * blockReachDistance, entityLook.yCoord * blockReachDistance, entityLook.zCoord * blockReachDistance)
            val entityList = mc.theWorld!!.getEntitiesInAABBexcluding(renderViewEntity, renderViewEntity.entityBoundingBox.addCoord(entityLook.xCoord * blockReachDistance, entityLook.yCoord * blockReachDistance, entityLook.zCoord * blockReachDistance).expand(1.0, 1.0, 1.0)) {
                it != null && (it !is EntityPlayer || !it.isSpectator) && it.canBeCollidedWith()
            }

            var pointedEntity: Entity? = null

            for (entity in entityList) {
                if (!entityFilter.canRaycast(entity))
                    continue

                val checkEntity = {
                    val axisAlignedBB = entity.hitBox

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

                    false
                }

                // Check newest entity first
                checkEntity()
                Backtrack.loopThroughBacktrackData(entity, checkEntity)
            }

            return pointedEntity
        }

        return null
    }

    interface EntityFilter {
        fun canRaycast(entity: Entity?): Boolean
    }
}