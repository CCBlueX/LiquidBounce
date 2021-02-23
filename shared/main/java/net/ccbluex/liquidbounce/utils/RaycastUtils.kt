/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper

object RaycastUtils : MinecraftInstance()
{

	@JvmStatic
	fun raycastEntity(theWorld: IWorldClient, thePlayer: IEntityPlayerSP, range: Double, entityFilter: EntityFilter) = raycastEntity(theWorld, thePlayer, range, RotationUtils.serverRotation.yaw, RotationUtils.serverRotation.pitch, entityFilter)

	@JvmStatic
	fun raycastEntity(theWorld: IWorldClient, thePlayer: IEntityPlayerSP, range: Double, yaw: Float, pitch: Float, entityFilter: EntityFilter): IEntity?
	{
		val func = functions

		var reach = range
		val rayStartPos = thePlayer.getPositionEyes(1f)
		val ridingEntity = thePlayer.ridingEntity
		val canRiderInteract = thePlayer.canRiderInteract()

		val yawRadians = WMathHelper.toRadians(yaw)
		val pitchRadians = WMathHelper.toRadians(pitch)

		val yawCos = func.cos(-yawRadians - WMathHelper.PI)
		val yawSin = func.sin(-yawRadians - WMathHelper.PI)
		val pitchCos = -func.cos(-pitchRadians)
		val pitchSin = func.sin(-pitchRadians)

		val lookX = (yawSin * pitchCos).toDouble()
		val lookY = pitchSin.toDouble()
		val lookZ = (yawCos * pitchCos).toDouble()

		val rayEndPos = rayStartPos.addVector(lookX * reach, lookY * reach, lookZ * reach)

		val entityList = theWorld.getEntitiesInAABBexcluding(thePlayer, thePlayer.entityBoundingBox.addCoord(lookX * reach, lookY * reach, lookZ * reach).expand(1.0, 1.0, 1.0)) { it != null && (!classProvider.isEntityPlayer(it) || !it.asEntityPlayer().spectator) && it.canBeCollidedWith() }

		var pointedEntity: IEntity? = null

		entityList.filter(entityFilter::canRaycast).forEach { entity ->
			val collisionBorderSize = entity.collisionBorderSize.toDouble()
			val collisionBorder = entity.entityBoundingBox.expand(collisionBorderSize, collisionBorderSize, collisionBorderSize)

			val rayIntercept = collisionBorder.calculateIntercept(rayStartPos, rayEndPos)

			if (collisionBorder.isVecInside(rayStartPos))
			{
				if (reach >= 0.0)
				{
					pointedEntity = entity
					reach = 0.0
				}
			}
			else if (rayIntercept != null)
			{
				val hitDistance = rayStartPos.distanceTo(rayIntercept.hitVec)

				if (hitDistance < reach || reach == 0.0) if (entity.isEntityEqual(ridingEntity) && !canRiderInteract)
				{
					if (reach == 0.0) pointedEntity = entity
				}
				else
				{
					pointedEntity = entity
					reach = hitDistance
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
