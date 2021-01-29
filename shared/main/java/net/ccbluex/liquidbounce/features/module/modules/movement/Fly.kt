/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.enums.StatType
import net.ccbluex.liquidbounce.api.minecraft.potion.PotionType
import net.ccbluex.liquidbounce.api.minecraft.util.*
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.exploit.Damage
import net.ccbluex.liquidbounce.features.module.modules.render.Bobbing
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.MovementUtils.direction
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TickTimer
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.*

@ModuleInfo(name = "Fly", description = "Allows you to fly in survival mode.", category = ModuleCategory.MOVEMENT, keyBind = Keyboard.KEY_F)
class Fly : Module()
{
	val modeValue = ListValue(
		"Mode", arrayOf(
			"Vanilla", "SmoothVanilla", "Teleport",

			// NCP
			"NCP", "OldNCP",

			// AAC
			"AAC1.9.10", "AAC3.0.5", "AAC3.1.6-Gomme", "AAC3.3.12", "AAC3.3.12-Glide", "AAC3.3.13",

			// CubeCraft
			"CubeCraft",

			// Hypixel
			"Hypixel", "FreeHypixel", // BoostHypixel mode is merged with Hypixel mode

			// Rewinside
			"Rewinside", "TeleportRewinside",

			// Other server specific flys
			"Mineplex", "NeruxVace", "Minesucht", "RedeSky", "MCCentral",

			// Spartan
			"Spartan", "SpartanGlide", "BugSpartan",

			// Other anticheats
			"MineSecure", "HawkEye", "HAC", "WatchCat", "AntiCheatPlus",

			// Other
			"Jetpack", "KeepAlive", "Flag"
		), "Vanilla"
	)

	// Damage
	private val damageOnStartValue = BoolValue("DamageOnStart", false)
	private val damageModeValue = ListValue("DamageMode", arrayOf("NCP", "Hypixel"), "NCP")

	// Vanilla
	val vanillaSpeedValue = FloatValue("VanillaSpeed", 2f, 0f, 5f)
	private val vanillaKickBypassValue = BoolValue("VanillaKickBypass", false)

	// Teleport
	private val teleportDistanceValue = FloatValue("TeleportDistance", 1.0f, 1.0f, 5.0f)
	private val teleportDelayValue = IntegerValue("TeleportDelay", 100, 0, 1000)

	// NCP
	private val ncpMotionValue = FloatValue("NCPMotion", 0f, 0f, 1f)

	// AAC
	private val aacSpeedValue = FloatValue("AAC1.9.10-Speed", 0.3f, 0f, 5f)
	private val aacFast = BoolValue("AAC3.0.5-Fast", true)
	private val aac3_3_12_motion = FloatValue("AAC3.3.12-Motion", 10f, 0.1f, 10f)
	private val aac3_3_13_motion = FloatValue("AAC3.3.13-Motion", 10f, 0.1f, 10f)

	// Hypixel
	private val hypixelDMGBoost: BoolValue = object : BoolValue("Hypixel-DamageBoost", false)
	{
		override fun onChanged(oldValue: Boolean, newValue: Boolean)
		{
			if (modeValue.get().equals("Hypixel", ignoreCase = true) && newValue) damageOnStartValue.set(true)
		}
	}
	private val hypixelDMGBoostStartTiming = ListValue("Hypixel-DamageBoost-BoostTiming", arrayOf("Immediately", "AfterDamage"), "Immediately")
	private val hypixelDMGBoostAirStartMode = ListValue("Hypixel-DamageBoost-AirStartMode", arrayOf("WaitForDamage", "JustFlyWithoutDamageBoost"), "WaitForDamage")
	private val hypixelOnGroundValue = BoolValue("Hypixel-OnGround", false)
	private val hypixelYchIncValue = BoolValue("Hypixel-ychinc", true)
	private val hypixelJumpValue = BoolValue("Hypixel-Jump", false)

	private val hypixelTimerBoost = BoolValue("Hypixel-TimerBoost", true)
	private val hypixelTimerBoostDelay = IntegerValue("Hypixel-TimerBoost-BoostDelay", 1200, 0, 2000)
	private val hypixelTimerBoostTimer = FloatValue("Hypixel-TimerBoost-BoostTimer", 1f, 0f, 5f)

	private val mineplexSpeedValue = FloatValue("MineplexSpeed", 1f, 0.5f, 10f)
	private val neruxVaceTicks = IntegerValue("NeruxVace-Ticks", 6, 0, 20)
	private val redeskyVClipHeight = FloatValue("RedeSky-Height", 4f, 1f, 7f)
	private val mccTimerSpeedValue = FloatValue("MCCentral-Timer", 2.0f, 1.0f, 5.0f)

	// Reset Motions On Disable
	private val resetMotionOnDisable = BoolValue("ResetMotionOnDisable", false)

	// Visuals
	private val bobValue = BoolValue("Bob", true)
	private val markValue = BoolValue("Mark", true)

	// MODULE

	private val hypixelFlyTimer = MSTimer()
	private val groundTimer = MSTimer()
	private val mineSecureVClipTimer = MSTimer()
	private val spartanTimer = TickTimer()
	private val mineplexTimer = MSTimer()
	private val hypixelTimer = TickTimer()
	private val acpTickTimer = TickTimer()
	private val cubecraftTeleportTickTimer = TickTimer()
	val freeHypixelTimer = TickTimer()
	private val teleportTimer = MSTimer()

	@Suppress("PrivatePropertyName")
	private var aac3_1_6_touchedVoid = false
	private var minesuchtTP: Long = 0
	private var wasDead = false
	private var aacJump = 0.0
	private var aac3delay = 0
	private var aac3glideDelay = 0

	private var startY = 0.0
	private var markStartY = 0.0
	private var noPacketModify = false

	private var hypixelFlyStarted = false
	private var hypixelDamageBoostFailed = false
	private var canPerformHypixelDamageFly = false
	private var hypixelBoostStep = 1
	private var hypixelBoostSpeed = 0.0
	private var lastDistance = 0.0

	private var freeHypixelYaw = 0f
	private var freeHypixelPitch = 0f

	private var waitForDamage: Boolean = false

	override fun onEnable()
	{
		val thePlayer = mc.thePlayer ?: return

		hypixelFlyTimer.reset()
		noPacketModify = true

		val posX = thePlayer.posX
		val posY = thePlayer.posY
		val posZ = thePlayer.posZ
		val onGround = thePlayer.onGround

		val mode = modeValue.get()
		val damageOnStart = damageOnStartValue.get()

		val networkManager = mc.netHandler.networkManager

		if (damageOnStart && onGround)
		{
			var goalFallDistance = 3.0125 //add 0.0125 to ensure we get the fall dmg

			WorkerUtils.workers.submit {
				when (damageModeValue.get().toLowerCase())
				{
					"ncp" -> Damage.ncpDamage(1)

					"hypixel" ->
					{                        // TODO: Maximum packets per ticks limit
						for (i in 0..9)
						{

							//Imagine flagging to NCP.
							networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(posX, posY, posZ, true))
						}

						while (goalFallDistance > 0)
						{
							networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(posX, posY + 0.0624986421, posZ, false))
							networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(posX, posY + 0.0625, posZ, false))
							networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(posX, posY + 0.0624986421, posZ, false))
							networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(posX, posY + 0.0000013579, posZ, false))
							goalFallDistance -= 0.0624986421
						}

						networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(posX, posY, posZ, true))
					}
				}
			}

		}

		run {
			when (mode.toLowerCase())
			{
				"ncp" ->
				{
					if (!onGround) return@run

					if (!damageOnStart) Damage.ncpDamage(1)

					thePlayer.motionX *= 0.1
					thePlayer.motionZ *= 0.1
					thePlayer.swingItem()
				}

				"oldncp" ->
				{
					if (!onGround) return@run

					if (!damageOnStart) for (i in 0..3)
					{
						networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(posX, posY + 1.01, posZ, false))
						networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(posX, posY, posZ, false))
					}

					thePlayer.jump()
					thePlayer.swingItem()
				}

				"bugspartan" ->
				{
					if (!damageOnStart) Damage.ncpDamage(1)

					thePlayer.motionX *= 0.1
					thePlayer.motionZ *= 0.1
					thePlayer.swingItem()
				}

				"infinitycubecraft" -> ClientUtils.displayChatMessage("\u00A78[\u00A7c\u00A7lCubeCraft-\u00A7a\u00A7lFly\u00A78] \u00A7aPlace a block before landing.")

				"infinityvcubecraft" ->
				{
					ClientUtils.displayChatMessage("\u00A78[\u00A7c\u00A7lCubeCraft-\u00A7a\u00A7lFly\u00A78] \u00A7aPlace a block before landing.")

					thePlayer.setPosition(posX, posY + 2, posZ)
				}

				"hypixel" ->
				{
					val hypixelJump = hypixelJumpValue.get()
					if ((hypixelDMGBoost.get() && (hypixelDMGBoostAirStartMode.get().equals("WaitForDamage", ignoreCase = true) || onGround)).also { canPerformHypixelDamageFly = it })
					{
						if (onGround) // If player is on ground, try to damage.
						{
							if (!hypixelFlyStarted) if (hypixelDMGBoostStartTiming.get().equals("Immediately", ignoreCase = true))
							{
								if (hypixelJump) jump()

								hypixelBoostStep = 1
								hypixelBoostSpeed = 0.1
								lastDistance = 0.0
								hypixelDamageBoostFailed = false
								hypixelFlyStarted = true
								hypixelFlyTimer.reset()
							} else waitForDamage = true
						} else waitForDamage = true
					} else if (hypixelJump && onGround) jump()
				}

				"redesky" -> if (onGround) redeskyVClip(redeskyVClipHeight.get())

				"mccentral" -> mc.timer.timerSpeed = mccTimerSpeedValue.get()

				"anticheatplus" -> networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(posX, posY + 0.4, posZ, onGround))
			}
		}

		startY = posY
		markStartY = posY
		aacJump = -3.8
		noPacketModify = false

		if (mode.equals("freehypixel", ignoreCase = true))
		{
			freeHypixelTimer.reset()
			thePlayer.setPositionAndUpdate(posX, posY + 0.42, posZ)
			freeHypixelYaw = thePlayer.rotationYaw
			freeHypixelPitch = thePlayer.rotationPitch
		}
	}

	override fun onDisable()
	{
		wasDead = false

		val isRedeSkyMode = modeValue.get().equals("Redesky", ignoreCase = true)
		if (isRedeSkyMode) redeskySpeed(0)

		val thePlayer = mc.thePlayer ?: return

		aac3_1_6_touchedVoid = false

		waitForDamage = false
		hypixelFlyStarted = false
		canPerformHypixelDamageFly = false


		if (resetMotionOnDisable.get())
		{
			thePlayer.motionX = 0.0
			thePlayer.motionY = 0.0
			thePlayer.motionZ = 0.0
		}

		if (isRedeSkyMode) redeskyPacketHClip(0.0)

		thePlayer.capabilities.isFlying = false
		mc.timer.timerSpeed = 1f
		thePlayer.speedInAir = 0.02f
	}

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent?)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		val posX = thePlayer.posX
		val posY = thePlayer.posY
		val posZ = thePlayer.posZ
		val onGround = thePlayer.onGround
		val rotationYaw = thePlayer.rotationYaw
		val rotationPitch = thePlayer.rotationPitch

		val networkManager = mc.netHandler.networkManager
		val gameSettings = mc.gameSettings
		val jumpKeyDown = gameSettings.keyBindJump.isKeyDown
		val sneakKeyDown = gameSettings.keyBindSneak.isKeyDown

		val vanillaSpeed = vanillaSpeedValue.get()

		run {
			when (modeValue.get().toLowerCase())
			{
				"vanilla" ->
				{
					thePlayer.capabilities.isFlying = false
					thePlayer.motionY = 0.0
					thePlayer.motionX = 0.0
					thePlayer.motionZ = 0.0
					if (jumpKeyDown) thePlayer.motionY += vanillaSpeed
					if (sneakKeyDown) thePlayer.motionY -= vanillaSpeed
					MovementUtils.strafe(vanillaSpeed)
					handleVanillaKickBypass()
				}

				"smoothvanilla" ->
				{
					thePlayer.capabilities.isFlying = true
					handleVanillaKickBypass()
				}

				"teleport" ->
				{
					thePlayer.sprinting = true
					thePlayer.motionX = 0.0
					thePlayer.motionY = 0.0
					thePlayer.motionZ = 0.0
					if ((isMoving || jumpKeyDown || sneakKeyDown) && teleportTimer.hasTimePassed(teleportDelayValue.get().toLong()))
					{
						val yaw = direction
						val speed = teleportDistanceValue.get().toDouble()
						var x = 0.0
						var y = 0.0
						var z = 0.0
						if (isMoving && !thePlayer.isCollidedHorizontally)
						{
							x = -functions.sin(yaw) * speed
							z = functions.cos(yaw) * speed
						}

						if (!thePlayer.isCollidedVertically) if (jumpKeyDown && !sneakKeyDown) y = speed else if (!jumpKeyDown && sneakKeyDown) y = -speed

						if (isMoving && !thePlayer.isCollidedHorizontally)
						{
							x = -functions.sin(yaw) * speed
							z = functions.cos(yaw) * speed
						}

						if (!thePlayer.isCollidedVertically) if (jumpKeyDown && !sneakKeyDown) y = speed else if (!jumpKeyDown && sneakKeyDown) y = -speed

						thePlayer.setPosition(x.let { thePlayer.posX += it; posX }, y.let { thePlayer.posY += it; posY }, z.let { thePlayer.posZ += it; posZ })
						teleportTimer.reset()
					}
				}

				"cubecraft" ->
				{
					mc.timer.timerSpeed = 0.6f
					cubecraftTeleportTickTimer.update()
				}

				"ncp" ->
				{
					thePlayer.motionY = (-ncpMotionValue.get()).toDouble()
					if (sneakKeyDown) thePlayer.motionY = -0.5
					MovementUtils.strafe()
				}

				"oldncp" ->
				{
					if (startY > posY) thePlayer.motionY = -0.000000000000000000000000000000001
					if (sneakKeyDown) thePlayer.motionY = -0.2
					if (jumpKeyDown && posY < startY - 0.1) thePlayer.motionY = 0.2
					MovementUtils.strafe()
				}

				"aac1.9.10" ->
				{
					if (jumpKeyDown) aacJump += 0.2
					if (sneakKeyDown) aacJump -= 0.2

					if (startY + aacJump > posY)
					{
						networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayer(true))
						thePlayer.motionY = 0.8
						MovementUtils.strafe(aacSpeedValue.get())
					}
					MovementUtils.strafe()
				}

				"aac3.0.5" ->
				{
					if (aac3delay == 2) thePlayer.motionY = 0.1 else if (aac3delay > 2) aac3delay = 0
					if (aacFast.get())
					{
						if (thePlayer.movementInput.moveStrafe == 0.0f) thePlayer.jumpMovementFactor = 0.08f
						else thePlayer.jumpMovementFactor = 0f
					}
					aac3delay++
				}

				"aac3.1.6-gomme" ->
				{
					thePlayer.capabilities.isFlying = true

					if (aac3delay == 2) thePlayer.motionY += 0.05 else if (aac3delay > 2)
					{
						thePlayer.motionY -= 0.05
						aac3delay = 0
					}

					aac3delay++

					if (!aac3_1_6_touchedVoid) networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(posX, posY, posZ, onGround))
					if (posY <= 0.0) aac3_1_6_touchedVoid = true
				}

				"flag" ->
				{
					networkManager.sendPacketWithoutEvent(
						classProvider.createCPacketPlayerPosLook(
							posX + thePlayer.motionX * 999, posY + (if (jumpKeyDown) 1.5624 else 0.00000001) - if (sneakKeyDown) 0.0624 else 0.00000002, posZ + thePlayer.motionZ * 999, rotationYaw, rotationPitch, true
						)
					)
					networkManager.sendPacketWithoutEvent(
						classProvider.createCPacketPlayerPosLook(
							posX + thePlayer.motionX * 999, posY - 6969, posZ + thePlayer.motionZ * 999, rotationYaw, rotationPitch, true
						)
					)
					thePlayer.setPosition(posX + thePlayer.motionX * 11, posY, posZ + thePlayer.motionZ * 11)
					thePlayer.motionY = 0.0
				}

				"keepalive" ->
				{
					networkManager.sendPacketWithoutEvent(classProvider.createCPacketKeepAlive())
					thePlayer.capabilities.isFlying = false
					thePlayer.motionY = 0.0
					thePlayer.motionX = 0.0
					thePlayer.motionZ = 0.0
					if (jumpKeyDown) thePlayer.motionY += vanillaSpeed
					if (sneakKeyDown) thePlayer.motionY -= vanillaSpeed
					MovementUtils.strafe(vanillaSpeed)
				}

				"minesecure" ->
				{
					thePlayer.capabilities.isFlying = false
					if (!sneakKeyDown) thePlayer.motionY = -0.01

					thePlayer.motionX = 0.0
					thePlayer.motionZ = 0.0
					MovementUtils.strafe(vanillaSpeed)
					if (mineSecureVClipTimer.hasTimePassed(150) && jumpKeyDown)
					{
						networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(posX, posY + 5, posZ, false))
						networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(0.5, -1000.0, 0.5, false))
						val yaw = WMathHelper.toRadians(rotationYaw)
						val x = -functions.sin(yaw) * 0.4
						val z = functions.cos(yaw) * 0.4

						thePlayer.setPosition(posX + x, posY, posZ + z)
						mineSecureVClipTimer.reset()
					}
				}

				"hac" ->
				{
					thePlayer.motionX *= 0.8
					thePlayer.motionZ *= 0.8
					thePlayer.motionY = if (thePlayer.motionY <= -0.42) 0.42 else -0.42
				}

				"hawkeye" -> thePlayer.motionY = if (thePlayer.motionY <= -0.42) 0.42 else -0.42

				"teleportrewinside" ->
				{
					val vectorStart = WVec3(posX, posY, posZ)
					val yaw = -rotationYaw
					val pitch = -rotationPitch
					val length = 9.9
					val yawRadians = WMathHelper.toRadians(yaw)
					val pitchRadians = WMathHelper.toRadians(pitch)
					val vectorEnd = WVec3(
						functions.sin(yawRadians) * functions.cos(pitchRadians) * length + vectorStart.xCoord, functions.sin(pitchRadians) * length + vectorStart.yCoord, functions.cos(yawRadians) * functions.cos(pitchRadians) * length + vectorStart.zCoord
					)
					networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(vectorEnd.xCoord, posY + 2, vectorEnd.zCoord, true))
					networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(vectorStart.xCoord, posY + 2, vectorStart.zCoord, true))
					thePlayer.motionY = 0.0
				}

				"minesucht" ->
				{
					if (!gameSettings.keyBindForward.isKeyDown) return@run

					if (System.currentTimeMillis() - minesuchtTP > 99)
					{
						val vec3: WVec3 = thePlayer.getPositionEyes(0.0f)
						val vec31: WVec3 = thePlayer.getLook(0.0f)
						val vec32: WVec3 = vec3.addVector(vec31.xCoord * 7, vec31.yCoord * 7, vec31.zCoord * 7)
						if (thePlayer.fallDistance > 0.8)
						{
							thePlayer.sendQueue.networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(posX, posY + 50, posZ, false))
							thePlayer.fall(100.0f, 100.0f)
							thePlayer.fallDistance = 0.0f
							thePlayer.sendQueue.networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(posX, posY + 20, posZ, true))
						}
						thePlayer.sendQueue.networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(vec32.xCoord, posY + 50, vec32.zCoord, true))
						thePlayer.sendQueue.networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(posX, posY, posZ, false))
						thePlayer.sendQueue.networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(vec32.xCoord, posY, vec32.zCoord, true))
						thePlayer.sendQueue.networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(posX, posY, posZ, false))
						minesuchtTP = System.currentTimeMillis()
					} else
					{
						thePlayer.sendQueue.networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(posX, posY, posZ, false))
						thePlayer.sendQueue.networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(posX, posY, posZ, true))
					}
				}

				"jetpack" -> if (jumpKeyDown)
				{ //                    mc.effectRenderer.spawnEffectParticle(EnumParticleTypes.FLAME.getParticleID(), thePlayer.posX, thePlayer.posY + 0.2, thePlayer.posZ, -thePlayer.motionX, -0.5, -thePlayer.motionZ)
					thePlayer.motionY += 0.15
					thePlayer.motionX *= 1.1
					thePlayer.motionZ *= 1.1
				}
				"mineplex" -> if (thePlayer.inventory.getCurrentItemInHand() == null)
				{
					if (jumpKeyDown && mineplexTimer.hasTimePassed(100))
					{
						thePlayer.setPosition(posX, posY + 0.6, posZ)
						mineplexTimer.reset()
					}
					if (thePlayer.sneaking && mineplexTimer.hasTimePassed(100))
					{
						thePlayer.setPosition(posX, posY - 0.6, posZ)
						mineplexTimer.reset()
					}
					val blockPos = WBlockPos(posX, thePlayer.entityBoundingBox.minY - 1, posZ)
					val vec: WVec3 = WVec3(blockPos).addVector(0.4, 0.4, 0.4).add(WVec3(classProvider.getEnumFacing(EnumFacingType.UP).directionVec))
					mc.playerController.onPlayerRightClick(thePlayer, theWorld, thePlayer.inventory.getCurrentItemInHand()!!, blockPos, classProvider.getEnumFacing(EnumFacingType.UP), WVec3(vec.xCoord * 0.4f, vec.yCoord * 0.4f, vec.zCoord * 0.4f))
					MovementUtils.strafe(0.27f)
					mc.timer.timerSpeed = 1 + mineplexSpeedValue.get()
				} else
				{
					mc.timer.timerSpeed = 1.0f
					state = false
					ClientUtils.displayChatMessage("\u00A78[\u00A7c\u00A7lMineplex-\u00A7a\u00A7lFly\u00A78] \u00A7aSelect an empty slot to fly.")
				}

				"aac3.3.12" ->
				{
					if (posY < -70) thePlayer.motionY = aac3_3_12_motion.get().toDouble()

					mc.timer.timerSpeed = 1f

					if (sneakKeyDown) // Help you to MLG
					{
						mc.timer.timerSpeed = 0.2f
						mc.rightClickDelayTimer = 0
					}
				}

				"aac3.3.12-glide" ->
				{
					if (!onGround) aac3glideDelay++

					if (aac3glideDelay == 2) mc.timer.timerSpeed = 1f

					if (aac3glideDelay == 12) mc.timer.timerSpeed = 0.1f

					if (aac3glideDelay >= 12 && !onGround)
					{
						aac3glideDelay = 0
						thePlayer.motionY = .015
					}
				}

				"aac3.3.13" ->
				{
					if (thePlayer.isDead) wasDead = true
					if (wasDead || onGround)
					{
						wasDead = false
						thePlayer.motionY = aac3_3_13_motion.get().toDouble()
						thePlayer.onGround = false
					}

					mc.timer.timerSpeed = 1f

					if (sneakKeyDown) // Help you to MLG
					{
						mc.timer.timerSpeed = 0.2f
						mc.rightClickDelayTimer = 0
					}
				}

				"watchcat" ->
				{
					MovementUtils.strafe(0.15f)
					thePlayer.sprinting = true

					if (posY < startY + 2)
					{
						thePlayer.motionY = Math.random() * 0.5
						return@run
					}

					if (startY > posY) MovementUtils.strafe(0f)
				}

				"spartan" ->
				{
					thePlayer.motionY = 0.0

					spartanTimer.update()
					if (spartanTimer.hasTimePassed(12))
					{
						networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(posX, posY + 8, posZ, true))
						networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(posX, posY - 8, posZ, true))
						spartanTimer.reset()
					}
				}

				"spartanglide" ->
				{
					MovementUtils.strafe(0.264f)

					if (thePlayer.ticksExisted % 8 == 0) thePlayer.sendQueue.networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(posX, posY + 10, posZ, true))
				}

				"neruxvace" ->
				{
					if (!onGround) aac3glideDelay++

					if (aac3glideDelay >= neruxVaceTicks.get() && !onGround)
					{
						aac3glideDelay = 0
						thePlayer.motionY = .015
					}
				}

				"hypixel" ->
				{
					val boostDelay = hypixelTimerBoostDelay.get().toLong()
					val boostTimer = hypixelTimerBoostTimer.get()

					if (hypixelFlyStarted)
					{

						// TimerBoost
						if (hypixelTimerBoost.get())
						{
							if (hypixelFlyTimer.hasTimePassed(boostDelay)) mc.timer.timerSpeed = 1.0F
							else mc.timer.timerSpeed = 1.0F + boostTimer * (hypixelFlyTimer.hasTimeLeft(boostDelay).toFloat() / boostDelay.toFloat())
						}

						// ychinc
						if (hypixelYchIncValue.get() && !canPerformHypixelDamageFly)
						{
							hypixelTimer.update()
							if (hypixelTimer.hasTimePassed(2))
							{
								thePlayer.setPosition(posX, posY + 1.0E-5, posZ)
								hypixelTimer.reset()
							}
						}
					} else if (!canPerformHypixelDamageFly)
					{

						// Start without boost
						hypixelFlyStarted = true
						hypixelFlyTimer.reset()
					} else if (waitForDamage && thePlayer.hurtTime > 0)
					{

						// Start boost when the player takes damage
						if (hypixelJumpValue.get()) jump()

						hypixelBoostStep = 1
						hypixelBoostSpeed = 0.1
						lastDistance = 0.0
						hypixelDamageBoostFailed = false
						hypixelFlyStarted = true
						hypixelFlyTimer.reset()
						waitForDamage = false
						markStartY = posY
					}
				}

				"freehypixel" ->
				{
					if (freeHypixelTimer.hasTimePassed(10))
					{
						thePlayer.capabilities.isFlying = true
						return@run
					}

					// Watchdog Disabler Exploit
					RotationUtils.setTargetRotation(Rotation(freeHypixelYaw, freeHypixelPitch))

					thePlayer.motionY = 0.0
					thePlayer.motionZ = thePlayer.motionY
					thePlayer.motionX = thePlayer.motionZ

					if (startY == BigDecimal(posY).setScale(3, RoundingMode.HALF_DOWN).toDouble()) freeHypixelTimer.update()
				}

				"bugspartan" ->
				{
					thePlayer.capabilities.isFlying = false
					thePlayer.motionY = 0.0
					thePlayer.motionX = 0.0
					thePlayer.motionZ = 0.0

					if (jumpKeyDown) thePlayer.motionY += vanillaSpeed
					if (sneakKeyDown) thePlayer.motionY -= vanillaSpeed

					MovementUtils.strafe(vanillaSpeed)
				}

				"redesky" ->
				{
					mc.timer.timerSpeed = 0.3f
					redeskyPacketHClip(7.0)
					redeskyPacketVClip(10.0)
					redeskyVClip(-0.5f)
					redeskyHClip(2.0)
					redeskySpeed(1)
					thePlayer.motionY = -0.01
				}

				"anticheatplus" ->
				{
					thePlayer.motionY = 0.0

					if (jumpKeyDown)
					{
						thePlayer.motionX = 0.0
						thePlayer.motionY = 0.42
						thePlayer.motionZ = 0.0
					}

					if (sneakKeyDown)
					{
						thePlayer.motionX = 0.0
						thePlayer.motionY = -0.42
						thePlayer.motionZ = 0.0
					}
				}
			}
		}
	}

	@EventTarget
	fun onMotion(event: MotionEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		val posX = thePlayer.posX
		val posY = thePlayer.posY
		val posZ = thePlayer.posZ

		if (modeValue.get().equals("Hypixel", ignoreCase = true) && canPerformHypixelDamageFly && hypixelFlyStarted)
		{
			when (event.eventState)
			{
				EventState.PRE ->
				{
					if (hypixelYchIncValue.get())
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
					lastDistance = sqrt(xDist * xDist + zDist * zDist)
				}
			}
		} else if (modeValue.get().equals("AntiCheatPlus", ignoreCase = true))
		{
			acpTickTimer.update()
			if (acpTickTimer.hasTimePassed(4))
			{
				thePlayer.setPosition(posX, posY + 1.0E-5, posZ)
				acpTickTimer.reset()
			}
		}

		if (LiquidBounce.moduleManager[Bobbing::class.java].state && bobValue.get() && isMoving) thePlayer.cameraYaw = 0.1f
	}

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent?)
	{
		val mode = modeValue.get()

		if (!markValue.get() || mode.equals("Vanilla", ignoreCase = true) || mode.equals("SmoothVanilla", ignoreCase = true) || mode.equals("Hypixel", ignoreCase = true) && !hypixelFlyStarted) return
		val y = markStartY + 2.0

		RenderUtils.drawPlatform(y, if ((mc.thePlayer ?: return).entityBoundingBox.maxY < y) Color(0, 255, 0, 90) else Color(255, 0, 0, 90), 1.0)

		when (mode.toLowerCase())
		{
			"aac1.9.10" -> RenderUtils.drawPlatform(startY + aacJump, Color(0, 0, 255, 90), 1.0)
			"aac3.3.12" -> RenderUtils.drawPlatform(-70.0, Color(0, 0, 255, 90), 1.0)
		}
	}

	@EventTarget
	fun onPacket(event: PacketEvent)
	{
		if (noPacketModify) return

		val mode = modeValue.get()

		if (classProvider.isCPacketPlayer(event.packet))
		{
			val packetPlayer = event.packet.asCPacketPlayer()

			if (mode.equals("NCP", ignoreCase = true) || mode.equals("Rewinside", ignoreCase = true) || mode.equals("Mineplex", ignoreCase = true) && (mc.thePlayer ?: return).inventory.getCurrentItemInHand() == null) packetPlayer.onGround = true
			if (mode.equals("Hypixel", ignoreCase = true) && hypixelFlyStarted) packetPlayer.onGround = hypixelOnGroundValue.get()
		} else if (classProvider.isSPacketPlayerPosLook(event.packet))
		{
			if (mode.equals("Hypixel", ignoreCase = true) && canPerformHypixelDamageFly && hypixelFlyStarted)
			{
				hypixelDamageBoostFailed = true
				ClientUtils.displayChatMessage("\u00A78[\u00A7c\u00A7lHypixel-\u00A7a\u00A7lFly\u00A78] \u00A7cSetback detected.")
			}
		}
	}

	@EventTarget
	fun onMove(event: MoveEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		when (modeValue.get().toLowerCase())
		{
			"cubecraft" ->
			{
				val yaw = WMathHelper.toRadians(thePlayer.rotationYaw)
				if (cubecraftTeleportTickTimer.hasTimePassed(2))
				{
					event.x = -functions.sin(yaw) * 2.4
					event.z = functions.cos(yaw) * 2.4
					cubecraftTeleportTickTimer.reset()
				} else
				{
					event.x = -functions.sin(yaw) * 0.2
					event.z = functions.cos(yaw) * 0.2
				}
			}

			"hypixel" ->
			{
				if (!canPerformHypixelDamageFly || !hypixelFlyStarted) return

				if (!isMoving)
				{
					event.x = 0.0
					event.z = 0.0
					thePlayer.motionX = event.x
					thePlayer.motionZ = event.z
					return
				}

				if (hypixelDamageBoostFailed) return

				val amplifier = 1 + (if (thePlayer.isPotionActive(classProvider.getPotionEnum(PotionType.MOVE_SPEED))) 0.2 * (thePlayer.getActivePotionEffect(classProvider.getPotionEnum(PotionType.MOVE_SPEED))!!.amplifier + 1.0) else 0.0)
				val baseSpeed = 0.29 * amplifier

				when (hypixelBoostStep)
				{
					1 ->
					{
						hypixelBoostSpeed = (if (thePlayer.isPotionActive(classProvider.getPotionEnum(PotionType.MOVE_SPEED))) 1.56 else 2.034) * baseSpeed
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

				val dir = direction

				event.x = -functions.sin(dir) * hypixelBoostSpeed
				event.z = functions.cos(dir) * hypixelBoostSpeed

				thePlayer.motionX = event.x
				thePlayer.motionZ = event.z
			}

			"freehypixel" -> if (!freeHypixelTimer.hasTimePassed(10)) event.zero()
		}
	}

	@EventTarget
	fun onBB(event: BlockBBEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		val mode = modeValue.get()
		if (classProvider.isBlockAir(event.block) && (mode.equals("Hypixel", ignoreCase = true) && hypixelFlyStarted || mode.equals("Rewinside", ignoreCase = true) || mode.equals("MCCentral", ignoreCase = true) || mode.equals(
				"Mineplex", ignoreCase = true
			) && thePlayer.inventory.getCurrentItemInHand() == null) && event.y < thePlayer.posY
		) event.boundingBox = classProvider.createAxisAlignedBB(event.x.toDouble(), event.y.toDouble(), event.z.toDouble(), event.x + 1.0, thePlayer.posY, event.z + 1.0)
	}

	@EventTarget
	fun onJump(e: JumpEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		val mode = modeValue.get()
		if (mode.equals("Hypixel", ignoreCase = true) && hypixelFlyStarted || mode.equals("Rewinside", ignoreCase = true) || mode.equals("MCCentral", ignoreCase = true) || mode.equals(
				"Mineplex", ignoreCase = true
			) && thePlayer.inventory.getCurrentItemInHand() == null
		) e.cancelEvent()
	}

	@EventTarget
	fun onStep(e: StepEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		val mode = modeValue.get()
		if (mode.equals("Hypixel", ignoreCase = true) && hypixelFlyStarted || mode.equals("Rewinside", ignoreCase = true) || mode.equals("MCCentral", ignoreCase = true) || mode.equals(
				"Mineplex", ignoreCase = true
			) && thePlayer.inventory.getCurrentItemInHand() == null
		) e.stepHeight = 0f
	}

	private fun handleVanillaKickBypass()
	{
		val thePlayer = mc.thePlayer ?: return

		if (!vanillaKickBypassValue.get() || !groundTimer.hasTimePassed(1000)) return
		val ground = calculateGround()
		run {
			var posY = thePlayer.posY
			while (posY > ground)
			{
				mc.netHandler.networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(thePlayer.posX, posY, thePlayer.posZ, true))
				if (posY - 8.0 < ground) break // Prevent next step
				posY -= 8.0
			}
		}
		mc.netHandler.networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(thePlayer.posX, ground, thePlayer.posZ, true))
		var posY = ground
		while (posY < thePlayer.posY)
		{
			mc.netHandler.networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(thePlayer.posX, posY, thePlayer.posZ, true))
			if (posY + 8.0 > thePlayer.posY) break // Prevent next step
			posY += 8.0
		}
		mc.netHandler.networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(thePlayer.posX, thePlayer.posY, thePlayer.posZ, true))
		groundTimer.reset()
	}

	//<editor-fold desc="Redesky Fly">
	private fun redeskyHClip(horizontal: Double)
	{
		val thePlayer = mc.thePlayer ?: return

		val playerYaw = WMathHelper.toRadians(thePlayer.rotationYaw)
		thePlayer.setPosition(thePlayer.posX + horizontal * -functions.sin(playerYaw), thePlayer.posY, thePlayer.posZ + horizontal * functions.cos(playerYaw))
	}

	private fun redeskyPacketHClip(horizontal: Double)
	{
		val thePlayer = mc.thePlayer ?: return

		val playerYaw = WMathHelper.toRadians(thePlayer.rotationYaw)
		mc.netHandler.networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(thePlayer.posX + horizontal * -functions.sin(playerYaw), thePlayer.posY, thePlayer.posZ + horizontal * functions.cos(playerYaw), false))
	}

	private fun redeskyVClip(vertical: Float)
	{
		val thePlayer = mc.thePlayer ?: return

		thePlayer.setPosition(thePlayer.posX, thePlayer.posY + vertical, thePlayer.posZ)
	}

	private fun redeskyPacketVClip(vertical: Double)
	{
		val thePlayer = mc.thePlayer ?: return

		mc.netHandler.networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(thePlayer.posX, thePlayer.posY + vertical, thePlayer.posZ, false))
	}

	private fun redeskySpeed(speed: Int)
	{
		val thePlayer = mc.thePlayer ?: return

		val playerYaw = WMathHelper.toRadians(thePlayer.rotationYaw)
		thePlayer.motionX = (speed * -functions.sin(playerYaw)).toDouble()
		thePlayer.motionZ = (speed * functions.cos(playerYaw)).toDouble()
	}

	//</editor-fold>

	// TODO: Make better and faster calculation lol
	private fun calculateGround(): Double
	{
		val playerBoundingBox: IAxisAlignedBB = mc.thePlayer!!.entityBoundingBox
		var blockHeight = 1.0
		var ground = mc.thePlayer!!.posY
		while (ground > 0.0)
		{
			val customBox = classProvider.createAxisAlignedBB(playerBoundingBox.maxX, ground + blockHeight, playerBoundingBox.maxZ, playerBoundingBox.minX, ground, playerBoundingBox.minZ)
			if (mc.theWorld!!.checkBlockCollision(customBox))
			{
				if (blockHeight <= 0.05) return ground + blockHeight
				ground += blockHeight
				blockHeight = 0.05
			}
			ground -= blockHeight
		}
		return 0.0
	}

	private fun jump()
	{
		val thePlayer = mc.thePlayer ?: return

		val blockAbove = BlockUtils.getBlock(WBlockPos(thePlayer.posX, thePlayer.posY + 2, thePlayer.posZ))
		val normalJumpY = 0.42 + if (thePlayer.isPotionActive(classProvider.getPotionEnum(PotionType.JUMP))) (thePlayer.getActivePotionEffect(classProvider.getPotionEnum(PotionType.JUMP))!!.amplifier + 1) * 0.1f else 0f
		val jumpY = if (classProvider.isBlockAir(blockAbove)) normalJumpY else min(0.2 + (blockAbove?.getBlockBoundsMinY() ?: 0.0), normalJumpY)

		// Simulate Vanilla Player Jump
		thePlayer.setPosition(thePlayer.posX, thePlayer.posY + jumpY, thePlayer.posZ)

		// Jump Boost
		if (thePlayer.sprinting)
		{
			val f = direction
			thePlayer.motionX -= functions.sin(f) * 0.2f
			thePlayer.motionZ += functions.cos(f) * 0.2f
		}
		thePlayer.isAirBorne = true

		// ForgeHooks.onLivingJump(thePlayer)
		thePlayer.triggerAchievement(classProvider.getStatEnum(StatType.JUMP_STAT))

		// if (thePlayer.sprinting) mc.thePlayer.addExhaustion(0.8f) else mc.thePlayer.addExhaustion(0.2f)
	}

	override val tag: String
		get() = modeValue.get()

	val disableNoFall: Boolean
		get() = waitForDamage || modeValue.get().equals("AAC1.9.10", ignoreCase = true)
}
