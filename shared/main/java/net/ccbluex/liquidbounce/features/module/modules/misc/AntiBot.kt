/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.getFullName
import net.ccbluex.liquidbounce.utils.extensions.getPing
import net.ccbluex.liquidbounce.utils.render.ColorUtils.stripColor
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import java.awt.Color
import java.text.DecimalFormat
import kotlin.math.*

// TODO: Rename Option Names
@ModuleInfo(name = "AntiBot", description = "Prevents KillAura from attacking AntiCheat bots.", category = ModuleCategory.MISC)
object AntiBot : Module()
{
	/**
	 * Enable norifications
	 */
	private val notificationValue = BoolValue("DetectionNotification", false)

	/**
	 * Tab
	 */
	private val tabValue = BoolValue("Tab", true)
	private val tabModeValue = ListValue("TabMode", arrayOf("Equals", "Contains"), "Contains")
	private val tabStripColorsValue = BoolValue("TabStripColorsInDisplayname", true)
	private val tabNameModeValue = ListValue("TabNameMode", arrayOf("DisplayName", "GameProfileName"), "DisplayName")

	/**
	 *  Entity-ID
	 */
	private val entityIDValue = BoolValue("EntityID", true)
	private val entityIDLimitValue = IntegerValue("EntityIDLimit", 1000000000, 100000, 1000000000)

	/**
	 * Static Entity-ID
	 */
	private val staticEntityIDValue = IntegerValue("StaticEntityIDs", 0, 0, 3)
	private val staticEntityID1 = IntegerValue("StaticEntityID-1", 99999999, 0, Int.MAX_VALUE)
	private val staticEntityID2 = IntegerValue("StaticEntityID-2", 999999999, 0, Int.MAX_VALUE)
	private val staticEntityID3 = IntegerValue("StaticEntityID-3", -1337, 0, Int.MAX_VALUE)

	/**
	 * NoColor
	 */
	private val colorValue = BoolValue("Color", false)

	/**
	 * LivingTime (ticksExisted)
	 */
	private val livingTimeValue = BoolValue("LivingTime", false)
	private val livingTimeTicksValue = IntegerValue("LivingTimeTicks", 40, 1, 200)

	/**
	 * Ground
	 */
	private val groundValue = BoolValue("Ground", true)

	/**
	 * Air
	 */
	private val airValue = BoolValue("Air", false)

	/**
	 * Invalid-Ground
	 */
	private val invalidGroundValue = BoolValue("InvalidGround", true)

	/**
	 * Swing
	 */
	private val swingValue = BoolValue("Swing", false)

	/**
	 * Health
	 */
	private val healthValue = BoolValue("Health", false)

	/**
	 * Invalid-Pitch (a.k.a. Derp)
	 */
	private val invalidPitchValue = BoolValue("Derp", true)

	/**
	 * Was Invisible
	 */
	private val wasInvisibleValue = BoolValue("WasInvisible", false)

	/**
	 * NoArmor
	 */
	private val armorValue = BoolValue("Armor", false)

	/**
	 * Invalid Ping
	 */
	private val pingValue = BoolValue("Ping", false)

	/**
	 * Needs to got damaged
	 */
	private val needHitValue = BoolValue("NeedHit", false)

	/**
	 * Duplicate entity in the world
	 */
	private val duplicateInWorldValue = BoolValue("DuplicateInWorld", false)

	/**
	 * Duplicate player in tab
	 */
	private val duplicateInTabValue = BoolValue("DuplicateInTab", false)
	private val duplicateInTabNameModeValue = ListValue("DuplicateInTabNameMode", arrayOf("DisplayName", "GameProfileName"), "DisplayName")
	private val duplicateInTabStripColorsValue = BoolValue("DuplicateInTabStripColorsInDisplayname", true)

	/**
	 * Always In Radius
	 */
	private val alwaysInRadiusValue = BoolValue("AlwaysInRadius", false)
	private val alwaysRadiusValue = FloatValue("AlwaysInRadiusBlocks", 20F, 5F, 30F)

	/**
	 * Unusual Teleport Packet (In vanilla minecraft, SPacketEntityTeleport is only used on the entity movements further than 8 blocks)
	 */
	private val teleportPacketValue = BoolValue("TeleportPacket", false)
	private val teleportThresholdDistance = FloatValue("TeleportPacket-ThresholdDistance", 8.0f, 0.3125f, 16.0f)
	private val teleportPacketVLValue = BoolValue("TeleportPacket-VL", true)
	private val teleportPacketVLLimitValue = IntegerValue("TeleportPacket-VL-Threshold", 15, 1, 40)
	private val teleportPacketVLDecValue = BoolValue("TeleportPacket-VL-DecreaseIfNormal", true)

	/**
	 * Horizontal Speed
	 */
	private val hspeedValue = BoolValue("HSpeed", false)
	private val hspeedLimitValue = FloatValue("HSpeed-Limit", 4.0f, 1.0f, 255.0f)
	private val hspeedVLValue = BoolValue("HSpeed-VL", true)
	private val hspeedVLLimitValue = IntegerValue("HSpeed-VL-Threshold", 5, 1, 10)
	private val hspeedVLDecValue = BoolValue("HSpeed-VL-DecreaseIfNormal", true)

	/**
	 * Vertical Speed
	 */
	private val vspeedValue = BoolValue("VSpeed", false)
	private val vspeedLimitValue = FloatValue("VSpeed-Limit", 4.0f, 1.0f, 255.0f)
	private val vspeedVLValue = BoolValue("VSpeed-VL", true)
	private val vspeedVLLimitValue = IntegerValue("VSpeed-VL-Threshold", 2, 1, 10)
	private val vspeedVLDecValue = BoolValue("VSpeed-VL-DecreaseIfNormal", true)

	/**
	 * Spawn Position
	 * TODO: More spawnedPosition checks
	 */
	private val spawnPositionValue = BoolValue("SpawnPosition", true)
	private val spawnPositionBackValue = FloatValue("SpawnPosition-Back", 3.0f, 1.0f, 10.0f)
	private val spawnPositionYValue = FloatValue("SpawnPosition-Y", 3.0f, 0.0f, 10.0f)
	private val spawnPositionExpectLimitValue = FloatValue("SpawnPosition-DeltaLimit", 2F, 0.5F, 4F)

	/**
	 * Position Consistency
	 */
	private val positionValue = BoolValue("Position", true)

	private val positionBack1Value = FloatValue("Position-Back-1", 3.0f, 1.0f, 10.0f)
	private val positionY1Value = FloatValue("Position-Y-1", 3.0f, 0.0f, 10.0f)

	private val positionBack2Value = FloatValue("Position-Back-2", 6.0f, 1.0f, 10.0f)
	private val positionY2Value = FloatValue("Position-Y-2", 6.0f, 0.0f, 10.0f)

	/**
	 * Position Threshold
	 */
	private val positionDeltaLimitValue = FloatValue("Position-DeltaLimit", 1.0f, 0.1f, 3.0f)
	private val positionDeltaVLLimitValue = IntegerValue("Position-VL-Limit", 10, 2, 100)
	private val positionDeltaVLDecValue = BoolValue("Position-VL-DecreaseIfNormal", false)

	/**
	 * Position Consistency Delta Consistency (xd)
	 */
	private val positionDeltaConsistencyValue = BoolValue("Position-DeltaConsistency", false)
	private val positionRequiredExpectationDeltaToCheckConsistencyValue = FloatValue("Position-DeltaConsistency-RequiredDeltaToCheck", 1.0f, 0.1f, 3.0f)
	private val positionDeltaConsistencyLimitValue = FloatValue("Position-DeltaConsistency-ConsistencyLimit", 0.1f, 0.0f, 1f)
	private val positionDeltaConsistencyVLLimitValue = IntegerValue("Position-DeltaConsistency-VL-Limit", 10, 1, 100)
	private val positionDeltaConsistencyVLDecValue = BoolValue("Position-DeltaConsistency-VL-DecreaseIfNormal", false)

	/**
	 * CustomNameTag presence
	 */
	private val customNameValue = BoolValue("CustomName", false)
	private val emptyCustomNameValue = BoolValue("CustomName-Blank", false)
	private val customNameModeValue = ListValue("CustomName-Equality-Mode", arrayOf("Equals", "Contains"), "Contains")
	private val customNameStripColorsValue = BoolValue("CustomName-Equality-StripColorsInCustomName", true)
	private val customNameCompareToValue = ListValue("CustomName-Equality-CompareTo", arrayOf("DisplayName", "GameProfileName"), "DisplayName")

	/**
	 * "\u00A78[NPC] " prefix on name
	 */
	private val npcValue = BoolValue("NPC", true)

	private val bedWarsNPCValue = BoolValue("BedWarsNPC", false)
	private val gwenValue = BoolValue("GWEN", false)
	private val watchdogValue = BoolValue("Watchdog", false)

	/**
	 * Mark the expected positions of Position check
	 */
	private val drawExpectedPosValue = BoolValue("Position-Mark", false)
	private val drawExpectedPosAlphaValue = IntegerValue("Position-Mark-Alpha", 40, 5, 255)

	/**
	 * Player position ping-correction offset
	 */
	private val positionPingCorrectionOffsetValue = IntegerValue("Position-PingCorrection-Offset", 1, 0, 5)

	private val ground = mutableSetOf<Int>()
	private val air = mutableSetOf<Int>()
	private val swing = mutableSetOf<Int>()
	private val invisible = mutableSetOf<Int>()
	private val hitted = mutableSetOf<Int>()
	private val notAlwaysInRadius = mutableSetOf<Int>()
	private val spawnPosition = mutableSetOf<Int>()
	private val gwen = mutableSetOf<Int>()
	private val watchdog = mutableSetOf<Int>()

	private val spawnPositionSuspects = mutableSetOf<Int>()

	private val invalidGround = mutableMapOf<Int, Int>()
	private val hspeed = mutableMapOf<Int, Int>()
	private val vspeed = mutableMapOf<Int, Int>()
	private val positionVL = mutableMapOf<Int, Int>()
	private val positionConsistencyLastDistanceDelta = mutableMapOf<Int, Double>()
	private val positionConsistencyVL = mutableMapOf<Int, Int>()
	private val teleportpacket_violation = mutableMapOf<Int, Int>()

	private val locationList = mutableListOf<Location>()
	private const val yawListSize = 50

	private val DECIMAL_FORMAT = DecimalFormat("##0.000000")

	@EventTarget
	fun onMotion(event: MotionEvent)
	{
		if (event.eventState == EventState.POST)
		{
			val thePlayer = mc.thePlayer ?: return

			while (locationList.size >= yawListSize - 1) locationList.removeAt(0)

			locationList.add(Location(WVec3(thePlayer.posX, thePlayer.entityBoundingBox.minY, thePlayer.posZ), RotationUtils.serverRotation))
		}
	}

	private fun getPingCorrectionAppliedLocation(ping: Int, offset: Int = 0): Location
	{
		val correction = ceil(ping / 50F).toInt() + offset + positionPingCorrectionOffsetValue.get()

		val indexLimit = locationList.size - 1
		return locationList[(indexLimit - correction).coerceIn(0, indexLimit)]
	}

	@JvmStatic
	fun checkTabList(targetName: String, displayName: Boolean, equals: Boolean, stripColors: Boolean): Boolean = mc.netHandler.playerInfoMap.map { networkPlayerInfo ->
		var networkName = networkPlayerInfo.getFullName(displayName)

		if (stripColors) networkName = stripColor(networkName)

		networkName
	}.any { networkName -> if (equals) targetName == networkName else targetName.contains(networkName) }

	fun isBot(theWorld: IWorldClient, thePlayer: IEntityPlayerSP, entity: IEntityLivingBase): Boolean
	{
		// Check if anti bot is enabled
		if (!state) return false

		// Check if entity is a player
		val provider = classProvider

		if (!provider.isEntityPlayer(entity)) return false

		val displayName: String = entity.displayName.formattedText
		val profileName = entity.asEntityPlayer().gameProfile.name

		// Anti Bot checks

		// NoColor
		if (colorValue.get() && !displayName.replace("\u00A7r", "").contains("\u00A7")) return true

		// LivingTime
		if (livingTimeValue.get() && entity.ticksExisted < livingTimeTicksValue.get()) return true

		val entityId = entity.entityId

		// Ground
		if (groundValue.get() && !ground.contains(entityId)) return true

		// Air
		if (airValue.get() && !air.contains(entityId)) return true

		// Swing
		if (swingValue.get() && !swing.contains(entityId)) return true

		// Health
		if (healthValue.get() && entity.health > 20F) return true

		// EntityID
		if (entityIDValue.get() && (entityId >= entityIDLimitValue.get() || entityId <= -1)) return true

		// StaticEntityID
		if (staticEntityIDValue.get() > 0)
		{
			val ids = arrayOf(staticEntityID1.get(), staticEntityID2.get(), staticEntityID3.get())
			if ((0 until staticEntityIDValue.get()).map(ids::get).any { entityId == it }) return true
		}

		// Invalid pitch (Derp)
		if (invalidPitchValue.get() && (entity.rotationPitch > 90F || entity.rotationPitch < -90F)) return true

		// Was Invisible
		if (wasInvisibleValue.get() && invisible.contains(entityId)) return true

		// Armor
		if (armorValue.get() && entity.asEntityPlayer().inventory.armorInventory.all { it == null }) return true

		val netHandler = mc.netHandler

		// Ping
		if (pingValue.get() && netHandler.getPlayerInfo(entity.asEntityPlayer().uniqueID)?.responseTime == 0) return true

		// NeedHit
		if (needHitValue.get() && !hitted.contains(entityId)) return true

		// Invalid-Ground
		if (invalidGroundValue.get() && invalidGround[entityId] ?: 0 >= 10) return true

		// Tab
		if (tabValue.get())
		{
			val equals = tabModeValue.get().equals("Equals", ignoreCase = true)
			val displayNameMode: Boolean = tabNameModeValue.get().equals("DisplayName", ignoreCase = true)
			var targetName = if (displayNameMode) displayName else profileName

			if (targetName != null)
			{
				if (tabStripColorsValue.get()) targetName = stripColor(targetName)

				if (!checkTabList(targetName, displayNameMode, equals, tabStripColorsValue.get())) return true
			}
		}

		// Duplicate in the world
		if (duplicateInWorldValue.get() && theWorld.loadedEntityList.count { provider.isEntityPlayer(it) && it.asEntityPlayer().displayNameString == it.asEntityPlayer().displayNameString } > 1) return true

		// Duplicate in the tab
		if (duplicateInTabValue.get() && netHandler.playerInfoMap.count {
				var entityName = entity.name

				if (duplicateInTabStripColorsValue.get()) entityName = stripColor(entityName)

				var itName = it.getFullName(duplicateInTabNameModeValue.get().equals("DisplayName", true))

				if (duplicateInTabStripColorsValue.get()) itName = stripColor(itName)

				entityName == itName
			} > 1) return true

		// Always in radius
		if (alwaysInRadiusValue.get() && !notAlwaysInRadius.contains(entityId)) return true

		// XZ Speed
		if (hspeedValue.get() && hspeed.containsKey(entityId) && (!hspeedVLValue.get() || hspeed[entityId] ?: 0 >= hspeedVLLimitValue.get())) return true

		// Y Speed
		if (vspeedValue.get() && vspeed.containsKey(entityId) && (!vspeedVLValue.get() || vspeed[entityId] ?: 0 >= vspeedVLLimitValue.get())) return true

		// Teleport Packet
		if (teleportPacketValue.get() && teleportpacket_violation.containsKey(entityId) && (!teleportPacketVLValue.get() || teleportpacket_violation[entityId] ?: 0 >= teleportPacketVLLimitValue.get())) return true

		// Spawned Position
		if (spawnPositionValue.get() && spawnPosition.contains(entityId)) return true

		if (positionValue.get() && positionVL[entityId] ?: 0 >= positionDeltaVLLimitValue.get() || positionDeltaConsistencyValue.get() && positionConsistencyVL[entityId] ?: 0 >= positionDeltaConsistencyVLLimitValue.get()) return true

		val customName = entity.customNameTag
		if (customNameValue.get())
		{
			if (customName.isBlank()) return emptyCustomNameValue.get()

			val compareTo = if (customNameCompareToValue.get().equals("DisplayName", ignoreCase = true)) displayName else profileName
			if (compareTo != null && !(if (customNameModeValue.get().equals("Equals", ignoreCase = true)) compareTo == customName else compareTo.contains(customName))) return true
		}

		if (npcValue.get() && displayName.contains("\u00A78[NPC]")) return true

		if (bedWarsNPCValue.get() && (displayName.isEmpty() || displayName[0] != '\u00A7') && displayName.endsWith("\u00A7r")) return true

		if (gwenValue.get() && gwen.contains(entityId)) return true

		if (watchdogValue.get() && watchdog.contains(entityId)) return true

		return entity.name.isEmpty() || entity.name == thePlayer.name
	}

	override fun onDisable()
	{
		clearAll()
		super.onDisable()
	}

	@EventTarget
	fun onPacket(event: PacketEvent)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return
		val playerPing = thePlayer.getPing()

		val notify = notificationValue.get()

		val packet = event.packet

		val provider = classProvider

		if (provider.isSPacketEntity(packet))
		{
			val movePacket = packet.asSPacketEntity()
			val entity = movePacket.getEntity(theWorld)

			if (entity != null && provider.isEntityPlayer(entity))
			{
				val entityId = entity.entityId

				val displayName: String = entity.displayName.formattedText
				val customName: String = entity.asEntityPlayer().customNameTag

				val packetGround = movePacket.onGround

				// Ground
				if (packetGround && !ground.contains(entityId)) ground.add(entityId)

				// Air
				if (!packetGround && !air.contains(entityId)) air.add(entityId)

				// Invalid-Ground
				val invalidGroundPrevVL = invalidGround[entityId] ?: 0
				if (packetGround)
				{
					if (notify && invalidGroundValue.get() && (invalidGroundPrevVL + 5) % 10 == 0) notification("InvalidGround", "Suspicious onGround flag: $displayName")

					if (entity.prevPosY != entity.posY) invalidGround[entityId] = invalidGroundPrevVL + 2
				}
				else
				{
					val currentVL = invalidGroundPrevVL - 1
					if (currentVL <= 0) invalidGround.remove(entityId)
					else invalidGround[entityId] = currentVL
				}

				// Was Invisible
				if (entity.invisible && !invisible.contains(entityId)) invisible.add(entityId)

				// Always in radius
				if (!notAlwaysInRadius.contains(entityId) && thePlayer.getDistanceToEntity(entity) > alwaysRadiusValue.get()) notAlwaysInRadius.add(entityId)

				// Horizontal Speed
				val hspeed = hypot(abs(entity.prevPosX - entity.posX), abs(entity.prevPosZ - entity.posZ))
				if (hspeed > hspeedLimitValue.get())
				{
					if (notify && hspeedValue.get()) notification("HSpeed", "Moved too fast (horizontally): $displayName (${DECIMAL_FORMAT.format(hspeed)} blocks/tick)")
					this.hspeed[entityId] = this.hspeed[entityId] ?: 0 + 2
				}
				else if (hspeedVLDecValue.get())
				{
					val currentVL = (this.hspeed[entityId] ?: 0) - 1
					if (currentVL <= 0) this.hspeed.remove(entityId) else this.hspeed[entityId] = currentVL
				}

				// Vertical Speed
				val vspeed = abs(entity.prevPosY - entity.posY)
				if (vspeed > vspeedLimitValue.get())
				{
					if (notify && vspeedValue.get()) notification("VSpeed", "Moved too fast (vertically): $displayName (${DECIMAL_FORMAT.format(vspeed)} blocks/tick)")
					this.vspeed[entityId] = (this.vspeed[entityId] ?: 0) + 2
				}
				else if (vspeedVLDecValue.get())
				{
					val currentVL = (this.vspeed[entityId] ?: 0) - 1
					if (currentVL <= 0) this.vspeed.remove(entityId) else this.vspeed[entityId] = currentVL
				}

				val isSuspectedForSpawnPosition = spawnPositionSuspects.contains(entityId)

				val serverLocation = getPingCorrectionAppliedLocation(playerPing)
				val lastServerYaw = getPingCorrectionAppliedLocation(playerPing, 1).rotation.yaw
				val lastServerYaw2 = getPingCorrectionAppliedLocation(playerPing, 2).rotation.yaw

				val serverPos = serverLocation.pos
				val serverYaw = serverLocation.rotation.yaw

				val yawRadians = WMathHelper.toRadians(serverYaw - 180.0F)

				val func = functions

				val back1 = positionBack1Value.get()
				val back2 = positionBack2Value.get()

				val expectedX = serverPos.xCoord - func.sin(yawRadians) * back1
				val expectedY = serverPos.yCoord + positionY1Value.get()
				val expectedZ = serverPos.zCoord + func.cos(yawRadians) * back1

				val expectedX2 = serverPos.xCoord - func.sin(yawRadians) * back2
				val expectedY2 = serverPos.yCoord + positionY2Value.get()
				val expectedZ2 = serverPos.zCoord + func.cos(yawRadians) * back2

				val positionDeltaLimit = positionDeltaLimitValue.get()
				val positionDeltaVLDec = positionDeltaVLDecValue.get()

				val positionRequiredDeltaToCheckConsistency = positionRequiredExpectationDeltaToCheckConsistencyValue.get()
				val positionDeltaConsistencyLimit = positionDeltaConsistencyLimitValue.get()
				val positionDeltaConsistencyVLDec = positionDeltaConsistencyVLDecValue.get()

				val distances = doubleArrayOf(entity.getDistance(expectedX, expectedY, expectedZ), entity.getDistance(expectedX2, expectedY2, expectedZ2))

				for (distance in distances)
				{
					val prevVL = positionVL[entityId] ?: 0

					// Position Delta
					if (distance <= positionDeltaLimit)
					{
						val vlIncrement = ceil(max(abs(lastServerYaw - serverYaw), abs(lastServerYaw2 - serverYaw)) / 15F).toInt() + when
						{
							distance <= positionDeltaLimit * 0.1F -> 10
							distance <= positionDeltaLimit * 0.25F -> 8
							distance <= positionDeltaLimit * 0.5F -> 5
							distance <= positionDeltaLimit * 0.75F -> 2
							else -> 1
						} + if (isSuspectedForSpawnPosition) 5 else 0

						if (notify && positionValue.get() && ((prevVL + 5) % 20 == 0) || (vlIncrement >= 5)) notification("Position(Expect)", "Suspicious position: $displayName (+$vlIncrement) (dist: ${DECIMAL_FORMAT.format(distance)}, VL: ${prevVL + vlIncrement})")
						positionVL[entityId] = prevVL + vlIncrement
					}
					else if (positionDeltaVLDec)
					{
						val currentVL = prevVL - 1
						if (currentVL <= 0) positionVL.remove(entityId) else positionVL[entityId] = currentVL
					}

					val prevConsistencyVL = positionConsistencyVL[entityId] ?: 0

					// Position Delta Consistency
					if (distance <= positionRequiredDeltaToCheckConsistency)
					{
						val lastdistance = positionConsistencyLastDistanceDelta[entityId] ?: Double.MAX_VALUE
						val consistency = abs(lastdistance - distance)

						if (consistency <= positionDeltaConsistencyLimit)
						{
							val vlIncrement = when
							{
								consistency <= positionDeltaConsistencyLimit * 0.1F -> 10
								consistency <= positionDeltaConsistencyLimit * 0.25F -> 8
								consistency <= positionDeltaConsistencyLimit * 0.5F -> 5
								consistency <= positionDeltaConsistencyLimit * 0.75F -> 2
								else -> 1
							} + if (isSuspectedForSpawnPosition) 10 else 0

							if (notify && positionDeltaConsistencyValue.get() && ((prevVL + 5) % 10 == 0 || vlIncrement >= 5)) notification("Position(Expect-Consistency)", "Suspicious position consistency: $displayName (delta: ${DECIMAL_FORMAT.format(consistency)}, posVL: $prevVL, posConsistencyVL: $prevConsistencyVL)")
							positionConsistencyVL[entityId] = prevConsistencyVL + vlIncrement
						}
						else if (positionDeltaConsistencyVLDec)
						{
							val currentVL = prevConsistencyVL - 1
							if (currentVL <= 0) positionConsistencyVL.remove(entityId) else positionConsistencyVL[entityId] = currentVL
						}

						positionConsistencyLastDistanceDelta[entityId] = distance
					}
					else
					{
						val currentVL = prevConsistencyVL - 1

						if (currentVL <= 0) positionConsistencyVL.remove(entityId) else positionConsistencyVL[entityId] = currentVL
					}
				}

				// ticksExisted > 40 && custom name tag is empty = Mineplex GWEN bot
				if (thePlayer.ticksExisted > 40 && entity.asEntityPlayer().customNameTag.isBlank() && !gwen.contains(entityId)) gwen.add(entityId)

				// invisible + display name isn't red but ends with color reset char (\u00A7r) + displayname equals customname + entity is near than 3 block horizontally + y delta between entity and player is 10~13 = Watchdog Bot
				if (entity.invisible && !displayName.startsWith("\u00A7c") && displayName.endsWith("\u00A7r") && displayName.equals(customName, ignoreCase = true))
				{
					val hDist = hypot(abs(entity.posX - thePlayer.posX), abs(entity.posZ - thePlayer.posZ))
					val vDist = abs(entity.posY - thePlayer.posY)

					if (vDist > 10 && vDist < 13 && hDist < 3 && !checkTabList(entity.asEntityPlayer().gameProfile.name, displayName = false, equals = true, stripColors = true))
					{
						if (notify && watchdogValue.get()) notification("Watchdog(Distance)", "Detected watchdog bot: $displayName (hDist: ${DECIMAL_FORMAT.format(hDist)}, vDist: ${DECIMAL_FORMAT.format(vDist)})")
						watchdog.add(entityId)
					}
				}

				// invisible + custom name is red and contains color reset char (\u00A7r) = watchdog bot
				if (entity.invisible && customName.toLowerCase().contains("\u00A7c") && customName.toLowerCase().contains("\u00A7r"))
				{
					if (notify && watchdogValue.get()) notification("Watchdog(Invisible)", "Detected watchdog bot: $displayName")
					watchdog.add(entityId)
				}
			}
		}

		if (provider.isSPacketEntityTeleport(packet))
		{
			val entityTeleportPacket = packet.asSPacketEntityTeleport()
			val entity: IEntity? = theWorld.getEntityByID(entityTeleportPacket.entityId)

			if (entity != null && provider.isEntityPlayer(entity))
			{
				val newX: Double = entityTeleportPacket.x
				val newY: Double = entityTeleportPacket.y
				val newZ: Double = entityTeleportPacket.z

				val distSq = entity.asEntityPlayer().getDistanceSq(newX, newY, newZ)
				val distThreshold = teleportThresholdDistance.get()

				val prevVL = teleportpacket_violation[entity.entityId] ?: 0

				if (distSq <= distThreshold * distThreshold)
				{
					if (notify && teleportPacketValue.get() && (prevVL + 5) % 10 == 0) notification("Teleport Packet", "Suspicious SPacketEntityTeleport: ${entity.displayName.formattedText} (dist: ${sqrt(distSq)}, VL: $prevVL)")
					teleportpacket_violation[entity.entityId] = prevVL + 2
				}
				else if (teleportPacketVLDecValue.get())
				{
					val currentVL = prevVL - 1
					if (currentVL <= 0) teleportpacket_violation.remove(entity.entityId) else teleportpacket_violation[entity.entityId] = currentVL
				}
			}
		}

		if (provider.isSPacketAnimation(packet))
		{
			val swingPacket = packet.asSPacketAnimation()
			val entityId = swingPacket.entityID

			val entity = theWorld.getEntityByID(entityId)

			if (entity != null && provider.isEntityLivingBase(entity) && swingPacket.animationType == 0 && !swing.contains(entityId)) swing.add(entityId)
		}

		if (provider.isSPacketSpawnPlayer(packet))
		{
			val playerSpawnPacket = packet.asSPacketSpawnPlayer()

			val entityId = playerSpawnPacket.entityID

			val entityX: Double = playerSpawnPacket.x.toDouble() / 32.0
			val entityY: Double = playerSpawnPacket.y.toDouble() / 32.0
			val entityZ: Double = playerSpawnPacket.z.toDouble() / 32.0

			val serverLocation = getPingCorrectionAppliedLocation(playerPing)

			val serverPos = serverLocation.pos
			val serverYaw = serverLocation.rotation.yaw

			if (hypot(serverPos.xCoord - entityX, serverPos.zCoord - entityZ) <= 10 && entityY >= serverPos.yCoord) spawnPositionSuspects.add(entityId)

			val yawRadians = WMathHelper.toRadians(serverYaw - 180.0F)

			val func = functions

			val back = spawnPositionBackValue.get()

			val expectedX = serverPos.xCoord - func.sin(yawRadians) * back
			val expectedY = serverPos.yCoord + spawnPositionYValue.get()
			val expectedZ = serverPos.zCoord + func.cos(yawRadians) * back

			val positionDeltaLimit = spawnPositionExpectLimitValue.get()

			val expectDeltaX = expectedX - entityX
			val expectDeltaY = expectedY - entityY
			val expectDeltaZ = expectedZ - entityZ

			val expectDelta = sqrt(expectDeltaX * expectDeltaX + expectDeltaY * expectDeltaY + expectDeltaZ * expectDeltaZ)

			ClientUtils.displayChatMessage(thePlayer, "$expectDelta")

			// Position Delta
			if (expectDelta <= positionDeltaLimit)
			{
				if (notify && spawnPositionValue.get()) notification("Spawn(Expect)", "Suspicious spawn: Entity #$entityId (dist: ${DECIMAL_FORMAT.format(expectDelta)})")
				spawnPosition.add(entityId)
			}
		}
	}

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") e: Render3DEvent)
	{
		if (!drawExpectedPosValue.get()) return

		val thePlayer = mc.thePlayer ?: return
		val ping = thePlayer.getPing()

		val partialTicks = e.partialTicks

		val serverLocation = getPingCorrectionAppliedLocation(ping)
		val lastServerLocation = getPingCorrectionAppliedLocation(ping, 1)

		val lastServerYaw = lastServerLocation.rotation.yaw

		val serverPos = serverLocation.pos
		val lastServerPos = lastServerLocation.pos

		val yaw = lastServerYaw + (serverLocation.rotation.yaw - lastServerYaw) * partialTicks

		val dir = WMathHelper.toRadians(yaw - 180.0F)

		val func = functions

		val sin = -func.sin(dir)
		val cos = func.cos(dir)

		val posX = lastServerPos.xCoord + (serverPos.xCoord - lastServerPos.xCoord) * partialTicks
		val posY = lastServerPos.yCoord + (serverPos.yCoord - lastServerPos.yCoord) * partialTicks
		val posZ = lastServerPos.zCoord + (serverPos.zCoord - lastServerPos.zCoord) * partialTicks

		val provider = classProvider

		val renderManager = mc.renderManager
		val renderPosX = renderManager.renderPosX
		val renderPosY = renderManager.renderPosY
		val renderPosZ = renderManager.renderPosZ

		val alpha = drawExpectedPosAlphaValue.get()

		if (positionValue.get())
		{
			val back1 = positionBack1Value.get()
			val y1 = positionY1Value.get()

			val y2 = positionY2Value.get()
			val back2 = positionBack2Value.get()

			val expectedX = posX + sin * back1
			val expectedY = posY + y1
			val expectedZ = posZ + cos * back1

			val expectedX2 = posX + sin * back2
			val expectedY2 = posY + y2
			val expectedZ2 = posZ + cos * back2

			val deltaLimit = positionDeltaLimitValue.get()
			val width = thePlayer.width + deltaLimit
			val height = thePlayer.height + deltaLimit

			RenderUtils.drawAxisAlignedBB(provider.createAxisAlignedBB(expectedX - width - renderPosX, expectedY - renderPosY, expectedZ - width - renderPosZ, expectedX + width - renderPosX, expectedY + height - renderPosY, expectedZ + width - renderPosZ), Color(255, 0, 0, alpha))
			RenderUtils.drawAxisAlignedBB(provider.createAxisAlignedBB(expectedX2 - width - renderPosX, expectedY2 - deltaLimit - renderPosY, expectedZ2 - width - renderPosZ, expectedX2 + width - renderPosX, expectedY2 + height - renderPosY, expectedZ2 + width - renderPosZ), Color(0, 255, 0, alpha))
		}

		if (spawnPositionValue.get())
		{
			val ySpawn = spawnPositionYValue.get()
			val backSpawn = spawnPositionBackValue.get()

			val expectedXSpawn = posX + sin * backSpawn
			val expectedYSpawn = posY + ySpawn
			val expectedZSpawn = posZ + cos * backSpawn

			val deltaLimitSpawn = spawnPositionExpectLimitValue.get()

			val widthSpawn = thePlayer.width + deltaLimitSpawn
			val heightSpawn = thePlayer.height + deltaLimitSpawn

			RenderUtils.drawAxisAlignedBB(provider.createAxisAlignedBB(expectedXSpawn - widthSpawn - renderPosX, expectedYSpawn - deltaLimitSpawn - renderPosY, expectedZSpawn - widthSpawn - renderPosZ, expectedXSpawn + widthSpawn - renderPosX, expectedYSpawn + heightSpawn - renderPosY, expectedZSpawn + widthSpawn - renderPosZ), Color(0, 0, 255, alpha))
		}
	}

	@EventTarget
	fun onAttack(e: AttackEvent)
	{
		val entity = e.targetEntity

		if (entity != null && classProvider.isEntityLivingBase(entity) && !hitted.contains(entity.entityId)) hitted.add(entity.entityId)
	}

	@EventTarget
	fun onWorld(@Suppress("UNUSED_PARAMETER") event: WorldEvent?)
	{
		clearAll()
	}

	private fun clearAll()
	{
		ground.clear()
		air.clear()
		swing.clear()
		invisible.clear()
		hitted.clear()
		notAlwaysInRadius.clear()
		spawnPosition.clear()
		gwen.clear()
		watchdog.clear()

		invalidGround.clear()
		hspeed.clear()
		vspeed.clear()
		positionVL.clear()
		positionConsistencyLastDistanceDelta.clear()
		positionConsistencyVL.clear()
		teleportpacket_violation.clear()
	}

	private fun notification(checkName: String, message: String)
	{
		LiquidBounce.hud.addNotification("AntiBot: $checkName check", message, 10000L, Color.red)
	}
}

private data class Location(val pos: WVec3, val rotation: Rotation)
