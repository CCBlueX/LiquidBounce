/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.player.Blink
import net.ccbluex.liquidbounce.features.module.modules.render.FreeCam
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.NotificationIcon
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.misc.StringUtils.DECIMALFORMAT_1
import net.ccbluex.liquidbounce.utils.misc.StringUtils.DECIMALFORMAT_6
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.easeOutCubic
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.*
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemFood
import net.minecraft.item.ItemPotion
import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.potion.Potion
import net.minecraft.util.*
import net.minecraft.world.World
import net.minecraft.world.WorldSettings
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

// TODO: Asynchronously start-stop blocking as Xave
// TODO: BlockCPS option
@ModuleInfo(name = "KillAura", description = "Automatically attacks targets around you.", category = ModuleCategory.COMBAT, defaultKeyBinds = [Keyboard.KEY_R])
class KillAura : Module()
{
    /**
     * OPTIONS
     */

    private val cpsValue: IntegerRangeValue = object : IntegerRangeValue("CPS", 5, 8, 1, 20, "MaxCPS" to "MinCPS", "Number of attack tries per a second")
    {
        override fun onMaxValueChanged(oldValue: Int, newValue: Int)
        {
            attackDelay = TimeUtils.randomClickDelay(getMin(), newValue)
        }

        override fun onMinValueChanged(oldValue: Int, newValue: Int)
        {
            attackDelay = TimeUtils.randomClickDelay(newValue, getMax())
        }
    }

    private val hurtTimeValue = IntegerValue("HurtTime", 10, 0, 10)

    private val rangeGroup = ValueGroup("Range")
    private val rangeAttackGroup = ValueGroup("Attack")
    private val rangeAttackOnGroundValue = object : FloatValue("OnGround", 3.7f, 1f, 8f, "Range")
    {
        override fun onChanged(oldValue: Float, newValue: Float)
        {
            val i = swingRangeValue.get()
            if (i < newValue) this.set(i)
        }
    }
    private val rangeAttackOffGroundValue = object : FloatValue("OffGround", 3.7f, 1f, 8f, "Range")
    {
        override fun onChanged(oldValue: Float, newValue: Float)
        {
            val i = swingRangeValue.get()
            if (i < newValue) this.set(i)
        }
    }

    private val rangeThroughWallsAttackValue = FloatValue("ThroughWallsAttack", 3f, 0f, 8f, "ThroughWallsRange")
    private val rangeAimValue: FloatValue = FloatValue("Aim", 6f, 1f, 12f, "AimRange")
    private val rangeSprintReducementValue = FloatValue("RangeSprintReducement", 0f, 0f, 0.4f, "RangeSprintReducement")

    private val swingGroup = ValueGroup("Swing")
    private val swingEnabledValue = BoolValue("Enabled", true, "Swing")
    private val swingFakeSwingValue = BoolValue("FakeSwing", true, "FakeSwing")
    private val swingRangeValue: FloatValue = object : FloatValue("Range", 6f, 1f, 12f, "SwingRange")
    {
        override fun onChanged(oldValue: Float, newValue: Float)
        {
            val i = min(rangeAttackOnGroundValue.get(), rangeAttackOffGroundValue.get())
            if (i > newValue) this.set(i)

            val i2 = rangeAimValue.get()
            if (i2 < newValue) this.set(i2)
        }
    }

    private val comboReachGroup = ValueGroup("ComboReach")
    private val comboReachEnabledValue = BoolValue("Enabled", false, "ComboReach")
    private val comboReachIncrementValue = FloatValue("Increment", 0.1F, 0.02F, 0.5F, "ComboReachIncrement")
    private val comboReachLimitValue = FloatValue("Limit", 0.5F, 0.02F, 3F, "ComboReachMax")

    private val targetGroup = ValueGroup("Target")
    private val targetPriorityValue = ListValue("Priority", arrayOf("Health", "Distance", "ServerDirection", "ClientDirection", "LivingTime"), "Distance", "Priority")
    private val targetModeValue = ListValue("Mode", arrayOf("Single", "Switch", "Multi"), "Switch", "TargetMode")
    private val targetLimitedMultiTargetsValue = object : IntegerValue("LimitedMultiTargets", 0, 0, 50, "LimitedMultiTargets")
    {
        override fun showCondition() = targetModeValue.get().equals("Multi", ignoreCase = true)
    }

    private val switchDelayValue: IntegerRangeValue = object : IntegerRangeValue("SwitchDelay", 0, 0, 0, 1000, "MaxSwitchDelay" to "MinSwitchDelay")
    {
        override fun onMaxValueChanged(oldValue: Int, newValue: Int)
        {
            switchDelay = TimeUtils.randomDelay(getMin(), newValue)
        }

        override fun onMinValueChanged(oldValue: Int, newValue: Int)
        {
            switchDelay = TimeUtils.randomDelay(newValue, getMax())
        }
    }

    private val autoBlockGroup = ValueGroup("AutoBlock")
    private val autoBlockValue = ListValue("Mode", arrayOf("Off", "Fake", "Packet", "AfterTick"), "Packet")
    private val autoBlockRangeValue: FloatValue = FloatValue("Range", 6f, 1f, 12f)
    private val autoBlockRate = IntegerValue("Rate", 100, 1, 100)
    private val autoBlockHitableCheckValue = BoolValue("HitableCheck", false)
    private val autoBlockHurtTimeCheckValue = BoolValue("HurtTimeCheck", true)
    private val autoBlockWallCheckValue = BoolValue("WallCheck", false)

    private val interactAutoBlockGroup = ValueGroup("Interact")
    private val interactAutoBlockEnabledValue = BoolValue("Enabled", false, "InteractAutoBlock")
    private val interactAutoBlockRangeValue: FloatValue = FloatValue("Range", 3f, 1f, 8f)

    private val rayCastGroup = ValueGroup("RayCast")
    private val rayCastEnabledValue = BoolValue("Enabled", true, "RayCast")
    private val rayCastSkipEnemyCheckValue = BoolValue("SkipEnemyCheck", false, "RayCastIgnored", description = "Disable the enemy checks in raycast filter")
    private val rayCastLivingOnlyValue = BoolValue("LivingOnly", true, "LivingRayCast", description = "Only include living entities; drop otherwise")
    private val rayCastIncludeCollidedValue = BoolValue("IncludeCollided", true, "AAC", description = "Include entities which were collided with the target")

    private val bypassGroup = ValueGroup("Bypass")
    private val bypassKeepSprintValue = BoolValue("KeepSprint", true, "KeepSprint", description = "Don't cancel sprinting while attacking enemy")
    private val bypassAACValue = BoolValue("AAC", false, "AAC", description = "Bypass several anti-cheats such as AAC")
    private val bypassMissChanceValue = FloatValue("MissChance", 0f, 0f, 100f, "FailRate")
    private val bypassSuspendWhileConsumingValue = BoolValue("SuspendWhileConsuming", true, "SuspendWhileConsuming", description = "Suspend KillAura if you're consuming something")

    private val noInventoryGroup = ValueGroup("NoInvAttack")
    private val noInventoryAttackEnabledValue = BoolValue("Enabled", false, "NoInvAttack", description = "Suspend KillAura if your inventory is open")
    private val noInventoryDelayValue = IntegerValue("Delay", 200, 0, 500, "NoInvDelay", description = "Time between inventory close and resume of KillAura")

    private val rotationGroup = ValueGroup("Rotation")
    private val rotationMode = ListValue("Mode", arrayOf("Off", "SearchCenter", "LockCenter", "RandomCenter", "Outborder"), "SearchCenter", "Rotation")
    private val rotationLockValue = BoolValue("Lock", true, "Rotation-Lock")
    private val rotationLockExpandRangeValue = object : FloatValue("FacedCheckBoxExpand", 0.0f, 0.0F, 2.0F)
    {
        override fun showCondition() = !rotationLockValue.get()
    }
    private val rotationSilentValue = BoolValue("Silent", true, "SilentRotation")
    private val rotationRandomCenterSizeValue = object : FloatValue("RandomCenterSize", 0.8F, 0.1F, 1.0F, "Rotation-RandomCenter-RandomSize")
    {
        override fun showCondition() = rotationMode.get().equals("RandomCenter", ignoreCase = true)
    }
    private val rotationSearchCenterGroup = object : ValueGroup("SearchCenter")
    {
        override fun showCondition() = rotationMode.get().equals("SearchCenter", ignoreCase = true)
    }
    private val rotationSearchCenterHitboxShrinkValue = FloatValue("Shrink", 0.15f, 0f, 0.3f, "Rotation-SearchCenter-HitboxShrink", description = "Shrinkage of the enemy hitbox when rotation calculation")
    private val rotationSearchCenterSensitivityValue = IntegerValue("Steps", 7, 4, 20, "Rotation-SearchCenter-Steps", description = "Steps of rotation calculation")

    private val rotationJitterGroup = ValueGroup("Jitter")
    private val rotationJitterEnabledValue = BoolValue("Enabled", false, "Jitter")
    private val rotationJitterYawRate = IntegerValue("YawRate", 50, 0, 100, "YawJitterRate")
    private val rotationJitterPitchRate = IntegerValue("PitchRate", 50, 0, 100, "PitchJitterRate")
    private val rotationJitterYawIntensityValue = FloatRangeValue("YawIntensity", 0f, 1f, 0f, 5f, "MaxYawJitterStrength" to "MinYawJitterStrength")
    private val rotationJitterPitchIntensityValue = FloatRangeValue("PitchIntensity", 0f, 1f, 0f, 5f, "MaxPitchJitterStrength" to "MinPitchJitterStrength")

    private val rotationKeepRotationGroup = ValueGroup("KeepRotation")
    private val rotationKeepRotationEnabledValue = BoolValue("Enabled", false, "KeepRotation")
    private val rotationKeepRotationTicks = IntegerRangeValue("Ticks", 20, 30, 0, 60, "MaxKeepRotationTicks" to "MinKeepRotationTicks")

    private val rotationLockAfterTeleportGroup = ValueGroup("LockAfterTeleport")
    private val rotationLockAfterTeleportEnabledValue = BoolValue("Enabled", false)
    private val rotationLockAfterTeleportDelayValue = IntegerRangeValue("Delay", 100, 100, 0, 500)

    private val rotationAccelerationRatioValue = FloatRangeValue("Acceleration", 0f, 0f, 0f, .99f, "MaxAccelerationRatio" to "MinAccelerationRatio")
    private val rotationTurnSpeedValue = FloatRangeValue("TurnSpeed", 180f, 180f, 0f, 180f, "MaxTurnSpeed" to "MinTurnSpeed")
    private val rotationResetSpeedValue = object : FloatRangeValue("RotationResetSpeed", 180f, 180f, 10f, 180f, "MaxRotationResetSpeed" to "MinRotationResetSpeed")
    {
        override fun showCondition() = rotationSilentValue.get()
    }

    private val rotationStrafeGroup = ValueGroup("Strafe")
    private val rotationStrafeValue = ListValue("Mode", arrayOf("Off", "Strict", "Silent"), "Off", "Strafe")
    private val rotationStrafeOnlyGroundValue = BoolValue("OnlyGround", false, "StrafeOnlyGround")

    private val rotationPredictGroup = ValueGroup("Predict")
    private val rotationPredictEnemyGroup = ValueGroup("Enemy")
    private val rotationPredictEnemyEnabledValue = BoolValue("Enabled", true, "Predict")
    private val rotationPredictEnemyIntensityValue = FloatRangeValue("Intensity", 1f, 1f, -2f, 2f, "MaxPredictSize" to "MinPredictSize")

    private val rotationPredictPlayerGroup = ValueGroup("Player")
    private val rotationPredictPlayerEnabledValue = BoolValue("Enabled", true, "PlayerPredict")
    private val rotationPredictPlayerIntensityValue = FloatRangeValue("Intensity", 1f, 1f, -2f, 2f, "MaxPlayerPredictSize" to "MinPlayerPredictSize")

    private val rotationBacktrackGroup = ValueGroup("Backtrack")
    private val rotationBacktrackEnabledValue = BoolValue("Enabled", false, "Backtrace")
    private val rotationBacktrackTicksValue: IntegerValue = IntegerValue("Ticks", 3, 1, 6, "BacktraceTicks")

    private val fovGroup = ValueGroup("FoV")
    private val fovModeValue = ListValue("Type", arrayOf("ServerRotation", "ClientRotation"), "ClientRotation", "FovMode")
    private val fovValue = FloatValue("FoV", 180f, 0f, 180f, "FoV")

    private val visualGroup = ValueGroup("Visual")
    private val visualFakeSharpValue = BoolValue("FakeSharp", true, "FakeSharp")
    private val visualParticles = IntegerValue("Particles", 1, 0, 10, "Particles")

    private val visualMarkGroup = ValueGroup("Mark")

    private val visualMarkTargetGroup = ValueGroup("Target")
    private val visualMarkTargetModeValue = ListValue("Mode", arrayOf("None", "Platform", "Box"), "Platform", "Mark.Target")
    private val visualMarkTargetEyePosValue = object : BoolValue("EyePosition", false)
    {
        override fun showCondition() = visualMarkTargetModeValue.get().equals("Platform", ignoreCase = true)
    }
    private val visualMarkTargetFadeSpeedValue = object : IntegerValue("FadeSpeed", 5, 1, 9)
    {
        override fun showCondition() = !visualMarkTargetModeValue.get().equals("None", ignoreCase = true)
    }

    private val visualMarkTargetColorGroup = object : ValueGroup("Color")
    {
        override fun showCondition() = !visualMarkTargetModeValue.get().equals("None", ignoreCase = true)
    }

    private val visualMarkTargetColorSuccessColorValue = RGBAColorValue("Success", 0, 255, 0, 70)
    private val visualMarkTargetColorMissColorValue = RGBAColorValue("Miss", 0, 0, 255, 70)
    private val visualMarkTargetColorFailedColorValue = RGBAColorValue("Failed", 255, 0, 0, 70)

    private val visualMarkRangeGroup = ValueGroup("Range")
    private val visualMarkRangeModeValue = ListValue("Mode", arrayOf("None", "AttackRange", "ExceptBlockRange", "All"), "AttackRange", "Mark-Range")
    private val visualMarkRangeLineWidthValue = FloatValue("LineWidth", 1f, 0.5f, 2f)
    private val visualMarkRangeAccuracyValue = FloatValue("Accuracy", 10F, 0.5F, 20F, "Mark-Range-Accuracy")
    private val visualMarkRangeFadeSpeedValue = IntegerValue("FadeSpeed", 5, 1, 9)
    private val visualMarkRangeAlphaFadeSpeedValue = IntegerValue("AlphaFadeSpeed", 5, 1, 9, "Visual.Mark.Range.FadeSpeed")

    private val visualMarkRangeColorGroup = object : ValueGroup("Color")
    {
        override fun showCondition() = !visualMarkRangeModeValue.get().equals("None", ignoreCase = true)
    }
    private val visualMarkRangeColorAttackValue = RGBColorValue("Attack", 0, 255, 0)
    private val visualMarkRangeColorThroughWallsAttackValue = RGBColorValue("ThroughWallsAttack", 200, 128, 0)

    private val visualMarkRangeColorAimValue = object : RGBColorValue("Aim", 255, 0, 0)
    {
        override fun showCondition() = !rotationMode.get().equals("Off", ignoreCase = true) && !visualMarkRangeModeValue.get().equals("AttackRange", ignoreCase = true)
    }
    private val visualMarkRangeColorSwingValue = object : RGBColorValue("Swing", 0, 0, 255)
    {
        override fun showCondition() = swingEnabledValue.get() && !visualMarkRangeModeValue.get().equals("AttackRange", ignoreCase = true)
    }

    private val visualMarkRangeColorBlockValue = object : RGBColorValue("Block", 255, 0, 255)
    {
        override fun showCondition() = !autoBlockValue.get().equals("Off", ignoreCase = true) && arrayOf("None", "AttackRange", "ExceptBlockRange").none { visualMarkRangeModeValue.get().equals(it, ignoreCase = true) }
    }
    private val visualMarkRangeColorInteractBlockValue = object : RGBColorValue("InteractBlock", 255, 64, 255)
    {
        override fun showCondition() = !autoBlockValue.get().equals("Off", ignoreCase = true) && arrayOf("None", "AttackRange", "ExceptBlockRange").none { visualMarkRangeModeValue.get().equals(it, ignoreCase = true) }
    }

    private val disableOnDeathValue = BoolValue("DisableOnDeath", true)

    /**
     * MODULE
     */

    // Target
    var target: EntityLivingBase? = null
    private var currentTarget: EntityLivingBase? = null
    private var hitable = false
    private val previouslySwitchedTargets = mutableSetOf<Int>()

    // Variables in below must only used to render visual marks
    private var lastTargetBB: AxisAlignedBB? = null
    private var lastTargetEyeHeight: Float = -1f

    private var lastTargetID: Int = -1

    // Attack delay
    private val attackTimer = MSTimer()
    private var attackDelay = 0L
    private var clicks = 0

    // Suspend killaura timer
    private val suspendTimer = MSTimer()
    private var suspend = 0L

    // Lock rotation timer
    private val lockRotationTimer = MSTimer()
    private var lockRotationDelay = 0L
    private var lockRotation: Rotation? = null

    // Ranges
    private var attackRange = 0f
    private var aimRange = 0f
    private var swingRange = 0f
    private var blockRange = 0f
    private var interactBlockRange = 0f

    private var comboReach = 0f

    private var lastYaw = 0f
    private var lastPitch = 0f

    private var predictX = 1F
    private var predictY = 1F
    private var predictZ = 1F

    // Container Delay
    private val containerOpenTimer = MSTimer()

    private var switchDelay = switchDelayValue.getRandomLong()
    private val switchDelayTimer = MSTimer()

    // Server-side block status
    var serverSideBlockingStatus: Boolean = false

    // Client-side(= visual) block status
    var clientSideBlockingStatus: Boolean = false

    var updateHitableDebug: Array<String>? = null
    var updateRotationsDebug: Array<String>? = null
    var startBlockingDebug: Array<String>? = null

    /**
     * Did last attack failed because of FailRate
     */
    private var missed = false

    private var failedToRotate = false

    /**
     * Hit-box of the current target
     */
    private val getHitbox: (Entity, Double) -> AxisAlignedBB = { target: Entity, expand: Double ->
        var bb = target.entityBoundingBox

        val collisionExpand = target.collisionBorderSize.toDouble()
        bb = bb.expand(collisionExpand, collisionExpand, collisionExpand)

        // Backtrace
        if (rotationBacktrackEnabledValue.get()) bb = LocationCache.getAABBBeforeNTicks(target.entityId, rotationBacktrackTicksValue.get(), bb)

        // Entity movement predict
        if (rotationPredictEnemyEnabledValue.get()) bb = bb.offset((target.posX - target.lastTickPosX) * predictX, (target.posY - target.lastTickPosY) * predictY, (target.posZ - target.lastTickPosZ) * predictZ)

        bb.expand(expand, expand, expand)
    }

    /**
     * Maximum attack range
     */
    private val maxAttackRange: Float
        get() = max(attackRange, rangeThroughWallsAttackValue.get()) + comboReach

    /**
     * Maximum target-search range
     */
    private val maxTargetRange: Float
        get() = max(aimRange, max(maxAttackRange, if (swingFakeSwingValue.get()) swingRange else 0f))

    /**
     * HUD Tag
     */
    override val tag: String
        get() = "${targetModeValue.get()}, ${DECIMALFORMAT_1.format(maxTargetRange)}, ${DECIMALFORMAT_1.format(maxAttackRange)}"

    /**
     * Is KillAura has target?
     */
    val hasTarget: Boolean
        get() = state && target != null

    /**
     * Target of auto-block
     */
    private var autoBlockTarget: EntityLivingBase? = null
    private var rangeMarks: List<Pair<Float, Int>>? = null
    private var easingRangeAndAlphas: ArrayList<Pair<Float, Float>>? = null

    private var easingMarkAlpha: Float = 0f

    init
    {
        comboReachGroup.addAll(comboReachEnabledValue, comboReachIncrementValue, comboReachLimitValue)
        rangeAttackGroup.addAll(rangeAttackOnGroundValue, rangeAttackOffGroundValue)
        rangeGroup.addAll(rangeAttackGroup, rangeThroughWallsAttackValue, rangeAimValue, rangeSprintReducementValue, comboReachGroup)
        swingGroup.addAll(swingEnabledValue, swingFakeSwingValue, swingRangeValue)
        targetGroup.addAll(targetPriorityValue, targetModeValue, targetLimitedMultiTargetsValue)
        interactAutoBlockGroup.addAll(interactAutoBlockEnabledValue, interactAutoBlockRangeValue)
        autoBlockGroup.addAll(autoBlockValue, autoBlockRangeValue, autoBlockRate, autoBlockHitableCheckValue, autoBlockHurtTimeCheckValue, autoBlockWallCheckValue, interactAutoBlockGroup)
        rayCastGroup.addAll(rayCastEnabledValue, rayCastSkipEnemyCheckValue, rayCastLivingOnlyValue, rayCastIncludeCollidedValue)
        bypassGroup.addAll(bypassKeepSprintValue, bypassAACValue, bypassMissChanceValue, bypassSuspendWhileConsumingValue, noInventoryGroup, rayCastGroup)
        noInventoryGroup.addAll(noInventoryAttackEnabledValue, noInventoryDelayValue)
        rotationSearchCenterGroup.addAll(rotationSearchCenterHitboxShrinkValue, rotationSearchCenterSensitivityValue)
        rotationJitterGroup.addAll(rotationJitterEnabledValue, rotationJitterYawRate, rotationJitterPitchRate, rotationJitterYawIntensityValue, rotationJitterPitchIntensityValue)
        rotationKeepRotationGroup.addAll(rotationKeepRotationEnabledValue, rotationKeepRotationTicks)
        rotationLockAfterTeleportGroup.addAll(rotationLockAfterTeleportEnabledValue, rotationLockAfterTeleportDelayValue)
        rotationStrafeGroup.addAll(rotationStrafeValue, rotationStrafeOnlyGroundValue)
        rotationPredictEnemyGroup.addAll(rotationPredictEnemyEnabledValue, rotationPredictEnemyIntensityValue)
        rotationPredictPlayerGroup.addAll(rotationPredictPlayerEnabledValue, rotationPredictPlayerIntensityValue)
        rotationPredictGroup.addAll(rotationPredictEnemyGroup, rotationPredictPlayerGroup)
        rotationBacktrackGroup.addAll(rotationBacktrackEnabledValue, rotationBacktrackTicksValue)
        rotationGroup.addAll(rotationMode, rotationLockValue, rotationLockExpandRangeValue, rotationSilentValue, rotationRandomCenterSizeValue, rotationSearchCenterGroup, rotationJitterGroup, rotationKeepRotationGroup, rotationLockAfterTeleportGroup, rotationAccelerationRatioValue, rotationTurnSpeedValue, rotationResetSpeedValue, rotationStrafeGroup, rotationPredictGroup, rotationBacktrackGroup)
        fovGroup.addAll(fovModeValue, fovValue)
        visualMarkTargetColorGroup.addAll(visualMarkTargetColorSuccessColorValue, visualMarkTargetColorMissColorValue, visualMarkTargetColorFailedColorValue)
        visualMarkTargetGroup.addAll(visualMarkTargetModeValue, visualMarkTargetEyePosValue, visualMarkTargetFadeSpeedValue, visualMarkTargetColorGroup)
        visualMarkRangeColorGroup.addAll(visualMarkRangeColorAttackValue, visualMarkRangeColorThroughWallsAttackValue, visualMarkRangeColorAimValue, visualMarkRangeColorSwingValue, visualMarkRangeColorBlockValue, visualMarkRangeColorInteractBlockValue)
        visualMarkRangeGroup.addAll(visualMarkRangeModeValue, visualMarkRangeLineWidthValue, visualMarkRangeAccuracyValue, visualMarkRangeFadeSpeedValue, visualMarkRangeColorGroup)
        visualMarkGroup.addAll(visualMarkTargetGroup, visualMarkRangeGroup)
        visualGroup.addAll(visualFakeSharpValue, visualParticles, visualMarkGroup)
    }

    /**
     * Enable kill aura module
     */
    override fun onEnable()
    {
        updateTarget(mc.theWorld ?: return, mc.thePlayer ?: return)
    }

    /**
     * Disable kill aura module
     */
    override fun onDisable()
    {
        target = null
        currentTarget = null
        hitable = false
        missed = false
        previouslySwitchedTargets.clear()
        attackTimer.reset()
        clicks = 0
        comboReach = 0.0F
        stopBlocking()
    }

    @EventTarget
    fun onWorldChange(@Suppress("UNUSED_PARAMETER") event: WorldEvent)
    {
        if (disableOnDeathValue.get())
        {
            state = false
            LiquidBounce.hud.addNotification(Notification(NotificationIcon.WARNING, "Disabled KillAura", "due world change", 1000L))
        }
    }

    @EventTarget
    fun onTick(@Suppress("UNUSED_PARAMETER") event: TickEvent)
    {
        if (mc.thePlayer == null || mc.theWorld == null)
        {
            state = false
            LiquidBounce.hud.addNotification(Notification(NotificationIcon.WARNING, "Disabled KillAura", "due world change", 1000L))
        }
    }

    /**
     * Motion event
     */
    @EventTarget
    fun onMotion(event: MotionEvent)
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        if (event.eventState == EventState.POST)
        {
            updateComboReach()

            target ?: return
            currentTarget ?: return

            // Update hitable
            updateHitable(theWorld, thePlayer)

            // Delayed-AutoBlock
            if (autoBlockValue.get().equals("AfterTick", true) && canAutoBlock(thePlayer)) startBlocking(thePlayer, currentTarget, interactAutoBlockEnabledValue.get() && hitable)

            return
        }
        else if (rotationStrafeValue.get().equals("Off", true)) update(theWorld, thePlayer)
    }

    /**
     * Strafe event
     */
    @EventTarget
    fun onStrafe(event: StrafeEvent)
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        if (rotationStrafeValue.get().equals("Off", true)) return

        update(theWorld, thePlayer)

        if (currentTarget != null && RotationUtils.targetRotation != null && (thePlayer.onGround || !rotationStrafeOnlyGroundValue.get()))
        {
            when (rotationStrafeValue.get().lowercase(Locale.getDefault()))
            {
                "strict" ->
                {
                    val (yaw, _) = RotationUtils.targetRotation ?: return
                    var strafe = event.strafe
                    var forward = event.forward
                    val friction = event.friction

                    var f = strafe * strafe + forward * forward

                    if (f >= 1.0E-4F)
                    {
                        f = sqrt(f)

                        if (f < 1.0F) f = 1.0F

                        f = friction / f
                        strafe *= f
                        forward *= f

                        val yawRadians = yaw.toRadians
                        val yawSin = yawRadians.sin
                        val yawCos = yawRadians.cos

                        thePlayer.motionX += strafe * yawCos - forward * yawSin
                        thePlayer.motionZ += forward * yawCos + strafe * yawSin
                    }
                    event.cancelEvent()
                }

                "silent" ->
                {
                    update(theWorld, thePlayer)

                    RotationUtils.targetRotation?.applyStrafeToPlayer(event)
                    event.cancelEvent()
                }
            }
        }
    }

    fun update(theWorld: World, thePlayer: EntityPlayer)
    {
        // CancelRun & NoInventory
        if (shouldCancelRun(thePlayer) || (noInventoryAttackEnabledValue.get() && (mc.currentScreen is GuiContainer || containerOpenTimer.hasTimePassed(noInventoryDelayValue.get().toLong())))) return

        // Update target
        updateTarget(theWorld, thePlayer)

        // Pre-AutoBlock
        if (autoBlockTarget != null && !autoBlockValue.get().equals("AfterTick", ignoreCase = true) && canAutoBlock(thePlayer) && (!autoBlockHitableCheckValue.get() || hitable)) startBlocking(thePlayer, autoBlockTarget, interactAutoBlockEnabledValue.get()) else if (canAutoBlock(thePlayer)) stopBlocking()

        // Target
        currentTarget = target ?: return
        if (!targetModeValue.get().equals("Switch", ignoreCase = true) && currentTarget.isEnemy(bypassAACValue.get())) target = currentTarget
    }

    /**
     * Update event
     */
    @EventTarget
    fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
    {
        val thePlayer = mc.thePlayer ?: return

        if (shouldCancelRun(thePlayer))
        {
            target = null
            currentTarget = null
            hitable = false
            comboReach = 0.0F
            stopBlocking()
            return
        }

        val screen = mc.currentScreen

        attackRange = (if (thePlayer.onGround) rangeAttackOnGroundValue else rangeAttackOffGroundValue).get()
        aimRange = rangeAimValue.get()
        swingRange = swingRangeValue.get()
        blockRange = autoBlockRangeValue.get()
        interactBlockRange = interactAutoBlockRangeValue.get()

        // Range mark
        val markRangeMode = visualMarkRangeModeValue.get().lowercase(Locale.getDefault())
        if (markRangeMode != "none")
        {
            val arr = arrayOfNulls<Pair<Float, Int>?>(6)

            val attackRangeOffset = (if (thePlayer.isSprinting) -rangeSprintReducementValue.get() else 0F) + comboReach
            arr[0] = attackRange + attackRangeOffset to visualMarkRangeColorAttackValue.get()

            val throughWallsRange = rangeThroughWallsAttackValue.get()
            if (throughWallsRange > 0) arr[1] = throughWallsRange + attackRangeOffset to visualMarkRangeColorThroughWallsAttackValue.get()

            arr[2] = aimRange to visualMarkRangeColorAimValue.get()

            if (swingFakeSwingValue.get()) arr[3] = swingRange to visualMarkRangeColorSwingValue.get()

            if (!autoBlockValue.get().equals("Off", ignoreCase = true))
            {
                arr[4] = blockRange to visualMarkRangeColorBlockValue.get()
                if (interactAutoBlockEnabledValue.get()) arr[5] = interactBlockRange to visualMarkRangeColorInteractBlockValue.get()
            }

            rangeMarks = arr.take(when (markRangeMode)
            {
                "attackrange" -> 2
                "exceptblockrange" -> 4
                else -> 6
            }).filterNotNull()
        }

        if (noInventoryAttackEnabledValue.get() && (screen is GuiContainer || containerOpenTimer.hasTimePassed(noInventoryDelayValue.get().toLong())))
        {
            target = null
            currentTarget = null
            hitable = false
            comboReach = 0.0F

            if (screen is GuiContainer) containerOpenTimer.reset()

            return
        }

        target ?: return

        if (target != null && currentTarget != null)
        {
            while (clicks > 0)
            {
                runAttack()
                clicks--
            }
        }
    }

    /**
     * Render event
     */
    @EventTarget(ignoreCondition = true)
    fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
    {
        var cancelRun = !state

        if (!cancelRun && shouldCancelRun(mc.thePlayer ?: return))
        {
            target = null
            currentTarget = null
            hitable = false
            comboReach = 0.0F
            stopBlocking()
            cancelRun = true
        }

        val screen = mc.currentScreen

        // NoInventory
        if (!cancelRun && noInventoryAttackEnabledValue.get() && (screen is GuiContainer || containerOpenTimer.hasTimePassed(noInventoryDelayValue.get().toLong())))
        {
            target = null
            currentTarget = null
            hitable = false
            comboReach = 0.0F
            if (screen is GuiContainer) containerOpenTimer.reset()
            cancelRun = true
        }

        if ((state || easingRangeAndAlphas != null) && !visualMarkRangeModeValue.get().equals("None", ignoreCase = true))
        {
            val lineWidth = visualMarkRangeLineWidthValue.get()
            val accuracy = visualMarkRangeAccuracyValue.get()

            rangeMarks?.let { rangeMarks ->
                easingRangeAndAlphas?.let { easingRangeMarks ->
                    for (i in rangeMarks.indices)
                    {
                        val (range, alpha) = easingRangeMarks[i]
                        rangeMarks[i].let { (originalRange, color) ->
                            if (easingRangeMarks.size <= i) easingRangeMarks.add(originalRange to 1f)

                            GL11.glPushMatrix()
                            RenderUtils.drawRadius(range, accuracy, lineWidth, ColorUtils.multiplyAlphaChannel(color, alpha))
                            GL11.glPopMatrix()

                            easingRangeMarks[i] = easeOutCubic(range, if (cancelRun) 0f else originalRange, visualMarkRangeFadeSpeedValue.get()) to easeOutCubic(alpha, if (cancelRun) 0f else 1f, visualMarkRangeAlphaFadeSpeedValue.get())
                            if (cancelRun && easingRangeMarks[i].second <= 0.1f) easingRangeMarks[i] = originalRange + 1 to 0f
                        }
                    }

                    // GC the list after all ranges are successfully eased-out
                    if (cancelRun && easingRangeMarks.all { it.second == 0f }) easingRangeAndAlphas = null
                } ?: run { if (!cancelRun) easingRangeAndAlphas = rangeMarks.mapTo(ArrayList()) { it.first + 1f to 0f } }
            }
        }

        // Mark
        if (state || lastTargetBB != null)
        {
            val markMode = visualMarkTargetModeValue.get().lowercase(Locale.getDefault())
            if (markMode != "none" && !targetModeValue.get().equals("Multi", ignoreCase = true))
            {
                (target?.let { target ->
                    val targetEntityId = target.entityId

                    val partialTicks = event.partialTicks

                    var bb = target.entityBoundingBox
                    bb = if (rotationBacktrackEnabledValue.get())
                    {
                        val backtraceTicks = rotationBacktrackTicksValue.get()

                        val backtrack = LocationCache.getAABBBeforeNTicks(targetEntityId, backtraceTicks, bb)
                        val prevBacktrace = LocationCache.getAABBBeforeNTicks(targetEntityId, backtraceTicks + 1, backtrack)

                        AxisAlignedBB(prevBacktrace.minX + (backtrack.minX - prevBacktrace.minX) * partialTicks, prevBacktrace.minY + (backtrack.minY - prevBacktrace.minY) * partialTicks, prevBacktrace.minZ + (backtrack.minZ - prevBacktrace.minZ) * partialTicks, prevBacktrace.maxX + (backtrack.maxX - prevBacktrace.maxX) * partialTicks, prevBacktrace.maxY + (backtrack.maxY - prevBacktrace.maxY) * partialTicks, prevBacktrace.maxZ + (backtrack.maxZ - prevBacktrace.maxZ) * partialTicks)
                    }
                    else
                    {
                        val posX = target.posX
                        val posY = target.posY
                        val posZ = target.posZ

                        val lastTickPosX = target.lastTickPosX
                        val lastTickPosY = target.lastTickPosY
                        val lastTickPosZ = target.lastTickPosZ

                        val x = lastTickPosX + (posX - lastTickPosX) * partialTicks
                        val y = lastTickPosY + (posY - lastTickPosY) * partialTicks
                        val z = lastTickPosZ + (posZ - lastTickPosZ) * partialTicks

                        bb.offset(-posX, -posY, -posZ).offset(x, y, z)
                    }

                    // Entity movement predict
                    if (rotationPredictEnemyEnabledValue.get())
                    {
                        val xPredict = (target.posX - target.lastTickPosX) * predictX
                        val yPredict = (target.posY - target.lastTickPosY) * predictY
                        val zPredict = (target.posZ - target.lastTickPosZ) * predictZ

                        bb = bb.offset(xPredict, yPredict, zPredict)
                    }

                    easingMarkAlpha = easeOutCubic(easingMarkAlpha, 1f, visualMarkTargetFadeSpeedValue.get())

                    lastTargetEyeHeight = target.eyeHeight

                    bb.also { lastTargetBB = it }
                } ?: lastTargetBB?.let {
                    easingMarkAlpha = easeOutCubic(easingMarkAlpha, 0f, visualMarkTargetFadeSpeedValue.get())
                    if (easingMarkAlpha > 0.1f) it
                    else
                    {
                        easingMarkAlpha = 0f
                        if (cancelRun) lastTargetBB = null
                        null
                    }
                })?.let {
                    val renderManager = mc.renderManager
                    it.offset(-renderManager.renderPosX, -renderManager.renderPosY, -renderManager.renderPosZ)
                }?.let { targetBB ->
                    val markColor = ColorUtils.multiplyAlphaChannel(when
                    {
                        missed -> visualMarkTargetColorMissColorValue.get()
                        hitable -> visualMarkTargetColorSuccessColorValue.get()
                        else -> visualMarkTargetColorFailedColorValue.get()
                    }, easingMarkAlpha)

                    val eyePos = visualMarkTargetEyePosValue.get()
                    when (markMode)
                    {
                        "platform" ->
                        {
                            val eyeHeight = targetBB.minY + lastTargetEyeHeight
                            AxisAlignedBB(targetBB.minX, if (eyePos) eyeHeight - 0.03f else targetBB.maxY + 0.2, targetBB.minZ, targetBB.maxX, if (eyePos) eyeHeight + 0.03f else targetBB.maxY + 0.26, targetBB.maxZ)
                        }

                        "box" -> AxisAlignedBB(targetBB.minX, targetBB.minY, targetBB.minZ, targetBB.maxX, targetBB.maxY, targetBB.maxZ)
                        else -> null
                    }?.let { RenderUtils.drawAxisAlignedBB(it, markColor) }
                }
            }
        }

        if (cancelRun || target == null) return

        if ((currentTarget ?: return).hurtTime <= hurtTimeValue.get() && attackTimer.hasTimePassed(attackDelay))
        {
            clicks++
            attackTimer.reset()
            attackDelay = cpsValue.getRandomClickDelay()
        }
    }

    @EventTarget
    fun onRender2D(@Suppress("UNUSED_PARAMETER") event: Render2DEvent)
    {
        if (fovValue.get() < 180) RenderUtils.drawFoVCircle(fovValue.get())
    }

    /**
     * Handle entity move
     */
    @EventTarget
    fun onEntityMove(event: EntityMovementEvent)
    {
        val movedEntity = event.movedEntity

        updateComboReach()

        if (target == null || movedEntity != currentTarget) return

        updateHitable(mc.theWorld ?: return, mc.thePlayer ?: return)
    }

    @EventTarget
    fun onPacket(event: PacketEvent)
    {
        val packet = event.packet
        if (packet is S08PacketPlayerPosLook)
        {
            val thePlayer = mc.thePlayer ?: return

            if (rotationLockAfterTeleportEnabledValue.get())
            {
                lockRotation = Rotation(packet.yaw, packet.pitch)
                lockRotationTimer.reset()
                lockRotationDelay = rotationLockAfterTeleportDelayValue.getRandomLong()

                if (rotationSilentValue.get()) RotationUtils.setTargetRotation(lockRotation, 0) else lockRotation?.applyRotationToPlayer(thePlayer)
            }
        }
    }

    /**
     * Attack enemy
     */
    private fun runAttack()
    {
        val thePlayer = mc.thePlayer ?: return
        val theWorld = mc.theWorld ?: return
        val netHandler = mc.netHandler

        val theTarget = target ?: return
        val theCurrentTarget = currentTarget ?: return

        val distance = thePlayer.getDistanceToEntityBox(theCurrentTarget)

        // Settings
        val missChance = bypassMissChanceValue.get()
        val aac = bypassAACValue.get()

        val openInventory = aac && mc.currentScreen is GuiContainer
        val limitedMultiTargets = targetLimitedMultiTargetsValue.get()

        // FailRate
        missed = missChance > 0 && Random.nextInt(100) <= missChance

        // Close inventory when open
        if (openInventory) netHandler.addToSendQueue(C0DPacketCloseWindow())

        // Check is not hitable or check failrate
        val fakeAttack = !hitable || missed || failedToRotate

        if (fakeAttack)
        {
            if (swingEnabledValue.get() && distance <= swingRange && (missed || swingFakeSwingValue.get()))
            {
                val isBlocking = thePlayer.isBlocking

                // Stop Blocking before FAKE attack
                if (isBlocking || serverSideBlockingStatus)
                {
                    netHandler.addToSendQueue(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                    serverSideBlockingStatus = false
                    clientSideBlockingStatus = false
                }

                // FAKE Swing (to bypass hit/miss rate checks)
                thePlayer.swingItem()

                // Start blocking after FAKE attack
                if ((isBlocking || (canAutoBlock(thePlayer) && distance <= blockRange)) && !autoBlockValue.get().equals("AfterTick", true)) startBlocking(thePlayer, theCurrentTarget, interactAutoBlockEnabledValue.get())
            }
        }
        else
        {
            if (comboReachEnabledValue.get()) comboReach = (comboReach + comboReachIncrementValue.get()).coerceAtMost(comboReachLimitValue.get())

            // Attack
            if (targetModeValue.get().equals("Multi", ignoreCase = true))
            {
                var targets = 0

                run {
                    theWorld.getEntitiesInRadius(thePlayer, maxAttackRange + 2.0).filterIsInstance<EntityLivingBase>().filter { it.isEnemy(aac) }.filter { thePlayer.getDistanceToEntityBox(it) <= getAttackRange(thePlayer, it) }.forEach { entity ->
                        attackEntity(entity)
                        targets += 1

                        if (limitedMultiTargets != 0 && targets >= limitedMultiTargets) return@run
                    }
                }
            }
            else attackEntity(theCurrentTarget)
        }

        if (switchDelayTimer.hasTimePassed(switchDelay))
        {
            previouslySwitchedTargets.add(if (aac) theTarget.entityId else theCurrentTarget.entityId)

            switchDelayTimer.reset()
            switchDelay = switchDelayValue.getRandomLong()
        }

        if (!fakeAttack && theTarget == theCurrentTarget)
        {
            lastTargetID = theTarget.entityId
            target = null
        }

        // Open inventory
        if (openInventory) netHandler.addToSendQueue(createOpenInventoryPacket())
    }

    /**
     * Update current target
     */
    private fun updateTarget(theWorld: World, thePlayer: EntityPlayer)
    {
        if (target != null) lastTargetID = target?.entityId ?: -1

        // Reset fixed target to null
        target = null

        // Settings
        val hurtTime = hurtTimeValue.get()
        val fov = fovValue.get()
        val switchMode = targetModeValue.get().equals("Switch", ignoreCase = true)
        val playerPredict = rotationPredictPlayerEnabledValue.get()
        val playerPredictSize = RotationUtils.MinMaxPair(rotationPredictPlayerIntensityValue.getMin(), rotationPredictPlayerIntensityValue.getMax())

        // Find possible targets
        val targets = mutableListOf<EntityLivingBase>()
        val abTargets = mutableListOf<EntityLivingBase>()

        val aac = bypassAACValue.get()
        val fovMode = fovModeValue.get()

        val autoBlockHurtTimeCheck = autoBlockHurtTimeCheckValue.get()
        val smartBlock = autoBlockWallCheckValue.get()

        val entityList = theWorld.getEntitiesInRadius(thePlayer, maxTargetRange + 2.0).filterIsInstance<EntityLivingBase>().filter { it.isEnemy(aac) }.filterNot { switchMode && previouslySwitchedTargets.contains(it.entityId) }.run { if (fov < 180f) filter { (if (fovMode.equals("ServerRotation", ignoreCase = true)) RotationUtils.getServerRotationDifference(thePlayer, it, playerPredict, playerPredictSize) else RotationUtils.getClientRotationDifference(thePlayer, it, playerPredict, playerPredictSize)) <= fov } else this }.map { it to thePlayer.getDistanceToEntityBox(it) }
        entityList.forEach { (entity, distance) ->
            val entityHurtTime = entity.hurtTime

            if (distance <= blockRange && (!autoBlockHurtTimeCheck || entityHurtTime <= hurtTime) && (!smartBlock || RotationUtils.isVisible(theWorld, thePlayer, RotationUtils.getCenter(entity.entityBoundingBox))) /* Simple wall check */) abTargets.add(entity)
            if (distance <= getAttackRange(thePlayer, entity) && entityHurtTime <= hurtTime) targets.add(entity)
        }

        // If there is no attackable entities found, search about pre-aimable entities and pre-swingable entities instead.
        if (targets.isEmpty()) entityList.filter { it.second <= maxTargetRange }.forEach { targets.add(it.first) }

        val checkIsClientTarget = { entity: Entity -> if (entity.isClientTarget()) -1000000.0 else 0.0 }

        // Sort targets by priority
        when (targetPriorityValue.get().lowercase(Locale.getDefault()))
        {
            "distance" ->
            {
                // Sort by distance
                val selector = { entity: Entity -> thePlayer.getDistanceToEntityBox(entity) + checkIsClientTarget(entity) }

                targets.sortBy(selector)
                abTargets.sortBy(selector)
            }

            "health" ->
            {
                // Sort by health
                val selector = { entity: EntityLivingBase -> entity.health + checkIsClientTarget(entity) }

                targets.sortBy(selector)
                abTargets.sortBy(selector)
            }

            "serverdirection" ->
            {
                // Sort by server-sided rotation difference
                val selector = { entity: EntityLivingBase -> RotationUtils.getServerRotationDifference(thePlayer, entity, playerPredict, playerPredictSize) + checkIsClientTarget(entity) }

                targets.sortBy(selector)
                abTargets.sortBy(selector)
            }

            "clientdirection" ->
            {
                // Sort by client-sided rotation difference
                val selector = { entity: EntityLivingBase -> RotationUtils.getClientRotationDifference(thePlayer, entity, playerPredict, playerPredictSize) + checkIsClientTarget(entity) }

                targets.sortBy(selector)
                abTargets.sortBy(selector)
            }

            "livingtime" ->
            {
                // Sort by existence
                val selector = { entity: EntityLivingBase -> -entity.ticksExisted + checkIsClientTarget(entity) }

                targets.sortBy(selector)
                abTargets.sortBy(selector)
            }
        }

        autoBlockTarget = abTargets.firstOrNull()

        // Find best target
        targets.firstOrNull {
            // Update rotations to current target
            val distance = thePlayer.getDistanceToEntityBox(it)
            distance <= aimRange && updateRotations(theWorld, thePlayer, it, distance <= attackRange)
        }?.let { entity ->
            // Set target to current entity
            target = entity

            if (entity.entityId != lastTargetID) comboReach = 0f

            return@updateTarget
        }

        // Cleanup previouslySwitchedTargets when no target found and try again
        if (previouslySwitchedTargets.isNotEmpty())
        {
            previouslySwitchedTargets.clear()
            updateTarget(theWorld, thePlayer)
        }
    }

    /**
     * Attack [entity]
     */
    private fun attackEntity(entity: EntityLivingBase)
    {
        val thePlayer = mc.thePlayer ?: return
        val netHandler = mc.netHandler

        val swing = swingEnabledValue.get()

        // Stop blocking
        if (thePlayer.isBlocking || serverSideBlockingStatus)
        {
            netHandler.addToSendQueue(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
            serverSideBlockingStatus = false
            clientSideBlockingStatus = false
        }

        // Call attack event
        LiquidBounce.eventManager.callEvent(AttackEvent(entity, Vec3(thePlayer.posX, thePlayer.posY, thePlayer.posZ)))

        // Attack target
        if (swing) thePlayer.swingItem()

        netHandler.addToSendQueue(C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK))

        CPSCounter.registerClick(CPSCounter.MouseButton.LEFT)

        if (!bypassKeepSprintValue.get() && mc.playerController.currentGameType != WorldSettings.GameType.SPECTATOR) thePlayer.attackTargetEntityWithCurrentItem(entity)

        // Extra critical effects
        val criticals = LiquidBounce.moduleManager[Criticals::class.java] as Criticals

        val crackSize = visualParticles.get()
        if (crackSize > 0) repeat(crackSize) {
            val target = target ?: return@attackEntity

            // Critical Effect
            if (thePlayer.fallDistance > 0F && !thePlayer.onGround && !thePlayer.isOnLadder && !thePlayer.isInWater && !thePlayer.isPotionActive(Potion.blindness.id) && thePlayer.ridingEntity == null || criticals.state && criticals.canCritical(thePlayer)) thePlayer.onCriticalHit(target)

            // Enchant Effect
            if (EnchantmentHelper.getModifierForCreature(thePlayer.heldItem, target.creatureAttribute) > 0.0f || visualFakeSharpValue.get()) thePlayer.onEnchantmentCritical(target)
        }

        // Start blocking after attack
        if ((thePlayer.isBlocking || (canAutoBlock(thePlayer) && thePlayer.getDistanceToEntityBox(entity) <= blockRange)) && !autoBlockValue.get().equals("AfterTick", true)) startBlocking(thePlayer, entity, interactAutoBlockEnabledValue.get())
    }

    /**
     * Update killaura rotations to enemy
     */
    private fun updateRotations(theWorld: World, thePlayer: EntityPlayer, entity: Entity, isAttackRotation: Boolean): Boolean
    {
        val predictEnemy = rotationPredictEnemyEnabledValue.get()
        if (predictEnemy)
        {
            predictX = rotationPredictEnemyIntensityValue.getRandom()
            predictY = rotationPredictEnemyIntensityValue.getRandom()
            predictZ = rotationPredictEnemyIntensityValue.getRandom()
        }

        val targetBox = getHitbox(entity, 0.0)

        val jitter = rotationJitterEnabledValue.get()

        // Jitter
        val jitterData = if (jitter) RotationUtils.JitterData(rotationJitterYawRate.get(), rotationJitterPitchRate.get(), rotationJitterYawIntensityValue.getMin(), rotationJitterYawIntensityValue.getMax(), rotationJitterPitchIntensityValue.getMin(), rotationJitterPitchIntensityValue.getMax()) else null

        var flags = 0

        val rotationMode = rotationMode.get().lowercase(Locale.getDefault())

        // Apply rotation mode to flags
        flags = flags or when (rotationMode)
        {
            "lockcenter" -> RotationUtils.LOCK_CENTER
            "outborder" -> if (!attackTimer.hasTimePassed(attackDelay shr 1)) RotationUtils.OUT_BORDER else RotationUtils.RANDOM_CENTER
            "randomcenter" -> RotationUtils.RANDOM_CENTER
            else -> 0
        }

        val predictPlayer = rotationPredictPlayerEnabledValue.get()

        if (jitter && (thePlayer.getDistanceToEntityBox(entity) <= max(maxAttackRange, if (swingFakeSwingValue.get()) swingRange else Float.MIN_VALUE))) flags = flags or RotationUtils.JITTER
        if (predictPlayer) flags = flags or RotationUtils.PLAYER_PREDICT
        if (thePlayer.getDistanceToEntityBox(entity) <= rangeThroughWallsAttackValue.get()) flags = flags or RotationUtils.SKIP_VISIBLE_CHECK

        failedToRotate = false

        val searchCenter = { distance: Float, distanceOutOfRangeCallback: (() -> Unit)? -> RotationUtils.searchCenter(theWorld, thePlayer, targetBox, flags, jitterData, RotationUtils.MinMaxPair(rotationPredictPlayerIntensityValue.getMin(), rotationPredictPlayerIntensityValue.getMax()), distance, rotationSearchCenterHitboxShrinkValue.get().toDouble(), rotationSearchCenterSensitivityValue.get(), rotationRandomCenterSizeValue.get().toDouble(), distanceOutOfRangeCallback) }

        // Search
        var fallBackRotation: VecRotation? = null
        var useFallback = false
        val rotation = if (rotationLockAfterTeleportEnabledValue.get() && lockRotation != null && !lockRotationTimer.hasTimePassed(lockRotationDelay)) lockRotation!!
        else if (!rotationLockValue.get() && RotationUtils.isFaced(theWorld, thePlayer, entity, aimRange.toDouble(), 3.0) { getHitbox(entity, rotationLockExpandRangeValue.get().toDouble()) }) Rotation(lastYaw, lastPitch)
        else (searchCenter(if (isAttackRotation) attackRange else aimRange) {
            // Because of '.getDistanceToEntityBox()' is not perfect. (searchCenter() >>> 넘사벽 >>> getDistanceToEntityBox())
            failedToRotate = true

            // TODO: Make better fallback
            fallBackRotation = searchCenter(aimRange, null)
        } ?: run {
            useFallback = true
            fallBackRotation
        } ?: return false).rotation

        lastYaw = rotation.yaw
        lastPitch = rotation.pitch

        if (rotationMode.equals("Off", ignoreCase = true))
        {
            updateRotationsDebug = arrayOf("state".equalTo("DISABLED", "\u00A74"), "reason" equalTo "Rotation is turned off".withParentheses("\u00A7c"))
            return true
        }

        if (rotationTurnSpeedValue.getMax() <= 0F)
        {
            updateRotationsDebug = arrayOf("state".equalTo("DISABLED", "\u00A74"), "reason" equalTo "TurnSpeed is zero or negative".withParentheses("\u00A7c"))
            return true
        }

        // Limit TurnSpeed
        val turnSpeed = rotationTurnSpeedValue.getRandomStrict()

        // Acceleration
        val acceleration = rotationAccelerationRatioValue.getRandomStrict()

        val limitedRotation = RotationUtils.limitAngleChange(RotationUtils.serverRotation, rotation, turnSpeed, acceleration)

        lastYaw = limitedRotation.yaw
        lastPitch = limitedRotation.pitch

        val commonDebug = arrayOf(

            "state".equalTo("SUCCESS", "\u00A7a"), // rotationResult
            "fallback" equalTo useFallback, // useFallback
            "jitter" equalTo (flags and RotationUtils.JITTER != 0), // jitter
            "skipVisibleChecks" equalTo (flags and RotationUtils.SKIP_VISIBLE_CHECK != 0) // skipVisibleCheck

        )

        if (rotationSilentValue.get())
        {
            val keepLength = if (rotationKeepRotationEnabledValue.get() && rotationKeepRotationTicks.getMax() > 0) rotationKeepRotationTicks.getRandom() else 0

            updateRotationsDebug = arrayOf(*commonDebug, "applicationMode" equalTo "silent", "keepTicks" equalTo keepLength)

            RotationUtils.setTargetRotation(limitedRotation, keepLength)
        }
        else
        {
            updateRotationsDebug = arrayOf(*commonDebug, "applicationMode" equalTo "direct")

            limitedRotation.applyRotationToPlayer(thePlayer)
        }

        val maxResetSpeed = rotationResetSpeedValue.getMax().coerceAtLeast(10F)
        val minResetSpeed = rotationResetSpeedValue.getMin().coerceAtLeast(10F)
        if (maxResetSpeed < 180) RotationUtils.setNextResetTurnSpeed(minResetSpeed, maxResetSpeed)

        return true
    }

    private fun updateComboReach()
    {
        if (target == null || currentTarget == null || !hitable) comboReach = 0f
    }

    /**
     * Check if enemy is hitable with current rotations
     */
    private fun updateHitable(theWorld: World, thePlayer: Entity)
    {
        val currentTarget = currentTarget
        val reach = min(maxAttackRange.toDouble(), thePlayer.getDistanceToEntityBox(target ?: return) + 1)

        val updateHitableByRange = {
            hitable = currentTarget != null && thePlayer.getDistanceToEntityBox(currentTarget) <= reach
            arrayOf("raycast" equalTo false, "rangeCheck" equalTo hitable)
        }

        if (rotationMode.get().equals("Off", ignoreCase = true))
        {
            updateHitableDebug = arrayOf(*updateHitableByRange(), "reason" equalTo "Rotation is turned off".withParentheses("\u00A7c"))
            return
        }

        if (rotationTurnSpeedValue.getMax() <= 0F)
        {
            updateHitableDebug = arrayOf(*updateHitableByRange(), "reason" equalTo "TurnSpeed is zero or negative".withParentheses("\u00A7c"))
            return
        }

        if (targetModeValue.get().equals("Multi", ignoreCase = true))
        {
            updateHitableDebug = arrayOf(*updateHitableByRange(), "reason" equalTo "MultiAura".withParentheses("\u00A7c"))
            return
        }

        val aac = bypassAACValue.get()
        val livingOnly = rayCastLivingOnlyValue.get()
        val skipEnemyCheck = rayCastSkipEnemyCheckValue.get()
        val includeCollidedWithTarget = rayCastIncludeCollidedValue.get()
        val bbGetter: (Entity) -> AxisAlignedBB = { getHitbox(it, if (rotationLockValue.get()) 0.0 else rotationLockExpandRangeValue.get().toDouble()) }

        if (rayCastEnabledValue.get())
        {
            val distanceToTarget = currentTarget?.let(thePlayer::getDistanceToEntityBox)
            val raycastedEntity = theWorld.raycastEntity(thePlayer, reach + 1.0, lastYaw, lastPitch, 3.0, bbGetter) { entity -> entity != null && (!livingOnly || (entity is EntityLivingBase && entity !is EntityArmorStand)) && (skipEnemyCheck || entity.isEnemy(aac) || includeCollidedWithTarget && theWorld.getEntitiesWithinAABBExcludingEntity(entity, entity.entityBoundingBox).isNotEmpty()) }
            val distanceToRaycasted = raycastedEntity?.let(thePlayer::getDistanceToEntityBox)

            if (raycastedEntity != null && raycastedEntity is EntityLivingBase && (LiquidBounce.moduleManager[NoFriends::class.java].state || raycastedEntity !is EntityPlayer || !raycastedEntity.isClientFriend())) this.currentTarget = raycastedEntity

            updateHitableDebug = if (distanceToTarget != null)
            {
                if (distanceToRaycasted != null)
                {
                    if (currentTarget != raycastedEntity) arrayOf("raycast" equalTo true, "result" equalTo "\u00A7aSUCCESS", "from" equalTo listOf("name".equalTo(currentTarget.name, "\u00A7e\u00A7l"), "id".equalTo(currentTarget.entityId, "\u00A7e")).serialize().withParentheses("\u00A78"), "to" equalTo listOf("name".equalTo(raycastedEntity.name, "\u00A7e\u00A7l"), "id".equalTo(raycastedEntity.entityId, "\u00A7e")).serialize().withParentheses("\u00A78"), "distance" equalTo DECIMALFORMAT_6.format(distanceToRaycasted - distanceToTarget))
                    else arrayOf("raycast" equalTo true, "result" equalTo "\u00A7eEQUAL", "reason" equalTo "currentTarget = raycastedTarget".withParentheses("\u00A7e"))
                }
                else arrayOf("raycast" equalTo false, "reason" equalTo "raycastedTarget is null".withParentheses("\u00A7c"))
            }
            else arrayOf("raycast" equalTo false, "reason" equalTo "currentTarget is null".withParentheses("\u00A7c"))

            hitable = (distanceToTarget == null || distanceToTarget <= reach) && (distanceToRaycasted == null || distanceToRaycasted <= reach) && this.currentTarget == raycastedEntity
        }
        else
        {
            hitable = if (currentTarget != null)
            {
                val faced = theWorld.raycastEntity(thePlayer, reach, lastYaw, lastPitch, 3.0, bbGetter) { entity -> currentTarget == entity } != null // RotationUtils.isFaced(theWorld, thePlayer, currentTarget, reach, bbGetter)
                updateHitableDebug = arrayOf("raycast" equalTo false, "faced" equalTo faced)
                faced
            }
            else
            {
                updateHitableDebug = arrayOf("raycast" equalTo false, "faced" equalTo "currentTarget is null")
                false
            }
        }
    }

    /**
     * Start blocking
     */
    private fun startBlocking(thePlayer: Entity, interactEntity: Entity?, interact: Boolean)
    {
        val autoBlockMode = autoBlockValue.get()
        val blockRate = autoBlockRate.get()

        // BlockRate check
        if (blockRate <= 0 || Random.nextInt(100) > blockRate) return

        val visual = !autoBlockMode.equals("Off", true) // Fake, Packet, AfterTick
        val packet = visual && !autoBlockMode.equals("Fake", true) // Packet, AfterTick

        if (packet && !serverSideBlockingStatus)
        {
            val netHandler = mc.netHandler

            // Interact block
            if (interact && interactEntity != null)
            {
                val positionEye = thePlayer.getPositionEyes(1F)

                val boundingBox = getHitbox(interactEntity, 0.0)

                val (yaw, pitch) = RotationUtils.targetRotation ?: RotationUtils.clientRotation
                val yawRadians = yaw.toRadians
                val pitchRadians = pitch.toRadians

                val yawCos = (-yawRadians - PI).cos
                val yawSin = (-yawRadians - PI).sin
                val pitchSin = (-pitchRadians).sin
                val pitchCos = -(-pitchRadians).cos

                val range = min(interactBlockRange.toDouble(), thePlayer.getDistanceToEntityBox(interactEntity)) + 1
                val lookAt = positionEye.plus(yawSin * pitchCos * range, pitchSin * range, yawCos * pitchCos * range)

                val movingObject = boundingBox.calculateIntercept(positionEye, lookAt)
                startBlockingDebug = if (movingObject != null && movingObject.typeOfHit != MovingObjectPosition.MovingObjectType.MISS)
                {
                    val hitVec = movingObject.hitVec

                    netHandler.addToSendQueue(C02PacketUseEntity(interactEntity, Vec3(hitVec.xCoord - interactEntity.posX, hitVec.yCoord - interactEntity.posY, hitVec.zCoord - interactEntity.posZ)))
                    netHandler.addToSendQueue(C02PacketUseEntity(interactEntity, C02PacketUseEntity.Action.INTERACT))

                    if (movingObject.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) arrayOf("result".equalTo("BLOCK HIT", "\u00A7e"), "blockPos" equalTo "\u00A7e${movingObject.blockPos}", "blockSide" equalTo movingObject.sideHit, "hitVec" equalTo hitVec)
                    else arrayOf("result".equalTo("ENTITY HIT", "\u00A7a"), "name" equalTo "\u00A7e${movingObject.entityHit?.name}", "dispName" equalTo movingObject.entityHit?.displayName?.formattedText, "hitVec" equalTo hitVec)
                }
                else arrayOf("result".equalTo("FAILED", "\u00A7c"), "reason" equalTo "raytraceResult = null".withParentheses("\u00A74"))
            }

            netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, (mc.thePlayer ?: return).inventory.getCurrentItem(), 0.0F, 0.0F, 0.0F))
            serverSideBlockingStatus = true
        }

        if (!clientSideBlockingStatus && visual) clientSideBlockingStatus = true
    }

    /**
     * Stop blocking
     */
    private fun stopBlocking()
    {
        if (serverSideBlockingStatus)
        {
            mc.netHandler.addToSendQueue(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
            serverSideBlockingStatus = false
        }
        clientSideBlockingStatus = false
    }

    /**
     * Check if run should be cancelled
     */
    private fun shouldCancelRun(thePlayer: EntityPlayer): Boolean
    {
        val moduleManager = LiquidBounce.moduleManager

        val shouldDisableOnDeath = thePlayer.isSpectator || !thePlayer.isAlive(false)

        if (shouldDisableOnDeath && disableOnDeathValue.get())
        {
            state = false
            LiquidBounce.hud.addNotification(Notification(NotificationIcon.WARNING, "Disabled KillAura", "due player death", 1000L))
        }

        return shouldDisableOnDeath || (bypassSuspendWhileConsumingValue.get() && thePlayer.isUsingItem && thePlayer.heldItem == thePlayer.itemInUse && (thePlayer.heldItem?.item is ItemFood || thePlayer.heldItem?.item is ItemPotion)) || !suspendTimer.hasTimePassed(suspend) || (moduleManager[Blink::class.java] as Blink).state || moduleManager[FreeCam::class.java].state
    }

    /**
     * Check if player is able to block
     */
    private fun canAutoBlock(thePlayer: EntityPlayer): Boolean = thePlayer.heldItem != null && thePlayer.heldItem?.item is ItemSword

    private fun getAttackRange(thePlayer: Entity, entity: Entity): Float
    {
        val throughWallsRange = rangeThroughWallsAttackValue.get()
        return (if (thePlayer.getDistanceToEntityBox(entity) >= throughWallsRange) attackRange else throughWallsRange) - (if (thePlayer.isSprinting) rangeSprintReducementValue.get() else 0F) + comboReach
    }

    fun suspend(time: Long)
    {
        if (time <= 0) return
        suspend = time
        suspendTimer.reset()
    }
}
