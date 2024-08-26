/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.features.module.modules.combat.Backtrack
import net.ccbluex.liquidbounce.features.module.modules.combat.Backtrack.loopThroughBacktrackData
import net.ccbluex.liquidbounce.utils.RotationUtils.getVectorForRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.isVisible
import net.ccbluex.liquidbounce.utils.RotationUtils.serverRotation
import net.ccbluex.liquidbounce.utils.extensions.eyes
import net.ccbluex.liquidbounce.utils.extensions.hitBox
import net.ccbluex.liquidbounce.utils.extensions.plus
import net.ccbluex.liquidbounce.utils.extensions.times
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.item.EntityItemFrame
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.projectile.EntityLargeFireball
import net.minecraft.util.Box
import net.minecraft.util.math.BlockPos
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.Vec3d
import java.util.*

object RaycastUtils : MinecraftInstance() {
    @JvmOverloads
    fun raycastEntity(
        range: Double,
        yaw: Float = serverRotation.yaw,
        pitch: Float = serverRotation.pitch,
        entityFilter: (Entity) -> Boolean
    ): Entity? {
        val renderViewEntity = mc.renderViewEntity

        if (renderViewEntity == null || mc.world == null)
            return null

        var blockReachDistance = range
        val eyePosition = renderViewEntity.eyes
        val entityLook = getVectorForRotation(yaw, pitch)
        val vec = eyePosition + (entityLook * blockReachDistance)

        val entityList = mc.world.getEntities(Entity::class.java) {
            it != null && (it is LivingEntity || it is EntityLargeFireball) && (it !is PlayerEntity || !it.isSpectator) && it.canBeCollidedWith() && it != renderViewEntity
        }

        var pointedEntity: Entity? = null

        for (entity in entityList) {
            if (!entityFilter(entity)) continue

            val checkEntity = {
                val Box = entity.hitBox

                val movingObjectPosition = Box.calculateIntercept(eyePosition, vec)

                if (Box.isVecInside(eyePosition)) {
                    if (blockReachDistance >= 0.0) {
                        pointedEntity = entity
                        blockReachDistance = 0.0
                    }
                } else if (movingObjectPosition != null) {
                    val eyeDistance = eyePosition.distanceTo(movingObjectPosition.hitVec)

                    if (eyeDistance < blockReachDistance || blockReachDistance == 0.0) {
                        if (entity == renderViewEntity.ridingEntity && !renderViewEntity.canRiderInteract()) {
                            if (blockReachDistance == 0.0) pointedEntity = entity
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
            if (Backtrack.mode == "Legacy")
                loopThroughBacktrackData(entity, checkEntity)
        }

        return pointedEntity
    }

    /**
     * Modified mouse object pickup
     */
    fun runWithModifiedRaycastResult(rotation: Rotation, range: Double, wallRange: Double, action: (BlockHitResult) -> Unit) {
        val entity = mc.renderViewEntity

        val prevPointedEntity = mc.pointedEntity
        val prevObjectMouseOver = mc.objectMouseOver

        if (entity != null && mc.world != null) {
            mc.pointedEntity = null

            val buildReach = if (mc.interactionManager.currentGameMode.isCreative) 5.0 else 4.5

            val Vec3d = entity.eyes
            val Vec3d1 = getVectorForRotation(rotation)
            val Vec3d2 = Vec3d.addVector(Vec3d1.xCoord * buildReach, Vec3d1.yCoord * buildReach, Vec3d1.zCoord * buildReach)

            mc.objectMouseOver = entity.world.rayTrace(Vec3d, Vec3d2, false, false, true)

            var d1 = buildReach
            var flag = false

            if (mc.interactionManager.extendedReach()) {
                d1 = 6.0
            } else if (buildReach > 3) {
                flag = true
            }

            if (mc.objectMouseOver != null) {
                d1 = mc.objectMouseOver.hitVec.distanceTo(Vec3d)
            }

            var pointedEntity: Entity? = null
            var Vec3d3: Vec3d? = null

            val list = mc.world.getEntities(LivingEntity::class.java) {
                it != null && (it !is PlayerEntity || !it.isSpectator) && it.canBeCollidedWith() && it != entity
            }

            var d2 = d1

            for (entity1 in list) {
                val f1 = entity1.collisionBorderSize
                val boxes = ArrayList<Box>()

                boxes.add(entity1.entityBoundingBox.expand(f1.toDouble(), f1.toDouble(), f1.toDouble()))

                loopThroughBacktrackData(entity1) {
                    boxes.add(entity1.entityBoundingBox.expand(f1.toDouble(), f1.toDouble(), f1.toDouble()))
                    false
                }

                for (box in boxes) {
                    val intercept = box.calculateIntercept(Vec3d, Vec3d2)

                    if (box.isVecInside(Vec3d)) {
                        if (d2 >= 0) {
                            pointedEntity = entity1
                            Vec3d3 = if (intercept == null) Vec3d else intercept.hitVec
                            d2 = 0.0
                        }
                    } else if (intercept != null) {
                        val d3 = Vec3d.distanceTo(intercept.hitVec)

                        if (!isVisible(intercept.hitVec)) {
                            if (d3 <= wallRange) {
                                if (d3 < d2 || d2 == 0.0) {
                                    pointedEntity = entity1
                                    Vec3d3 = intercept.hitVec
                                    d2 = d3
                                }
                            }

                            continue
                        }

                        if (d3 < d2 || d2 == 0.0) {
                            if (entity1 === entity.ridingEntity && !entity.canRiderInteract()) {
                                if (d2 == 0.0) {
                                    pointedEntity = entity1
                                    Vec3d3 = intercept.hitVec
                                }
                            } else {
                                pointedEntity = entity1
                                Vec3d3 = intercept.hitVec
                                d2 = d3
                            }
                        }
                    }
                }
            }

            if (pointedEntity != null && flag && Vec3d.distanceTo(Vec3d3) > range) {
                pointedEntity = null
                mc.objectMouseOver = BlockHitResult(BlockHitResult.Type.MISS,
                    Objects.requireNonNull(Vec3d3),
                    null,
                    BlockPos(Vec3d3)
                )
            }

            if (pointedEntity != null && (d2 < d1 || mc.objectMouseOver == null)) {
                mc.objectMouseOver = BlockHitResult(pointedEntity, Vec3d3)

                if (pointedEntity is LivingEntity || pointedEntity is EntityItemFrame) {
                    mc.pointedEntity = pointedEntity
                }
            }

            action(mc.objectMouseOver)

            mc.objectMouseOver = prevObjectMouseOver
            mc.pointedEntity = prevPointedEntity
        }
    }
}