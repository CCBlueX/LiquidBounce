/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.misc.*
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.movement.*
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.name.*
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.position.AlwaysInRadiusCheck
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.position.FoVCheck
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.position.PositionCheck
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.position.SpawnedPositionCheck
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.rotation.InvalidPitchCheck
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.rotation.PitchMovementCheck
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.rotation.YawMovementCheck
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.status.*
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.status.equipment.EquipmentChangeFrequencyCheck
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.status.equipment.EquipmentEmptyCheck
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.status.ping.PingUpdatePresenceCheck
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.status.ping.PingZeroCheck
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.swing.AlwaysSwingCheck
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.swing.SwingExistenceCheck
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.tab.DuplicateInTabAdditionCheck
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.tab.DuplicateInTabExistenceCheck
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.tab.TabCheck
import net.ccbluex.liquidbounce.value.*
import java.util.*
import kotlin.math.*

@ModuleInfo(name = "AntiBot", description = "Prevents KillAura from attacking AntiCheat bots.", category = ModuleCategory.MISC)
object AntiBot : Module()
{
    /**
     * Enable norifications
     */
    val notificationValue = BoolValue("DetectionNotification", false)

    /**
     * Tab
     */
    private val tabGroup = ValueGroup("Tab")
    val tabEnabledValue = BoolValue("Enabled", true, "Tab")
    val tabModeValue = ListValue("Mode", arrayOf("Equals", "Contains"), "Contains", "TabMode")
    val tabNameModeValue = ListValue("NameMode", arrayOf("DisplayName", "GameProfileName"), "DisplayName", "TabNameMode")
    val tabStripColorsValue = BoolValue("StripColorsInName", true, "TabStripColorsInDisplayname")

    /**
     *  Entity-ID
     */
    private val entityIDGroup = ValueGroup("EntityID")
    val entityIDEnabledValue = BoolValue("Enabled", true, "EntityID")
    val entityIDLimitValue = IntegerValue("Limit", 1000000000, 100000, 1000000000, "EntityIDLimit")

    /**
     * Static Entity-ID
     */
    private val entityIDStaticEntityIDGroup = ValueGroup("Static")
    val entityIDStaticEntityIDEntityIDCountValue = IntegerValue("Count", 0, 0, 3, "StaticEntityIDs")
    val entityIDStaticEntityIDEntityID1Value = object : IntegerValue("ID1", 99999999, Int.MIN_VALUE, Int.MAX_VALUE, "StaticEntityID-1")
    {
        override fun showCondition() = entityIDStaticEntityIDEntityIDCountValue.get() >= 1
    }
    val entityIDStaticEntityIDEntityID2Value = object : IntegerValue("ID2", 999999999, Int.MIN_VALUE, Int.MAX_VALUE, "StaticEntityID-2")
    {
        override fun showCondition() = entityIDStaticEntityIDEntityIDCountValue.get() >= 2
    }
    val entityIDStaticEntityIDEntityID3Value = object : IntegerValue("ID3", -1337, Int.MIN_VALUE, Int.MAX_VALUE, "StaticEntityID-3")
    {
        override fun showCondition() = entityIDStaticEntityIDEntityIDCountValue.get() >= 3
    }

    val invalidProfileNameValue = BoolValue("InvalidProfileName", false)

    /**
     * Color (The player who have colored-name is a bot)
     */
    val colorValue: BoolValue = object : BoolValue("Color", false)
    {
        override fun onChange(oldValue: Boolean, newValue: Boolean)
        {
            if (noColorValue.get()) noColorValue.set(false)
        }
    }

    /**
     * NoColor (The player who havn't colored-name is a bot)
     */
    val noColorValue: BoolValue = object : BoolValue("NoColor", false)
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
    val livingTimeEnabledValue = BoolValue("Enabled", false, "LivingTime")
    val livingTimeTicksValue = IntegerValue("Ticks", 40, 1, 200, "LivingTimeTicks")

    /**
     * Ground
     */
    val groundValue = BoolValue("Ground", true)

    /**
     * Air
     */
    val airValue = BoolValue("Air", false)

    /**
     * Invalid-Ground
     */
    val invalidGroundValue = BoolValue("InvalidGround", true)

    private val wasMovedGroup = ValueGroup("WasMoved")
    val wasMovedEnabledValue = BoolValue("Enabled", false)
    val wasMovedThresholdDistanceValue = FloatValue("ThresholdDistance", 1f, 0f, 10f)

    /**
     * Swing
     */
    val swingValue = BoolValue("Swing", false)

    private val alwaysSwingGroup = ValueGroup("AlwaysSwing")
    val alwaysSwingEnabledValue = BoolValue("Enabled", false)
    val alwaysSwingThresholdTimeValue = IntegerValue("Threshold", 1000, 500, 5000)

    /**
     * Health
     */
    val healthValue = BoolValue("Health", false)

    private val rotationGroup = ValueGroup("Rotation")

    private val rotationYawGroup = ValueGroup("Yaw")
    val rotationYawEnabledValue = BoolValue("Enabled", false, "YawMovements")
    val rotationYawThresholdValue = FloatValue("Threshold", 0f, 0f, 45f)

    private val rotationPitchGroup = ValueGroup("Pitch")
    val rotationPitchEnabledValue = BoolValue("Enabled", false, "PitchMovements")
    val rotationPitchThresholdValue = FloatValue("Threshold", 0f, 0f, 45f)

    /**
     * Invalid-Pitch (a.k.a. Derp)
     */
    private val rotationInvalidPitchGroup = ValueGroup("Derp")
    val rotationInvalidPitchEnabledValue = BoolValue("Enabled", true, "Derp")
    val rotationInvalidPitchKeepVLValue = BoolValue("KeepVL", true, "Derp-KeepVL")

    /**
     * Was Invisible
     */
    val wasInvisibleValue = BoolValue("WasInvisible", false)

    /**
     * NoArmor
     */
    private val equipmentGroup = ValueGroup("Equipment")
    val equipmentValue = BoolValue("Enabled", false, "Armor")
    val equipmentHelmetValue = BoolValue("Helmet", true)
    val equipmentChestplateValue = BoolValue("Chestplate", true)
    val equipmentLeggingsValue = BoolValue("Leggings", true)
    val equipmentBootsValue = BoolValue("Boots", true)

    private val equipmentChangeFrequencyGroup = ValueGroup("ChangeFrequency")
    val equipmentChangeFrequencyEnabledValue = BoolValue("Enabled", false)
    val equipmentChangeFrequencyOverallDelayValue = IntegerValue("OverallDelayLimit", 100, 5, 200)
    val equipmentChangeFrequencyPerSlotDelayValue = IntegerValue("PerSlotDelayLimit", 50, 10, 100)

    private val equipmentChangeFrequencyVLGroup = ValueGroup("Violation")
    val equipmentChangeFrequencyVLLimitValue = IntegerValue("Threshold", 10, 1, 10)
    val equipmentChangeFrequencyVLDecValue = BoolValue("DecreaseIfNormal", true)

    /**
     * Fixed Ping
     */
    private val pingGroup = ValueGroup("Ping")
    val pingZeroValue = BoolValue("NotZero", false, "Ping")

    /**
     * Ping update presence
     */
    private val pingUpdatePresenceGroup = ValueGroup("UpdatePresence")
    val pingUpdatePresenceEnabledValue = BoolValue("Enabled", false, "PingUpdate")

    private val pingUpdatePresenceValidateGroup = ValueGroup("Validate")
    val pingUpdatePresenceValidateEnabledValue = BoolValue("Enabled", false, "PingUpdate-Validation")
    val pingUpdatePresenceValidateModeValue = ListValue("Mode", arrayOf("AnyMatches", "AllMatches"), "AnyMatches", "PingUpdate-Validation-Mode")

    /**
     * Needs to got damaged
     */
    val needHitValue = BoolValue("NeedHit", false)

    /**
     * Duplicate entity in the world
     */
    private val duplicateInWorldGroup = ValueGroup("DuplicateInWorld")

    private val duplicateInWorldExistenceGroup = ValueGroup("Existence")
    val duplicateInWorldExistenceEnabledValue = BoolValue("Enabled", false, "DuplicateName-World")
    val duplicateInWorldExistenceNameModeValue = ListValue("Mode", arrayOf("DisplayName", "CustomNameTag", "GameProfileName"), "DisplayName", "DuplicateName-World-NameMode")
    val duplicateInWorldExistenceStripColorsValue = BoolValue("StripColorsInName", true, "DuplicateName-World-StripColorsInName")

    private val duplicateInWorldAdditionGroup = ValueGroup("Addition")
    val duplicateInWorldAdditionEnabledValue = BoolValue("Enabled", false, "AttemptToAddDuplicate-World")
    val duplicateInWorldAdditionModeValue = ListValue("Mode", arrayOf("DisplayName", "GameProfileName"), "DisplayName", "AttemptToAddDuplicate-World-NameMode")
    val duplicateInWorldAdditionStripColorsValue = BoolValue("StripColorsInName", false, "AttemptToAddDuplicate-World-StripColorsInName")

    /**
     * Duplicate player in tab
     */
    private val duplicateInTab = ValueGroup("DuplicateInTab")

    private val duplicateInTabExistenceGroup = ValueGroup("Existence")
    val duplicateInTabExistenceEnabledValue = BoolValue("Enabled", false, "DuplicateName-Tab")
    val duplicateInTabExistenceModeValue = ListValue("Mode", arrayOf("DisplayName", "CustomNameTag", "GameProfileName"), "DisplayName", "DuplicateName-Tab-WorldNameMode")
    val duplicateInTabExistenceNameModeValue = ListValue("NameMode", arrayOf("DisplayName", "GameProfileName"), "DisplayName", "DuplicateName-Tab-NameMode")
    val duplicateInTabExistenceStripColorsValue = BoolValue("StripColorsInName", true, "DuplicateName-Tab-StripColorsInDisplayName")

    private val duplicateInTabAdditionGroup = ValueGroup("Addition")
    val duplicateInTabAdditionEnabledValue = BoolValue("Enabled", false, "AttemptToAddDuplicate-Tab")
    val duplicateInTabAdditionNameModeValue = ListValue("NameMode", arrayOf("DisplayName", "GameProfileName"), "GameProfileName", "AttemptToAddDuplicate-Tab-NameMode")
    val duplicateInTabAdditionStripColorsValue = BoolValue("StripColorsInName", false, "AttemptToAddDuplicate-Tab-StripColorsInDisplayName")

    /**
     * Always In Radius
     */
    private val alwaysInRadiusGroup = ValueGroup("AlwaysInRadius")
    val alwaysInRadiusEnabledValue = BoolValue("Enabled", false, "AlwaysInRadius")
    val alwaysInRadiusRadiusValue = FloatValue("Radius", 20F, 5F, 30F, "AlwaysInRadiusBlocks")

    /**
     * Unusual Teleport Packet (In vanilla minecraft, SPacketEntityTeleport is only used on the entity movements further than 8 blocks)
     */
    private val teleportPacketGroup = ValueGroup("TeleportPacket")
    val teleportPacketEnabledValue = BoolValue("Enabled", false, "TeleportPacket")
    val teleportPacketThresholdDistanceValue = FloatValue("ThresholdDistance", 8.0f, 0.3125f, 16.0f, "TeleportPacket-ThresholdDistance")

    private val teleportPacketVLGroup = ValueGroup("Violation")
    val teleportPacketVLEnabledValue = BoolValue("Enabled", true, "TeleportPacket-VL")
    val teleportPacketVLLimitValue = IntegerValue("Threshold", 15, 1, 40, "TeleportPacket-VL-Threshold")
    val teleportPacketVLDecValue = BoolValue("DecreaseIfNormal", true, "TeleportPacket-VL-DecreaseIfNormal")

    private val noClipGroup = ValueGroup("NoClip")
    val noClipEnabledValue = BoolValue("Enabled", false)

    private val noClipVLGroup = ValueGroup("Violation")
    val noClipVLLimitValue = IntegerValue("Threshold", 15, 1, 40, "TeleportPacket-VL-Threshold")
    val noClipVLDecValue = BoolValue("DecreaseIfNormal", true, "TeleportPacket-VL-DecreaseIfNormal")

    /**
     * Horizontal Speed
     */
    private val hspeedGroup = ValueGroup("HSpeed")
    val hspeedEnabledValue = BoolValue("Enabled", false, "HSpeed")
    val hspeedLimitValue = FloatValue("Limit", 4.0f, 1.0f, 255.0f, "HSpeed-Limit")

    private val hspeedVLGroup = ValueGroup("Violation")
    val hspeedVLEnabledValue = BoolValue("Enabled", true, "HSpeed-VL")
    val hspeedVLLimitValue = IntegerValue("Threshold", 5, 1, 10, "HSpeed-VL-Threshold")
    val hspeedVLDecValue = BoolValue("DecreaseIfNormal", true, "HSpeed-VL-DecreaseIfNormal")

    /**
     * Vertical Speed
     */
    private val vspeedGroup = ValueGroup("VSpeed")
    val vspeedEnabledValue = BoolValue("Enabled", false, "VSpeed")
    val vspeedLimitValue = FloatValue("Limit", 4.0f, 1.0f, 255.0f, "VSpeed-Limit")

    private val vspeedVLGroup = ValueGroup("Violation")
    val vspeedVLValue = BoolValue("Enabled", true, "VSpeed-VL")
    val vspeedVLLimitValue = IntegerValue("Threshold", 2, 1, 10, "VSpeed-VL-Threshold")
    val vspeedVLDecValue = BoolValue("DecreaseIfNormal", true, "VSpeed-VL-DecreaseIfNormal")

    /**
     * Position Consistency
     */
    private val positionGroup = ValueGroup("Position")
    val positionEnabledValue = BoolValue("Enabled", true, "Position")

    private val positionRemoveDetectedGroup = ValueGroup("RemoveDetected")
    val positionRemoveDetectedEnabledValue = BoolValue("Enabled", true, "Position-RemoveDetected")
    val positionRemoveDetectedVLValue = IntegerValue("VL", 25, 10, 200, "Position-RemoveDetected-VL")

    private val positionPosition1Group = ValueGroup("Position1")
    val positionPosition1BackValue = FloatValue("Back", 3.0f, 1.0f, 16.0f, "Position-Back-1")
    val positionPosition1YValue = FloatValue("Y", 0.0f, 0.0f, 16.0f, "Position-Y-1")

    private val positionPosition2Group = ValueGroup("Position2")
    val positionPosition2BackValue = FloatValue("Back", 3.0f, 1.0f, 16.0f, "Position-Back-2")
    val positionPosition2YValue = FloatValue("Y", 3.0f, 0.0f, 16.0f, "Position-Y-2")

    private val positionPosition3Group = ValueGroup("Position3")
    val positionPosition3BackValue = FloatValue("Back", 6.0f, 1.0f, 16.0f, "Position-Back-3")
    val positionPosition3YValue = FloatValue("Y", 0.0f, 0.0f, 16.0f, "Position-Y-3")

    private val positionPosition4Group = ValueGroup("Position4")
    val positionPosition4BackValue = FloatValue("Back", 6.0f, 1.0f, 16.0f, "Position-Back-4")
    val positionPosition4YValue = FloatValue("Y", 6.0f, 0.0f, 16.0f, "Position-Y-4")

    /**
     * Position Threshold
     */
    val positionDeltaThresholdValue = FloatValue("DeltaThreshold", 1.0f, 0.1f, 3.0f, "Position-DeltaLimit")

    private val positionDeltaVLGroup = ValueGroup("Violation")
    val positionDeltaVLLimitValue = IntegerValue("Limit", 10, 2, 100, "Position-VL-Limit")
    val positionDeltaVLDecValue = BoolValue("DecreaseIfNormal", false, "Position-VL-DecreaseIfNormal")

    /**
     * Spawn Position
     */
    private val positionSpawnedPositionGroup = ValueGroup("SpawnedPosition")
    val positionSpawnedPositionEnabledValue = BoolValue("Enabled", true, "SpawnPosition")
    val positionSpawnedPositionDeltaThresholdValue = FloatValue("DeltaThreshold", 2F, 0.5F, 4F, "SpawnPosition-DeltaLimit")

    private val positionSpawnedPositionPosition1Group = ValueGroup("Position1") //
    val positionSpawnedPositionPosition1BackValue = FloatValue("Back", 3.0f, 1.0f, 16.0f, "SpawnPosition-Back-1")
    val positionSpawnedPositionPosition1YValue = FloatValue("Y", 3.0f, 0.0f, 16.0f, "SpawnPosition-Y-1")

    private val positionSpawnedPositionPosition2Group = ValueGroup("Position2") //
    val positionSpawnedPositionPosition2BackValue = FloatValue("Back", 6.0f, 1.0f, 16.0f, "SpawnPosition-Back-2")
    val positionSpawnedPositionPosition2YValue = FloatValue("Y", 6.0f, 0.0f, 16.0f, "SpawnPosition-Y-2")

    /**
     * Position Consistency Delta Consistency (xd)
     */
    private val positionDeltaConsistencyGroup = ValueGroup("DeltaConsistency")
    val positionDeltaConsistencyEnabledValue = BoolValue("Enabled", false, "Position-DeltaConsistency")
    val positionDeltaConsistencyRequiredDeltaToCheckValue = FloatValue("RequiredDeltaToCheck", 1.0f, 0.1f, 3.0f, "Position-DeltaConsistency-RequiredDeltaToCheck")
    val positionDeltaConsistencyConsistencyThresholdValue = FloatValue("ConsistencyThreshold", 0.1f, 0.0f, 1f, "Position-DeltaConsistency-ConsistencyLimit")

    private val positionDeltaConsitencyVLGroup = ValueGroup("Violation")
    val positionDeltaConsistencyVLLimitValue = IntegerValue("Limit", 10, 1, 100, "Position-DeltaConsistency-VL-Limit")
    val positionDeltaConsistencyVLDecValue = BoolValue("DecreaseIfNormal", false, "Position-DeltaConsistency-VL-DecreaseIfNormal")

    /**
     * Mark the expected positions of Position check
     */
    private val positionMarkGroup = ValueGroup("Mark")
    val positionMarkEnabledValue = BoolValue("Enabled", false, "Position-Mark")
    val positionMarkAlphaValue = IntegerValue("Alpha", 40, 5, 255, "Position-Mark-Alpha")

    /**
     * Player position ping-correction offset
     */
    val positionPingCorrectionOffsetValue = IntegerValue("PingCorrectionOffset", 1, -2, 5, "Position-PingCorrection-Offset")

    private val fovGroup = ValueGroup("FoV")
    val fovEnabledValue = BoolValue("Enabled", false)
    val fovFoVValue = FloatValue("FoV", 120F, 0f, 170F)
    val fovPingCorrectionOffsetValue = IntegerValue("PingCorrectionOffset", 1, -2, 5)

    private val fovVLGroup = ValueGroup("Violation")
    val fovVLLimitValue = IntegerValue("Limit", 10, 1, 100, "Position-DeltaConsistency-VL-Limit")
    val fovVLDecValue = BoolValue("DecreaseIfNormal", false, "Position-DeltaConsistency-VL-DecreaseIfNormal")

    /**
     * CustomNameTag presence
     */
    private val customNameGroup = ValueGroup("CustomName")
    val customNameEnabledValue = BoolValue("Enabled", false, "CustomName")
    val customNameBlankValue = BoolValue("Blank", false, "CustomName-Blank")
    val customNameModeValue = ListValue("Mode", arrayOf("Equals", "Contains"), "Contains", "CustomName-Equality-Mode")
    val customNameStripColorsValue = BoolValue("StripColorsInName", true, "CustomName-Equality-StripColorsInCustomName")
    val customNameCompareToValue = ListValue("CompareTo", arrayOf("DisplayName", "GameProfileName"), "DisplayName", "CustomName-Equality-CompareTo")

    // TODO: private val collisionValue = BoolValue("Collision", true)

    /**
     * "\u00A78[NPC] " prefix on name
     */
    val npcValue = BoolValue("NPC", true)

    val bedWarsNPCValue = BoolValue("BedWarsNPC", false)

    // TODO: private val collision_violation = mutableMapOf<Int, Int>()

    private val checks = arrayOf(

        // Tab
        TabCheck(), DuplicateInTabExistenceCheck(), DuplicateInTabAdditionCheck(),

        // Rotation
        YawMovementCheck(), PitchMovementCheck(), InvalidPitchCheck(),

        // Position
        AlwaysInRadiusCheck(), PositionCheck(), SpawnedPositionCheck(), FoVCheck(),

        // Name
        DuplicateInWorldExistenceCheck(), DuplicateInWorldAdditionCheck(), CustomNameCheck(), InvalidNameCheck(), NPCCheck(), BedwarsNPCCheck(),

        // Movement
        HorizontalSpeedCheck(), VerticalSpeedCheck(), TeleportPacketCheck(), WasMovedCheck(), NoClipCheck(),

        // Status
        AirCheck(), GroundCheck(), InvalidGroundCheck(), EquipmentEmptyCheck(), EquipmentChangeFrequencyCheck(), WasInvisibleCheck(),

        // Health
        HealthCheck(),

        // Ping
        PingZeroCheck(), PingUpdatePresenceCheck(),

        // Swing
        SwingExistenceCheck(), AlwaysSwingCheck(),

        // Misc.
        ColorCheck(), NoColorCheck(), EntityIDCheck(), LivingTimeCheck(), NeedHitCheck()

    )

    init
    {
        tabGroup.addAll(tabEnabledValue, tabModeValue, tabNameModeValue, tabStripColorsValue)

        entityIDStaticEntityIDGroup.addAll(entityIDStaticEntityIDEntityIDCountValue, entityIDStaticEntityIDEntityID1Value, entityIDStaticEntityIDEntityID2Value, entityIDStaticEntityIDEntityID3Value)
        entityIDGroup.addAll(entityIDEnabledValue, entityIDLimitValue, entityIDStaticEntityIDGroup)

        livingTimeGroup.addAll(livingTimeEnabledValue, livingTimeTicksValue)

        wasMovedGroup.addAll(wasMovedEnabledValue, wasMovedThresholdDistanceValue)

        alwaysSwingGroup.addAll(alwaysSwingEnabledValue, alwaysSwingThresholdTimeValue)

        rotationPitchGroup.addAll(rotationPitchEnabledValue, rotationPitchThresholdValue)
        rotationYawGroup.addAll(rotationYawEnabledValue, rotationYawThresholdValue)
        rotationGroup.addAll(rotationYawGroup, rotationPitchGroup)

        rotationInvalidPitchGroup.addAll(rotationInvalidPitchEnabledValue, rotationInvalidPitchKeepVLValue)
        rotationGroup.addAll(rotationYawEnabledValue, rotationPitchEnabledValue, rotationInvalidPitchGroup)

        equipmentChangeFrequencyVLGroup.addAll(equipmentChangeFrequencyVLLimitValue, equipmentChangeFrequencyVLDecValue)
        equipmentChangeFrequencyGroup.addAll(equipmentChangeFrequencyEnabledValue, equipmentChangeFrequencyOverallDelayValue, equipmentChangeFrequencyPerSlotDelayValue, equipmentChangeFrequencyVLGroup)
        equipmentGroup.addAll(equipmentValue, equipmentHelmetValue, equipmentChestplateValue, equipmentLeggingsValue, equipmentBootsValue, equipmentChangeFrequencyGroup)

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

        noClipVLGroup.addAll(noClipVLLimitValue, noClipVLDecValue)
        noClipGroup.addAll(noClipEnabledValue, noClipVLGroup)

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

        fovVLGroup.addAll(fovVLLimitValue, fovVLDecValue)
        fovGroup.addAll(fovEnabledValue, fovFoVValue, fovVLGroup)

        customNameGroup.addAll(customNameEnabledValue, customNameBlankValue, customNameModeValue, customNameStripColorsValue, customNameCompareToValue)
    }

    fun isBot(theWorld: IWorldClient, thePlayer: IEntityPlayer, target: IEntityLivingBase): Boolean
    {
        if (!state) return false

        if (!classProvider.isEntityPlayer(target)) return false

        val entity = target.asEntityPlayer()
        return entity.name.isEmpty() || entity.name.equals(thePlayer.name, ignoreCase = true) || entity.gameProfile.name.equals(thePlayer.gameProfile.name, ignoreCase = true) || checks.filter(BotCheck::isActive).any { it.isBot(theWorld, thePlayer, entity) }
    }

    // TODO
    // fun isBotReason(theWorld: IWorldClient, thePlayer: IEntityPlayer, target: IEntityLivingBase): String?
    // {
    // 	if (!state) return null
    //
    // 	if (!classProvider.isEntityPlayer(target)) return null
    //
    // 	val entity = target.asEntityPlayer()
    //
    // 	return if (entity.name.isEmpty()) "name.empty"
    // 	else if (entity.name.equals(thePlayer.name, ignoreCase = true)) "name.equalsWithPlayer"
    // 	else if (entity.gameProfile.name.equals(thePlayer.gameProfile.name, ignoreCase = true)) "name.profileName.equalsWithPlayer"
    // 	else checks.filter(BotCheck::isActive).find { it.isBot(theWorld, thePlayer, entity) }?.modeName
    // }

    override fun onDisable()
    {
        clearAll()
    }

    @EventTarget
    fun onPacket(event: PacketEvent)
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

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

                handleEntityMovement(theWorld, thePlayer, targetPlayer, false, WVec3((targetPlayer.serverPosX + movePacket.posX) / 32.0, (targetPlayer.serverPosY + movePacket.posY) / 32.0, (targetPlayer.serverPosZ + movePacket.posZ) / 32.0), movePacket.rotating, movePacket.yaw, movePacket.pitch, movePacket.onGround)
            }
        }

        // Teleport packet check & Movement checks
        if (provider.isSPacketEntityTeleport(packet))
        {
            val teleportPacket = packet.asSPacketEntityTeleport()
            val entity: IEntity? = theWorld.getEntityByID(teleportPacket.entityId)

            if (entity != null && provider.isEntityPlayer(entity)) handleEntityMovement(theWorld, thePlayer, entity.asEntityPlayer(), true, WVec3(teleportPacket.x, teleportPacket.y, teleportPacket.z), true, teleportPacket.yaw, teleportPacket.pitch, teleportPacket.onGround)
        }

        if (!event.isCancelled) checks.filter(BotCheck::isActive).forEach { it.onPacket(event) }
    }

    private fun handleEntityMovement(theWorld: IWorldClient, thePlayer: IEntityPlayer, target: IEntityPlayer, isTeleport: Boolean, newPos: WVec3, rotating: Boolean, encodedYaw: Byte, encodedPitch: Byte, onGround: Boolean)
    {
        checks.filter(BotCheck::isActive).forEach { it.onEntityMove(theWorld, thePlayer, target, isTeleport, newPos, rotating, if (rotating) encodedYaw * 360.0F / 256.0F else target.rotationYaw, if (rotating) encodedPitch * 360.0F / 256.0F else target.rotationPitch, onGround) }
    }

    @EventTarget
    fun onAttack(event: AttackEvent)
    {
        checks.filter(BotCheck::isActive).forEach { it.onAttack(event) }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent)
    {
        checks.filter(BotCheck::isActive).forEach { it.onRender3D(event) }
    }

    @EventTarget
    fun onWorld(@Suppress("UNUSED_PARAMETER") event: WorldEvent)
    {
        clearAll()
    }

    private fun clearAll()
    {
        checks.forEach(BotCheck::clear)
    }
}
