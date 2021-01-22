/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.util.IAxisAlignedBB
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import kotlin.math.cos
import kotlin.math.sin

@ModuleInfo(name = "HighJump", description = "Allows you to jump higher.", category = ModuleCategory.MOVEMENT)
class HighJump : Module()
{
	private val modeValue = ListValue("Mode", arrayOf("Vanilla", "Damage", "AAC3.0.1", "DAC", "Mineplex", "OldMineplex"), "Vanilla")
	private val heightValue = FloatValue("Height", 2f, 1.1f, 5f)
	private val mineplexHeightValue = FloatValue("MineplexHeight", 0.1f, 5.0f, 10.0f)
	private val glassValue = BoolValue("OnlyGlassPane", false)
	private val autodisable = BoolValue("AutoDisable", true)

	private var jumped = false
	private var mineplexStage = 0

	override fun onEnable()
	{
		mineplexStage = -1
		jumped = false
		if (modeValue.get().equals("mineplex", ignoreCase = true)) ClientUtils.displayChatMessage("\u00a78[\u00a7c\u00a7lMineplex Highjump\u00a78] \u00a7cWalk off an island to highjump.")
	}

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent?)
	{
		val thePlayer = mc.thePlayer ?: return

		if (thePlayer.onGround && jumped)
		{
			jumped = false
			if (autodisable.get()) state = false
		}

		if (glassValue.get() && !classProvider.isBlockPane(getBlock(WBlockPos(thePlayer.posX, thePlayer.posY, thePlayer.posZ)))) return

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

			"mineplex" -> mineplexHighJump(thePlayer)

			"oldmineplex" -> if (!thePlayer.onGround) strafe(0.35f)
		}
	}

	private fun mineplexHighJump(thePlayer: IEntityPlayerSP)
	{
		val x = thePlayer.posX
		val z = thePlayer.posZ
		val y = thePlayer.posY
		val forward = thePlayer.movementInput.moveForward.toDouble()
		val strafe = thePlayer.movementInput.moveStrafe.toDouble()
		val dist = 1.0 // .1

		val yaw = thePlayer.rotationYaw
		val cos = cos(Math.toRadians((yaw + 90.0f).toDouble()))
		val sin = sin(Math.toRadians((yaw + 90.0f).toDouble()))
		val nextX = x + (forward * 0.45 * cos + strafe * 0.45 * sin) * dist
		val nextZ = z + (forward * 0.45 * sin - strafe * 0.45 * cos) * dist

		val bb = classProvider.createAxisAlignedBB(nextX - 0.3, y, nextZ - 0.3, nextX + 0.3, y + 2, nextZ + 0.3)
		val should = (mc.theWorld ?: return).getCollidingBoundingBoxes(thePlayer, bb.offset(0.0, -1.0, 0.0)).isEmpty()

		if (jumped) if (!thePlayer.onGround)
		{
			if (!isMoving) strafe(0.05f)
			var speed = 0.55f - mineplexStage.toFloat() / 650
			if (speed < MovementUtils.speed) speed = MovementUtils.speed
			strafe(speed)
			mineplexStage++
		}
		if (should && thePlayer.onGround)
		{
			val a: Double = getBestMineplexExploit(nextX, y, nextZ)
			if (a != 11.0)
			{
				mineplexStage = 1
				jumped = true
				val p1 = classProvider.createCPacketPlayerPosition(nextX, y + a, nextZ, true)
				val p2 = classProvider.createCPacketPlayerPosition(nextX, y, nextZ, true)
				mc.netHandler.networkManager.sendPacketWithoutEvent(p2)
				mc.netHandler.networkManager.sendPacketWithoutEvent(p1)
				thePlayer.setPosition(nextX, y, nextZ)

				// em.setX(nextX);
				// em.setZ(nextZ);
				thePlayer.motionY = mineplexHeightValue.get().toDouble()
			}
		}
	}

	override fun onDisable()
	{
		strafe(0.2f)
		mc.timer.timerSpeed = 1.0f
	}

	@EventTarget
	fun onMove(@Suppress("UNUSED_PARAMETER") event: MoveEvent?)
	{
		val thePlayer = mc.thePlayer ?: return

		if (glassValue.get() && !classProvider.isBlockPane(getBlock(WBlockPos(thePlayer.posX, thePlayer.posY, thePlayer.posZ)))) return
		if (!thePlayer.onGround && modeValue.get().equals("oldmineplex", ignoreCase = true)) thePlayer.motionY += if (thePlayer.fallDistance == 0.0f) 0.0499 else 0.05
	}

	@EventTarget
	fun onJump(event: JumpEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		if (glassValue.get() && !classProvider.isBlockPane(getBlock(WBlockPos(thePlayer.posX, thePlayer.posY, thePlayer.posZ)))) return
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
			thePlayer.jumpMovementFactor = 0.0F
			ClientUtils.displayChatMessage("\u00a78[\u00a7c\u00a7lMineplex Highjump\u00a78] \u00a7cLagback detected. Disabled highjump.")
		}
	}

	private fun getBestMineplexExploit(x: Double, y: Double, z: Double): Double
	{
		var yOff = -1.5
		var bb: IAxisAlignedBB

		do
		{
			bb = classProvider.createAxisAlignedBB(x - 0.3, y + yOff, z - 0.3, x + 0.3, y + 2 + yOff, z + 0.3)
			if (!mc.theWorld!!.getCollidingBoundingBoxes(mc.thePlayer!!, bb.offset(0.0, -1.0, 0.0)).isEmpty() && mc.theWorld!!.getCollidingBoundingBoxes(mc.thePlayer!!, bb).isEmpty() && mc.theWorld!!.getCollidingBoundingBoxes(
					mc.thePlayer!!, bb.offset(0.0, -0.5, 0.0)
				).isEmpty() && yOff <= -4.5 || yOff <= -9
			) return yOff
			yOff -= 0.5
		} while (mc.theWorld!!.getCollidingBoundingBoxes(mc.thePlayer!!, bb).isEmpty())

		return 11.0
	}

	override val tag: String
		get() = modeValue.get()
}
