/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketEntityAction
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketUseEntity
import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.WorkerUtils
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import org.lwjgl.input.Keyboard
import java.util.concurrent.TimeUnit

// Original author: turtl (https://github.com/chocopie69/Liquidbounce-Scripts/blob/main/combat/superKB.js and https://github.com/CzechHek/Core/blob/master/Scripts/SuperKnock.js)
@ModuleInfo(name = "SuperKnockback", description = "Increases knockback dealt to other entities.", category = ModuleCategory.COMBAT)
class SuperKnockback : Module()
{
	/**
	 * Mode
	 */
	private val modeValue = ListValue("Mode", arrayOf("Packet", "Packet_W-Tap", "W-Tap", "SuperPacket", "Deprecated"), "Packet")

	/**
	 * Hurt-time
	 */
	private val hurtTimeValue = IntegerValue("HurtTime", 10, 0, 10)

	/**
	 * Delay in ticks
	 */
	private val ticksDelayValue = IntegerValue("TicksDelay", 0, 0, 60)

	/**
	 * Exploits
	 */
	private val noMoveExploitValue = BoolValue("NoMoveExploit", true) // NoMove is not applicable with W-Tap mode
	private val wtapNoMoveExploitValue = BoolValue("NoMoveExploit_W-Tap", true)
	private val noSprintExploitValue = BoolValue("NoSprintExploit", true)
	private val notSprintingSlowdownValue = BoolValue("NotSprintingSlowdown", true)

	/**
	 * Delay
	 */
	private val maxDelayValue: IntegerValue = object : IntegerValue("MaxDelay", 55, 1, 1000)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = minDelayValue.get()
			if (i > newValue) set(i)
		}
	}
	private val minDelayValue: IntegerValue = object : IntegerValue("MinDelay", 45, 1, 1000)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = maxDelayValue.get()
			if (i < newValue) set(i)
		}
	}

	/**
	 * Multiplier
	 */
	private val maxMultiplierValue: FloatValue = object : FloatValue("MaxMultiplier", 2.3F, 1.1F, 3F)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val i = minMultiplierValue.get()
			if (i > newValue) set(i)
		}
	}
	private val minMultiplierValue: FloatValue = object : FloatValue("MinMultiplier", 2F, 1.1F, 3F)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val i = maxMultiplierValue.get()
			if (i < newValue) set(i)
		}
	}

	private val packetOverride = BoolValue("PacketOverride", true)

	private var knockTicks = 0
	private var superKnockback = false
	private var sprinting = false

	@EventTarget
	fun onAttack(event: AttackEvent)
	{
		val provider = classProvider

		val targetEntity = event.targetEntity

		if (modeValue.get().equals("Deprecated", ignoreCase = true) && targetEntity != null && provider.isEntityLivingBase(targetEntity))
		{
			if (targetEntity.asEntityLivingBase().hurtTime > hurtTimeValue.get()) return

			val thePlayer = mc.thePlayer ?: return
			val netHandler = mc.netHandler

			if (thePlayer.sprinting) netHandler.addToSendQueue(provider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.STOP_SPRINTING))

			netHandler.addToSendQueue(provider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.START_SPRINTING))
			netHandler.addToSendQueue(provider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.STOP_SPRINTING))
			netHandler.addToSendQueue(provider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.START_SPRINTING))

			thePlayer.sprinting = true
			thePlayer.serverSprintState = true
		}
	}

	override fun onEnable()
	{
		knockTicks = 0
		superKnockback = false
		sprinting = false
	}

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
	{
		if (knockTicks > 0) knockTicks -= 1
	}

	@EventTarget
	fun onPacket(event: PacketEvent)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		val packet = event.packet

		val netHandler = mc.netHandler

		// Packet Override
		if ((classProvider.isCPacketPlayerPosition(packet) || classProvider.isCPacketPlayerLook(packet) || classProvider.isCPacketPlayerPosLook(packet)) && packetOverride.get())
		{
			val movePacket = packet.asCPacketPlayer()
			if (!movePacket.moving && (thePlayer.onGround || movePacket.onGround)) movePacket.moving = true
		}
		else if (classProvider.isCPacketUseEntity(packet))
		{
			val mode = modeValue.get().toLowerCase()
			if (mode.equals("deprecated", ignoreCase = true)) return

			val attackPacket = packet.asCPacketUseEntity()
			if (attackPacket.action == ICPacketUseEntity.WAction.ATTACK)
			{
				val target = attackPacket.getEntityFromWorld(theWorld)?.asEntityLivingBase() ?: return

				val gameSettings = mc.gameSettings

				val noMoveExploit = noMoveExploitValue.get()

				val movementInput = MovementUtils.isMoving(thePlayer)
				val positionChanged = thePlayer.posX - thePlayer.lastTickPosX + thePlayer.posZ - thePlayer.lastTickPosZ == 0.0

				sprinting = thePlayer.sprinting
				superKnockback = (sprinting || noSprintExploitValue.get()) && (movementInput || noMoveExploit)

				if (target.hurtTime <= hurtTimeValue.get() && knockTicks <= ticksDelayValue.get() && superKnockback)
				{
					val minDelay = minDelayValue.get()
					val maxDelay = maxDelayValue.get()

					val minDelayMultiplier = minMultiplierValue.get()
					val maxDelayMultiplier = maxMultiplierValue.get()

					val notSprintingSlowdown = notSprintingSlowdownValue.get()

					val scheduleDelayedTask = { delayMillis: Long, task: () -> Unit -> WorkerUtils.scheduledWorkers.schedule(task, delayMillis, TimeUnit.MILLISECONDS) }

					when (mode)
					{
						"packet" ->
						{
							// NoMove exploit

							if (!movementInput && noMoveExploit)
							{
								if (!thePlayer.sprinting) thePlayer.sprinting = true

								MovementUtils.addMotion(thePlayer, 1.0E-5F)
							}

							netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.STOP_SPRINTING))
							netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.START_SPRINTING))

							// Restore the original sprinting state
							if (!sprinting) scheduleDelayedTask(1L) {
								netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.STOP_SPRINTING))
								thePlayer.sprinting = false
							}
						}

						"superpacket" ->
						{
							if (!thePlayer.sprinting) netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.START_SPRINTING))

							netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.STOP_SPRINTING))
							netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.START_SPRINTING))

							// Restore the original sprinting state
							if (!sprinting)
							{
								thePlayer.sprinting = false
								scheduleDelayedTask(1L) { netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.STOP_SPRINTING)) }
							}
						}

						"w-tap" ->
						{
							if ((!movementInput || !positionChanged) && wtapNoMoveExploitValue.get())
							{
								// NoMove exploit for W-Tap

								if (!sprinting) netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.START_SPRINTING))

								scheduleDelayedTask(TimeUtils.randomDelay(minDelay, maxDelay)) {
									netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.STOP_SPRINTING))

									if (notSprintingSlowdown && !thePlayer.sprinting) thePlayer.sprinting = false
								}

								// Restore the original sprinting state
								if (sprinting) scheduleDelayedTask((TimeUtils.randomDelay(minDelay, maxDelay) * RandomUtils.nextFloat(minDelayMultiplier, maxDelayMultiplier)).toLong()) {
									netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.START_SPRINTING))

									if (notSprintingSlowdown) thePlayer.sprinting = true
								}
							}
							else
							{
								// A legit W-Tap
								if (!sprinting && !thePlayer.sprinting) thePlayer.sprinting = true

								// Stop sprinting
								if (gameSettings.keyBindForward.pressed) gameSettings.keyBindForward.pressed = false

								// Start sprinting after some delay
								scheduleDelayedTask(TimeUtils.randomDelay(minDelay, maxDelay)) {
									gameSettings.keyBindForward.pressed = Keyboard.isKeyDown(gameSettings.keyBindForward.keyCode)
								}

								// Restore the original sprinting state
								if (!sprinting) scheduleDelayedTask((TimeUtils.randomDelay(minDelay, maxDelay) * RandomUtils.nextFloat(minDelayMultiplier, maxDelayMultiplier)).toLong()) {
									if (thePlayer.sprinting) thePlayer.sprinting = false
								}
							}
						}

						"packet_w-tap" ->
						{
							// Start sprinting
							if (!sprinting) netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.START_SPRINTING))

							// Stop sprinting after some delay
							scheduleDelayedTask(TimeUtils.randomDelay(minDelay, maxDelay)) {
								netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.STOP_SPRINTING))
								if (notSprintingSlowdown && thePlayer.sprinting) thePlayer.sprinting = false
							}

							// Restore the original sprinting state
							if (sprinting) scheduleDelayedTask((TimeUtils.randomDelay(minDelay, maxDelay) * RandomUtils.nextFloat(minDelayMultiplier, maxDelayMultiplier)).toLong()) {
								netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.START_SPRINTING))

								if (notSprintingSlowdown && !thePlayer.sprinting) thePlayer.sprinting = true
							}
						}
					}
				}

				if (knockTicks == 0) knockTicks = ticksDelayValue.get()
			}
		}
	}

	override val tag: String
		get() = modeValue.get()
}
