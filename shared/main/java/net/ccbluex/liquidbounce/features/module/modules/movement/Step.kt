/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.enums.StatType
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.exploit.Phase
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.*

@ModuleInfo(name = "Step", description = "Allows you to step up blocks.", category = ModuleCategory.MOVEMENT)
class Step : Module()
{

	/**
	 * OPTIONS
	 */

	private val modeValue = ListValue("Mode", arrayOf("Vanilla", "Jump", "NCP", "MotionNCP", "OldNCP", "AAC3.1.5", "AAC3.2.0", "AAC3.3.4", "Spartan127", "Rewinside"), "NCP")

	val airStepValue = BoolValue("AirStep", false)
	private val heightValue = FloatValue("Height", 1F, 0.6F, 10F)
	private val jumpHeightValue = FloatValue("JumpHeight", 0.42F, 0.37F, 0.42F)
	private val maxDelayValue = IntegerValue("MaxDelay", 0, 0, 500)
	private val minDelayValue = IntegerValue("MinDelay", 0, 0, 500)
	private val motionNCPBoostValue = FloatValue("MotionNCP-Boost", 0.7F, 0F, 0.7F)

	/**
	 * VALUES
	 */

	private var isStep = false
	private var isAAC3_3_4Step = false

	private var stepX = 0.0
	private var stepY = 0.0
	private var stepZ = 0.0

	private var motionNCPNextStep = 0
	private var spartanSwitch = false

	private val timer = MSTimer()
	private var delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())

	override fun onDisable()
	{
		// Change step height back to default (0.5 is default)
		(mc.thePlayer ?: return).stepHeight = 0.5F
		motionNCPNextStep = 0
	}

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		// Motion steps
		when (modeValue.get().toLowerCase())
		{
			"jump" -> if (thePlayer.isCollidedHorizontally && thePlayer.onGround && !mc.gameSettings.keyBindJump.isKeyDown)
			{
				fakeJump(thePlayer)
				thePlayer.motionY = jumpHeightValue.get().toDouble()
			}

			"aac3.2.0" -> if (thePlayer.isCollidedHorizontally && !thePlayer.isOnLadder && !thePlayer.isInWater && !thePlayer.isInLava && !thePlayer.isInWeb)
			{
				if (thePlayer.onGround && timer.hasTimePassed(delay))
				{
					isStep = true

					fakeJump(thePlayer)
					thePlayer.motionY += 0.620000001490116

					val func = functions

					val f = WMathHelper.toRadians(thePlayer.rotationYaw)
					thePlayer.motionX -= func.sin(f) * 0.2
					thePlayer.motionZ += func.cos(f) * 0.2
					resetTimer()
				}

				thePlayer.onGround = true
			}
			else isStep = false

			"aac3.3.4" -> if (thePlayer.isCollidedHorizontally && MovementUtils.isMoving(thePlayer))
			{
				if (thePlayer.onGround && couldStep(theWorld, thePlayer))
				{
					thePlayer.motionX *= 1.26
					thePlayer.motionZ *= 1.26
					thePlayer.jump()
					isAAC3_3_4Step = true
				}

				if (isAAC3_3_4Step)
				{
					thePlayer.motionY -= 0.015

					if (!thePlayer.isUsingItem && thePlayer.movementInput.moveStrafe == 0F) thePlayer.jumpMovementFactor = 0.3F
				}
			}
			else isAAC3_3_4Step = false
		}
	}

	@EventTarget
	fun onMove(event: MoveEvent)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		val mode = modeValue.get()
		val motionNCPBoost = motionNCPBoostValue.get()

		// NCP Motion steps
		if (mode.equals("MotionNCP", ignoreCase = true) && thePlayer.isCollidedHorizontally && !mc.gameSettings.keyBindJump.isKeyDown) when
		{
			thePlayer.onGround && couldStep(theWorld, thePlayer) ->
			{
				fakeJump(thePlayer)
				thePlayer.motionY = 0.0
				event.y = 0.41999998688698 // Jump step 1 (0.42)
				motionNCPNextStep = 1
			}

			motionNCPNextStep == 1 ->
			{
				event.y = 0.33319999363422 // Jump step 2 (0.333)
				motionNCPNextStep = 2
			}

			motionNCPNextStep == 2 ->
			{
				val yaw = MovementUtils.getDirection(thePlayer)

				event.y = 0.248135998590947 // Jump step 3 (0.248)

				if (motionNCPBoost > 0F)
				{
					val func = functions

					event.x = (-func.sin(yaw) * motionNCPBoost).toDouble()
					event.z = (func.cos(yaw) * motionNCPBoost).toDouble()
				}

				motionNCPNextStep = 0
			}
		}
	}

	@EventTarget
	fun onStep(event: StepEvent)
	{
		val moduleManager = LiquidBounce.moduleManager

		// Phase  should disable step
		if (moduleManager[Phase::class.java].state)
		{
			event.stepHeight = 0F
			return
		}

		val thePlayer = mc.thePlayer ?: return

		// Some fly modes should disable step
		val fly = moduleManager[Fly::class.java] as Fly

		if (fly.state)
		{
			val flyMode = fly.modeValue.get()

			if (flyMode.equals("Hypixel", ignoreCase = true) || flyMode.equals("Rewinside", ignoreCase = true) || flyMode.equals("Mineplex", ignoreCase = true) && thePlayer.inventory.getCurrentItemInHand() == null)
			{
				event.stepHeight = 0F
				return
			}
		}

		val mode = modeValue.get()

		// Set step to default in some cases
		if ((!thePlayer.onGround && !airStepValue.get()) || !timer.hasTimePassed(delay) || sequenceOf("Jump", "MotionNCP", "AAC3.2.0", "AAC3.3.4").any { mode.equals(it, ignoreCase = true) })
		{
			thePlayer.stepHeight = 0.5F
			event.stepHeight = 0.5F
			return
		}

		// Set step height
		val height = heightValue.get()

		thePlayer.stepHeight = height
		event.stepHeight = height

		// Detect possible step
		if (event.stepHeight > 0.5F)
		{
			isStep = true
			stepX = thePlayer.posX
			stepY = thePlayer.posY
			stepZ = thePlayer.posZ
		}
	}

	@EventTarget(ignoreCondition = true)
	fun onStepConfirm(@Suppress("UNUSED_PARAMETER") event: StepConfirmEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		if (!isStep) // Check if step
			return

		if (thePlayer.entityBoundingBox.minY - stepY > 0.5)
		{

			// Check if full block step
			val mode = modeValue.get().toLowerCase()
			val networkManager = mc.netHandler.networkManager

			val provider = classProvider

			when (mode)
			{
				"ncp", "aac3.1.5" ->
				{
					fakeJump(thePlayer)

					// Half legit step (1 packet missing) [COULD TRIGGER TOO MANY PACKETS]
					networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(stepX, stepY + 0.41999998688698, stepZ, false)) // 0.42
					networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(stepX, stepY + 0.7531999805212, stepZ, false)) // 0.333
					resetTimer()
				}

				"spartan127" ->
				{
					fakeJump(thePlayer)

					if (spartanSwitch)
					{
						// Vanilla step (3 packets) [COULD TRIGGER TOO MANY PACKETS]
						networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(stepX, stepY + 0.41999998688698, stepZ, false)) // 0.42
						networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(stepX, stepY + 0.7531999805212, stepZ, false)) // 0.333
						networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(stepX, stepY + 1.001335979112147, stepZ, false)) // 0.248
					}
					else // Force step
						networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(stepX, stepY + 0.6, stepZ, false))

					// Spartan b127 allows one unlegit step so just swap between legit and unlegit
					spartanSwitch = !spartanSwitch

					// Reset timer
					resetTimer()
				}

				"rewinside" ->
				{
					fakeJump(thePlayer)

					// Vanilla step (3 packets) [COULD TRIGGER TOO MANY PACKETS]
					networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(stepX, stepY + 0.41999998688698, stepZ, false)) // 0.42
					networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(stepX, stepY + 0.7531999805212, stepZ, false)) // 0.333
					networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(stepX, stepY + 1.001335979112147, stepZ, false)) // 0.248

					// Reset timer
					resetTimer()
				}
			}
		}

		isStep = false
		stepX = 0.0
		stepY = 0.0
		stepZ = 0.0
	}

	private fun resetTimer()
	{
		timer.reset()
		delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())
	}

	@EventTarget(ignoreCondition = true)
	fun onPacket(event: PacketEvent)
	{
		val packet = event.packet

		if (classProvider.isCPacketPlayer(packet) && isStep && modeValue.get().equals("OldNCP", ignoreCase = true))
		{
			packet.asCPacketPlayer().y += 0.07
			isStep = false
		}
	}

	// There could be some anti cheats which tries to detect step by checking for achievements and stuff
	private fun fakeJump(thePlayer: IEntityPlayerSP)
	{
		thePlayer.isAirBorne = true
		thePlayer.triggerAchievement(classProvider.getStatEnum(StatType.JUMP_STAT))
	}

	private fun couldStep(theWorld: IWorldClient, thePlayer: IEntityPlayerSP): Boolean
	{
		val func = functions

		val yaw = MovementUtils.getDirection(thePlayer)
		val x = -func.sin(yaw) * 0.4
		val z = func.cos(yaw) * 0.4

		return theWorld.getCollisionBoxes(thePlayer.entityBoundingBox.offset(x, 1.001335979112147, z)).isEmpty()
	}

	fun canAirStep(): Boolean = sequenceOf("Vanilla", "NCP", "OldNCP", "AAC3.1.5", "Spartan127", "Rewinside").any { modeValue.get().equals(it, ignoreCase = true) }

	override val tag: String
		get() = modeValue.get()
}
