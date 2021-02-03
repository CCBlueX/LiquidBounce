/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.extensions

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.util.IAxisAlignedBB
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.utils.render.ColorUtils.stripColor
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Allows to get the distance between the current entity and [entity] from the nearest corner of the bounding box
 */
fun IEntity.getDistanceToEntityBox(entity: IEntity): Double
{
	val eyes = getPositionEyes(1F)
	val pos = getNearestPointBB(eyes, entity.entityBoundingBox)

	val xDelta = abs(pos.xCoord - eyes.xCoord)
	val yDelta = abs(pos.yCoord - eyes.yCoord)
	val zDelta = abs(pos.zCoord - eyes.zCoord)

	return sqrt(xDelta.pow(2) + yDelta.pow(2) + zDelta.pow(2))
}

fun getNearestPointBB(eye: WVec3, box: IAxisAlignedBB): WVec3
{
	val origin = doubleArrayOf(eye.xCoord, eye.yCoord, eye.zCoord)
	val destMins = doubleArrayOf(box.minX, box.minY, box.minZ)
	val destMaxs = doubleArrayOf(box.maxX, box.maxY, box.maxZ)

	for (i in 0..2) if (origin[i] > destMaxs[i]) origin[i] = destMaxs[i] else if (origin[i] < destMins[i]) origin[i] = destMins[i]

	return WVec3(origin[0], origin[1], origin[2])
}

fun IEntityPlayer.getPing(): Int
{
	val playerInfo = LiquidBounce.wrapper.minecraft.netHandler.getPlayerInfo(uniqueID)
	return playerInfo?.responseTime ?: 0
}

fun IEntity.isAnimal(): Boolean
{
	val classProvider = LiquidBounce.wrapper.classProvider
	return classProvider.isEntityAnimal(this) || classProvider.isEntitySquid(this) || classProvider.isEntityGolem(this) || classProvider.isEntityBat(this)
}

fun IEntity.isMob(): Boolean
{
	val classProvider = LiquidBounce.wrapper.classProvider
	return classProvider.isEntityMob(this) || classProvider.isEntityVillager(this) || classProvider.isEntitySlime(this) || classProvider.isEntityGhast(this) || classProvider.isEntityDragon(this) || classProvider.isEntityShulker(this)
}

fun IEntityPlayer.isClientFriend(): Boolean
{
	val entityName = name ?: return false

	return LiquidBounce.fileManager.friendsConfig.isFriend(stripColor(entityName))
}
