/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.MovementUtils.direction
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import kotlin.math.cos
import kotlin.math.sin

@ModuleInfo(name = "LongJump", description = "Allows you to jump further.", category = ModuleCategory.MOVEMENT)
class LongJump : Module()
{
	private val modeValue = ListValue("Mode", arrayOf("NCP", "Teleport", "AAC3.0.1", "AAC3.0.5", "AAC3.1.0", "Mineplex", "Mineplex2", "Mineplex3", "RedeSky"), "NCP")
	private val ncpBoostValue = FloatValue("NCPBoost", 4.25f, 1f, 10f)
	private val teleportDistanceValue = FloatValue("TeleportDistance", 2.5f, 1.0f, 10.0f)
	private val autoJumpValue = BoolValue("AutoJump", false)
	private val autoDisableValue = BoolValue("AutoDisable", true)

	private var jumped = false
	private var canBoost = false
	private var boosted = false
	private var teleported = false
	private var canMineplexBoost = false

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent?)
	{
		if (LadderJump.jumped) MovementUtils.strafe(MovementUtils.speed * 1.08f)

		val thePlayer = mc.thePlayer ?: return

		val autoDisable = autoDisableValue.get()

		if (jumped)
		{
			val mode = modeValue.get()

			if (thePlayer.onGround || thePlayer.capabilities.isFlying)
			{
				jumped = false
				canMineplexBoost = false

				if (mode.equals("NCP", ignoreCase = true))
				{
					thePlayer.motionX = 0.0
					thePlayer.motionZ = 0.0
				}

				if (boosted && autoDisable) state = false
				return
			}
			run {
				when (mode.toLowerCase())
				{
					"ncp" ->
					{
						MovementUtils.strafe(MovementUtils.speed * if (canBoost) ncpBoostValue.get() else 1f)
						canBoost = false
						if (boosted && autoDisable) state = false
					}

					"aac3.0.1" ->
					{
						thePlayer.motionY += 0.05999
						MovementUtils.strafe(MovementUtils.speed * 1.08f)
						boosted = true
					}

					"aac3.0.5", "mineplex3" ->
					{
						thePlayer.jumpMovementFactor = 0.09f
						thePlayer.motionY += 0.0132099999999999999999999999999
						thePlayer.jumpMovementFactor = 0.08f
						MovementUtils.strafe()
						boosted = true
					}

					"aac3.1.0" ->
					{
						if (thePlayer.fallDistance > 0.5f && !teleported)
						{
							val value = 3.0
							val horizontalFacing = thePlayer.horizontalFacing
							var x = 0.0
							var z = 0.0

							when
							{
								horizontalFacing.isNorth() -> z = -value
								horizontalFacing.isEast() -> x = +value
								horizontalFacing.isSouth() -> z = +value
								horizontalFacing.isWest() -> x = -value

								else ->
								{
								}
							}

							thePlayer.setPosition(thePlayer.posX + x, thePlayer.posY, thePlayer.posZ + z)
							teleported = true
						}
					}

					"mineplex" ->
					{
						thePlayer.motionY += 0.0132099999999999999999999999999
						thePlayer.jumpMovementFactor = 0.08f
						boosted = true
						MovementUtils.strafe()
					}

					"mineplex2" ->
					{
						if (!canMineplexBoost) return@run

						thePlayer.jumpMovementFactor = 0.1f
						if (thePlayer.fallDistance > 1.5f)
						{
							thePlayer.jumpMovementFactor = 0f
							thePlayer.motionY = (-10f).toDouble()
						}
						boosted = true
						MovementUtils.strafe()
					}

					"redesky" ->
					{
						thePlayer.jumpMovementFactor = 0.15f
						thePlayer.motionY += 0.05f
						boosted = true
					}
				}
			}
		}
		if (autoJumpValue.get() && thePlayer.onGround && isMoving)
		{
			jumped = true
			thePlayer.jump()
		}
	}

	@EventTarget
	fun onMove(event: MoveEvent)
	{
		val thePlayer = mc.thePlayer ?: return
		val mode = modeValue.get()

		if (mode.equals("mineplex3", ignoreCase = true))
		{
			if (thePlayer.fallDistance != 0.0f) thePlayer.motionY += 0.037
		} else if (jumped)

			if (mode.equals("Teleport", ignoreCase = true) && isMoving && canBoost)
			{
				val dir = direction
				event.x = -sin(dir) * teleportDistanceValue.get()
				event.z = cos(dir) * teleportDistanceValue.get()
				canBoost = false
				boosted = true
				if (autoDisableValue.get()) state = false
			} else if (mode.equals("NCP", ignoreCase = true) && !isMoving)
			{
				thePlayer.motionX = 0.0
				thePlayer.motionZ = 0.0
				event.zeroXZ()
			}
	}

	@EventTarget(ignoreCondition = true)
	fun onJump(event: JumpEvent)
	{
		jumped = true
		canBoost = true
		teleported = false

		if (state)
		{
			when (modeValue.get().toLowerCase())
			{
				"mineplex" -> event.motion = event.motion * 4.08f

				"mineplex2" ->
				{
					if (mc.thePlayer!!.isCollidedHorizontally)
					{
						event.motion = 2.31f
						canMineplexBoost = true
						mc.thePlayer!!.onGround = false
					}
				}
			}
		}
	}

	override val tag: String
		get() = when
		{
			modeValue.get().equals("NCP", ignoreCase = true) -> "NCP-${ncpBoostValue.get()}"
			modeValue.get().equals("Teleport", ignoreCase = true) -> "Teleport-${teleportDistanceValue.get()}"
			else -> modeValue.get()
		}
}
