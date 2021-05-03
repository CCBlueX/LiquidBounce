/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.util.IAxisAlignedBB
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.world.Scaffold
import net.ccbluex.liquidbounce.features.module.modules.world.Tower
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import java.awt.Color

@ModuleInfo(name = "HighJump", description = "Allows you to jump higher.", category = ModuleCategory.MOVEMENT)
class HighJump : Module()
{
	private val modeValue = ListValue("Mode", arrayOf("Vanilla", "Damage", "AAC3.0.1", "DAC", "Mineplex", "OldMineplex"), "Vanilla")
	private val heightValue = FloatValue("Height", 2f, 1.1f, 5f)
	private val mineplexHeightValue = FloatValue("MineplexHeight", 0.1f, 5.0f, 10.0f)
	private val glassValue = BoolValue("OnlyGlassPane", false)
	private val autodisable = BoolValue("AutoDisable", true)
	private val autoDisableScaffoldValue = BoolValue("DisableScaffoldAndTower", true)

	private var jumped = false
	private var mineplexStage = 0

	override fun onEnable()
	{
		mineplexStage = -1
		jumped = false

		if (autoDisableScaffoldValue.get())
		{
			val moduleManager = LiquidBounce.moduleManager

			val scaffold = moduleManager[Scaffold::class.java]
			val tower = moduleManager[Tower::class.java]

			val disableScaffold = scaffold.state
			val disableTower = tower.state

			if (disableScaffold) scaffold.state = false
			if (disableTower) tower.state = false

			if (disableScaffold || disableTower) LiquidBounce.hud.addNotification("HighJump", "Disabled ${if (disableScaffold && disableTower) "Scaffold and Tower" else if (disableScaffold) "Scaffold" else "Tower"}", 1000, Color.yellow)
		}

		if (modeValue.get().equals("mineplex", ignoreCase = true)) ClientUtils.displayChatMessage(mc.thePlayer, "\u00A78[\u00A7c\u00A7lMineplex Highjump\u00A78] \u00A7cWalk off an island to highjump.")
	}

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent?)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		val onGround = thePlayer.onGround

		if (onGround && jumped)
		{
			jumped = false
			if (autodisable.get()) state = false
		}

		if (glassValue.get() && !classProvider.isBlockPane(getBlock(theWorld, WBlockPos(thePlayer.posX, thePlayer.posY, thePlayer.posZ)))) return // 'AAC Ground-check always returns true when player is collided with glass pane or iron bars, etc.' bug exploit

		when (modeValue.get().toLowerCase())
		{
			"damage" -> if (thePlayer.hurtTime > 0 && onGround)
			{
				thePlayer.motionY += 0.42f * heightValue.get()
				jumped = true
			}

			"aac3.0.1" -> if (!onGround)
			{
				thePlayer.motionY += 0.059
				jumped = true
			}

			"dac" -> if (!onGround)
			{
				thePlayer.motionY += 0.049999
				jumped = true
			}

			"mineplex" -> mineplexHighJump(theWorld, thePlayer)

			"oldmineplex" -> if (!onGround) strafe(thePlayer, 0.35f)
		}
	}

	private fun mineplexHighJump(theWorld: IWorldClient, thePlayer: IEntityPlayerSP)
	{
		val networkManager = mc.netHandler.networkManager

		val posY = thePlayer.posY

		val func = functions

		val dir = MovementUtils.getDirection(thePlayer)
		val nextX = thePlayer.posX - func.sin(dir) * 0.45f
		val nextZ = thePlayer.posZ + func.cos(dir) * 0.45f

		val provider = classProvider

		if (jumped) if (!thePlayer.onGround)
		{
			if (!isMoving(thePlayer)) strafe(thePlayer, 0.05f)

			strafe(thePlayer, (0.55f - mineplexStage / 650.0f).coerceAtLeast(MovementUtils.getSpeed(thePlayer)))
			mineplexStage++
		}

		if (thePlayer.onGround && theWorld.getCollidingBoundingBoxes(thePlayer, provider.createAxisAlignedBB(nextX - 0.3, posY - 1.0, nextZ - 0.3, nextX + 0.3, posY + 1.0, nextZ + 0.3)).isEmpty())
		{
			val noCollisionYOffset: Double = findNoCollisionYOffset(theWorld, thePlayer, nextX, posY, nextZ)
			if (noCollisionYOffset != 0.0)
			{
				mineplexStage = 1
				jumped = true
				networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(nextX, posY, nextZ, true))
				networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(nextX, posY + noCollisionYOffset, nextZ, true))
				thePlayer.setPosition(nextX, posY, nextZ)

				thePlayer.motionY = mineplexHeightValue.get().toDouble()
			}
		}
	}

	override fun onDisable()
	{
		strafe(mc.thePlayer ?: return, 0.2f)
		mc.timer.timerSpeed = 1.0f
	}

	@EventTarget
	fun onMove(@Suppress("UNUSED_PARAMETER") event: MoveEvent?)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		if (glassValue.get() && !classProvider.isBlockPane(getBlock(theWorld, WBlockPos(thePlayer.posX, thePlayer.posY, thePlayer.posZ)))) return

		if (!thePlayer.onGround && modeValue.get().equals("oldmineplex", ignoreCase = true)) thePlayer.motionY += if (thePlayer.fallDistance == 0.0f) 0.0499 else 0.05
	}

	@EventTarget
	fun onJump(event: JumpEvent)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		if (glassValue.get() && !classProvider.isBlockPane(getBlock(theWorld, WBlockPos(thePlayer.posX, thePlayer.posY, thePlayer.posZ)))) return

		when (modeValue.get().toLowerCase())
		{
			"vanilla" -> event.motion *= heightValue.get()
			"oldmineplex" -> event.motion = 0.47f
		}
	}

	@EventTarget
	fun onPacket(event: PacketEvent)
	{
		if (classProvider.isSPacketPlayerPosLook(event.packet) && modeValue.get().equals("mineplex", ignoreCase = true) && jumped)
		{
			val thePlayer = mc.thePlayer ?: return

			state = false

			MovementUtils.zeroXZ(thePlayer)
			thePlayer.jumpMovementFactor = 0.02F

			LiquidBounce.hud.addNotification("Mineplex HighJump", "A teleport has been detected. Disabled HighJump to prevent kick.", 1000L, Color.red)
		}
	}

	private fun findNoCollisionYOffset(theWorld: IWorldClient, thePlayer: IEntityPlayerSP, x: Double, y: Double, z: Double): Double
	{
		var yOff = -1.5
		var bb: IAxisAlignedBB

		do
		{
			bb = classProvider.createAxisAlignedBB(x - 0.3, y + yOff, z - 0.3, x + 0.3, y + 2 + yOff, z + 0.3)
			if (!theWorld.getCollidingBoundingBoxes(thePlayer, bb.offset(0.0, -1.0, 0.0)).isEmpty() && theWorld.getCollidingBoundingBoxes(thePlayer, bb).isEmpty() && theWorld.getCollidingBoundingBoxes(thePlayer, bb.offset(0.0, -0.5, 0.0)).isEmpty() && yOff <= -4.5 || yOff <= -9) return yOff
			yOff -= 0.5
		} while (theWorld.getCollidingBoundingBoxes(thePlayer, bb).isEmpty())

		return 0.0
	}

	override val tag: String
		get() = modeValue.get()
}
