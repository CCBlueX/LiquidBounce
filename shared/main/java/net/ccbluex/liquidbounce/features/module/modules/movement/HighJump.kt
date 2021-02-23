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
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
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
			val scaffold = LiquidBounce.moduleManager[Scaffold::class.java]
			val tower = LiquidBounce.moduleManager[Tower::class.java]

			if (scaffold.state) scaffold.state = false
			if (tower.state) tower.state = false
		}

		if (modeValue.get().equals("mineplex", ignoreCase = true)) ClientUtils.displayChatMessage(mc.thePlayer, "\u00A78[\u00A7c\u00A7lMineplex Highjump\u00A78] \u00A7cWalk off an island to highjump.")
	}

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent?)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		if (thePlayer.onGround && jumped)
		{
			jumped = false
			if (autodisable.get()) state = false
		}

		if (glassValue.get() && !classProvider.isBlockPane(getBlock(theWorld, WBlockPos(thePlayer.posX, thePlayer.posY, thePlayer.posZ)))) return // 'AAC Ground-check always returns true when player is collided with glass pane or iron bars, etc.' bug exploit

		when (modeValue.get().toLowerCase())
		{
			"damage" -> if (thePlayer.hurtTime > 0 && thePlayer.onGround)
			{
				thePlayer.motionY += 0.42f * heightValue.get()
				jumped = true
			}

			"aac3.0.1" -> if (!thePlayer.onGround)
			{
				thePlayer.motionY += 0.059
				jumped = true
			}

			"dac" -> if (!thePlayer.onGround)
			{
				thePlayer.motionY += 0.049999
				jumped = true
			}

			"mineplex" -> mineplexHighJump(theWorld, thePlayer)

			"oldmineplex" -> if (!thePlayer.onGround) strafe(thePlayer, 0.35f)
		}
	}

	private fun mineplexHighJump(theWorld: IWorldClient, thePlayer: IEntityPlayerSP)
	{
		val networkManager = mc.netHandler.networkManager

		val posX = thePlayer.posX
		val posY = thePlayer.posZ
		val posZ = thePlayer.posY

		val forward = thePlayer.movementInput.moveForward.toDouble()
		val strafe = thePlayer.movementInput.moveStrafe.toDouble()
		val dist = 1.0 // .1

		val func = functions

		val yawRadians = WMathHelper.toRadians((thePlayer.rotationYaw + 90.0f))
		val cos = func.cos(yawRadians)
		val sin = func.sin(yawRadians)
		val nextX = posX + (forward * 0.45 * cos + strafe * 0.45 * sin) * dist
		val nextZ = posY + (forward * 0.45 * sin - strafe * 0.45 * cos) * dist

		val provider = classProvider

		val bb = provider.createAxisAlignedBB(nextX - 0.3, posZ, nextZ - 0.3, nextX + 0.3, posZ + 2, nextZ + 0.3)
		val should = theWorld.getCollidingBoundingBoxes(thePlayer, bb.offset(0.0, -1.0, 0.0)).isEmpty()

		if (jumped) if (!thePlayer.onGround)
		{
			if (!isMoving(thePlayer)) strafe(thePlayer, 0.05f)
			var speed = 0.55f - mineplexStage / 650.0f
			if (speed < MovementUtils.getSpeed(thePlayer)) speed = MovementUtils.getSpeed(thePlayer)
			strafe(thePlayer, speed)
			mineplexStage++
		}

		if (should && thePlayer.onGround)
		{
			val a: Double = getBestMineplexExploit(theWorld, thePlayer, nextX, posZ, nextZ)
			if (a != 11.0)
			{
				mineplexStage = 1
				jumped = true
				networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(nextX, posZ, nextZ, true))
				networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(nextX, posZ + a, nextZ, true))
				thePlayer.setPosition(nextX, posZ, nextZ)

				// em.setX(nextX);
				// em.setZ(nextZ);
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
			"vanilla" -> event.motion = event.motion * heightValue.get()
			"oldmineplex" -> event.motion = 0.47f
		}
	}

	@EventTarget
	fun onPacket(event: PacketEvent)
	{
		val thePlayer = mc.thePlayer ?: return
		if (classProvider.isSPacketPlayerPosLook(event.packet) && modeValue.get().equals("mineplex", ignoreCase = true) && jumped)
		{
			state = false
			thePlayer.motionX *= 0
			thePlayer.motionZ *= 0
			thePlayer.jumpMovementFactor = 0.02F
			ClientUtils.displayChatMessage(thePlayer, "\u00A78[\u00A7c\u00A7lMineplex Highjump\u00A78] \u00A7cSetback detected. Disabled highjump.")
		}
	}

	private fun getBestMineplexExploit(theWorld: IWorldClient, thePlayer: IEntityPlayerSP, x: Double, y: Double, z: Double): Double
	{
		var yOff = -1.5
		var bb: IAxisAlignedBB

		do
		{
			bb = classProvider.createAxisAlignedBB(x - 0.3, y + yOff, z - 0.3, x + 0.3, y + 2 + yOff, z + 0.3)
			if (!theWorld.getCollidingBoundingBoxes(thePlayer, bb.offset(0.0, -1.0, 0.0)).isEmpty() && theWorld.getCollidingBoundingBoxes(thePlayer, bb).isEmpty() && theWorld.getCollidingBoundingBoxes(thePlayer, bb.offset(0.0, -0.5, 0.0)).isEmpty() && yOff <= -4.5 || yOff <= -9) return yOff
			yOff -= 0.5
		} while (theWorld.getCollidingBoundingBoxes(thePlayer, bb).isEmpty())

		return 11.0
	}

	override val tag: String
		get() = modeValue.get()
}
