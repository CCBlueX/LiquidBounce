package net.ccbluex.liquidbounce.utils.extensions

import net.minecraft.entity.Entity
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Allows to get the distance between the current entity and [entity] from the nearest corner of the bounding box
 */
fun Entity.getDistanceToEntityBox(entity: Entity): Double {
    val x = this.posX - if (entity.entityBoundingBox.minX < entity.entityBoundingBox.maxX)
        entity.entityBoundingBox.minX else entity.entityBoundingBox.maxX
    val y = this.posY - if (entity.entityBoundingBox.minY < entity.entityBoundingBox.maxY)
        entity.entityBoundingBox.minY else entity.entityBoundingBox.maxY
    val z = this.posZ - if (entity.entityBoundingBox.minZ < entity.entityBoundingBox.maxZ)
        entity.entityBoundingBox.minZ else entity.entityBoundingBox.maxZ

    return sqrt(x.pow(2) + y.pow(2) + z.pow(2))
}