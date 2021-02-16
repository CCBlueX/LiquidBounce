/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3

object RaycastUtils : MinecraftInstance()
{

	@JvmStatic
	fun raycastEntity(range: Double, entityFilter: EntityFilter) = raycastEntity(range, RotationUtils.serverRotation.yaw, RotationUtils.serverRotation.pitch, entityFilter)

	@JvmStatic
	fun raycastEntity(range: Double, yaw: Float, pitch: Float, entityFilter: EntityFilter): IEntity?
	{
		val theWorld = mc.theWorld ?: return null
		val renderViewEntity = mc.renderViewEntity ?: return null

		var blockReachDistance = range
		val eyePosition = renderViewEntity.getPositionEyes(1f)

		val yawRadians = WMathHelper.toRadians(yaw)
		val pitchRadians = WMathHelper.toRadians(pitch)
		val yawCos = functions.cos(-yawRadians - WMathHelper.PI)
		val yawSin = functions.sin(-yawRadians - WMathHelper.PI)
		val pitchCos = -functions.cos(-pitchRadians)
		val pitchSin = functions.sin(-pitchRadians)

		val entityLook = WVec3((yawSin * pitchCos).toDouble(), pitchSin.toDouble(), (yawCos * pitchCos).toDouble())
		val xCoord = entityLook.xCoord
		val yCoord = entityLook.yCoord
		val zCoord = entityLook.zCoord

		val reachEndPos = eyePosition.addVector(xCoord * blockReachDistance, yCoord * blockReachDistance, zCoord * blockReachDistance)

		val entityList = theWorld.getEntitiesInAABBexcluding(renderViewEntity, renderViewEntity.entityBoundingBox.addCoord(xCoord * blockReachDistance, yCoord * blockReachDistance, zCoord * blockReachDistance).expand(1.0, 1.0, 1.0)) { it != null && (!classProvider.isEntityPlayer(it) || !it.asEntityPlayer().spectator) && it.canBeCollidedWith() }

		var pointedEntity: IEntity? = null

		for (entity in entityList)
		{
			if (!entityFilter.canRaycast(entity)) continue

			val collisionBorderSize = entity.collisionBorderSize.toDouble()
			val axisAlignedBB = entity.entityBoundingBox.expand(collisionBorderSize, collisionBorderSize, collisionBorderSize)

			val rayIntercept = axisAlignedBB.calculateIntercept(eyePosition, reachEndPos)

			if (axisAlignedBB.isVecInside(eyePosition))
			{
				if (blockReachDistance >= 0.0)
				{
					pointedEntity = entity
					blockReachDistance = 0.0
				}
			}
			else if (rayIntercept != null)
			{
				val hitDistance = eyePosition.distanceTo(rayIntercept.hitVec)

				if (hitDistance < blockReachDistance || blockReachDistance == 0.0) if (entity == renderViewEntity.ridingEntity && !renderViewEntity.canRiderInteract())
				{
					if (blockReachDistance == 0.0) pointedEntity = entity
				}
				else
				{
					pointedEntity = entity
					blockReachDistance = hitDistance
				}
			}
		}

		return pointedEntity
	}

	interface EntityFilter
	{
		fun canRaycast(entity: IEntity?): Boolean
	}
}
