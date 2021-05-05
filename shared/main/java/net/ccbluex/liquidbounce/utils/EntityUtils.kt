/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.client.network.INetworkPlayerInfo
import net.ccbluex.liquidbounce.api.minecraft.scoreboard.IScoreboard
import net.ccbluex.liquidbounce.api.minecraft.world.IChunk
import net.ccbluex.liquidbounce.features.module.modules.combat.NoFriends
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot.isBot
import net.ccbluex.liquidbounce.features.module.modules.misc.Teams
import net.ccbluex.liquidbounce.utils.extensions.isAnimal
import net.ccbluex.liquidbounce.utils.extensions.isArmorStand
import net.ccbluex.liquidbounce.utils.extensions.isClientFriend
import net.ccbluex.liquidbounce.utils.extensions.isMob
import kotlin.math.ceil
import kotlin.math.floor

object EntityUtils : MinecraftInstance()
{

	@JvmField
	var targetInvisible = false

	@JvmField
	var targetPlayer = true

	@JvmField
	var targetMobs = true

	@JvmField
	var targetAnimals = false

	@JvmField
	var targetArmorStand = false

	@JvmField
	var targetDead = false

	@JvmStatic
	fun isSelected(entity: IEntity?, canAttackCheck: Boolean): Boolean
	{
		val provider = classProvider

		val theWorld = mc.theWorld ?: return false
		val thePlayer = mc.thePlayer ?: return false

		if (entity != null && provider.isEntityLivingBase(entity) && (targetDead || entity.entityAlive) && entity != mc.thePlayer)
		{
			if (targetInvisible || !entity.invisible)
			{
				if (targetPlayer && provider.isEntityPlayer(entity))
				{
					val entityPlayer = entity.asEntityPlayer()

					if (canAttackCheck)
					{
						if (isBot(theWorld, thePlayer, entityPlayer)) return false

						if (isFriend(entityPlayer)) return false

						if (entityPlayer.spectator) return false
						val teams = LiquidBounce.moduleManager[Teams::class.java] as Teams
						return !teams.state || !teams.isInYourTeam(entityPlayer)
					}

					return true
				}

				return targetMobs && entity.isMob() || targetAnimals && entity.isAnimal() || targetArmorStand && entity.isArmorStand()
			}
		}

		return false
	}

	fun isFriend(entity: IEntity): Boolean = classProvider.isEntityPlayer(entity) && entity.asEntityPlayer().isClientFriend() && !LiquidBounce.moduleManager[NoFriends::class.java].state

	/**
	 * Check if [entity] is alive
	 */
	@JvmStatic
	fun isAlive(entity: IEntityLivingBase, aac: Boolean) = entity.entityAlive && entity.health > 0 || aac && entity.hurtTime > 5 // AAC RayCast bots

	/**
	 * Check if [entity] is selected as enemy with current target options and other modules
	 */
	@JvmStatic
	fun isEnemy(entity: IEntity?, aac: Boolean): Boolean
	{
		val provider = classProvider

		val theWorld = mc.theWorld ?: return false
		val thePlayer = mc.thePlayer ?: return false

		if (provider.isEntityLivingBase(entity) && entity != null && (targetDead || isAlive(entity.asEntityLivingBase(), aac)) && entity != mc.thePlayer)
		{
			if (!targetInvisible && entity.invisible) return false

			if (targetPlayer && provider.isEntityPlayer(entity))
			{
				val player = entity.asEntityPlayer()

				if (player.spectator || isBot(theWorld, thePlayer, player)) return false

				val moduleManager = LiquidBounce.moduleManager

				if (player.isClientFriend() && !moduleManager[NoFriends::class.java].state) return false

				val teams = moduleManager[Teams::class.java] as Teams

				return !teams.state || !teams.isInYourTeam(entity.asEntityLivingBase())
			}

			return targetMobs && entity.isMob() || targetAnimals && entity.isAnimal() || targetArmorStand && entity.isArmorStand()
		}

		return false
	}

	@JvmStatic
	fun getPlayerHealthFromScoreboard(playername: String?, isMineplex: Boolean): Int
	{
		val theWorld = mc.theWorld ?: return 0
		val thePlayer = mc.thePlayer ?: return 0

		val scoreboard: IScoreboard = theWorld.scoreboard
		val provider = classProvider

		val multiplier = if (isMineplex) 2 else 1

		theWorld.loadedEntityList.filter(provider::isEntityPlayer).filter { it != thePlayer }.forEach { entity ->
			val profileName = entity.asEntityPlayer().gameProfile.name

			scoreboard.getObjectivesForEntity(profileName).values.filter { playername.equals(profileName, ignoreCase = true) }.forEach { return@getPlayerHealthFromScoreboard it.scorePoints * multiplier }
		}

		return 0
	}

	@JvmStatic
	fun getEntitiesInRadius(theWorld: IWorldClient, thePlayer: IEntityPlayerSP, radius: Double = 16.0): List<IEntity>
	{
		val box = thePlayer.entityBoundingBox.expand(radius, radius, radius)

		val chunkMinX = floor(box.minX * 0.0625).toInt()
		val chunkMaxX = ceil(box.maxX * 0.0625).toInt()

		val chunkMinZ = floor(box.minZ * 0.0625).toInt()
		val chunkMaxZ = ceil(box.maxZ * 0.0625).toInt()

		val entities = mutableListOf<IEntity>()

		(chunkMinX..chunkMaxX).forEach { x ->
			(chunkMinZ..chunkMaxZ).asSequence().map { z -> theWorld.getChunkFromChunkCoords(x, z) }.filter(IChunk::isLoaded).forEach { it.getEntitiesWithinAABBForEntity(thePlayer, box, entities, null) }
		}

		return entities
	}

	fun getPing(entityPlayer: IEntityLivingBase): Int = mc.netHandler.getPlayerInfo(entityPlayer.uniqueID)?.let(INetworkPlayerInfo::responseTime) ?: -1
}
