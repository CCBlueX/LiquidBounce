/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.extensions

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.LiquidBounce.wrapper
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
import net.ccbluex.liquidbounce.api.minecraft.util.IAxisAlignedBB
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.features.module.modules.combat.NoFriends
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.Teams
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.stripColor
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

val IEntity.isFriend: Boolean
	get() = wrapper.classProvider.isEntityPlayer(this) && asEntityPlayer().isClientFriend() && !LiquidBounce.moduleManager[NoFriends::class.java].state

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

	repeat(3) { i -> if (origin[i] > destMaxs[i]) origin[i] = destMaxs[i] else if (origin[i] < destMins[i]) origin[i] = destMins[i] }

	return WVec3(origin[0], origin[1], origin[2])
}

fun IEntity.isAnimal(): Boolean
{
	val classProvider = wrapper.classProvider
	return classProvider.isEntityAnimal(this) || classProvider.isEntitySquid(this) || classProvider.isEntityGolem(this) || classProvider.isEntityBat(this)
}

fun IEntity.isMob(): Boolean
{
	val classProvider = wrapper.classProvider
	return classProvider.isEntityMob(this) || classProvider.isEntityVillager(this) || classProvider.isEntitySlime(this) || classProvider.isEntityGhast(this) || classProvider.isEntityDragon(this) || classProvider.isEntityShulker(this)
}

fun IEntity.isArmorStand(): Boolean = wrapper.classProvider.isEntityArmorStand(this)

fun IEntity.isClientTarget(): Boolean = wrapper.classProvider.isEntityPlayer(this) && LiquidBounce.fileManager.targetsConfig.isTarget(stripColor(name))

/**
 * Check if entity is alive
 */
fun IEntityLivingBase.isAlive(aac: Boolean = false) = entityAlive && health > 0 || aac && hurtTime > 5 // AAC Raycast bots

fun IEntity?.isSelected(attackableCheck: Boolean = false): Boolean
{
	val thePlayer = wrapper.minecraft.thePlayer

	if (this != null && wrapper.classProvider.isEntityLivingBase(this) && (EntityUtils.targetDead || entityAlive) && this != thePlayer && entityId >= 0)
	{
		if (!EntityUtils.targetInvisible && invisible) return false

		if (EntityUtils.targetPlayer && wrapper.classProvider.isEntityPlayer(this))
		{
			if (!attackableCheck) return true

			val player = asEntityPlayer()

			// Spectator check
			if (player.spectator) return false

			// Friend check
			if (player.isFriend) return false

			// Team check
			val teams = LiquidBounce.moduleManager[Teams::class.java] as Teams
			if (teams.state && teams.isInYourTeam(player)) return false

			// Bot check
			return !AntiBot.isBot(wrapper.minecraft.theWorld ?: return false, thePlayer ?: return false, player)
		}

		return EntityUtils.targetMobs && isMob() || EntityUtils.targetAnimals && isAnimal() || EntityUtils.targetArmorStand && isArmorStand()
	}

	return false
}

/**
 * Check if entity is selected as enemy with current target options and other modules
 */
fun IEntity?.isEnemy(aac: Boolean = false): Boolean
{
	val thePlayer = wrapper.minecraft.thePlayer ?: return false

	if (this != null && wrapper.classProvider.isEntityLivingBase(this) && (EntityUtils.targetDead || asEntityLivingBase().isAlive(aac)) && this != thePlayer && entityId >= 0)
	{
		if (!EntityUtils.targetInvisible && invisible) return false

		if (EntityUtils.targetPlayer && wrapper.classProvider.isEntityPlayer(this))
		{
			val player = asEntityPlayer()

			// Spectator check
			if (player.spectator) return false

			// Friend check
			if (player.isFriend) return false

			// Team check
			val teams = LiquidBounce.moduleManager[Teams::class.java] as Teams
			if (teams.state && teams.isInYourTeam(asEntityLivingBase())) return false

			// Bot check
			return !AntiBot.isBot(wrapper.minecraft.theWorld ?: return false, thePlayer, player)
		}

		return EntityUtils.targetMobs && isMob() || EntityUtils.targetAnimals && isAnimal() || EntityUtils.targetArmorStand && isArmorStand()
	}

	return false
}
