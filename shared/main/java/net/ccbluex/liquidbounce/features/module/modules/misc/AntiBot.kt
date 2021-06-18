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
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketPlayerListItem
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.Location
import net.ccbluex.liquidbounce.utils.LocationCache
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.getFullName
import net.ccbluex.liquidbounce.utils.extensions.getPing
import net.ccbluex.liquidbounce.utils.misc.StringUtils.DECIMALFORMAT_6
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.stripColor
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import java.awt.Color
import java.util.*
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

	private val invalidProfileNameValue = BoolValue("InvalidProfileName", false)

	/**
	 * Color (The player who have colored-name is a bot)
	 */
	private val colorValue: BoolValue = object : BoolValue("Color", false)
	{
		override fun onChange(oldValue: Boolean, newValue: Boolean)
		{
			if (noColorValue.get()) noColorValue.set(false)
		}
	}

	/**
	 * NoColor (The player who havn't colored-name is a bot)
	 */
	private val noColorValue: BoolValue = object : BoolValue("NoColor", false)
	{
		override fun onChange(oldValue: Boolean, newValue: Boolean)
		{
			if (colorValue.get()) colorValue.set(false)
		}
	}

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
	 * Yaw Movements
	 */
	private val yawValue = BoolValue("YawMovements", false)

	/**
	 * Pitch Movements
	 */
	private val pitchValue = BoolValue("PitchMovements", false)

	/**
	 * Invalid-Pitch (a.k.a. Derp)
	 */
	private val invalidPitchValue = BoolValue("Derp", true)
	private val invalidPitchKeepVLValue = BoolValue("Derp-KeepVL", true)

	/**
	 * Was Invisible
	 */
	private val wasInvisibleValue = BoolValue("WasInvisible", false)

	/**
	 * NoArmor
	 */
	private val armorValue = BoolValue("Armor", false)

	/**
	 * Fixed Ping
	 */
	private val pingValue = BoolValue("Ping", false)

	/**
	 * Ping update presence
	 */
	private val pingUpdateValue = BoolValue("PingUpdate", false)
	private val pingUpdateValidationValue = BoolValue("PingUpdate-Validation", false)
	private val pingUpdateValidationModeValue = ListValue("PingUpdate-Validation-Mode", arrayOf("AnyMatches", "AllMatches"), "AnyMatches")

	/**
	 * Needs to got damaged
	 */
	private val needHitValue = BoolValue("NeedHit", false)

	/**
	 * Duplicate entity in the world
	 */
	private val duplicateInWorldValue = BoolValue("DuplicateName-World", false)
	private val duplicateInWorldNameModeValue = ListValue("DuplicateName-World-NameMode", arrayOf("DisplayName", "CustomNameTag", "GameProfileName"), "DisplayName")
	private val duplicateInWorldStripColorsValue = BoolValue("DuplicateName-World-StripColorsInName", true)

	private val attemptToAddDuplicatesWorldValue = BoolValue("AttemptToAddDuplicate-World", false)
	private val attemptToAddDuplicatesWorldModeValue = ListValue("AttemptToAddDuplicate-World-NameMode", arrayOf("DisplayName", "GameProfileName"), "DisplayName")
	private val attemptToAddDuplicatesWorldStripColorsValue = BoolValue("AttemptToAddDuplicate-World-StripColorsInName", false)

	/**
	 * Duplicate player in tab
	 */
	private val duplicateInTabValue = BoolValue("DuplicateName-Tab", false)
	private val duplicateInTabWorldNameModeValue = ListValue("DuplicateName-Tab-WorldNameMode", arrayOf("DisplayName", "CustomNameTag", "GameProfileName"), "DisplayName")
	private val duplicateInTabNameModeValue = ListValue("DuplicateName-Tab-NameMode", arrayOf("DisplayName", "GameProfileName"), "DisplayName")
	private val duplicateInTabStripColorsValue = BoolValue("DuplicateName-Tab-StripColorsInDisplayName", true)

	private val attemptToAddDuplicatesTabValue = BoolValue("AttemptToAddDuplicate-Tab", false)
	private val attemptToAddDuplicatesTabModeValue = ListValue("AttemptToAddDuplicate-Tab-NameMode", arrayOf("DisplayName", "GameProfileName"), "GameProfileName")
	private val attemptToAddDuplicatesTabStripColorsValue = BoolValue("AttemptToAddDuplicate-Tab-StripColorsInDisplayName", false)

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
	 */
	private val spawnPositionValue = BoolValue("SpawnPosition", true)

	private val spawnPositionBack1Value = FloatValue("SpawnPosition-Back-1", 3.0f, 1.0f, 16.0f)
	private val spawnPositionY1Value = FloatValue("SpawnPosition-Y-1", 3.0f, 0.0f, 16.0f)

	private val spawnPositionBack2Value = FloatValue("SpawnPosition-Back-2", 6.0f, 1.0f, 16.0f)
	private val spawnPositionY2Value = FloatValue("SpawnPosition-Y-2", 6.0f, 0.0f, 16.0f)

	private val spawnPositionExpectLimitValue = FloatValue("SpawnPosition-DeltaLimit", 2F, 0.5F, 4F)

	/**
	 * Position Consistency
	 */
	private val positionValue = BoolValue("Position", true)
	private val positionRemoveValue = BoolValue("Position-RemoveDetected", true)
	private val positionRemoveVLValue = IntegerValue("Position-RemoveDetected-VL", 25, 10, 200)

	private val positionBack1Value = FloatValue("Position-Back-1", 3.0f, 1.0f, 16.0f)
	private val positionY1Value = FloatValue("Position-Y-1", 0.0f, 0.0f, 16.0f)

	private val positionBack2Value = FloatValue("Position-Back-2", 3.0f, 1.0f, 16.0f)
	private val positionY2Value = FloatValue("Position-Y-2", 3.0f, 0.0f, 16.0f)

	private val positionBack3Value = FloatValue("Position-Back-3", 6.0f, 1.0f, 16.0f)
	private val positionY3Value = FloatValue("Position-Y-3", 0.0f, 0.0f, 16.0f)

	private val positionBack4Value = FloatValue("Position-Back-4", 6.0f, 1.0f, 16.0f)
	private val positionY4Value = FloatValue("Position-Y-4", 6.0f, 0.0f, 16.0f)

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
	 * TODO: Check if the player is inside one or more solid blocks (정상적인 플레이어라면 (Phase를 사용하지 않는 한)블럭을 뚫고 움직일 수 없지만 Anti-cheat NPC(봇)은 그 자리에 블럭이 있건 없건 씹고 그냥 자유롭게 움직이는 경우가 매우 많다)
	 */
	// TODO: private val collisionValue = BoolValue("Collision", true)

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
	private val positionPingCorrectionOffsetValue = IntegerValue("Position-PingCorrection-Offset", 1, -2, 5)

	private val invalidProfileNameRegex = Regex("[^a-zA-Z0-9_]*")

	private val ground = mutableSetOf<Int>()
	private val air = mutableSetOf<Int>()
	private val swing = mutableSetOf<Int>()
	private val invisible = mutableSetOf<Int>()
	private val hitted = mutableSetOf<Int>()
	private val notAlwaysInRadius = mutableSetOf<Int>()
	private val spawnPosition = mutableSetOf<Int>()
	private val gwen = mutableSetOf<Int>()
	private val watchdog = mutableSetOf<Int>()
	private val derp = mutableSetOf<Int>()
	private val yawMovement = mutableSetOf<Int>()
	private val pitchMovement = mutableSetOf<Int>()
	private val pingNotUpdated = mutableSetOf<UUID>()

	private val spawnPositionSuspects = mutableSetOf<Int>()

	private val invalidGround = mutableMapOf<Int, Int>()
	private val hspeed = mutableMapOf<Int, Int>()
	private val vspeed = mutableMapOf<Int, Int>()
	private val positionVL = mutableMapOf<Int, Int>()
	private val positionConsistencyLastDistanceDelta = mutableMapOf<Int, MutableMap<Int, Double>>()
	private val positionConsistencyVL = mutableMapOf<Int, Int>()
	private val teleportpacket_violation = mutableMapOf<Int, Int>()
	// TODO: private val collision_violation = mutableMapOf<Int, Int>()

	private fun getPingCorrectionAppliedLocation(thePlayer: IEntityPlayerSP, offset: Int = 0) = LocationCache.getPlayerLocationBeforeNTicks((ceil(thePlayer.getPing() / 50F).toInt() + offset + positionPingCorrectionOffsetValue.get()).coerceAtLeast(0), Location(WVec3(thePlayer.posX, thePlayer.entityBoundingBox.minY, thePlayer.posZ), RotationUtils.serverRotation))

	@JvmStatic
	fun checkTabList(targetName: String, displayName: Boolean, equals: Boolean, stripColors: Boolean): Boolean = mc.netHandler.playerInfoMap.map { networkPlayerInfo ->
		var networkName = networkPlayerInfo.getFullName(displayName)

		if (stripColors) networkName = stripColor(networkName)

		networkName
	}.any { networkName -> if (equals) targetName == networkName else networkName in targetName }

	fun isBot(theWorld: IWorldClient, thePlayer: IEntityPlayerSP, target: IEntityLivingBase): Boolean
	{
		// Check if anti bot is enabled
		if (!state) return false

		// Check if entity is a player
		val provider = classProvider

		if (!provider.isEntityPlayer(target)) return false
		val entity = target.asEntityPlayer()

		val displayName: String = entity.displayName.formattedText
		val profileName = entity.gameProfile.name

		val customNameTag = entity.customNameTag
		val customNameTagFailsafe = customNameTag.ifBlank { profileName }

		// Invalid Profile Name
		if (invalidProfileNameValue.get() && invalidProfileNameRegex.containsMatchIn(profileName)) return true

		// Anti Bot checks

		val hasColor = "\u00A7" in displayName.replace("\u00A7r", "")

		// Color
		if (colorValue.get() && hasColor || noColorValue.get() && !hasColor) return true

		// LivingTime
		if (livingTimeValue.get() && entity.ticksExisted < livingTimeTicksValue.get()) return true

		val entityId = entity.entityId

		val netHandler = mc.netHandler
		val uuid = entity.uniqueID

		// Ground
		if (groundValue.get() && entityId !in ground) return true

		// Air
		if (airValue.get() && entityId !in air) return true

		// Swing
		if (swingValue.get() && entityId !in swing) return true

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

		// Yaw & Pitch movements
		if (yawValue.get() && entityId !in yawMovement) return true
		if (pitchValue.get() && entityId !in pitchMovement) return true

		// Invalid pitch (Derp)
		if (invalidPitchValue.get() && if (invalidPitchKeepVLValue.get()) entityId in derp else (entity.rotationPitch > 90F || entity.rotationPitch < -90F)) return true

		// Was Invisible
		if (wasInvisibleValue.get() && entityId in invisible) return true

		// Armor
		if (armorValue.get() && entity.inventory.armorInventory.all { it == null }) return true

		// Ping
		if (pingValue.get() && netHandler.getPlayerInfo(uuid)?.responseTime == 0) return true

		if (pingUpdateValue.get() && uuid in pingNotUpdated) return true

		// NeedHit
		if (needHitValue.get() && entityId !in hitted) return true

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

		var dupInWorldTargetName = when (duplicateInWorldNameModeValue.get().toLowerCase())
		{
			"displayname" -> displayName
			"customnametag" -> customNameTagFailsafe
			else -> profileName
		}

		if (duplicateInWorldStripColorsValue.get()) dupInWorldTargetName = stripColor(dupInWorldTargetName)

		// Duplicate in the world
		if (duplicateInWorldValue.get() && theWorld.loadedEntityList.filter(provider::isEntityPlayer).map(IEntity::asEntityPlayer).count {
				dupInWorldTargetName == when (duplicateInWorldNameModeValue.get().toLowerCase())
				{
					"displayname" -> it.displayName.formattedText
					"customnametag" -> it.customNameTag.ifBlank { it.gameProfile.name }
					else -> it.gameProfile.name
				}
			} > 1) return true

		// Duplicate in the tab
		if (duplicateInTabValue.get() && netHandler.playerInfoMap.count {
				var entityName = when (duplicateInTabWorldNameModeValue.get().toLowerCase())
				{
					"displayname" -> displayName
					"customnametag" -> customNameTagFailsafe
					else -> profileName
				}

				if (duplicateInTabStripColorsValue.get()) entityName = stripColor(entityName)

				var itName = it.getFullName(duplicateInTabNameModeValue.get().equals("DisplayName", ignoreCase = true))

				if (duplicateInTabStripColorsValue.get()) itName = stripColor(itName)

				entityName == itName
			} > 1) return true

		// Always in radius
		if (alwaysInRadiusValue.get() && entityId !in notAlwaysInRadius) return true

		// XZ Speed
		if (hspeedValue.get() && entityId in hspeed && (!hspeedVLValue.get() || hspeed[entityId] ?: 0 >= hspeedVLLimitValue.get())) return true

		// Y Speed
		if (vspeedValue.get() && entityId in vspeed && (!vspeedVLValue.get() || vspeed[entityId] ?: 0 >= vspeedVLLimitValue.get())) return true

		// Teleport Packet
		if (teleportPacketValue.get() && entityId in teleportpacket_violation && (!teleportPacketVLValue.get() || teleportpacket_violation[entityId] ?: 0 >= teleportPacketVLLimitValue.get())) return true

		// Spawned Position
		if (spawnPositionValue.get() && entityId in spawnPosition) return true

		if (positionValue.get() && positionVL[entityId] ?: 0 >= positionDeltaVLLimitValue.get() || positionDeltaConsistencyValue.get() && positionConsistencyVL[entityId] ?: 0 >= positionDeltaConsistencyVLLimitValue.get()) return true

		if (customNameValue.get())
		{
			val customName = customNameTag.let { if (customNameStripColorsValue.get()) stripColor(it) else it }

			if (customName.isBlank()) return emptyCustomNameValue.get()

			val compareTo = if (customNameCompareToValue.get().equals("DisplayName", ignoreCase = true)) displayName else profileName
			if (compareTo != null && !(if (customNameModeValue.get().equals("Equals", ignoreCase = true)) compareTo == customName else customName in compareTo)) return true
		}

		if (npcValue.get() && "\u00A78[NPC]" in displayName) return true

		if (bedWarsNPCValue.get() && (displayName.isEmpty() || displayName[0] != '\u00A7') && displayName.endsWith("\u00A7r")) return true

		if (gwenValue.get() && entityId in gwen) return true

		if (watchdogValue.get() && entityId in watchdog) return true

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

		val shouldNotify = notificationValue.get()

		val packet = event.packet

		val provider = classProvider

		// Movement checks
		if (provider.isSPacketEntity(packet))
		{
			val movePacket = packet.asSPacketEntity()

			val target = movePacket.getEntity(theWorld)
			if (target != null && provider.isEntityPlayer(target))
			{
				val targetPlayer = target.asEntityPlayer()

				val newPos = WVec3((targetPlayer.serverPosX + movePacket.posX) / 32.0, (targetPlayer.serverPosY + movePacket.posY) / 32.0, (targetPlayer.serverPosZ + movePacket.posZ) / 32.0)
				val onGround = movePacket.onGround

				checkEntityMovements(theWorld, thePlayer, targetPlayer, newPos, movePacket.rotating, movePacket.yaw, movePacket.pitch, onGround, shouldNotify)

				val entityId = targetPlayer.entityId

				val displayName: String = targetPlayer.displayName.formattedText
				val customName: String = targetPlayer.customNameTag

				// Invalid-Ground
				val invalidGroundPrevVL = invalidGround[entityId] ?: 0
				if (onGround)
				{
					if (shouldNotify && invalidGroundValue.get() && (invalidGroundPrevVL + 5) % 10 == 0) notification("InvalidGround", "Suspicious onGround flag: $displayName")

					if (target.prevPosY != target.posY) invalidGround[entityId] = invalidGroundPrevVL + 2
				}
				else
				{
					val currentVL = invalidGroundPrevVL - 1
					if (currentVL <= 0) invalidGround.remove(entityId)
					else invalidGround[entityId] = currentVL
				}

				// ticksExisted > 40 && custom name tag is empty = Mineplex GWEN bot
				if (thePlayer.ticksExisted > 40 && targetPlayer.customNameTag.isBlank() && entityId !in gwen) gwen.add(entityId)

				// invisible + display name isn't red but ends with color reset char (\u00A7r) + displayname equals customname + entity is near than 3 block horizontally + y delta between entity and player is 10~13 = Watchdog Bot
				if (targetPlayer.invisible && !displayName.startsWith("\u00A7c") && displayName.endsWith("\u00A7r") && displayName.equals(customName, ignoreCase = true))
				{
					val hDist = hypot(abs(newPos.xCoord - thePlayer.posX), abs(newPos.zCoord - thePlayer.posZ))
					val vDist = abs(newPos.yCoord - thePlayer.posY)

					if (vDist > 10 && vDist < 13 && hDist < 3 && !checkTabList(targetPlayer.gameProfile.name, displayName = false, equals = true, stripColors = true))
					{
						if (shouldNotify && watchdogValue.get()) notification("Watchdog(Distance)", "Detected watchdog bot: $displayName (hDist: ${DECIMALFORMAT_6.format(hDist)}, vDist: ${DECIMALFORMAT_6.format(vDist)})")
						watchdog.add(entityId)
					}
				}

				// invisible + custom name is red and contains color reset char (\u00A7r) = watchdog bot
				if (targetPlayer.invisible && "\u00A7c" in customName.toLowerCase() && "\u00A7r" in customName.toLowerCase())
				{
					if (shouldNotify && watchdogValue.get()) notification("Watchdog(Invisible)", "Detected watchdog bot: $displayName")
					watchdog.add(entityId)
				}
			}
		}

		// Teleport packet check & Movement checks
		if (provider.isSPacketEntityTeleport(packet))
		{
			val teleportPacket = packet.asSPacketEntityTeleport()
			val entity: IEntity? = theWorld.getEntityByID(teleportPacket.entityId)

			if (entity != null && provider.isEntityPlayer(entity))
			{
				val entityId = entity.entityId

				val newPos = WVec3(teleportPacket.x, teleportPacket.y, teleportPacket.z)

				checkEntityMovements(theWorld, thePlayer, entity.asEntityPlayer(), newPos, true, teleportPacket.yaw, teleportPacket.pitch, teleportPacket.onGround, shouldNotify)

				val prevVL = teleportpacket_violation[entityId] ?: 0

				val distSq = entity.asEntityPlayer().getDistanceSq(newPos.xCoord, newPos.yCoord, newPos.zCoord)
				val distThreshold = teleportThresholdDistance.get()

				if (distSq <= distThreshold * distThreshold)
				{
					if (shouldNotify && teleportPacketValue.get() && (prevVL + 5) % 10 == 0) notification("Teleport Packet", "Suspicious SPacketEntityTeleport: ${entity.displayName.formattedText} (dist: ${sqrt(distSq)}, VL: $prevVL)")
					teleportpacket_violation[entityId] = prevVL + 2
				}
				else if (teleportPacketVLDecValue.get())
				{
					val currentVL = prevVL - 1
					if (currentVL <= 0) teleportpacket_violation.remove(entityId) else teleportpacket_violation[entityId] = currentVL
				}
			}
		}

		// Swing check
		if (provider.isSPacketAnimation(packet))
		{
			val swingPacket = packet.asSPacketAnimation()
			val entityId = swingPacket.entityID

			val entity = theWorld.getEntityByID(entityId)

			if (entity != null && provider.isEntityLivingBase(entity) && swingPacket.animationType == 0 && entityId !in swing) swing.add(entityId)
		}

		// Spawn check
		if (provider.isSPacketSpawnPlayer(packet))
		{
			val playerSpawnPacket = packet.asSPacketSpawnPlayer()

			val entityId = playerSpawnPacket.entityID
			val uuid = playerSpawnPacket.uuid

			val playerInfo = mc.netHandler.playerInfoMap.firstOrNull { it.gameProfile.id == uuid }

			val entityX: Double = playerSpawnPacket.x.toDouble() / 32.0
			val entityY: Double = playerSpawnPacket.y.toDouble() / 32.0
			val entityZ: Double = playerSpawnPacket.z.toDouble() / 32.0

			val serverLocation = getPingCorrectionAppliedLocation(thePlayer)

			val serverPos = serverLocation.position
			val serverYaw = serverLocation.rotation.yaw

			if (hypot(serverPos.xCoord - entityX, serverPos.zCoord - entityZ) <= 10 && entityY >= serverPos.yCoord) spawnPositionSuspects.add(entityId)

			val yawRadians = WMathHelper.toRadians(serverYaw - 180.0F)

			val func = functions

			val positionDeltaLimit = spawnPositionExpectLimitValue.get()

			for ((posIndex, back, y) in arrayOf(Triple(1, spawnPositionBack1Value.get(), spawnPositionY1Value.get()), Triple(2, spawnPositionBack2Value.get(), spawnPositionY2Value.get())))
			{
				val expectDeltaX = serverPos.xCoord - func.sin(yawRadians) * back - entityX
				val expectDeltaY = serverPos.yCoord + y - entityY
				val expectDeltaZ = serverPos.zCoord + func.cos(yawRadians) * back - entityZ

				val expectSqrt = expectDeltaX * expectDeltaX + expectDeltaY * expectDeltaY + expectDeltaZ * expectDeltaZ

				// Position Delta
				if (expectSqrt <= positionDeltaLimit * positionDeltaLimit)
				{
					if (shouldNotify && spawnPositionValue.get()) notification("Spawn(Expect)", "Suspicious spawn: Entity #$entityId (posIndex: $posIndex, dist: ${DECIMALFORMAT_6.format(expectSqrt)})")
					spawnPosition.add(entityId)
				}
			}

			// Duplicate In World
			if (attemptToAddDuplicatesWorldValue.get() && playerInfo != null)
			{
				val stripColors = attemptToAddDuplicatesWorldStripColorsValue.get()
				val useDisplayName = attemptToAddDuplicatesWorldModeValue.get().equals("DisplayName", ignoreCase = true)

				val profileName = playerInfo.gameProfile.name
				val displayName = playerInfo.displayName?.formattedText

				var playerName = (if (useDisplayName) displayName else profileName) ?: ""

				if (stripColors) playerName = stripColor(playerName)

				if (theWorld.loadedEntityList.filter(classProvider::isEntityPlayer).map(IEntity::asEntityPlayer).any {
						var itName = (if (useDisplayName) it.displayName.formattedText else it.gameProfile.name) ?: return@any false

						if (stripColors) itName = stripColor(itName)

						playerName == itName
					})
				{
					event.cancelEvent()
					remove(theWorld, entityId, profileName, displayName, "AttemptToAddDuplicate(World)")
				}
			}
		}

		if (provider.isSPacketPlayerListItem(packet))
		{
			val playerListItem = packet.asSPacketPlayerListItem()
			val players = playerListItem.players

			val playerInfoMap = mc.netHandler.playerInfoMap

			when (playerListItem.action)
			{
				ISPacketPlayerListItem.WAction.ADD_PLAYER ->
				{
					val useDisplayName = attemptToAddDuplicatesTabModeValue.get().equals("DisplayName", ignoreCase = true)

					val tryStripColors = { name: String -> if (attemptToAddDuplicatesTabStripColorsValue.get()) stripColor(name) else name }

					val currentPlayerList = playerInfoMap.map { tryStripColors((if (useDisplayName) it.displayName?.formattedText else it.gameProfile.name) ?: "") }

					val itr = players.listIterator()
					while (itr.hasNext())
					{
						val player = itr.next()

						if (attemptToAddDuplicatesTabValue.get())
						{
							if (tryStripColors((if (useDisplayName) player.displayName?.formattedText else player.profile.name) ?: continue) in currentPlayerList)
							{
								itr.remove()

								if (notificationValue.get()) LiquidBounce.hud.addNotification("AntiBot", "Removed ${player.profile.name}(${player.displayName?.formattedText}\u00A7r) from the tab list -[AttemptToAddDuplicate(Tab)]", 10000L, Color.red)
							}
						}

						pingNotUpdated.add(player.profile.id)
					}

					if (players.isEmpty()) event.cancelEvent()
				}

				ISPacketPlayerListItem.WAction.UPDATE_LATENCY ->
				{
					val pingUpdatedPlayerUUIDList = players.map { it.profile.id }
					val allPlayerUUIDList = playerInfoMap.map { it.gameProfile.id }

					val toString = { uuidList: Collection<UUID> -> uuidList.joinToString(prefix = "[", postfix = "]") { uuid -> playerInfoMap.firstOrNull { it.gameProfile.id == uuid }?.gameProfile?.name ?: "$uuid" } }

					if (pingUpdateValidationValue.get())
					{
						val allMatches = pingUpdateValidationModeValue.get().equals("AllMatches", ignoreCase = true)
						val prevPingUpdatedPlayerUUIDList = allPlayerUUIDList.filterNot(pingNotUpdated::contains)
						if (if (allMatches) !pingUpdatedPlayerUUIDList.all(prevPingUpdatedPlayerUUIDList::contains) else pingUpdatedPlayerUUIDList.none(prevPingUpdatedPlayerUUIDList::contains))
						{
							notification("PingUpdate-Validate", "Received suspicious ping update packet: missing players = ${if (allMatches) toString(pingUpdatedPlayerUUIDList.filterNot(prevPingUpdatedPlayerUUIDList::contains)) else "none matches"}")
							return
						}
					}

					if (pingNotUpdated.isEmpty()) pingNotUpdated.addAll(allPlayerUUIDList.filterNot(pingUpdatedPlayerUUIDList::contains))
					else pingNotUpdated.removeAll(allPlayerUUIDList.filter(pingUpdatedPlayerUUIDList::contains).filter(pingNotUpdated::contains))

					if (pingUpdateValue.get() && notificationValue.get() && pingNotUpdated.isNotEmpty()) notification("PingUpdate", "Ping not updated: ${toString(pingNotUpdated)}")
				}

				ISPacketPlayerListItem.WAction.REMOVE_PLAYER -> pingNotUpdated.removeAll(players.map { it.profile.id })

				else ->
				{
				}
			}
		}
	}

	private fun checkEntityMovements(theWorld: IWorldClient, thePlayer: IEntityPlayerSP, target: IEntityPlayer, newPos: WVec3, rotating: Boolean, encodedYaw: Byte, encodedPitch: Byte, onGround: Boolean, shouldNotify: Boolean)
	{
		val entityId = target.entityId

		val displayName: String = target.displayName.formattedText
		val profileName = target.gameProfile.name

		// Decode encoded rotations
		val newYaw = if (rotating) encodedYaw * 360.0F / 256.0F else target.rotationYaw
		val newPitch = if (rotating) encodedPitch * 360.0F / 256.0F else target.rotationPitch

		// Ground
		if (onGround && entityId !in ground) ground.add(entityId)

		// Air
		if (!onGround && entityId !in air) air.add(entityId)

		// Yaw & Pitch movements
		// TODO: Movement threshold
		if (rotating)
		{
			if (newYaw != target.rotationYaw % 360.0F && entityId !in yawMovement) yawMovement.add(entityId)
			if (newPitch != target.rotationPitch % 360.0F && entityId !in pitchMovement) pitchMovement.add(entityId)
		}

		// Invalid Pitch
		if ((newPitch > 90.0F || newPitch < -90.0F) && entityId !in derp) derp.add(entityId)

		// Was Invisible
		if (target.invisible && entityId !in invisible) invisible.add(entityId)

		// Always in radius
		if (entityId !in notAlwaysInRadius && thePlayer.getDistanceToEntity(target) > alwaysRadiusValue.get()) notAlwaysInRadius.add(entityId)

		// Horizontal Speed
		val hspeed = hypot(target.posX - newPos.xCoord, target.posZ - newPos.zCoord)
		if (hspeed > hspeedLimitValue.get())
		{
			if (shouldNotify && hspeedValue.get()) notification("HSpeed", "Moved too fast (horizontally): $displayName (${DECIMALFORMAT_6.format(hspeed)} blocks/tick)")
			this.hspeed[entityId] = this.hspeed[entityId] ?: 0 + 2
		}
		else if (hspeedVLDecValue.get())
		{
			val currentVL = (this.hspeed[entityId] ?: 0) - 1
			if (currentVL <= 0) this.hspeed.remove(entityId) else this.hspeed[entityId] = currentVL
		}

		// Vertical Speed
		val vspeed = abs(target.posY - newPos.yCoord)
		if (vspeed > vspeedLimitValue.get())
		{
			if (shouldNotify && vspeedValue.get()) notification("VSpeed", "Moved too fast (vertically): $displayName (${DECIMALFORMAT_6.format(vspeed)} blocks/tick)")
			this.vspeed[entityId] = (this.vspeed[entityId] ?: 0) + 2
		}
		else if (vspeedVLDecValue.get())
		{
			val currentVL = (this.vspeed[entityId] ?: 0) - 1
			if (currentVL <= 0) this.vspeed.remove(entityId) else this.vspeed[entityId] = currentVL
		}

		// <editor-fold desc="Position Checks">

		if (positionValue.get())
		{
			val isSuspectedForSpawnPosition = spawnPositionValue.get() && entityId in spawnPositionSuspects

			val serverLocation = getPingCorrectionAppliedLocation(thePlayer)

			val serverPos = serverLocation.position
			val serverYaw = serverLocation.rotation.yaw

			var yawMovementScore = ceil(max(abs(getPingCorrectionAppliedLocation(thePlayer, 1).rotation.yaw - serverYaw), abs(getPingCorrectionAppliedLocation(thePlayer, 2).rotation.yaw - serverYaw)) / 5F).toInt()
			if (yawMovementScore <= 5) yawMovementScore = 0

			val yawRadians = WMathHelper.toRadians(serverYaw - 180.0F)

			val func = functions

			// Position delta limit
			val positionDeltaLimitSq = positionDeltaLimitValue.get().pow(2)
			val positionDeltaVLDec = positionDeltaVLDecValue.get()

			// Position delta consistency
			val positionRequiredDeltaToCheckConsistency = positionRequiredExpectationDeltaToCheckConsistencyValue.get()
			val positionDeltaConsistencyLimit = positionDeltaConsistencyLimitValue.get()
			val positionDeltaConsistencyVLDec = positionDeltaConsistencyVLDecValue.get()

			// Remove on caught
			val removeOnCaught = positionRemoveValue.get()
			val removeOnVL = positionRemoveVLValue.get()

			for ((posIndex, back, y) in arrayOf(Triple(1, positionBack1Value.get(), positionY1Value.get()), Triple(2, positionBack2Value.get(), positionY2Value.get()), Triple(3, positionBack3Value.get(), positionY3Value.get()), Triple(4, positionBack4Value.get(), positionY4Value.get())))
			{
				val deltaX = newPos.xCoord - (serverPos.xCoord - func.sin(yawRadians) * back)
				val deltaY = newPos.yCoord - (serverPos.yCoord + y)
				val deltaZ = newPos.zCoord - (serverPos.zCoord + func.cos(yawRadians) * back)

				val distanceSq = deltaX * deltaX + deltaY * deltaY * deltaZ * deltaZ

				val prevVL = positionVL[entityId] ?: 0

				// Position Delta
				if (distanceSq <= positionDeltaLimitSq)
				{
					val baseScore = when
					{
						distanceSq <= positionDeltaLimitSq * 0.0005F -> 40
						distanceSq <= positionDeltaLimitSq * 0.02F -> 20
						distanceSq <= positionDeltaLimitSq * 0.05F -> 7
						distanceSq <= positionDeltaLimitSq * 0.1F -> 4
						distanceSq <= positionDeltaLimitSq * 0.2F -> 2
						else -> 1
					}
					val spawnPosScore = if (isSuspectedForSpawnPosition) 5 else 0
					val speedScore = if (hspeed >= 2) ceil(hspeed * 2).toInt() else 0
					val vlIncrement = baseScore + yawMovementScore + spawnPosScore + speedScore

					val newVL = prevVL + vlIncrement

					if (removeOnCaught && newVL > removeOnVL)
					{
						// Remove bot from the game
						remove(theWorld, entityId, profileName, displayName, "Position(Expect)")
						positionVL.remove(entityId)
					}
					else
					{
						if (shouldNotify && ((prevVL + 5) % 20 == 0) || (vlIncrement >= 5)) notification("Position(Expect)", "Suspicious position: $displayName (+$baseScore +(yaw)$yawMovementScore +(spawnPos)$spawnPosScore +(speed)$speedScore) (posIndex: $posIndex, dist: ${DECIMALFORMAT_6.format(distanceSq)}, VL: $newVL)")

						positionVL[entityId] = newVL
					}
				}
				else if (positionDeltaVLDec)
				{
					val currentVL = prevVL - 1
					if (currentVL <= 0) positionVL.remove(entityId) else positionVL[entityId] = currentVL
				}

				val prevConsistencyVL = positionConsistencyVL[entityId] ?: 0

				// Position Delta Consistency
				if (distanceSq <= positionRequiredDeltaToCheckConsistency)
				{
					val lastDistance = positionConsistencyLastDistanceDelta[entityId]

					if (lastDistance == null) positionConsistencyLastDistanceDelta[entityId] = LinkedHashMap(4)
					else
					{
						if (posIndex in lastDistance)
						{
							val consistency = abs(lastDistance[posIndex]!! - distanceSq)

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

								if (shouldNotify && positionDeltaConsistencyValue.get() && ((prevVL + 5) % 10 == 0 || vlIncrement >= 5)) notification("Position(Expect-Consistency)", "Suspicious position consistency: $displayName (posIndex: $posIndex,delta: ${DECIMALFORMAT_6.format(consistency)}, posVL: $prevVL, posConsistencyVL: $prevConsistencyVL)")
								positionConsistencyVL[entityId] = prevConsistencyVL + vlIncrement
							}
							else if (positionDeltaConsistencyVLDec)
							{
								val currentVL = prevConsistencyVL - 1
								if (currentVL <= 0) positionConsistencyVL.remove(entityId) else positionConsistencyVL[entityId] = currentVL
							}
						}
					}

					positionConsistencyLastDistanceDelta[entityId]!![posIndex] = distanceSq
				}
				else
				{
					val currentVL = prevConsistencyVL - 1
					if (currentVL <= 0) positionConsistencyVL.remove(entityId) else positionConsistencyVL[entityId] = currentVL
				}
			}
		}
		// </editor-fold>
	}

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") e: Render3DEvent)
	{
		if (!drawExpectedPosValue.get()) return

		val thePlayer = mc.thePlayer ?: return

		val partialTicks = e.partialTicks

		val serverLocation = getPingCorrectionAppliedLocation(thePlayer)
		val lastServerLocation = getPingCorrectionAppliedLocation(thePlayer, 1)

		val lastServerYaw = lastServerLocation.rotation.yaw

		val serverPos = serverLocation.position
		val lastServerPos = lastServerLocation.position

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
			val deltaLimit = positionDeltaLimitValue.get()

			val width = thePlayer.width + deltaLimit
			val height = thePlayer.height + deltaLimit

			val bb = provider.createAxisAlignedBB(-width - renderPosX, -renderPosY, -width - renderPosZ, width - renderPosX, height - renderPosY, width - renderPosZ)

			for ((back, y, color) in arrayOf(Triple(positionBack1Value.get(), positionY1Value.get(), 0xFF0000), Triple(positionBack2Value.get(), positionY2Value.get(), 0xFF8800), Triple(positionBack3Value.get(), positionY3Value.get(), 0x88FF00), Triple(positionBack4Value.get(), positionY4Value.get(), 0x00FF00))) RenderUtils.drawAxisAlignedBB(bb.offset(posX + sin * back, posY + y, posZ + cos * back), ColorUtils.applyAlphaChannel(color, alpha))
		}

		if (spawnPositionValue.get())
		{
			val deltaLimit = spawnPositionExpectLimitValue.get()

			val width = thePlayer.width + deltaLimit
			val height = thePlayer.height + deltaLimit

			val bb = provider.createAxisAlignedBB(-width - renderPosX, -renderPosY, -width - renderPosZ, width - renderPosX, height - renderPosY, width - renderPosZ)

			for ((back, y, color) in arrayOf(Triple(spawnPositionBack1Value.get(), spawnPositionY1Value.get(), 0x0088FF), Triple(spawnPositionBack2Value.get(), spawnPositionY2Value.get(), 0x0000FF))) RenderUtils.drawAxisAlignedBB(bb.offset(posX + sin * back, posY + y, posZ + cos * back), ColorUtils.applyAlphaChannel(color, alpha))
		}
	}

	@EventTarget
	fun onAttack(e: AttackEvent)
	{
		val entity = e.targetEntity

		if (entity != null && classProvider.isEntityLivingBase(entity) && entity.entityId !in hitted) hitted.add(entity.entityId)
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
		pingNotUpdated.clear()

		invalidGround.clear()
		hspeed.clear()
		vspeed.clear()
		positionVL.clear()
		positionConsistencyLastDistanceDelta.clear()
		positionConsistencyVL.clear()
		teleportpacket_violation.clear()

		spawnPositionSuspects.clear()
	}

	private fun notification(checkName: String, message: String)
	{
		LiquidBounce.hud.addNotification("AntiBot - [$checkName]", message, 10000L, Color.red)
	}

	private fun remove(theWorld: IWorldClient, entityId: Int?, profileName: String, displayName: String?, reason: String)
	{
		entityId?.let(theWorld::removeEntityFromWorld)
		if (notificationValue.get()) LiquidBounce.hud.addNotification("AntiBot", "Removed $profileName($displayName\u00A7r) from the game -[$reason]", 10000L, Color.red)
	}
}
