package net.ccbluex.liquidbounce.features.module.modules.movement.flies

import net.ccbluex.liquidbounce.api.enums.StatType
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.potion.PotionType
import net.ccbluex.liquidbounce.api.minecraft.util.IAxisAlignedBB
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.world.IWorld
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import kotlin.math.min

abstract class FlyMode(val modeName: String) : MinecraftInstance()
{
	open val shouldDisableNoFall: Boolean
		get() = false

	open val mark: Boolean
		get() = true

	open val damageOnStart: DamageOnStart
		get() = DamageOnStart.OFF

	abstract fun onUpdate()

	open fun onMotion(eventState: EventState)
	{
	}

	open fun onRender3D(partialTicks: Float)
	{
	}

	open fun onPacket(event: PacketEvent)
	{
	}

	open fun onMove(event: MoveEvent)
	{
	}

	open fun onBlockBB(event: BlockBBEvent)
	{
	}

	open fun onJump(event: JumpEvent)
	{
	}

	open fun onStep(event: StepEvent)
	{
	}

	open fun onEnable()
	{
	}

	open fun onDisable()
	{
	}

	companion object
	{
		fun handleVanillaKickBypass(theWorld: IWorld, thePlayer: IEntity)
		{
			if (!Fly.vanillaKickBypassValue.get() || !Fly.groundTimer.hasTimePassed(1000)) return

			val networkManager = mc.netHandler.networkManager
			val provider = classProvider

			val ground = calculateGround(theWorld, thePlayer)

			val posX = thePlayer.posX
			val originalPosY = thePlayer.posY
			val posZ = thePlayer.posZ

			run {
				var posY = originalPosY
				while (posY > ground)
				{
					networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(posX, posY, posZ, true))
					if (posY - 8.0 < ground) break // Prevent next step
					posY -= 8.0
				}
			}

			networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(posX, ground, posZ, true))

			var posY = ground
			while (posY < originalPosY)
			{
				networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(posX, posY, posZ, true))
				if (posY + 8.0 > originalPosY) break // Prevent next step
				posY += 8.0
			}

			networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(posX, originalPosY, posZ, true))

			Fly.groundTimer.reset()
		}

		// TODO: Make better and faster calculation lol
		private fun calculateGround(theWorld: IWorld, thePlayer: IEntity): Double
		{
			val playerBoundingBox: IAxisAlignedBB = thePlayer.entityBoundingBox
			var blockHeight = 1.0
			var ground = thePlayer.posY
			while (ground > 0.0)
			{
				val customBox = classProvider.createAxisAlignedBB(playerBoundingBox.maxX, ground + blockHeight, playerBoundingBox.maxZ, playerBoundingBox.minX, ground, playerBoundingBox.minZ)
				if (theWorld.checkBlockCollision(customBox))
				{
					if (blockHeight <= 0.05) return ground + blockHeight

					ground += blockHeight
					blockHeight = 0.05
				}
				ground -= blockHeight
			}
			return 0.0
		}

		fun jump(theWorld: IWorld, thePlayer: IEntityPlayer)
		{
			val provider = classProvider

			val blockAboveState = BlockUtils.getState(theWorld, WBlockPos(thePlayer.posX, thePlayer.posY + 2, thePlayer.posZ))
			val blockAbove = blockAboveState.block
			val normalJumpY = 0.42 + MovementUtils.getEffectAmplifier(thePlayer, PotionType.JUMP) * 0.1f
			val jumpY = if (provider.isBlockAir(blockAbove)) normalJumpY else min(blockAboveState.let { BlockUtils.getBlockCollisionBox(theWorld, it)?.minY?.plus(0.2) } ?: normalJumpY, normalJumpY)

			// Simulate Vanilla Player Jump
			thePlayer.setPosition(thePlayer.posX, thePlayer.posY + jumpY, thePlayer.posZ)

			// Jump Boost
			if (thePlayer.sprinting)
			{
				val func = functions

				val dir = MovementUtils.getDirection(thePlayer)
				thePlayer.motionX -= func.sin(dir) * 0.2f
				thePlayer.motionZ += func.cos(dir) * 0.2f
			}
			thePlayer.isAirBorne = true

			// ForgeHooks.onLivingJump(thePlayer)
			thePlayer.triggerAchievement(provider.getStatEnum(StatType.JUMP_STAT))
		}
	}
}

enum class DamageOnStart(val execute: () -> Unit)
{
	OFF({ }),
	NONE({ }), // Used internally
	NCP({ net.ccbluex.liquidbounce.features.module.modules.exploit.Damage.ncpDamage() }),
	OLD_NCP({ net.ccbluex.liquidbounce.features.module.modules.exploit.Damage.ncpDamage(motionSize = 1.01) }),
	HYPIXEL({ net.ccbluex.liquidbounce.features.module.modules.exploit.Damage.hypixelDamage() });

	companion object
	{
		fun byName(name: String): DamageOnStart? = values().find { it.name.equals(name, ignoreCase = true) }
	}
}
