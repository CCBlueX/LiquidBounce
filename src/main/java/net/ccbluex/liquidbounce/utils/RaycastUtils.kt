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
import net.minecraft.entity.decoration.ItemFrameEntity
import net.minecraft.entity.player.ClientPlayerEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.projectile.FireballEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.Box
import java.util.*

object RaycastUtils : MinecraftInstance() {
    @JvmOverloads
    fun raycastEntity(
        range: Double,
        yaw: Float = serverRotation.yaw,
        pitch: Float = serverRotation.pitch,
        entityFilter: (Entity) -> Boolean
    ): Entity? {
        val renderViewEntity = mc.cameraEntity

        if (renderViewEntity == null || mc.world == null)
            return null

        var blockReachDistance = range
        val eyePosition = renderViewEntity.eyes
        val entityLook = getVectorForRotation(yaw, pitch)
        val vec = eyePosition + (entityLook * blockReachDistance)

        val entityList = mc.world.entities.filter {
            it != null && (it is LivingEntity || it is FireballEntity) && (it !is ClientPlayerEntity || !it.isSpectator) && it.collides() && it != renderViewEntity
        }

        var pointedEntity: Entity? = null

        for (entity in entityList) {
            if (!entityFilter(entity)) continue

            val checkEntity = {
                val box = entity.hitBox

                val movingObjectPosition = box.method_585(eyePosition, vec)

                if (box.contains(eyePosition)) {
                    if (blockReachDistance >= 0.0) {
                        pointedEntity = entity
                        blockReachDistance = 0.0
                    }
                } else if (movingObjectPosition != null) {
                    val eyeDistance = eyePosition.distanceTo(movingObjectPosition.pos)

                    if (eyeDistance < blockReachDistance || blockReachDistance == 0.0) {
                        if (entity == renderViewEntity.vehicle && renderViewEntity !is LivingEntity) {
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
        val entity = mc.cameraEntity

        val prevPointedEntity = mc.targetedEntity
        val prevresult = mc.result

        if (entity != null && mc.world != null) {
            mc.targetedEntity = null

            val buildReach = if (mc.interactionManager.currentGameMode.isCreative) 5.0 else 4.5

            var vec3d = entity.eyes
            val vec3d1 = getVectorForRotation(rotation)
            val vec3d2 = vec3d.add(vec3d1.x * buildReach, vec3d1.y * buildReach, vec3d1.z * buildReach)

            mc.result = entity.world.rayTrace(vec3d, vec3d2, false, false, true)

            var d1 = buildReach
            var flag = false

            if (mc.interactionManager.hasExtendedReach()) {
                d1 = 6.0
            } else if (buildReach > 3) {
                flag = true
            }

            if (mc.result != null) {
                d1 = mc.result.pos.distanceTo(vec3d)
            }

            var pointedEntity: Entity? = null

            val list = mc.world.entities.filter {
                it != null && (it !is PlayerEntity || !it.isSpectator) && it.collides() && it != entity
            }

            var d2 = d1

            for (entity1 in list) {
                val f1 = entity1.targetingMargin
                val boxes = ArrayList<Box>()

                boxes.add(entity1.boundingBox.expand(f1.toDouble(), f1.toDouble(), f1.toDouble()))

                loopThroughBacktrackData(entity1) {
                    boxes.add(entity1.boundingBox.expand(f1.toDouble(), f1.toDouble(), f1.toDouble()))
                    false
                }

                for (box in boxes) {
                    val intercept = box.method_585(vec3d, vec3d2)

                    if (box.contains(vec3d)) {
                        if (d2 >= 0) {
                            pointedEntity = entity1
                            vec3d = if (intercept == null) vec3d else intercept.pos
                            d2 = 0.0
                        }
                    } else if (intercept != null) {
                        val d3 = vec3d.distanceTo(intercept.pos)

                        if (!isVisible(intercept.pos)) {
                            if (d3 <= wallRange) {
                                if (d3 < d2 || d2 == 0.0) {
                                    pointedEntity = entity1
                                    vec3d = intercept.pos
                                    d2 = d3
                                }
                            }

                            continue
                        }

                        if (d3 < d2 || d2 == 0.0) {
                            if (entity1 === entity.vehicle && entity !is LivingEntity) {
                                if (d2 == 0.0) {
                                    pointedEntity = entity1
                                    vec3d = intercept.pos
                                }
                            } else {
                                pointedEntity = entity1
                                vec3d = intercept.pos
                                d2 = d3
                            }
                        }
                    }
                }
            }

            if (pointedEntity != null && flag && vec3d.distanceTo(vec3d) > range) {
                pointedEntity = null
                mc.result = BlockHitResult(BlockHitResult.Type.MISS,
                    Objects.requireNonNull(vec3d),
                    null,
                    BlockPos(vec3d)
                )
            }

            if (pointedEntity != null && (d2 < d1 || mc.result == null)) {
                mc.result = BlockHitResult(pointedEntity, vec3d)

                if (pointedEntity is LivingEntity || pointedEntity is ItemFrameEntity) {
                    mc.targetedEntity = pointedEntity
                }
            }

            action(mc.result)

            mc.result = prevresult
            mc.targetedEntity = prevPointedEntity
        }
    }
}