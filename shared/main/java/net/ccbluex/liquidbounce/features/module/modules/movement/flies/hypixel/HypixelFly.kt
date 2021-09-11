package net.ccbluex.liquidbounce.features.module.modules.movement.flies.hypixel

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.potion.PotionType
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.DamageOnStart
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.NotificationType
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TickTimer
import kotlin.math.hypot
import kotlin.math.max

class HypixelFly : FlyMode("Hypixel")
{
	override val shouldDisableNoFall: Boolean
		get() = waitForDamage

	override val mark: Boolean
		get() = hypixelFlyStarted

	override val damageOnStart: DamageOnStart
		get() = if (Fly.hypixelDamageBoostEnabledValue.get()) DamageOnStart.HYPIXEL else DamageOnStart.OFF

	private val hypixelFlyTimer = MSTimer()
	private val hypixelTimer = TickTimer()

	private var hypixelFlyStarted = false
	private var hypixelDamageBoostFailed = false
	private var canPerformHypixelDamageFly = false
	private var hypixelBoostStep = 1
	private var hypixelBoostSpeed = 0.0
	private var lastDistance = 0.0
	private var waitForDamage: Boolean = false

	override fun onEnable()
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		val onGround = thePlayer.onGround

		hypixelFlyTimer.reset()

		val hypixelJump = Fly.hypixelJumpValue.get()
		if ((Fly.hypixelDamageBoostEnabledValue.get() && (Fly.hypixelDamageBoostAirStartModeValue.get().equals("WaitForDamage", ignoreCase = true) || onGround)).also { canPerformHypixelDamageFly = it })
		{
			if (onGround) // If player is on ground, try to damage.
			{
				if (!hypixelFlyStarted) if (Fly.hypixelDamageBoostStartTimingValue.get().equals("Immediately", ignoreCase = true))
				{
					if (hypixelJump) jump(theWorld, thePlayer)

					hypixelBoostStep = 1
					hypixelBoostSpeed = 0.1
					lastDistance = 0.0
					hypixelDamageBoostFailed = false
					hypixelFlyStarted = true
					hypixelFlyTimer.reset()
				}
				else waitForDamage = true
			}
			else waitForDamage = true
		}
		else if (hypixelJump && onGround) jump(theWorld, thePlayer)
	}

	override fun onUpdate()
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		val timer = mc.timer

		val boostDelay = Fly.hypixelTimerBoostDelayValue.get().toLong()
		val boostTimer = Fly.hypixelTimerBoostTimerValue.get()

		when
		{
			hypixelFlyStarted ->
			{

				// Timer Boost
				if (Fly.hypixelTimerBoostEnabledValue.get())
				{
					if (hypixelFlyTimer.hasTimePassed(boostDelay)) timer.timerSpeed = 1.0F
					else timer.timerSpeed = 1.0F + boostTimer * (hypixelFlyTimer.hasTimeLeft(boostDelay).toFloat() / boostDelay.toFloat())
				}

				// ychinc
				if (Fly.hypixelYchIncValue.get() && !canPerformHypixelDamageFly)
				{
					hypixelTimer.update()

					if (hypixelTimer.hasTimePassed(2))
					{
						thePlayer.setPosition(thePlayer.posX, thePlayer.posY + 1.0E-5, thePlayer.posZ)
						hypixelTimer.reset()
					}
				}
			}

			!canPerformHypixelDamageFly ->
			{
				// Start without boost
				hypixelFlyStarted = true
				hypixelFlyTimer.reset()
			}

			waitForDamage && thePlayer.hurtTime > 0 ->
			{
				// Start boost after the player takes damage
				if (Fly.hypixelJumpValue.get()) jump(theWorld, thePlayer)

				hypixelBoostStep = 1
				hypixelBoostSpeed = 0.1
				lastDistance = 0.0
				hypixelDamageBoostFailed = false
				hypixelFlyStarted = true
				hypixelFlyTimer.reset()
				waitForDamage = false
				Fly.markStartY = thePlayer.posY // apply y change caused by jump()
			}
		}
	}

	override fun onMotion(eventState: EventState)
	{
		val thePlayer = mc.thePlayer ?: return

		val posX = thePlayer.posX
		val posY = thePlayer.posY
		val posZ = thePlayer.posZ

		if (canPerformHypixelDamageFly && hypixelFlyStarted)
		{
			when (eventState)
			{
				EventState.PRE ->
				{
					if (Fly.hypixelYchIncValue.get())
					{
						hypixelTimer.update()
						if (hypixelTimer.hasTimePassed(2))
						{
							thePlayer.setPosition(posX, posY + 1.0E-5, posZ)
							hypixelTimer.reset()
						}
					}

					if (!hypixelDamageBoostFailed) thePlayer.motionY = 0.0
				}

				EventState.POST ->
				{
					val xDist = posX - thePlayer.prevPosX
					val zDist = posZ - thePlayer.prevPosZ
					lastDistance = hypot(xDist, zDist)
				}
			}
		}
	}

	override fun onPacket(event: PacketEvent)
	{
		val packet = event.packet

		if (classProvider.isCPacketPlayer(packet) && hypixelFlyStarted) packet.asCPacketPlayer().onGround = Fly.hypixelOnGroundValue.get()

		if (classProvider.isSPacketPlayerPosLook(packet) && canPerformHypixelDamageFly && hypixelFlyStarted && !hypixelDamageBoostFailed)
		{
			hypixelDamageBoostFailed = true
			LiquidBounce.hud.addNotification(NotificationType.WARNING, "Hypixel Damage-Boost Fly", "A teleport has been detected. Disabled Damage-Boost to prevent more flags.", 1000L)
		}
	}

	override fun onMove(event: MoveEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		if (!canPerformHypixelDamageFly || !hypixelFlyStarted) return

		if (!MovementUtils.isMoving(thePlayer))
		{
			event.x = 0.0
			event.z = 0.0

			thePlayer.motionX = event.x
			thePlayer.motionZ = event.z

			return
		}

		if (hypixelDamageBoostFailed) return

		val step1Speed = if (thePlayer.isPotionActive(classProvider.getPotionEnum(PotionType.MOVE_SPEED))) 1.56 else 2.034
		val speedEffectAffect = 1 + 0.2 * MovementUtils.getSpeedEffectAmplifier(thePlayer)
		val baseSpeed = 0.29 * speedEffectAffect

		when (hypixelBoostStep)
		{
			1 ->
			{
				hypixelBoostSpeed = step1Speed * baseSpeed
				hypixelBoostStep = 2
			}

			2 ->
			{
				hypixelBoostSpeed *= 2.16
				hypixelBoostStep = 3
			}

			3 ->
			{
				hypixelBoostSpeed = lastDistance - (if (thePlayer.ticksExisted % 2 == 0) 0.0103 else 0.0123) * (lastDistance - baseSpeed)
				hypixelBoostStep = 4
			}

			else -> hypixelBoostSpeed = lastDistance - lastDistance / 159.8
		}

		hypixelBoostSpeed = max(hypixelBoostSpeed, 0.3)

		val dir = MovementUtils.getDirection(thePlayer)

		event.x = -functions.sin(dir) * hypixelBoostSpeed
		event.z = functions.cos(dir) * hypixelBoostSpeed

		thePlayer.motionX = event.x
		thePlayer.motionZ = event.z
	}

	override fun onBlockBB(event: BlockBBEvent)
	{
		val posY = (mc.thePlayer ?: return).posY

		if (classProvider.isBlockAir(event.block) && hypixelFlyStarted && event.y < posY) event.boundingBox = classProvider.createAxisAlignedBB(event.x.toDouble(), event.y.toDouble(), event.z.toDouble(), event.x + 1.0, posY, event.z + 1.0)
	}

	override fun onJump(event: JumpEvent)
	{
		if (hypixelFlyStarted) event.cancelEvent()
	}

	override fun onStep(event: StepEvent)
	{
		if (hypixelFlyStarted) event.stepHeight = 0f
	}

	override fun onDisable()
	{
		waitForDamage = false
		hypixelFlyStarted = false
		canPerformHypixelDamageFly = false
	}
}
