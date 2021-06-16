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
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.utils.timer.TimeUtils.scheduleDelayedTask
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import org.lwjgl.input.Keyboard

// I'm not an author of this TargetStrafe code. Original author: turtl (Converted from JS https://github.com/chocopie69/Liquidbounce-Scripts/blob/main/combat/superKB.js)
@ModuleInfo(name = "SuperKnockback", description = "Increases knockback dealt to other entities.", category = ModuleCategory.COMBAT)
class SuperKnockback : Module()
{
	private val modeValue = ListValue("Mode", arrayOf("Packet", "Packet-WTap", "WTap", "SuperPacket", "Deprecated"), "Packet")
	private val hurtTimeValue = IntegerValue("HurtTime", 10, 0, 10)
	private val ticksDelayValue = IntegerValue("TicksDelay", 0, 0, 60)

	/**
	 * Exploits
	 */

	private val noMoveExploitValue = BoolValue("NoMoveExploit", true)
	private val wtapNoMoveExploitValue = BoolValue("WTap-NoMoveExploit", true)
	private val noSprintExploitValue = BoolValue("NoSprintExploit", true)
	private val notSprintingSlowdownValue = BoolValue("NotSprintingSlowdown", true)

	private val maxDelayValue: IntegerValue = object : IntegerValue("MaxDelay", 55, 1, 1000)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val v = minDelayValue.get()
			if (v > newValue) set(v)
		}
	}
	private val minDelayValue: IntegerValue = object : IntegerValue("MinDelay", 45, 1, 1000)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val v = maxDelayValue.get()
			if (v < newValue) set(v)
		}
	}

	private val maxDelayMultiplierValue: FloatValue = object : FloatValue("MaxDelayMultiplier", 2.3F, 1.1F, 3F)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = minDelayMultiplierValue.get()
			if (v > newValue) set(v)
		}
	}
	private val minDelayMultiplierValue: FloatValue = object : FloatValue("MinDelayMultiplier", 2F, 1.1F, 3F)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = maxDelayMultiplierValue.get()
			if (v < newValue) set(v)
		}
	}

	private val packetOverride = BoolValue("PacketOverride", true)

	private var knockTicks = 0
	private var superKnockback = false
	private var needSprint = false

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
		needSprint = false
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
				val victim = attackPacket.getEntityFromWorld(theWorld)?.asEntityLivingBase() ?: return

				val gameSettings = mc.gameSettings

				val noMoveExploit = noMoveExploitValue.get()

				val playerMove = MovementUtils.isMoving(thePlayer)
				val playerMove2 = thePlayer.posX - thePlayer.lastTickPosX + thePlayer.posZ - thePlayer.lastTickPosZ == 0.0
				needSprint = thePlayer.sprinting
				superKnockback = (needSprint || noSprintExploitValue.get()) && (playerMove || noMoveExploit)

				if (victim.hurtTime <= hurtTimeValue.get() && knockTicks <= ticksDelayValue.get() && superKnockback)
				{
					val minDelay = minDelayValue.get()
					val maxDelay = maxDelayValue.get()
					val minDelayMultiplier = minDelayMultiplierValue.get()
					val maxDelayMultiplier = maxDelayMultiplierValue.get()
					when (mode)
					{
						"packet" ->
						{
							// NoMove exploit
							if (!playerMove && noMoveExploit)
							{
								if (!thePlayer.sprinting) thePlayer.sprinting = true

								MovementUtils.addMotion(thePlayer, 1.0E-5F)
							}

							netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.STOP_SPRINTING))
							netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.START_SPRINTING))

							if (!needSprint)
							{
								scheduleDelayedTask({
									netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.STOP_SPRINTING))
									thePlayer.sprinting = false
								}, 1L)
							}
						}

						"packet-wtap" ->
						{
							if (!needSprint) netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.START_SPRINTING))

							scheduleDelayedTask({
								netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.STOP_SPRINTING))

								if (notSprintingSlowdownValue.get() && thePlayer.sprinting) thePlayer.sprinting = false

							}, TimeUtils.randomDelay(minDelay, maxDelay))

							if (needSprint) scheduleDelayedTask({
								netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.START_SPRINTING))

								if (notSprintingSlowdownValue.get() && !thePlayer.sprinting) thePlayer.sprinting = true

							}, (TimeUtils.randomDelay(minDelay, maxDelay) * RandomUtils.nextFloat(minDelayMultiplier, maxDelayMultiplier)).toLong())
						}

						"wtap" ->
						{
							if ((!playerMove || !playerMove2) && wtapNoMoveExploitValue.get())
							{
								if (!needSprint) netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.START_SPRINTING))

								scheduleDelayedTask({
									netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.STOP_SPRINTING))

									if (notSprintingSlowdownValue.get() && !thePlayer.sprinting) thePlayer.sprinting = false

								}, TimeUtils.randomDelay(minDelay, maxDelay))

								if (needSprint) scheduleDelayedTask({
									netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.START_SPRINTING))

									if (notSprintingSlowdownValue.get()) thePlayer.sprinting = true

								}, (TimeUtils.randomDelay(minDelay, maxDelay) * RandomUtils.nextFloat(minDelayMultiplier, maxDelayMultiplier)).toLong())
							}
							else
							{
								if (!needSprint && !thePlayer.sprinting) thePlayer.sprinting = true

								if (gameSettings.keyBindForward.pressed) gameSettings.keyBindForward.pressed = false

								scheduleDelayedTask({
									gameSettings.keyBindForward.pressed = Keyboard.isKeyDown(gameSettings.keyBindForward.keyCode)
								}, TimeUtils.randomDelay(minDelay, maxDelay))

								if (!needSprint) scheduleDelayedTask({
									if (thePlayer.sprinting) thePlayer.sprinting = false
								}, (TimeUtils.randomDelay(minDelay, maxDelay) * RandomUtils.nextFloat(minDelayMultiplier, maxDelayMultiplier)).toLong())
							}
						}

						"superpacket" ->
						{
							if (!thePlayer.sprinting) netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.START_SPRINTING))

							netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.STOP_SPRINTING))
							netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.START_SPRINTING))

							if (!needSprint)
							{
								thePlayer.sprinting = false
								scheduleDelayedTask({ netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.STOP_SPRINTING)) }, 1)
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
