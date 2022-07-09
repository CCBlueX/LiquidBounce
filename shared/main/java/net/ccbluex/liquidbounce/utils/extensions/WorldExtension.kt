package net.ccbluex.liquidbounce.utils.extensions

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.util.IAxisAlignedBB
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.api.minecraft.world.IChunk
import net.ccbluex.liquidbounce.api.minecraft.world.IWorld
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.RotationUtils
import kotlin.math.ceil
import kotlin.math.floor

fun IWorld.raycastEntity(entity: IEntity, range: Double, yaw: Float = RotationUtils.serverRotation.yaw, pitch: Float = RotationUtils.serverRotation.pitch, expandRange: Double = 1.0, aabbGetter: (IEntity) -> IAxisAlignedBB = IEntity::entityBoundingBox, entityFilter: (IEntity?) -> Boolean): IEntity?
{
    val func = MinecraftInstance.functions

    var reach = range
    val rayStartPos = entity.getPositionEyes(1f)
    val ridingEntity = entity.ridingEntity
    val canRiderInteract = entity.canRiderInteract()

    val yawRadians = WMathHelper.toRadians(yaw)
    val pitchRadians = WMathHelper.toRadians(pitch)

    val yawCos = func.cos(-yawRadians - WMathHelper.PI)
    val yawSin = func.sin(-yawRadians - WMathHelper.PI)
    val pitchCos = -func.cos(-pitchRadians)
    val pitchSin = func.sin(-pitchRadians)

    val lookX = yawSin * pitchCos
    val lookZ = yawCos * pitchCos

    val rayEndPos = rayStartPos.plus(lookX * reach, pitchSin * reach, lookZ * reach)

    val entityList = getEntitiesInAABBexcluding(entity, entity.entityBoundingBox.addCoord(lookX * reach, pitchSin * reach, lookZ * reach).expand(expandRange, expandRange, expandRange)) { it != null && (!MinecraftInstance.classProvider.isEntityPlayer(it) || !it.asEntityPlayer().spectator) && it.canBeCollidedWith() }

    var pointedEntity: IEntity? = null

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

fun IWorld.getEntitiesInRadius(entity: IEntity, radius: Double = 16.0): List<IEntity>
{
    val box = entity.entityBoundingBox.expand(radius, radius, radius)

    val chunkMinX = floor(box.minX * 0.0625).toInt()
    val chunkMaxX = ceil(box.maxX * 0.0625).toInt()

    val chunkMinZ = floor(box.minZ * 0.0625).toInt()
    val chunkMaxZ = ceil(box.maxZ * 0.0625).toInt()

    val entities = mutableListOf<IEntity>()

    (chunkMinX..chunkMaxX).forEach { x ->
        (chunkMinZ..chunkMaxZ).asSequence().map { z -> getChunkFromChunkCoords(x, z) }.filter(IChunk::isLoaded).forEach { it.getEntitiesWithinAABBForEntity(entity, box, entities, null) }
    }

    return entities
}
