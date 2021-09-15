/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac.*
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.minorACs.*
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.ncp.*
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other.*
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.spartan.SpartanYPort
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.spectre.SpectreBHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.spectre.SpectreLowHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.spectre.SpectreOnGround
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.vanilla.Vanilla
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.NotificationIcon
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.value.*

@ModuleInfo(name = "Speed", description = "Allows you to move faster.", category = ModuleCategory.MOVEMENT)
object Speed : Module()
{
	private val speedModes = arrayOf(

		// Vanilla
		Vanilla(),

		// NCP
		OldNCPBHop(), NCPFHop(), SNCPBHop(), NCPBHop(), YPort(), YPort2(), NCPYPort(), Boost(), Frame(), MiJump(), OnGround(),

		// AAC BHop, LowHop, (Y)Port, Ground speeds
		AAC1_9_10BHop(), AAC3_0_3LowHop(), AAC3_0_5BHop(), AAC3_1_0YPort(), AAC3_1_5LowHop(), AAC3_1_5FastLowHop(), AAC3_2_2LowHop(), AAC3_2_2YPort(), AAC3_3_7FlagBHop(), AAC3_3_9BHop(), AAC3_3_11LowHop(), AAC3_3_11BHop(), AAC3_3_11Ground(), AAC3_3_11Ground2(), AAC3_3_13LowHop(), AAC3_5_0BHop(), AAC3_5_0LowHop(), AAC4_4_0BHop(), AAC5BHop(),

		// AAC
		AACPort(),

		// Spartan
		SpartanYPort(),

		// Other AntiCheats
		ACP(), ACPBHop(), ACRBHop(), PACBHop(), DaedalusAACBHop(), MatrixBHop(),

		// Spectre
		SpectreLowHop(), SpectreBHop(), SpectreOnGround(), TeleportCubeCraft(),

		// Server
		HiveHop(), HypixelHop(),

		// Mineplex
		MineplexGround(), MineplexBHop(), MineplexBHop2(),

		// RedeSky
		RedeSkyBoostHop(), RedeSkySlowHop(),

		// Other
		SlowHop(), CustomSpeed()

	)

	private val speedModeMap = mapOf(*speedModes.map { it.modeName to it }.toTypedArray())

	val modeValue: ListValue = object : ListValue("Mode", speedModeMap.keys.toTypedArray(), "NCPBHop")
	{
		override fun onChange(oldValue: String, newValue: String)
		{
			if (state) onDisable()
		}

		override fun onChanged(oldValue: String, newValue: String)
		{
			if (state) onEnable()
		}
	}

	// Vanilla Speed
	val vanillaSpeedValue = object : FloatValue("VanillaSpeed", 0.5f, 0.2f, 10.0f)
	{
		override fun showCondition() = modeValue.get().equals("Vanilla", ignoreCase = true)
	}

	// Custom Speed
	private val customGroup = object : ValueGroup("Custom")
	{
		override fun showCondition() = modeValue.get().equals("Custom", ignoreCase = true)
	}
	val customSpeedValue = FloatValue("CustomSpeed", 1.6f, 0.2f, 2f)
	val customYValue = FloatValue("CustomY", 0f, 0f, 4f)
	val customTimerValue = FloatValue("CustomTimer", 1f, 0.1f, 2f)
	val customStrafeValue = BoolValue("CustomStrafe", true)
	val customResetXZValue = BoolValue("CustomResetXZ", false)
	val customResetYValue = BoolValue("CustomResetY", false)

	// AAC Port length
	val portMax = object : FloatValue("AAC-PortLength", 1f, 1f, 20f)
	{
		override fun showCondition() = modeValue.get().equals("AACPort", ignoreCase = true)
	}

	// AAC Ground timer
	val aacGroundTimerValue = object : FloatValue("AACGround-Timer", 3f, 1.1f, 10f)
	{
		override fun showCondition() = modeValue.get().equals("AAC3.3.11-Ground", ignoreCase = true) || modeValue.get().equals("AAC3.3.11-Ground2", ignoreCase = true)
	}

	// Cubecraft Port length
	val cubecraftPortLengthValue = object : FloatValue("CubeCraft-PortLength", 1f, 0.1f, 2f)
	{
		override fun showCondition() = modeValue.get().equals("CubeCraft", ignoreCase = true)
	}

	// Mineplex Ground speed
	val mineplexGroundSpeedValue = object : FloatValue("MineplexGround-Speed", 0.5f, 0.1f, 1f)
	{
		override fun showCondition() = modeValue.get().equals("Mineplex-Ground", ignoreCase = true)
	}

	// Slowhop multiplier
	val slowHopMultiplierValue = object : FloatValue("SlowHop-Multiplier", 1.011f, 1.001f, 1.015f)
	{
		override fun showCondition() = modeValue.get().equals("SlowHop", ignoreCase = true)
	}

	private val ncphopGroup = object : ValueGroup("NCPHop")
	{
		override fun showCondition() = modeValue.get().equals("NCPBHop", ignoreCase = true) || modeValue.get().equals("NCPFHop", ignoreCase = true)
	}
	val ncphopBoostTicks = IntegerValue("BoostTicks", 1, 1, 5)
	val ncphopNoBoostTicks = IntegerValue("NoBoostTicks", 0, 0, 5)

	private val disableOnFlagValue = BoolValue("DisableOnFlag", true)

	private var mode: SpeedMode? = null

	override val tag: String
		get() = modeValue.get()

	init
	{
		ncphopGroup.addAll(ncphopBoostTicks, ncphopNoBoostTicks)

		customGroup.addAll(customSpeedValue, customYValue, customTimerValue, customStrafeValue, customResetXZValue, customResetYValue)
	}

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent?)
	{
		val thePlayer = mc.thePlayer ?: return

		if (thePlayer.sneaking) return

		if (MovementUtils.isMoving(thePlayer)) thePlayer.sprinting = true

		mode?.onUpdate()
	}

	@EventTarget
	fun onMotion(event: MotionEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		if (thePlayer.sneaking) return

		mode?.onMotion(event.eventState)
	}

	@EventTarget
	fun onMove(event: MoveEvent)
	{
		if ((mc.thePlayer ?: return).sneaking) return
		mode?.onMove(event)
	}

	@EventTarget
	fun onTick(@Suppress("UNUSED_PARAMETER") event: TickEvent?)
	{
		if ((mc.thePlayer ?: return).sneaking) return

		mode?.onTick()
	}

	@EventTarget
	fun onPacket(event: PacketEvent)
	{
		if (classProvider.isSPacketPlayerPosLook(event.packet) && disableOnFlagValue.get())
		{
			val thePlayer = mc.thePlayer ?: return

			state = false

			MovementUtils.zeroXZ(thePlayer)
			thePlayer.jumpMovementFactor = 0.02F

			LiquidBounce.hud.addNotification(NotificationIcon.WARNING_RED, "Speed", "A teleport has been detected. Disabled Speed to prevent more flags.", 1000L)
		}
	}

	override fun onEnable()
	{
		mode = speedModeMap[modeValue.get()]

		mc.thePlayer ?: return

		mc.timer.timerSpeed = 1f

		mode?.onEnable()
	}

	override fun onDisable()
	{
		mc.thePlayer ?: return

		mc.timer.timerSpeed = 1f

		mode?.onDisable()
	}

	fun allowSprintBoost(): Boolean
	{
		val mode = modeValue.get()
		return sequenceOf("AAC3.3.11-Ground", "AAC3.3.11-Ground2", "AACPort", "ACP").any { mode.equals(it, ignoreCase = true) }
	}
}
