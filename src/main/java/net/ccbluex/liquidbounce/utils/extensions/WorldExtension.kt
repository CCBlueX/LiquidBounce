package net.ccbluex.liquidbounce.utils.extensions

import net.ccbluex.liquidbounce.utils.RotationUtils
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.AxisAlignedBB
import net.minecraft.world.World
import net.minecraft.world.chunk.Chunk
import kotlin.math.ceil
import kotlin.math.floor

fun World.raycastEntity(entity: Entity, range: Double, yaw: Float = RotationUtils.serverRotation.yaw, pitch: Float = RotationUtils.serverRotation.pitch, expandRange: Double = 1.0, aabbGetter: (Entity) -> AxisAlignedBB = Entity::getEntityBoundingBox, entityFilter: (Entity?) -> Boolean): Entity?
{
    var reach = range
    val rayStartPos = entity.getPositionEyes(1f)
    val ridingEntity = entity.ridingEntity
    val canRiderInteract = entity.canRiderInteract()

    val yawRadians = yaw.toRadians
    val pitchRadians = pitch.toRadians

    val yawCos = (-yawRadians - PI).cos
    val yawSin = (-yawRadians - PI).sin
    val pitchCos = -(-pitchRadians).cos
    val pitchSin = (-pitchRadians).sin

    val lookX = yawSin * pitchCos
    val lookZ = yawCos * pitchCos

    val rayEndPos = rayStartPos.plus(lookX * reach, pitchSin * reach, lookZ * reach)

    val entityList = getEntitiesInAABBexcluding(entity, entity.entityBoundingBox.addCoord(lookX * reach, pitchSin * reach, lookZ * reach).expand(expandRange, expandRange, expandRange)) { it != null && (it !is EntityPlayer || !it.isSpectator) && it.canBeCollidedWith() }

    var pointedEntity: Entity? = null

    entityList.filter(entityFilter::invoke).forEach { raycastedEntity ->
        val collisionBorder = aabbGetter(raycastedEntity)

        val rayIntercept = collisionBorder.calculateIntercept(rayStartPos, rayEndPos)

        if (collisionBorder.isVecInside(rayStartPos))
        {
            if (reach >= 0.0)
            {
                pointedEntity = raycastedEntity
                reach = 0.0
            }
        }
        else if (rayIntercept != null)
        {
            val hitDistance = rayStartPos.distanceTo(rayIntercept.hitVec)

            if (hitDistance < reach || reach == 0.0) if (raycastedEntity == ridingEntity && !canRiderInteract)
            {
                if (reach == 0.0) pointedEntity = raycastedEntity
            }
            else
            {
                pointedEntity = raycastedEntity
                reach = hitDistance
            }
        }
    }

    return pointedEntity
}

fun World.getEntitiesInRadius(entity: Entity, radius: Double = 16.0): List<Entity>
{
    val box = entity.entityBoundingBox.expand(radius, radius, radius)

    val chunkMinX = floor(box.minX * 0.0625).toInt()
    val chunkMaxX = ceil(box.maxX * 0.0625).toInt()

    val chunkMinZ = floor(box.minZ * 0.0625).toInt()
    val chunkMaxZ = ceil(box.maxZ * 0.0625).toInt()

    val entities = mutableListOf<Entity>()

    (chunkMinX..chunkMaxX).forEach { x ->
        (chunkMinZ..chunkMaxZ).asSequence().map { z -> getChunkFromChunkCoords(x, z) }.filter(Chunk::isLoaded).forEach { it.getEntitiesWithinAABBForEntity(entity, box, entities, null) }
    }

    return entities
}
