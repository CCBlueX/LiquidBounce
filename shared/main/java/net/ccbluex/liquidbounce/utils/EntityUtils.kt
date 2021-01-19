/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
import net.ccbluex.liquidbounce.features.module.modules.combat.NoFriends
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot.isBot
import net.ccbluex.liquidbounce.features.module.modules.misc.Teams
import net.ccbluex.liquidbounce.utils.extensions.isAnimal
import net.ccbluex.liquidbounce.utils.extensions.isClientFriend
import net.ccbluex.liquidbounce.utils.extensions.isMob

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
	var targetDead = false

	@JvmStatic
	fun isSelected(entity: IEntity?, canAttackCheck: Boolean): Boolean
	{
		if (classProvider.isEntityLivingBase(entity) && (targetDead || entity!!.entityAlive) && entity != null && entity != mc.thePlayer)
		{
			if (targetInvisible || !entity.invisible)
			{
				if (targetPlayer && classProvider.isEntityPlayer(entity))
				{
					val entityPlayer = entity.asEntityPlayer()

					if (canAttackCheck)
					{
						if (isBot(entityPlayer)) return false

						if (entityPlayer.isClientFriend() && !LiquidBounce.moduleManager.getModule(NoFriends::class.java).state) return false

						if (entityPlayer.spectator) return false
						val teams = LiquidBounce.moduleManager.getModule(Teams::class.java) as Teams
						return !teams.state || !teams.isInYourTeam(entityPlayer)
					}
					return true
				}

				return targetMobs && entity.isMob() || targetAnimals && entity.isAnimal()
			}
		}
		return false
	}

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
		if (classProvider.isEntityLivingBase(entity) && entity != null && (targetDead || isAlive(entity.asEntityLivingBase(), aac)) && entity != mc.thePlayer)
		{
			if (!targetInvisible && entity.invisible) return false

			if (targetPlayer && classProvider.isEntityPlayer(entity))
			{
				val player = entity.asEntityPlayer()

				if (player.spectator || isBot(player)) return false

				if (player.isClientFriend() && !LiquidBounce.moduleManager[NoFriends::class.java].state) return false

				val teams = LiquidBounce.moduleManager[Teams::class.java] as Teams

				return !teams.state || !teams.isInYourTeam(entity.asEntityLivingBase())
			}

			return targetMobs && entity.isMob() || targetAnimals && entity.isAnimal()
		}

		return false
	}
}
