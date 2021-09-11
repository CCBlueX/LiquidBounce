/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.util.IAxisAlignedBB
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.api.minecraft.world.IWorld

object RaycastUtils : MinecraftInstance()
{

	@JvmStatic
	fun raycastEntity(theWorld: IWorld, thePlayer: IEntity, range: Double, entityFilter: (IEntity?) -> Boolean, aabbGetter: (IEntity) -> IAxisAlignedBB = IEntity::entityBoundingBox) = raycastEntity(theWorld, thePlayer, range, RotationUtils.serverRotation.yaw, RotationUtils.serverRotation.pitch, aabbGetter, entityFilter)

	@JvmStatic
	fun raycastEntity(theWorld: IWorld, thePlayer: IEntity, range: Double, yaw: Float, pitch: Float, aabbGetter: (IEntity) -> IAxisAlignedBB = IEntity::entityBoundingBox, entityFilter: (IEntity?) -> Boolean): IEntity?
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

		entityList.filter(entityFilter::invoke).forEach { entity ->
			val collisionBorder = aabbGetter(entity)

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

				if (hitDistance < reach || reach == 0.0) if (entity == ridingEntity && !canRiderInteract)
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
}
