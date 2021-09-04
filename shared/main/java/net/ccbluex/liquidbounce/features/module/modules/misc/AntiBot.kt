/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
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
import net.ccbluex.liquidbounce.value.*
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
	private val tabGroup = ValueGroup("Tab")
	private val tabEnabledValue = BoolValue("Enabled", true, "Tab")
	private val tabModeValue = ListValue("Mode", arrayOf("Equals", "Contains"), "Contains", "TabMode")
	private val tabNameModeValue = ListValue("NameMode", arrayOf("DisplayName", "GameProfileName"), "DisplayName", "TabNameMode")
	private val tabStripColorsValue = BoolValue("StripColorsInName", true, "TabStripColorsInDisplayname")

	/**
	 *  Entity-ID
	 */
	private val entityIDGroup = ValueGroup("EntityID")
	private val entityIDEnabledValue = BoolValue("Enabled", true, "EntityID")
	private val entityIDLimitValue = IntegerValue("Limit", 1000000000, 100000, 1000000000, "EntityIDLimit")

	/**
	 * Static Entity-ID
	 */
	private val entityIDStaticEntityIDGroup = ValueGroup("Static")
	private val entityIDStaticEntityIDEntityIDCountValue = IntegerValue("Count", 0, 0, 3, "StaticEntityIDs")
	private val entityIDStaticEntityIDEntityID1Value = object : IntegerValue("ID1", 99999999, Int.MIN_VALUE, Int.MAX_VALUE, "StaticEntityID-1")
	{
		override fun showCondition() = entityIDStaticEntityIDEntityIDCountValue.get() >= 1
	}
	private val entityIDStaticEntityIDEntityID2Value = object : IntegerValue("ID2", 999999999, Int.MIN_VALUE, Int.MAX_VALUE, "StaticEntityID-2")
	{
		override fun showCondition() = entityIDStaticEntityIDEntityIDCountValue.get() >= 2
	}
	private val entityIDStaticEntityIDEntityID3Value = object : IntegerValue("ID3", -1337, Int.MIN_VALUE, Int.MAX_VALUE, "StaticEntityID-3")
	{
		override fun showCondition() = entityIDStaticEntityIDEntityIDCountValue.get() >= 3
	}

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
	private val livingTimeGroup = ValueGroup("LivingTime")
	private val livingTimeEnabledValue = BoolValue("Enabled", false, "LivingTime")
	private val livingTimeTicksValue = IntegerValue("Ticks", 40, 1, 200, "LivingTimeTicks")

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

	private val rotationGroup = ValueGroup("Rotation")
	private val rotationYawValue = BoolValue("Yaw", false, "YawMovements")
	private val rotationPitchValue = BoolValue("Pitch", false, "PitchMovements")

	/**
	 * Invalid-Pitch (a.k.a. Derp)
	 */
	private val rotationInvalidPitchGroup = ValueGroup("Derp")
	private val rotationInvalidPitchEnabledValue = BoolValue("Enabled", true, "Derp")
	private val rotationInvalidPitchKeepVLValue = BoolValue("KeepVL", true, "Derp-KeepVL")

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
	private val pingGroup = ValueGroup("Ping")
	private val pingZeroValue = BoolValue("NotZero", false, "Ping")

	/**
	 * Ping update presence
	 */
	private val pingUpdatePresenceGroup = ValueGroup("UpdatePresence")
	private val pingUpdatePresenceEnabledValue = BoolValue("Enabled", false, "PingUpdate")

	private val pingUpdatePresenceValidateGroup = ValueGroup("Validate")
	private val pingUpdatePresenceValidateEnabledValue = BoolValue("Enabled", false, "PingUpdate-Validation")
	private val pingUpdatePresenceValidateModeValue = ListValue("Mode", arrayOf("AnyMatches", "AllMatches"), "AnyMatches", "PingUpdate-Validation-Mode")

	/**
	 * Needs to got damaged
	 */
	private val needHitValue = BoolValue("NeedHit", false)

	/**
	 * Duplicate entity in the world
	 */
	private val duplicateInWorldGroup = ValueGroup("DuplicateInWorld")

	private val duplicateInWorldExistenceGroup = ValueGroup("Existence")
	private val duplicateInWorldExistenceEnabledValue = BoolValue("Enabled", false, "DuplicateName-World")
	private val duplicateInWorldExistenceNameModeValue = ListValue("Mode", arrayOf("DisplayName", "CustomNameTag", "GameProfileName"), "DisplayName", "DuplicateName-World-NameMode")
	private val duplicateInWorldExistenceStripColorsValue = BoolValue("StripColorsInName", true, "DuplicateName-World-StripColorsInName")

	private val duplicateInWorldAdditionGroup = ValueGroup("Addition")
	private val duplicateInWorldAdditionEnabledValue = BoolValue("AttemptToAddDuplicate-World", false, "AttemptToAddDuplicate-World")
	private val duplicateInWorldAdditionModeValue = ListValue("AttemptToAddDuplicate-World-NameMode", arrayOf("DisplayName", "GameProfileName"), "DisplayName", "AttemptToAddDuplicate-World-NameMode")
	private val duplicateInWorldAdditionStripColorsValue = BoolValue("AttemptToAddDuplicate-World-StripColorsInName", false, "AttemptToAddDuplicate-World-StripColorsInName")

	/**
	 * Duplicate player in tab
	 */
	private val duplicateInTab = ValueGroup("DuplicateInTab")

	private val duplicateInTabExistenceGroup = ValueGroup("Existence")
	private val duplicateInTabExistenceEnabledValue = BoolValue("Enabled", false, "DuplicateName-Tab")
	private val duplicateInTabExistenceModeValue = ListValue("Mode", arrayOf("DisplayName", "CustomNameTag", "GameProfileName"), "DisplayName", "DuplicateName-Tab-WorldNameMode")
	private val duplicateInTabExistenceNameModeValue = ListValue("NameMode", arrayOf("DisplayName", "GameProfileName"), "DisplayName", "DuplicateName-Tab-NameMode")
	private val duplicateInTabExistenceStripColorsValue = BoolValue("StripColorsInName", true, "DuplicateName-Tab-StripColorsInDisplayName")

	private val duplicateInTabAdditionGroup = ValueGroup("Addition")
	private val duplicateInTabAdditionEnabledValue = BoolValue("Enabled", false, "AttemptToAddDuplicate-Tab")
	private val duplicateInTabAdditionNameModeValue = ListValue("NameMode", arrayOf("DisplayName", "GameProfileName"), "GameProfileName", "AttemptToAddDuplicate-Tab-NameMode")
	private val duplicateInTabAdditionStripColorsValue = BoolValue("StripColorsInName", false, "AttemptToAddDuplicate-Tab-StripColorsInDisplayName")

	/**
	 * Always In Radius
	 */
	private val alwaysInRadiusGroup = ValueGroup("AlwaysInRadius")
	private val alwaysInRadiusEnabledValue = BoolValue("Enabled", false, "AlwaysInRadius")
	private val alwaysInRadiusRadiusValue = FloatValue("Radius", 20F, 5F, 30F, "AlwaysInRadiusBlocks")

	/**
	 * Unusual Teleport Packet (In vanilla minecraft, SPacketEntityTeleport is only used on the entity movements further than 8 blocks)
	 */
	private val teleportPacketGroup = ValueGroup("TeleportPacket")
	private val teleportPacketEnabledValue = BoolValue("Enabled", false, "TeleportPacket")
	private val teleportPacketThresholdDistanceValue = FloatValue("ThresholdDistance", 8.0f, 0.3125f, 16.0f, "TeleportPacket-ThresholdDistance")

	private val teleportPacketVLGroup = ValueGroup("Violation")
	private val teleportPacketVLEnabledValue = BoolValue("Enabled", true, "TeleportPacket-VL")
	private val teleportPacketVLLimitValue = IntegerValue("Threshold", 15, 1, 40, "TeleportPacket-VL-Threshold")
	private val teleportPacketVLDecValue = BoolValue("DecreaseIfNormal", true, "TeleportPacket-VL-DecreaseIfNormal")

	/**
	 * Horizontal Speed
	 */
	private val hspeedGroup = ValueGroup("HSpeed")
	private val hspeedEnabledValue = BoolValue("Enabled", false, "HSpeed")
	private val hspeedLimitValue = FloatValue("Limit", 4.0f, 1.0f, 255.0f, "HSpeed-Limit")

	private val hspeedVLGroup = ValueGroup("Violation")
	private val hspeedVLEnabledValue = BoolValue("Enabled", true, "HSpeed-VL")
	private val hspeedVLLimitValue = IntegerValue("Threshold", 5, 1, 10, "HSpeed-VL-Threshold")
	private val hspeedVLDecValue = BoolValue("DecreaseIfNormal", true, "HSpeed-VL-DecreaseIfNormal")

	/**
	 * Vertical Speed
	 */
	private val vspeedGroup = ValueGroup("VSpeed")
	private val vspeedEnabledValue = BoolValue("Enabled", false, "VSpeed")
	private val vspeedLimitValue = FloatValue("Limit", 4.0f, 1.0f, 255.0f, "VSpeed-Limit")

	private val vspeedVLGroup = ValueGroup("Violation")
	private val vspeedVLValue = BoolValue("Enabled", true, "VSpeed-VL")
	private val vspeedVLLimitValue = IntegerValue("Threshold", 2, 1, 10, "VSpeed-VL-Threshold")
	private val vspeedVLDecValue = BoolValue("DecreaseIfNormal", true, "VSpeed-VL-DecreaseIfNormal")

	/**
	 * Position Consistency
	 */
	private val positionGroup = ValueGroup("Position")
	private val positionEnabledValue = BoolValue("Enabled", true, "Position")

	private val positionRemoveDetectedGroup = ValueGroup("RemoveDetected")
	private val positionRemoveDetectedEnabledValue = BoolValue("Enabled", true, "Position-RemoveDetected")
	private val positionRemoveDetectedVLValue = IntegerValue("VL", 25, 10, 200, "Position-RemoveDetected-VL")

	private val positionPosition1Group = ValueGroup("Position1")
	private val positionPosition1BackValue = FloatValue("Back", 3.0f, 1.0f, 16.0f, "Position-Back-1")
	private val positionPosition1YValue = FloatValue("Y", 0.0f, 0.0f, 16.0f, "Position-Y-1")

	private val positionPosition2Group = ValueGroup("Position2")
	private val positionPosition2BackValue = FloatValue("Back", 3.0f, 1.0f, 16.0f, "Position-Back-2")
	private val positionPosition2YValue = FloatValue("Y", 3.0f, 0.0f, 16.0f, "Position-Y-2")

	private val positionPosition3Group = ValueGroup("Position3")
	private val positionPosition3BackValue = FloatValue("Back", 6.0f, 1.0f, 16.0f, "Position-Back-3")
	private val positionPosition3YValue = FloatValue("Y", 0.0f, 0.0f, 16.0f, "Position-Y-3")

	private val positionPosition4Group = ValueGroup("Position4")
	private val positionPosition4BackValue = FloatValue("Back", 6.0f, 1.0f, 16.0f, "Position-Back-4")
	private val positionPosition4YValue = FloatValue("Y", 6.0f, 0.0f, 16.0f, "Position-Y-4")

	/**
	 * Position Threshold
	 */
	private val positionDeltaThresholdValue = FloatValue("DeltaThreshold", 1.0f, 0.1f, 3.0f, "Position-DeltaLimit")

	private val positionDeltaVLGroup = ValueGroup("Violation")
	private val positionDeltaVLLimitValue = IntegerValue("Limit", 10, 2, 100, "Position-VL-Limit")
	private val positionDeltaVLDecValue = BoolValue("DecreaseIfNormal", false, "Position-VL-DecreaseIfNormal")

	/**
	 * Spawn Position
	 */
	private val positionSpawnedPositionGroup = ValueGroup("SpawnedPosition")
	private val positionSpawnedPositionEnabledValue = BoolValue("Enabled", true, "SpawnPosition")
	private val positionSpawnedPositionDeltaThresholdValue = FloatValue("DeltaThreshold", 2F, 0.5F, 4F, "SpawnPosition-DeltaLimit")

	private val positionSpawnedPositionPosition1Group = ValueGroup("Position1") //
	private val positionSpawnedPositionPosition1BackValue = FloatValue("Back", 3.0f, 1.0f, 16.0f, "SpawnPosition-Back-1")
	private val positionSpawnedPositionPosition1YValue = FloatValue("Y", 3.0f, 0.0f, 16.0f, "SpawnPosition-Y-1")

	private val positionSpawnedPositionPosition2Group = ValueGroup("Position2") //
	private val positionSpawnedPositionPosition2BackValue = FloatValue("Back", 6.0f, 1.0f, 16.0f, "SpawnPosition-Back-2")
	private val positionSpawnedPositionPosition2YValue = FloatValue("Y", 6.0f, 0.0f, 16.0f, "SpawnPosition-Y-2")

	/**
	 * Position Consistency Delta Consistency (xd)
	 */
	private val positionDeltaConsistencyGroup = ValueGroup("DeltaConsistency")
	private val positionDeltaConsistencyEnabledValue = BoolValue("Enabled", false, "Position-DeltaConsistency")
	private val positionDeltaConsistencyRequiredDeltaToCheckValue = FloatValue("RequiredDeltaToCheck", 1.0f, 0.1f, 3.0f, "Position-DeltaConsistency-RequiredDeltaToCheck")
	private val positionDeltaConsistencyConsistencyThresholdValue = FloatValue("ConsistencyThreshold", 0.1f, 0.0f, 1f, "Position-DeltaConsistency-ConsistencyLimit")

	private val positionDeltaConsitencyVLGroup = ValueGroup("Violation")
	private val positionDeltaConsistencyVLLimitValue = IntegerValue("Limit", 10, 1, 100, "Position-DeltaConsistency-VL-Limit")
	private val positionDeltaConsistencyVLDecValue = BoolValue("DecreaseIfNormal", false, "Position-DeltaConsistency-VL-DecreaseIfNormal")

	/**
	 * Mark the expected positions of Position check
	 */
	private val positionMarkGroup = ValueGroup("Mark")
	private val positionMarkEnabledValue = BoolValue("Enabled", false, "Position-Mark")
	private val positionMarkAlphaValue = IntegerValue("Alpha", 40, 5, 255, "Position-Mark-Alpha")

	/**
	 * Player position ping-correction offset
	 */
	private val positionPingCorrectionOffsetValue = IntegerValue("PingCorrectionOffset", 1, -2, 5, "Position-PingCorrection-Offset")

	/**
	 * CustomNameTag presence
	 */
	private val customNameGroup = ValueGroup("CustomName")
	private val customNameEnabledValue = BoolValue("Enabled", false, "CustomName")
	private val customNameBlankValue = BoolValue("Blank", false, "CustomName-Blank")
	private val customNameModeValue = ListValue("Mode", arrayOf("Equals", "Contains"), "Contains", "CustomName-Equality-Mode")
	private val customNameStripColorsValue = BoolValue("StripColorsInName", true, "CustomName-Equality-StripColorsInCustomName")
	private val customNameCompareToValue = ListValue("CompareTo", arrayOf("DisplayName", "GameProfileName"), "DisplayName", "CustomName-Equality-CompareTo")

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

	init
	{
		tabGroup.addAll(tabEnabledValue, tabModeValue, tabNameModeValue, tabStripColorsValue)

		entityIDStaticEntityIDGroup.addAll(entityIDStaticEntityIDEntityIDCountValue, entityIDStaticEntityIDEntityID1Value, entityIDStaticEntityIDEntityID2Value, entityIDStaticEntityIDEntityID3Value)
		entityIDGroup.addAll(entityIDEnabledValue, entityIDLimitValue, entityIDStaticEntityIDGroup)

		livingTimeGroup.addAll(livingTimeEnabledValue, livingTimeTicksValue)

		rotationInvalidPitchGroup.addAll(rotationInvalidPitchEnabledValue, rotationInvalidPitchKeepVLValue)
		rotationGroup.addAll(rotationYawValue, rotationPitchValue, rotationInvalidPitchGroup)

		pingUpdatePresenceValidateGroup.addAll(pingUpdatePresenceValidateEnabledValue, pingUpdatePresenceValidateModeValue)
		pingUpdatePresenceGroup.addAll(pingUpdatePresenceEnabledValue, pingUpdatePresenceValidateGroup)
		pingGroup.addAll(pingZeroValue, pingUpdatePresenceGroup)

		duplicateInWorldExistenceGroup.addAll(duplicateInWorldExistenceEnabledValue, duplicateInWorldExistenceNameModeValue, duplicateInWorldExistenceStripColorsValue)
		duplicateInWorldAdditionGroup.addAll(duplicateInWorldAdditionEnabledValue, duplicateInWorldAdditionModeValue, duplicateInWorldAdditionStripColorsValue)
		duplicateInWorldGroup.addAll(duplicateInWorldExistenceGroup, duplicateInWorldAdditionGroup)

		duplicateInTabExistenceGroup.addAll(duplicateInTabExistenceEnabledValue, duplicateInTabExistenceModeValue, duplicateInTabExistenceNameModeValue, duplicateInTabExistenceStripColorsValue)
		duplicateInTabAdditionGroup.addAll(duplicateInTabAdditionEnabledValue, duplicateInTabAdditionNameModeValue, duplicateInTabAdditionStripColorsValue)
		duplicateInTab.addAll(duplicateInTabExistenceGroup, duplicateInTabAdditionGroup)

		alwaysInRadiusGroup.addAll(alwaysInRadiusEnabledValue, alwaysInRadiusRadiusValue)

		teleportPacketVLGroup.addAll(teleportPacketVLEnabledValue, teleportPacketVLLimitValue, teleportPacketVLDecValue)
		teleportPacketGroup.addAll(teleportPacketEnabledValue, teleportPacketThresholdDistanceValue, teleportPacketVLGroup)

		hspeedVLGroup.addAll(hspeedVLEnabledValue, hspeedVLLimitValue, hspeedVLDecValue)
		hspeedGroup.addAll(hspeedEnabledValue, hspeedLimitValue, hspeedVLGroup)

		vspeedVLGroup.addAll(vspeedVLValue, vspeedVLLimitValue, vspeedVLDecValue)
		vspeedGroup.addAll(vspeedEnabledValue, vspeedLimitValue, vspeedVLGroup)

		positionRemoveDetectedGroup.addAll(positionRemoveDetectedEnabledValue, positionRemoveDetectedVLValue)
		positionPosition1Group.addAll(positionPosition1BackValue, positionPosition1YValue)
		positionPosition2Group.addAll(positionPosition2BackValue, positionPosition2YValue)
		positionPosition3Group.addAll(positionPosition3BackValue, positionPosition3YValue)
		positionPosition4Group.addAll(positionPosition4BackValue, positionPosition4YValue)

		positionDeltaVLGroup.addAll(positionDeltaVLLimitValue, positionDeltaVLDecValue)

		positionSpawnedPositionGroup.addAll(positionSpawnedPositionEnabledValue, positionSpawnedPositionDeltaThresholdValue, positionSpawnedPositionPosition1Group, positionSpawnedPositionPosition2Group)
		positionSpawnedPositionPosition1Group.addAll(positionSpawnedPositionPosition1BackValue, positionSpawnedPositionPosition1YValue)
		positionSpawnedPositionPosition2Group.addAll(positionSpawnedPositionPosition2BackValue, positionSpawnedPositionPosition2YValue)

		positionDeltaConsitencyVLGroup.addAll(positionDeltaConsistencyVLLimitValue, positionDeltaConsistencyVLDecValue)
		positionDeltaConsistencyGroup.addAll(positionDeltaConsistencyEnabledValue, positionDeltaConsistencyRequiredDeltaToCheckValue, positionDeltaConsistencyConsistencyThresholdValue, positionDeltaConsitencyVLGroup)

		positionMarkGroup.addAll(positionMarkEnabledValue, positionMarkAlphaValue)

		positionGroup.addAll(positionEnabledValue, positionRemoveDetectedGroup, positionPosition1Group, positionPosition2Group, positionPosition3Group, positionPosition4Group, positionDeltaThresholdValue, positionSpawnedPositionGroup, positionDeltaVLGroup, positionDeltaConsistencyGroup, positionMarkGroup, positionPingCorrectionOffsetValue)

		customNameGroup.addAll(customNameEnabledValue, customNameBlankValue, customNameModeValue, customNameStripColorsValue, customNameCompareToValue)
	}

	private fun getPingCorrectionAppliedLocation(thePlayer: IEntityPlayer, offset: Int = 0) = LocationCache.getPlayerLocationBeforeNTicks((ceil(thePlayer.getPing() / 50F).toInt() + offset + positionPingCorrectionOffsetValue.get()).coerceAtLeast(0), Location(WVec3(thePlayer.posX, thePlayer.entityBoundingBox.minY, thePlayer.posZ), RotationUtils.serverRotation))

	@JvmStatic
	fun checkTabList(targetName: String, displayName: Boolean, equals: Boolean, stripColors: Boolean): Boolean = mc.netHandler.playerInfoMap.map { networkPlayerInfo ->
		var networkName = networkPlayerInfo.getFullName(displayName)

		if (stripColors) networkName = stripColor(networkName)

		networkName
	}.any { networkName -> if (equals) targetName == networkName else networkName in targetName }

	fun isBot(theWorld: IWorldClient, thePlayer: IEntity, target: IEntityLivingBase): Boolean
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
		if (livingTimeEnabledValue.get() && entity.ticksExisted < livingTimeTicksValue.get()) return true

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
		if (entityIDEnabledValue.get() && (entityId >= entityIDLimitValue.get() || entityId <= -1)) return true

		// StaticEntityID
		if (entityIDStaticEntityIDEntityIDCountValue.get() > 0)
		{
			val ids = arrayOf(entityIDStaticEntityIDEntityID1Value.get(), entityIDStaticEntityIDEntityID2Value.get(), entityIDStaticEntityIDEntityID3Value.get())
			if ((0 until entityIDStaticEntityIDEntityIDCountValue.get()).map(ids::get).any { entityId == it }) return true
		}

		// Yaw & Pitch movements
		if (rotationYawValue.get() && entityId !in yawMovement) return true
		if (rotationPitchValue.get() && entityId !in pitchMovement) return true

		// Invalid pitch (Derp)
		if (rotationInvalidPitchEnabledValue.get() && if (rotationInvalidPitchKeepVLValue.get()) entityId in derp else (entity.rotationPitch > 90F || entity.rotationPitch < -90F)) return true

		// Was Invisible
		if (wasInvisibleValue.get() && entityId in invisible) return true

		// Armor
		if (armorValue.get() && entity.inventory.armorInventory.all { it == null }) return true

		// Ping
		if (pingZeroValue.get() && netHandler.getPlayerInfo(uuid)?.responseTime == 0) return true

		if (pingUpdatePresenceEnabledValue.get() && uuid in pingNotUpdated) return true

		// NeedHit
		if (needHitValue.get() && entityId !in hitted) return true

		// Invalid-Ground
		if (invalidGroundValue.get() && invalidGround[entityId] ?: 0 >= 10) return true

		// Tab
		if (tabEnabledValue.get())
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

		var dupInWorldTargetName = when (duplicateInWorldExistenceNameModeValue.get().toLowerCase())
		{
			"displayname" -> displayName
			"customnametag" -> customNameTagFailsafe
			else -> profileName
		}

		if (duplicateInWorldExistenceStripColorsValue.get()) dupInWorldTargetName = stripColor(dupInWorldTargetName)

		// Duplicate in the world
		if (duplicateInWorldExistenceEnabledValue.get() && theWorld.loadedEntityList.filter(provider::isEntityPlayer).map(IEntity::asEntityPlayer).count {
				dupInWorldTargetName == when (duplicateInWorldExistenceNameModeValue.get().toLowerCase())
				{
					"displayname" -> it.displayName.formattedText
					"customnametag" -> it.customNameTag.ifBlank { it.gameProfile.name }
					else -> it.gameProfile.name
				}
			} > 1) return true

		// Duplicate in the tab
		if (duplicateInTabExistenceEnabledValue.get() && netHandler.playerInfoMap.count {
				var entityName = when (duplicateInTabExistenceModeValue.get().toLowerCase())
				{
					"displayname" -> displayName
					"customnametag" -> customNameTagFailsafe
					else -> profileName
				}

				if (duplicateInTabExistenceStripColorsValue.get()) entityName = stripColor(entityName)

				var itName = it.getFullName(duplicateInTabExistenceNameModeValue.get().equals("DisplayName", ignoreCase = true))

				if (duplicateInTabExistenceStripColorsValue.get()) itName = stripColor(itName)

				entityName == itName
			} > 1) return true

		// Always in radius
		if (alwaysInRadiusEnabledValue.get() && entityId !in notAlwaysInRadius) return true

		// XZ Speed
		if (hspeedEnabledValue.get() && entityId in hspeed && (!hspeedVLEnabledValue.get() || hspeed[entityId] ?: 0 >= hspeedVLLimitValue.get())) return true

		// Y Speed
		if (vspeedEnabledValue.get() && entityId in vspeed && (!vspeedVLValue.get() || vspeed[entityId] ?: 0 >= vspeedVLLimitValue.get())) return true

		// Teleport Packet
		if (teleportPacketEnabledValue.get() && entityId in teleportpacket_violation && (!teleportPacketVLEnabledValue.get() || teleportpacket_violation[entityId] ?: 0 >= teleportPacketVLLimitValue.get())) return true

		// Spawned Position
		if (positionSpawnedPositionEnabledValue.get() && entityId in spawnPosition) return true

		if (positionEnabledValue.get() && positionVL[entityId] ?: 0 >= positionDeltaVLLimitValue.get() || positionDeltaConsistencyEnabledValue.get() && positionConsistencyVL[entityId] ?: 0 >= positionDeltaConsistencyVLLimitValue.get()) return true

		if (customNameEnabledValue.get())
		{
			val customName = customNameTag.let { if (customNameStripColorsValue.get()) stripColor(it) else it }

			if (customName.isBlank()) return customNameBlankValue.get()

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
				val distThreshold = teleportPacketThresholdDistanceValue.get()

				if (distSq <= distThreshold * distThreshold)
				{
					if (shouldNotify && teleportPacketEnabledValue.get() && (prevVL + 5) % 10 == 0) notification("Teleport Packet", "Suspicious SPacketEntityTeleport: ${entity.displayName.formattedText} (dist: ${sqrt(distSq)}, VL: $prevVL)")
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

			val positionDeltaLimit = positionSpawnedPositionDeltaThresholdValue.get()

			for ((posIndex, back, y) in arrayOf(Triple(1, positionSpawnedPositionPosition1BackValue.get(), positionSpawnedPositionPosition1YValue.get()), Triple(2, positionPosition2BackValue.get(), positionSpawnedPositionPosition2YValue.get())))
			{
				val expectDeltaX = serverPos.xCoord - func.sin(yawRadians) * back - entityX
				val expectDeltaY = serverPos.yCoord + y - entityY
				val expectDeltaZ = serverPos.zCoord + func.cos(yawRadians) * back - entityZ

				val expectSqrt = expectDeltaX * expectDeltaX + expectDeltaY * expectDeltaY + expectDeltaZ * expectDeltaZ

				// Position Delta
				if (expectSqrt <= positionDeltaLimit * positionDeltaLimit)
				{
					if (shouldNotify && positionSpawnedPositionEnabledValue.get()) notification("Spawn(Expect)", "Suspicious spawn: Entity #$entityId (posIndex: $posIndex, dist: ${DECIMALFORMAT_6.format(expectSqrt)})")
					spawnPosition.add(entityId)
				}
			}

			// Duplicate In World
			if (duplicateInWorldAdditionEnabledValue.get() && playerInfo != null)
			{
				val stripColors = duplicateInWorldAdditionStripColorsValue.get()
				val useDisplayName = duplicateInWorldAdditionModeValue.get().equals("DisplayName", ignoreCase = true)

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
					val useDisplayName = duplicateInTabAdditionNameModeValue.get().equals("DisplayName", ignoreCase = true)

					val tryStripColors = { name: String -> if (duplicateInTabAdditionStripColorsValue.get()) stripColor(name) else name }

					val currentPlayerList = playerInfoMap.map { tryStripColors((if (useDisplayName) it.displayName?.formattedText else it.gameProfile.name) ?: "") }

					val itr = players.listIterator()
					while (itr.hasNext())
					{
						val player = itr.next()

						if (duplicateInTabAdditionEnabledValue.get())
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

					if (pingUpdatePresenceValidateEnabledValue.get())
					{
						val allMatches = pingUpdatePresenceValidateModeValue.get().equals("AllMatches", ignoreCase = true)
						val prevPingUpdatedPlayerUUIDList = allPlayerUUIDList.filterNot(pingNotUpdated::contains)
						if (if (allMatches) !pingUpdatedPlayerUUIDList.all(prevPingUpdatedPlayerUUIDList::contains) else pingUpdatedPlayerUUIDList.none(prevPingUpdatedPlayerUUIDList::contains))
						{
							notification("PingUpdate-Validate", "Received suspicious ping update packet: missing players = ${if (allMatches) toString(pingUpdatedPlayerUUIDList.filterNot(prevPingUpdatedPlayerUUIDList::contains)) else "none matches"}")
							return
						}
					}

					if (pingNotUpdated.isEmpty()) pingNotUpdated.addAll(allPlayerUUIDList.filterNot(pingUpdatedPlayerUUIDList::contains))
					else pingNotUpdated.removeAll(allPlayerUUIDList.filter(pingUpdatedPlayerUUIDList::contains).filter(pingNotUpdated::contains))

					if (pingUpdatePresenceEnabledValue.get() && notificationValue.get() && pingNotUpdated.isNotEmpty()) notification("PingUpdate", "Ping not updated: ${toString(pingNotUpdated)}")
				}

				ISPacketPlayerListItem.WAction.REMOVE_PLAYER -> pingNotUpdated.removeAll(players.map { it.profile.id })

				else ->
				{
				}
			}
		}
	}

	private fun checkEntityMovements(theWorld: IWorldClient, thePlayer: IEntityPlayer, target: IEntityPlayer, newPos: WVec3, rotating: Boolean, encodedYaw: Byte, encodedPitch: Byte, onGround: Boolean, shouldNotify: Boolean)
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
		if (entityId !in notAlwaysInRadius && thePlayer.getDistanceToEntity(target) > alwaysInRadiusRadiusValue.get()) notAlwaysInRadius.add(entityId)

		// Horizontal Speed
		val hspeed = hypot(target.posX - newPos.xCoord, target.posZ - newPos.zCoord)
		if (hspeed > hspeedLimitValue.get())
		{
			if (shouldNotify && hspeedEnabledValue.get()) notification("HSpeed", "Moved too fast (horizontally): $displayName (${DECIMALFORMAT_6.format(hspeed)} blocks/tick)")
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
			if (shouldNotify && vspeedEnabledValue.get()) notification("VSpeed", "Moved too fast (vertically): $displayName (${DECIMALFORMAT_6.format(vspeed)} blocks/tick)")
			this.vspeed[entityId] = (this.vspeed[entityId] ?: 0) + 2
		}
		else if (vspeedVLDecValue.get())
		{
			val currentVL = (this.vspeed[entityId] ?: 0) - 1
			if (currentVL <= 0) this.vspeed.remove(entityId) else this.vspeed[entityId] = currentVL
		}

		// <editor-fold desc="Position Checks">

		if (positionEnabledValue.get())
		{
			val isSuspectedForSpawnPosition = positionSpawnedPositionEnabledValue.get() && entityId in spawnPositionSuspects

			val serverLocation = getPingCorrectionAppliedLocation(thePlayer)

			val serverPos = serverLocation.position
			val serverYaw = serverLocation.rotation.yaw

			var yawMovementScore = ceil(max(abs(getPingCorrectionAppliedLocation(thePlayer, 1).rotation.yaw - serverYaw), abs(getPingCorrectionAppliedLocation(thePlayer, 2).rotation.yaw - serverYaw)) / 5F).toInt()
			if (yawMovementScore <= 5) yawMovementScore = 0

			val yawRadians = WMathHelper.toRadians(serverYaw - 180.0F)

			val func = functions

			// Position delta limit
			val positionDeltaLimitSq = positionDeltaThresholdValue.get().pow(2)
			val positionDeltaVLDec = positionDeltaVLDecValue.get()

			// Position delta consistency
			val positionRequiredDeltaToCheckConsistency = positionDeltaConsistencyRequiredDeltaToCheckValue.get()
			val positionDeltaConsistencyLimit = positionDeltaConsistencyConsistencyThresholdValue.get()
			val positionDeltaConsistencyVLDec = positionDeltaConsistencyVLDecValue.get()

			// Remove on caught
			val removeOnCaught = positionRemoveDetectedEnabledValue.get()
			val removeOnVL = positionRemoveDetectedVLValue.get()

			for ((posIndex, back, y) in arrayOf(Triple(1, positionPosition1BackValue.get(), positionPosition1YValue.get()), Triple(2, positionPosition2BackValue.get(), positionPosition2YValue.get()), Triple(3, positionPosition3BackValue.get(), positionPosition3YValue.get()), Triple(4, positionPosition4BackValue.get(), positionPosition4YValue.get())))
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
						distanceSq <= positionDeltaLimitSq * 0.0005F -> 15
						distanceSq <= positionDeltaLimitSq * 0.02F -> 8
						distanceSq <= positionDeltaLimitSq * 0.05F -> 4
						distanceSq <= positionDeltaLimitSq * 0.1F -> 3
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

								if (shouldNotify && positionDeltaConsistencyEnabledValue.get() && ((prevVL + 5) % 10 == 0 || vlIncrement >= 5)) notification("Position(Expect-Consistency)", "Suspicious position consistency: $displayName (posIndex: $posIndex,delta: ${DECIMALFORMAT_6.format(consistency)}, posVL: $prevVL, posConsistencyVL: $prevConsistencyVL)")
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
		if (!positionMarkEnabledValue.get()) return

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

		val alpha = positionMarkAlphaValue.get()

		if (positionEnabledValue.get())
		{
			val deltaLimit = positionDeltaThresholdValue.get()

			val width = thePlayer.width + deltaLimit
			val height = thePlayer.height + deltaLimit

			val bb = provider.createAxisAlignedBB(-width - renderPosX, -renderPosY, -width - renderPosZ, width - renderPosX, height - renderPosY, width - renderPosZ)

			for ((back, y, color) in arrayOf(Triple(positionPosition1BackValue.get(), positionPosition1YValue.get(), 0xFF0000), Triple(positionPosition2BackValue.get(), positionPosition2YValue.get(), 0xFF8800), Triple(positionPosition3BackValue.get(), positionPosition3YValue.get(), 0x88FF00), Triple(positionPosition4BackValue.get(), positionPosition4YValue.get(), 0x00FF00))) RenderUtils.drawAxisAlignedBB(bb.offset(posX + sin * back, posY + y, posZ + cos * back), ColorUtils.applyAlphaChannel(color, alpha))
		}

		if (positionSpawnedPositionEnabledValue.get())
		{
			val deltaLimit = positionSpawnedPositionDeltaThresholdValue.get()

			val width = thePlayer.width + deltaLimit
			val height = thePlayer.height + deltaLimit

			val bb = provider.createAxisAlignedBB(-width - renderPosX, -renderPosY, -width - renderPosZ, width - renderPosX, height - renderPosY, width - renderPosZ)

			for ((back, y, color) in arrayOf(Triple(positionSpawnedPositionPosition1BackValue.get(), positionSpawnedPositionPosition1YValue.get(), 0x0088FF), Triple(positionSpawnedPositionPosition2BackValue.get(), positionSpawnedPositionPosition2YValue.get(), 0x0000FF))) RenderUtils.drawAxisAlignedBB(bb.offset(posX + sin * back, posY + y, posZ + cos * back), ColorUtils.applyAlphaChannel(color, alpha))
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
