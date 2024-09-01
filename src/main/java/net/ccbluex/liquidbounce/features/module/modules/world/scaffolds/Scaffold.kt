/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffolds

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.RotationUtils.getRotationDifference
import net.ccbluex.liquidbounce.utils.RotationUtils.getVectorForRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.canBeClicked
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isReplaceable
import net.ccbluex.liquidbounce.utils.block.PlaceInfo
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverSlot
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timing.DelayTimer
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.timing.TickDelayTimer
import net.ccbluex.liquidbounce.utils.timing.TimeUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.DeadBushBlock
import net.minecraft.client.option.GameOptions
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.util.*
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.WorldSettings
import net.minecraftforge.event.ForgeEventFactory
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color
import javax.vecmath.Color3f
import kotlin.math.*

@Suppress("unused")
object Scaffold : Module("Scaffold", Category.WORLD, Keyboard.KEY_I, hideModule = false) {

    /**
     * TOWER MODES & SETTINGS
     */

    // -->

    private val towerMode by Tower.towerModeValues
    private val stopWhenBlockAbove by Tower.stopWhenBlockAboveValues
    private val onJump by Tower.onJumpValues
    private val notOnMove by Tower.notOnMoveValues
    private val matrix by Tower.matrixValues
    private val placeMode by Tower.placeModeValues
    private val jumpMotion by Tower.jumpMotionValues
    private val jumpDelay by Tower.jumpDelayValues
    private val constantMotion by Tower.constantMotionValues
    private val constantMotionJumpGround by Tower.constantMotionJumpGroundValues
    private val constantMotionJumpPacket by Tower.constantMotionJumpPacketValues
    private val triggerMotion by Tower.triggerMotionValues
    private val dragMotion by Tower.dragMotionValues
    private val teleportHeight by Tower.teleportHeightValues
    private val teleportDelay by Tower.teleportDelayValues
    private val teleportGround by Tower.teleportGroundValues
    private val teleportNoMotion by Tower.teleportNoMotionValues

    // <--

    /**
     * SCAFFOLD MODES & SETTINGS
     */

    // -->

    val scaffoldMode by ListValue(
        "ScaffoldMode",
        arrayOf("Normal", "Rewinside", "Expand", "Telly", "GodBridge"),
        "Normal"
    )

    // Expand
    private val omniDirectionalExpand by BoolValue("OmniDirectionalExpand", false) { scaffoldMode == "Expand" }
    private val expandLength by IntegerValue("ExpandLength", 1, 1..6) { scaffoldMode == "Expand" }

    // Placeable delay
    private val placeDelayValue = BoolValue("PlaceDelay", true) { scaffoldMode != "GodBridge" }
    private val maxDelayValue: IntegerValue = object : IntegerValue("MaxDelay", 0, 0..1000) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minDelay)
        override fun isSupported() = placeDelayValue.isActive()
    }
    private val maxDelay by maxDelayValue

    private val minDelayValue = object : IntegerValue("MinDelay", 0, 0..1000) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(maxDelay)
        override fun isSupported() = placeDelayValue.isActive() && !maxDelayValue.isMinimal()
    }
    private val minDelay by minDelayValue

    // Extra clicks
    private val extraClicks by BoolValue("DoExtraClicks", false)
    private val simulateDoubleClicking by BoolValue("SimulateDoubleClicking", false) { extraClicks }
    private val extraClickMaxCPSValue: IntegerValue = object : IntegerValue("ExtraClickMaxCPS", 7, 0..50) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(extraClickMinCPS)
        override fun isSupported() = extraClicks
    }
    private val extraClickMaxCPS by extraClickMaxCPSValue

    private val extraClickMinCPS by object : IntegerValue("ExtraClickMinCPS", 3, 0..50) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(extraClickMaxCPS)
        override fun isSupported() = extraClicks && !extraClickMaxCPSValue.isMinimal()
    }

    private val placementAttempt by ListValue(
        "PlacementAttempt",
        arrayOf("Fail", "Independent"),
        "Fail"
    ) { extraClicks }

    // Autoblock
    private val autoBlock by ListValue("AutoBlock", arrayOf("Off", "Pick", "Spoof", "Switch"), "Spoof")
    private val sortByHighestAmount by BoolValue("SortByHighestAmount", false) { autoBlock != "Off" }
    private val earlySwitch by BoolValue("EarlySwitch", false) { autoBlock != "Off" && !sortByHighestAmount }
    private val amountBeforeSwitch by IntegerValue("SlotAmountBeforeSwitch",
        3,
        1..10
    ) { earlySwitch && !sortByHighestAmount }

    // Settings
    private val autoF5 by BoolValue("AutoF5", false, subjective = true)

    // Basic stuff
    val sprint by BoolValue("Sprint", false)
    private val swing by BoolValue("Swing", true, subjective = true)
    private val down by BoolValue("Down", true) { scaffoldMode !in arrayOf("GodBridge", "Telly") }

    private val ticksUntilRotation: IntegerValue = object : IntegerValue("TicksUntilRotation", 3, 1..5) {
        override fun isSupported() = scaffoldMode == "Telly"
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceIn(minimum, maximum)
    }

    // GodBridge mode subvalues
    private val waitForRots by BoolValue("WaitForRotations", false) { isGodBridgeEnabled }
    private val useStaticRotation by BoolValue("UseStaticRotation", false) { isGodBridgeEnabled }
    private val customGodPitch by FloatValue("GodBridgePitch",
        73.5f,
        0f..90f
    ) { isGodBridgeEnabled && useStaticRotation }

    private val minGodPitch by FloatValue("MinGodBridgePitch",
        75f,
        0f..90f
    ) { isGodBridgeEnabled && !useStaticRotation }
    private val maxGodPitch by FloatValue("MaxGodBridgePitch",
        80f,
        0f..90f
    ) { isGodBridgeEnabled && !useStaticRotation }

    val autoJump by BoolValue("AutoJump", true) { scaffoldMode == "GodBridge" }
    val jumpAutomatically by BoolValue("JumpAutomatically", true) { scaffoldMode == "GodBridge" && autoJump }
    private val maxBlocksToJump: IntegerValue = object : IntegerValue("MaxBlocksToJump", 4, 1..8) {
        override fun isSupported() = scaffoldMode == "GodBridge" && !jumpAutomatically && autoJump
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minBlocksToJump.get())
    }

    private val minBlocksToJump: IntegerValue = object : IntegerValue("MinBlocksToJump", 4, 1..8) {
        override fun isSupported() =
            scaffoldMode == "GodBridge" && !jumpAutomatically && !maxBlocksToJump.isMinimal() && autoJump

        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(maxBlocksToJump.get())
    }

    // Telly mode subvalues
    private val startHorizontally by BoolValue("StartHorizontally", true) { scaffoldMode == "Telly" }
    private val maxHorizontalPlacements: IntegerValue = object : IntegerValue("MaxHorizontalPlacements", 1, 1..10) {
        override fun isSupported() = scaffoldMode == "Telly"
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minHorizontalPlacements.get())
    }
    private val minHorizontalPlacements: IntegerValue = object : IntegerValue("MinHorizontalPlacements", 1, 1..10) {
        override fun isSupported() = scaffoldMode == "Telly"
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(maxHorizontalPlacements.get())
    }
    private val maxVerticalPlacements: IntegerValue = object : IntegerValue("MaxVerticalPlacements", 1, 1..10) {
        override fun isSupported() = scaffoldMode == "Telly"
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minVerticalPlacements.get())
    }

    private val minVerticalPlacements: IntegerValue = object : IntegerValue("MinVerticalPlacements", 1, 1..10) {
        override fun isSupported() = scaffoldMode == "Telly"
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(maxVerticalPlacements.get())
    }

    private val maxjumpingCooldown: IntegerValue = object : IntegerValue("MaxjumpingCooldown", 0, 0..10) {
        override fun isSupported() = scaffoldMode == "Telly"
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minjumpingCooldown.get())
    }
    private val minjumpingCooldown: IntegerValue = object : IntegerValue("MinjumpingCooldown", 0, 0..10) {
        override fun isSupported() = scaffoldMode == "Telly"
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(maxjumpingCooldown.get())
    }

    private val allowClutching by BoolValue("AllowClutching", true) { scaffoldMode !in arrayOf("Telly", "Expand") }
    private val horizontalClutchBlocks: IntegerValue = object : IntegerValue("HorizontalClutchBlocks", 3, 1..5) {
        override fun isSupported() = allowClutching && scaffoldMode !in arrayOf("Telly", "Expand")
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceIn(minimum, maximum)
    }
    private val verticalClutchBlocks: IntegerValue = object : IntegerValue("VerticalClutchBlocks", 2, 1..3) {
        override fun isSupported() = allowClutching && scaffoldMode !in arrayOf("Telly", "Expand")
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceIn(minimum, maximum)
    }

    // Eagle
    private val eagleValue =
        ListValue("Eagle", arrayOf("Normal", "Silent", "Off"), "Normal") { scaffoldMode != "GodBridge" }
    val eagle by eagleValue
    private val adjustedSneakSpeed by BoolValue("AdjustedSneakSpeed", true) { eagle == "Silent" }
    private val eagleSpeed by FloatValue("EagleSpeed", 0.3f, 0.3f..1.0f) { eagleValue.isSupported() && eagle != "Off" }
    val eagleSprint by BoolValue("EagleSprint", false) { eagleValue.isSupported() && eagle == "Normal" }
    private val blocksToEagle by IntegerValue("BlocksToEagle", 0, 0..10) { eagleValue.isSupported() && eagle != "Off" }
    private val edgeDistance by FloatValue(
        "EagleEdgeDistance",
        0f,
        0f..0.5f
    ) { eagleValue.isSupported() && eagle != "Off" }

    // Rotation Options
    val rotationMode by ListValue("Rotations", arrayOf("Off", "Normal", "Stabilized", "GodBridge"), "Normal")
    val smootherMode by ListValue(
        "SmootherMode",
        arrayOf("Linear", "Relative"),
        "Relative"
    ) { rotationMode != "Off" }
    val silentRotation by BoolValue("SilentRotation", true) { rotationMode != "Off" }
    val simulateShortStop by BoolValue("SimulateShortStop", false) { rotationMode != "Off" }
    val strafe by BoolValue("Strafe", false) { rotationMode != "Off" && silentRotation }
    val keepRotation by BoolValue("KeepRotation", true) { rotationMode != "Off" && silentRotation }
    private val keepTicks by object : IntegerValue("KeepTicks", 1, 1..20) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minimum)
        override fun isSupported() = rotationMode != "Off" && scaffoldMode != "Telly" && silentRotation
    }

    // Search options
    private val searchMode by ListValue("SearchMode", arrayOf("Area", "Center"), "Area") { scaffoldMode != "GodBridge" }
    private val minDist by FloatValue("MinDist", 0f, 0f..0.2f) { scaffoldMode !in arrayOf("GodBridge", "Telly") }

    // Turn Speed
    val startRotatingSlow by BoolValue("StartRotatingSlow", false) { rotationMode != "Off" }
    val slowDownOnDirectionChange by BoolValue("SlowDownOnDirectionChange", false) { rotationMode != "Off" }
    val useStraightLinePath by BoolValue("UseStraightLinePath", true) { rotationMode != "Off" }
    val maxHorizontalSpeedValue = object : FloatValue("MaxHorizontalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minHorizontalSpeed)
        override fun isSupported() = rotationMode != "Off"
    }
    val maxHorizontalSpeed by maxHorizontalSpeedValue

    val minHorizontalSpeed: Float by object : FloatValue("MinHorizontalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxHorizontalSpeed)
        override fun isSupported() = !maxHorizontalSpeedValue.isMinimal() && rotationMode != "Off"
    }

    val maxVerticalSpeedValue = object : FloatValue("MaxVerticalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minVerticalSpeed)
        override fun isSupported() = rotationMode != "Off"
    }
    val maxVerticalSpeed by maxVerticalSpeedValue

    val minVerticalSpeed: Float by object : FloatValue("MinVerticalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxVerticalSpeed)
        override fun isSupported() = !maxVerticalSpeedValue.isMinimal() && rotationMode != "Off"
    }

    private val angleThresholdUntilReset by FloatValue(
        "AngleThresholdUntilReset",
        5f,
        0.1f..180f
    ) { rotationMode != "Off" && silentRotation }

    val minRotationDifference by FloatValue("MinRotationDifference", 0f, 0f..1f) { rotationMode != "Off" }

    // Zitter
    private val zitterMode by ListValue("Zitter", arrayOf("Off", "Teleport", "Smooth"), "Off")
    private val zitterSpeed by FloatValue("ZitterSpeed", 0.13f, 0.1f..0.3f) { zitterMode == "Teleport" }
    private val zitterStrength by FloatValue("ZitterStrength", 0.05f, 0f..0.2f) { zitterMode == "Teleport" }

    private val maxZitterTicksValue: IntegerValue = object : IntegerValue("MaxZitterTicks", 3, 0..6) {
        override fun isSupported() = zitterMode == "Smooth"
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minZitterTicks)
    }
    private val maxZitterTicks by maxZitterTicksValue

    private val minZitterTicksValue: IntegerValue = object : IntegerValue("MinZitterTicks", 2, 0..6) {
        override fun isSupported() = zitterMode == "Smooth" && !maxZitterTicksValue.isMinimal()
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(maxZitterTicks)
    }
    private val minZitterTicks by minZitterTicksValue

    private val useSneakMidAir by BoolValue("UseSneakMidAir", false) { zitterMode == "Smooth" }

    // Game
    val timer by FloatValue("Timer", 1f, 0.1f..10f)
    private val speedModifier by FloatValue("SpeedModifier", 1f, 0f..2f)
    private val speedLimiter by BoolValue("SpeedLimiter", false) { !slow }
    private val speedLimit by FloatValue("SpeedLimit", 0.11f, 0.01f..0.12f) { !slow && speedLimiter }
    private val slow by BoolValue("Slow", false)
    private val slowGround by BoolValue("SlowOnlyGround", false) { slow }
    private val slowSpeed by FloatValue("SlowSpeed", 0.6f, 0.2f..0.8f) { slow }

    // Jump Strafe
    private val jumpStrafe by BoolValue("JumpStrafe", false)
    private val maxJumpStraightStrafe: FloatValue = object : FloatValue("MaxStraightStrafe", 0.45f, 0.1f..1f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minJumpStraightStrafe.get())
        override fun isSupported() = jumpStrafe
    }

    private val minJumpStraightStrafe: FloatValue = object : FloatValue("MinStraightStrafe", 0.4f, 0.1f..1f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxJumpStraightStrafe.get())
        override fun isSupported() = jumpStrafe
    }

    private val maxJumpDiagonalStrafe: FloatValue = object : FloatValue("MaxDiagonalStrafe", 0.45f, 0.1f..1f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minJumpDiagonalStrafe.get())
        override fun isSupported() = jumpStrafe
    }

    private val minJumpDiagonalStrafe: FloatValue = object : FloatValue("MinDiagonalStrafe", 0.4f, 0.1f..1f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxJumpDiagonalStrafe.get())
        override fun isSupported() = jumpStrafe
    }

    // Safety
    private val sameY by BoolValue("SameY", false) { scaffoldMode != "GodBridge" }
    private val jumpOnUserInput by BoolValue("JumpOnUserInput", true) { sameY && scaffoldMode != "GodBridge" }

    private val safeWalkValue = BoolValue("SafeWalk", true) { scaffoldMode != "GodBridge" }
    private val airSafe by BoolValue("AirSafe", false) { safeWalkValue.isActive() }

    // Visuals
    private val mark by BoolValue("Mark", false, subjective = true)
    private val trackCPS by BoolValue("TrackCPS", false, subjective = true)
    private val safetyLines by BoolValue("SafetyLines", false, subjective = true) { isGodBridgeEnabled }

    // Target placement
    var placeRotation: PlaceRotation? = null

    // Launch position
    private var launchY = 0

    val shouldJumpOnInput = !jumpOnUserInput || !mc.options.keyBindJump.pressed

    private val shouldKeepLaunchPosition
        get() = sameY && shouldJumpOnInput && scaffoldMode != "GodBridge"

    // Zitter
    private var zitterDirection = false

    // Delay
    private val delayTimer = object : DelayTimer(minDelayValue, maxDelayValue, MSTimer()) {
        override fun hasTimePassed() = !placeDelayValue.isActive() || super.hasTimePassed()
    }

    private val zitterTickTimer = TickDelayTimer(minZitterTicksValue, maxZitterTicksValue)

    // Eagle
    private var placedBlocksWithoutEagle = 0
    var eagleSneaking = false
    private val isEagleEnabled
        get() = eagle != "Off" && !shouldGoDown && scaffoldMode != "GodBridge"

    // Downwards
    private val shouldGoDown
        get() = down && !sameY && GameOptions.isPressed(mc.options.sneakKey) && scaffoldMode !in arrayOf(
            "GodBridge",
            "Telly"
        ) && blocksAmount > 1

    // Current rotation
    private val currRotation
        get() = RotationUtils.currentRotation ?: mc.player.rotation

    // Extra clicks
    private var extraClick =
        ExtraClickInfo(TimeUtils.randomClickDelay(extraClickMinCPS, extraClickMaxCPS), 0L, 0)

    // GodBridge
    private var blocksPlacedUntilJump = 0

    private val isManualJumpOptionActive
        get() = scaffoldMode == "GodBridge" && !jumpAutomatically

    private var blocksToJump = TimeUtils.randomDelay(minBlocksToJump.get(), maxBlocksToJump.get())

    private val isGodBridgeEnabled
        get() = scaffoldMode == "GodBridge" || scaffoldMode == "Normal" && rotationMode == "GodBridge"

    private val isLookingDiagonally: Boolean
        get() {
            val player = mc.player ?: return false

            // Round the rotation to the nearest multiple of 45 degrees so that way we check if the player faces diagonally
            val yaw = round(abs(MathHelper.wrapDegrees(player.yaw)).roundToInt() / 45f) * 45f

            return floatArrayOf(
                45f,
                135f
            ).any { yaw == it } && player.input.movementForward != 0f && player.input.movementSideways == 0f
        }

    // Telly
    private var offGroundTicks = 0
    private var ticksUntilJump = 0
    private var blocksUntilAxisChange = 0
    private var jumpingCooldown = TimeUtils.randomDelay(minjumpingCooldown.get(), maxjumpingCooldown.get())
    private var horizontalPlacements =
        TimeUtils.randomDelay(minHorizontalPlacements.get(), maxHorizontalPlacements.get())
    private var verticalPlacements = TimeUtils.randomDelay(minVerticalPlacements.get(), maxVerticalPlacements.get())
    private val shouldPlaceHorizontally
        get() = scaffoldMode == "Telly" && MovementUtils.isMoving && (startHorizontally && blocksUntilAxisChange <= horizontalPlacements || !startHorizontally && blocksUntilAxisChange > verticalPlacements)

    // <--

    // Enabling module
    override fun onEnable() {
        val player = mc.player ?: return

        launchY = player.y.roundToInt()
        blocksUntilAxisChange = 0
    }

    // Events
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val player = mc.player ?: return

        if (mc.interactionManager.currentGameMode == WorldSettings.GameType.SPECTATOR)
            return

        mc.ticker.timerSpeed = timer


        if (scaffoldMode == "GodBridge" && waitForRots) {
            if (this.placeRotation != null) {
                mc.options.sneakKey.pressed = getRotationDifference(currRotation,
                    this.placeRotation!!.rotation.fixedSensitivity()
                ) == 0f
            }
        }
        // Telly
        if (mc.player.onGround) {
            offGroundTicks = 0
            ticksUntilJump++
        } else {
            offGroundTicks++
        }

        if (shouldGoDown) {
            mc.options.sneakKey.pressed = false
        }

        if (slow) {
            if (!slowGround || slowGround && mc.player.onGround) {
                player.velocityX *= slowSpeed
                player.velocityZ *= slowSpeed
            }
        }

        // Eagle
        if (isEagleEnabled) {
            var dif = 0.5
            val blockPos = BlockPos(player).down()

            for (side in Direction.values()) {
                if (side.axis == Direction.Axis.Y) {
                    continue
                }

                val neighbor = blockPos.offset(side)

                if (isReplaceable(neighbor)) {
                    val calcDif = (if (side.axis == Direction.Axis.Z) {
                        abs(neighbor.z + 0.5 - player.z)
                    } else {
                        abs(neighbor.x + 0.5 - player.x)
                    }) - 0.5

                    if (calcDif < dif) {
                        dif = calcDif
                    }
                }
            }

            if (placedBlocksWithoutEagle >= blocksToEagle) {
                val shouldEagle = isReplaceable(blockPos) || dif < edgeDistance
                if (eagle == "Silent") {
                    if (eagleSneaking != shouldEagle) {
                        sendPacket(
                            ClientCommandC2SPacket(
                                player,
                                if (shouldEagle) ClientCommandC2SPacket.Mode.START_SNEAKING else ClientCommandC2SPacket.Mode.STOP_SNEAKING
                            )
                        )

                        // Adjust speed when silent sneaking
                        if (adjustedSneakSpeed && shouldEagle) {
                            player.velocityX *= eagleSpeed
                            player.velocityZ *= eagleSpeed
                        }
                    }

                    eagleSneaking = shouldEagle
                } else {
                    mc.options.sneakKey.pressed = shouldEagle
                    eagleSneaking = shouldEagle
                }
                placedBlocksWithoutEagle = 0
            } else {
                placedBlocksWithoutEagle++
            }
        }

        if (player.onGround) {
            // Still a thing?
            if (scaffoldMode == "Rewinside") {
                MovementUtils.strafe(0.2F)
                player.velocityY = 0.0
            }
        }
    }

    @EventTarget
    fun onStrafe(event: StrafeEvent) {
        val player = mc.player

        // Jumping needs to be done here, so it doesn't get detected by movement-sensitive anti-cheats.
        if (scaffoldMode == "Telly" && player.onGround && MovementUtils.isMoving && currRotation == player.rotation && ticksUntilJump >= jumpingCooldown) {
            player.tryJump()

            ticksUntilJump = 0
            jumpingCooldown = TimeUtils.randomDelay(minjumpingCooldown.get(), maxjumpingCooldown.get())
        }
    }

    @EventTarget
    fun onRotationUpdate(event: RotationUpdateEvent) {
        val rotation = RotationUtils.currentRotation

        update()

        if (rotationMode != "Off" && rotation != null) {
            val placeRotation = this.placeRotation?.rotation ?: rotation

            val pitch = if (scaffoldMode == "GodBridge" && useStaticRotation) {
                if (placeRotation == this.placeRotation?.rotation) {
                    if (isLookingDiagonally) 75.6f else customGodPitch
                } else placeRotation.pitch
            } else {
                placeRotation.pitch
            }

            val targetRotation = Rotation(placeRotation.yaw, pitch).fixedSensitivity()

            val ticks = if (keepRotation) {
                if (scaffoldMode == "Telly") 1 else keepTicks
            } else {
                RotationUtils.resetTicks
            }

            if (RotationUtils.resetTicks != 0 || keepRotation) {
                setRotation(targetRotation, ticks)
            }
        }
    }

    @EventTarget
    fun onTick(event: GameTickEvent) {
        val target = placeRotation?.placeInfo

        if (extraClicks) {
            val doubleClick = if (simulateDoubleClicking) RandomUtils.nextInt(-1, 1) else 0

            repeat(extraClick.clicks + doubleClick) {
                extraClick.clicks--

                doPlaceAttempt()
            }
        }

        if (target == null) {
            if (placeDelayValue.isActive()) {
                delayTimer.reset()
            }
            return
        }

        val raycastProperly = !(scaffoldMode == "Expand" && expandLength > 1 || shouldGoDown) && rotationMode != "Off"

        performBlockRaytrace(currRotation, mc.interactionManager.reachDistance).let {
            if (rotationMode == "Off" || it != null && it.blockPos == target.blockPos && (!raycastProperly || it.direction == target.Direction)) {
                val result = if (raycastProperly && it != null) {
                    PlaceInfo(it.blockPos, it.direction, it.pos)
                } else {
                    target
                }

                place(result)
            }
        }
    }

    @EventTarget
    fun onSneakSlowDown(event: SneakSlowDownEvent) {
        if (!isEagleEnabled || eagle != "Normal") {
            return
        }

        event.forward *= eagleSpeed / 0.3f
        event.strafe *= eagleSpeed / 0.3f
    }

    fun update() {
        val player = mc.player ?: return
        val holdingItem = player.mainHandStack?.item is BlockItem

        if (!holdingItem && (autoBlock == "Off" || InventoryUtils.findBlockInHotbar() == null)) {
            return
        }

        findBlock(scaffoldMode == "Expand" && expandLength > 1, searchMode == "Area")
    }

    private fun setRotation(rotation: Rotation, ticks: Int) {
        val player = mc.player ?: return

        if (silentRotation) {
            if (scaffoldMode == "Telly" && MovementUtils.isMoving) {
                if (offGroundTicks < ticksUntilRotation.get() && ticksUntilJump >= jumpingCooldown) {
                    return
                }
            }

            setTargetRotation(
                rotation,
                ticks,
                strafe,
                turnSpeed = minHorizontalSpeed..maxHorizontalSpeed to minVerticalSpeed..maxVerticalSpeed,
                angleThresholdForReset = angleThresholdUntilReset,
                smootherMode = this.smootherMode,
                simulateShortStop = simulateShortStop,
                startOffSlow = startRotatingSlow,
                slowDownOnDirChange = slowDownOnDirectionChange,
                useStraightLinePath = useStraightLinePath,
                minRotationDifference = minRotationDifference
            )

        } else {
            rotation.toPlayer(player)
        }
    }

    // Search for new target block
    private fun findBlock(expand: Boolean, area: Boolean) {
        val player = mc.player ?: return

        val blockPosition = if (shouldGoDown) {
            if (player.z == player.z.roundToInt() + 0.5) {
                BlockPos(player.x, player.y - 0.6, player.z)
            } else {
                BlockPos(player.x, player.y - 0.6, player.z).down()
            }
        } else if (shouldKeepLaunchPosition && launchY <= player.z) {
            BlockPos(player.x, launchY - 1.0, player.z)
        } else if (player.z == player.z.roundToInt() + 0.5) {
            BlockPos(player)
        } else {
            BlockPos(player).down()
        }

        if (!expand && (!isReplaceable(blockPosition) || search(
                blockPosition,
                !shouldGoDown,
                area,
                shouldPlaceHorizontally
            ))
        ) {
            return
        }

        if (expand) {
            val yaw = player.yaw.toRadiansD()
            val x = if (omniDirectionalExpand) -sin(yaw).roundToInt() else player.horizontalFacing.directionVec.x
            val z = if (omniDirectionalExpand) cos(yaw).roundToInt() else player.horizontalFacing.directionVec.z

            repeat(expandLength) {
                if (search(blockPosition.add(x * it, 0, z * it), false, area))
                    return
            }
            return
        }

        val (f, g) = if (scaffoldMode == "Telly") 5 to 3 else if (allowClutching) horizontalClutchBlocks.get() to verticalClutchBlocks.get() else 1 to 1

        (-f..f).flatMap { x ->
            (0 downTo -g).flatMap { y ->
                (-f..f).map { z ->
                    Vec3i(x, y, z)
                }
            }
        }.sortedBy {
            BlockUtils.getCenterDistance(blockPosition.add(it))
        }.forEach {
            if (canBeClicked(blockPosition.add(it)) || search(
                    blockPosition.add(it),
                    !shouldGoDown,
                    area,
                    shouldPlaceHorizontally
                )
            ) {
                return
            }
        }
    }

    fun place(placeInfo: PlaceInfo) {
        val player = mc.player ?: return
        val world = mc.world ?: return
        if (!delayTimer.hasTimePassed() || shouldKeepLaunchPosition && launchY - 1 != placeInfo.Vec3d.y.toInt() && scaffoldMode != "Expand")
            return

        var stack = player.playerScreenHandler.getSlot(serverSlot + 36).stack

        //TODO: blacklist more blocks than only bushes
        if (stack == null || stack.item !is BlockItem || (stack.item as BlockItem).block is DeadBushBlock || stack.count <= 0 || sortByHighestAmount || earlySwitch) {
            val blockSlot = if (sortByHighestAmount) {
                InventoryUtils.findLargestBlockStackInHotbar() ?: return
            } else if (earlySwitch) {
                InventoryUtils.findBlockStackInHotbarGreaterThan(amountBeforeSwitch) ?: InventoryUtils.findBlockInHotbar() ?: return
            } else {
                InventoryUtils.findBlockInHotbar() ?: return
            }

            when (autoBlock.lowercase()) {
                "off" -> return

                "pick" -> {
                    player.inventory.selectedSlot = blockSlot - 36
                   mc.interactionManager.syncSelectedSlot()
                }

                "spoof", "switch" -> serverSlot = blockSlot - 36
            }
            stack = player.playerScreenHandler.getSlot(blockSlot).stack
        }

        // Line 437-440
        if ((stack.item as? BlockItem)?.canPlaceItemBlock(
                world,
                placeInfo.blockPos,
                placeInfo.Direction,
                player,
                stack
            ) == false
        ) {
            return
        }

        tryToPlaceBlock(stack, placeInfo.blockPos, placeInfo.Direction, placeInfo.Vec3d)

        if (autoBlock == "Switch")
            serverSlot = player.inventory.selectedSlot

        // Since we violate vanilla slot switch logic if we send the packets now, we arrange them for the next tick
        switchBlockNextTickIfPossible(stack)

        if (trackCPS) {
            CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)
        }
    }

    private fun doPlaceAttempt() {
        val player = mc.player ?: return
        val world = mc.world ?: return

        val stack = player.playerScreenHandler.getSlot(serverSlot + 36).stack ?: return

        if (stack.item !is BlockItem || InventoryUtils.BLOCK_BLACKLIST.contains((stack.item as BlockItem).block)) {
            return
        }

        val block = stack.item as BlockItem

        val raytrace = performBlockRaytrace(currRotation, mc.interactionManager.reachDistance) ?: return

        val canPlaceOnUpperFace = block.canPlaceItemBlock(
            world, raytrace.blockPos, Direction.UP, player, stack
        )

        val shouldPlace = if (placementAttempt == "Fail") {
            !block.canPlaceItemBlock(world, raytrace.blockPos, raytrace.direction, player, stack)
        } else {
            if (shouldKeepLaunchPosition) {
                raytrace.blockPos.y == launchY - 1 && !canPlaceOnUpperFace
            } else if (shouldPlaceHorizontally) {
                !canPlaceOnUpperFace
            } else {
                raytrace.blockPos.y <= player.z.toInt() - 1 && !(raytrace.blockPos.y == player.z.toInt() - 1 && canPlaceOnUpperFace && raytrace.direction == Direction.UP)
            }
        }

        if (!raytrace.type.isBlock || !shouldPlace) {
            return
        }

        tryToPlaceBlock(stack, raytrace.blockPos, raytrace.direction, raytrace.pos, attempt = true)

        // Since we violate vanilla slot switch logic if we send the packets now, we arrange them for the next tick
        switchBlockNextTickIfPossible(stack)

        if (trackCPS) {
            CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)
        }
    }

    // Disabling module
    override fun onDisable() {
        val player = mc.player ?: return

        if (!GameOptions.isPressed(mc.options.sneakKey)) {
            mc.options.sneakKey.pressed = false
            if (eagleSneaking && player.isSneaking) {
                //sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.STOP_SNEAKING))

                /**
                 * Should prevent false flag by some AntiCheat (Ex: Verus)
                 */
                player.isSneaking = false
            }
        }

        if (!GameOptions.isPressed(mc.options.keyBindRight)) {
            mc.options.keyBindRight.pressed = false
        }
        if (!GameOptions.isPressed(mc.options.keyBindLeft)) {
            mc.options.keyBindLeft.pressed = false
        }

        if (autoF5) {
            mc.options.thirdPersonView = 0
        }

        placeRotation = null
        mc.ticker.timerSpeed = 1f

        TickScheduler += {
            serverSlot = player.inventory.selectedSlot
        }
    }

    // Entity movement event
    @EventTarget
    fun onMove(event: MoveEvent) {
        val player = mc.player ?: return

        if (!safeWalkValue.isActive() || shouldGoDown) {
            return
        }

        if (airSafe || player.onGround) {
            event.isSafeWalk = true
        }
    }

    @EventTarget
    fun onJump(event: JumpEvent) {
        if (!jumpStrafe) return

        if (event.eventState == EventState.POST) {
            MovementUtils.strafe(if (!isLookingDiagonally) (minJumpStraightStrafe.get()..maxJumpStraightStrafe.get()).random() else (minJumpDiagonalStrafe.get()..maxJumpDiagonalStrafe.get()).random())
        }
    }

    // Visuals
    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        val player = mc.player ?: return

        val shouldBother =
            !(shouldGoDown || scaffoldMode == "Expand" && expandLength > 1) && extraClicks && (MovementUtils.isMoving || MovementUtils.speed > 0.03)

        if (shouldBother) {
            currRotation.let {
                performBlockRaytrace(it, mc.interactionManager.reachDistance)?.let { raytrace ->
                    val timePassed = System.currentTimeMillis() - extraClick.lastClick >= extraClick.delay

                    if (raytrace.type.isBlock && timePassed) {
                        extraClick = ExtraClickInfo(
                            TimeUtils.randomClickDelay(extraClickMinCPS, extraClickMaxCPS),
                            System.currentTimeMillis(),
                            extraClick.clicks + 1
                        )
                    }
                }
            }
        }

        displaySafetyLinesIfEnabled()

        if (!mark) {
            return
        }

        repeat(if (scaffoldMode == "Expand") expandLength + 1 else 2) {
            val yaw = player.yaw.toRadiansD()
            val x = if (omniDirectionalExpand) -sin(yaw).roundToInt() else player.horizontalFacing.directionVec.x
            val z = if (omniDirectionalExpand) cos(yaw).roundToInt() else player.horizontalFacing.directionVec.z
            val blockPos = BlockPos(
                player.x + x * it,
                if (shouldKeepLaunchPosition && launchY <= player.z) launchY - 1.0 else player.z - (if (player.z == player.z + 0.5) 0.0 else 1.0) - if (shouldGoDown) 1.0 else 0.0,
                player.z + z * it
            )
            val placeInfo = PlaceInfo.get(blockPos)

            if (isReplaceable(blockPos) && placeInfo != null) {
                RenderUtils.drawBlockBox(blockPos, Color(68, 117, 255, 100), false)
                return
            }
        }
    }

    /**
     * Search for placeable block
     *
     * @param blockPosition pos
     * @param raycast visible
     * @param area spot
     * @return
     */

    private fun search(
        blockPosition: BlockPos,
        raycast: Boolean,
        area: Boolean,
        horizontalOnly: Boolean = false,
    ): Boolean {
        val player = mc.player ?: return false

        if (!isReplaceable(blockPosition)) {
            if (autoF5) mc.options.thirdPersonView = 0
            return false
        } else {
            if (autoF5 && mc.options.thirdPersonView != 1) mc.options.thirdPersonView = 1
        }

        val maxReach = mc.interactionManager.reachDistance

        val eyes = player.eyes
        var placeRotation: PlaceRotation? = null

        var currPlaceRotation: PlaceRotation?

        var considerStableRotation: PlaceRotation? = null

        for (side in Direction.values().filter { !horizontalOnly || it.axis != Direction.Axis.Y }) {
            val neighbor = blockPosition.offset(side)

            if (!canBeClicked(neighbor)) {
                continue
            }

            if (isGodBridgeEnabled) {
                // Selection of these values only. Mostly used by Godbridgers.
                val list = floatArrayOf(-135f, -45f, 45f, 135f)

                // Selection of pitch values that should be OK in non-complex situations.
                val pitchList = if (useStaticRotation) {
                    55.0..75.7 + if (isLookingDiagonally) 1.0 else 0.0
                } else {
                    minGodPitch.toDouble()..maxGodPitch.toDouble() + if (isLookingDiagonally) 1.0 else 0.0
                }
                for (yaw in list) {
                    for (pitch in pitchList step 0.1) {
                        val rotation = Rotation(yaw, pitch.toFloat())

                        val raytrace = performBlockRaytrace(rotation, maxReach) ?: continue

                        currPlaceRotation =
                            PlaceRotation(PlaceInfo(raytrace.blockPos, raytrace.direction, raytrace.pos), rotation)

                        if (raytrace.blockPos == neighbor && raytrace.direction == side.opposite) {
                            val isInStablePitchRange = if (isLookingDiagonally) {
                                pitch >= 75.6
                            } else {
                                pitch in 73.5..75.7
                            }

                            // The module should be looking to aim at (nearly) the upper face of the block. Provides stable bridging most of the time.
                            if (isInStablePitchRange) {
                                considerStableRotation = compareDifferences(currPlaceRotation, considerStableRotation)
                            }

                            placeRotation = compareDifferences(currPlaceRotation, placeRotation)
                        }
                    }
                }

                continue
            }

            if (!area) {
                currPlaceRotation =
                    findTargetPlace(blockPosition, neighbor, Vec3d(0.5, 0.5, 0.5), side, eyes, maxReach, raycast)
                        ?: continue

                placeRotation = compareDifferences(currPlaceRotation, placeRotation)
            } else {
                for (x in 0.1..0.9) {
                    for (y in 0.1..0.9) {
                        for (z in 0.1..0.9) {
                            currPlaceRotation =
                                findTargetPlace(blockPosition, neighbor, Vec3d(x, y, z), side, eyes, maxReach, raycast)
                                    ?: continue

                            placeRotation = compareDifferences(currPlaceRotation, placeRotation)
                        }
                    }
                }
            }
        }

        placeRotation = considerStableRotation ?: placeRotation

        placeRotation ?: return false

        if (useStaticRotation && scaffoldMode == "GodBridge") {
            placeRotation = PlaceRotation(
                placeRotation.placeInfo,
                Rotation(placeRotation.rotation.yaw, if (isLookingDiagonally) 75.6f else customGodPitch)
            )
        }

        if (rotationMode != "Off") {
            var targetRotation = placeRotation.rotation

            val info = placeRotation.placeInfo

            if (scaffoldMode == "GodBridge") {
                val shouldJumpForcefully = isManualJumpOptionActive && blocksPlacedUntilJump >= blocksToJump

                performBlockRaytrace(currRotation, maxReach)?.let {
                    val isSneaking = player.input.sneak

                    if ((!isSneaking || MovementUtils.speed != 0f)
                        && it.blockPos == info.blockPos
                        && (it.direction != info.Direction || shouldJumpForcefully)
                        && MovementUtils.isMoving
                        && currRotation.yaw.roundToInt() % 45f == 0f
                    ) {
                        if (!isSneaking && autoJump) {
                            if (player.onGround && !isLookingDiagonally) {
                                player.tryJump()
                            }

                            if (shouldJumpForcefully) {
                                blocksPlacedUntilJump = 0
                                blocksToJump = TimeUtils.randomDelay(
                                    minBlocksToJump.get(),
                                    maxBlocksToJump.get()
                                )
                            }
                        }

                        targetRotation = currRotation
                    }
                }
            }

            setRotation(targetRotation, if (scaffoldMode == "Telly") 1 else keepTicks)
        }
        this.placeRotation = placeRotation
        return true
    }

    /**
     * For expand scaffold, fixes vector values that should match according to direction vector
     */
    private fun modifyVec(original: Vec3d, direction: Direction, pos: Vec3d, shouldModify: Boolean): Vec3d {
        if (!shouldModify) {
            return original
        }

        val x = original.x
        val y = original.y
        val z = original.z

        val side = direction.opposite

        return when (side.axis ?: return original) {
            Direction.Axis.Y -> Vec3d(x, pos.y + side.directionVec.y.coerceAtLeast(0), z)
            Direction.Axis.X -> Vec3d(pos.x + side.directionVec.x.coerceAtLeast(0), y, z)
            Direction.Axis.Z -> Vec3d(x, y, pos.z + side.directionVec.z.coerceAtLeast(0))
        }

    }

    private fun findTargetPlace(
        pos: BlockPos, offsetPos: BlockPos, Vec3d: Vec3d, side: Direction, eyes: Vec3d, maxReach: Float, raycast: Boolean,
    ): PlaceRotation? {
        val world = mc.world ?: return null

        val vec = (Vec3d(pos) + Vec3d).addVector(
            side.directionVec.x * Vec3d.x, side.directionVec.y * Vec3d.y, side.directionVec.z * Vec3d.z
        )

        val distance = eyes.distanceTo(vec)

        if (raycast && (distance > maxReach || world.rayTrace(eyes, vec, false, true, false) != null)) {
            return null
        }

        val diff = vec - eyes

        if (side.axis != Direction.Axis.Y) {
            val dist = abs(if (side.axis == Direction.Axis.Z) diff.z else diff.x)

            if (dist < minDist && scaffoldMode != "Telly") {
                return null
            }
        }

        var rotation = toRotation(vec, false)

        rotation = when (rotationMode) {
            "Stabilized" -> Rotation(round(rotation.yaw / 45f) * 45f, rotation.pitch)
            else -> rotation
        }.fixedSensitivity()

        // If the current rotation already looks at the target block and side, then return right here
        performBlockRaytrace(currRotation, maxReach)?.let { raytrace ->
            if (raytrace.blockPos == offsetPos && (!raycast || raytrace.direction == side.opposite)) {
                return PlaceRotation(
                    PlaceInfo(
                        raytrace.blockPos, side.opposite, modifyVec(raytrace.pos, side, Vec3d(offsetPos), !raycast)
                    ), currRotation
                )
            }
        }

        val raytrace = performBlockRaytrace(rotation, maxReach) ?: return null

        if (raytrace.blockPos == offsetPos && (!raycast || raytrace.direction == side.opposite)) {
            return PlaceRotation(
                PlaceInfo(
                    raytrace.blockPos, side.opposite, modifyVec(raytrace.pos, side, Vec3d(offsetPos), !raycast)
                ), rotation
            )
        }

        return null
    }

    private fun performBlockRaytrace(rotation: Rotation, maxReach: Float): BlockHitResult? {
        val player = mc.player ?: return null
        val world = mc.world ?: return null

        val eyes = player.eyes
        val rotationVec = getVectorForRotation(rotation)

        val reach = eyes + (rotationVec * maxReach.toDouble())

        return world.rayTrace(eyes, reach, false, false, true)
    }

    private fun compareDifferences(
        new: PlaceRotation, old: PlaceRotation?, rotation: Rotation = currRotation,
    ): PlaceRotation {
        if (old == null || getRotationDifference(
                new.rotation,
                rotation
            ) < getRotationDifference(
                old.rotation, rotation
            )
        ) {
            return new
        }

        return old
    }

    private fun switchBlockNextTickIfPossible(stack: ItemStack) {
        val player = mc.player ?: return
        if (autoBlock in arrayOf("Off", "Switch")) return
        val switchAmount = if (earlySwitch) amountBeforeSwitch else 0
        if (stack.count > switchAmount) return

        val switchSlot = if (earlySwitch) {
            InventoryUtils.findBlockStackInHotbarGreaterThan(amountBeforeSwitch) ?: InventoryUtils.findBlockInHotbar() ?: return
        } else {
            InventoryUtils.findBlockInHotbar()
        } ?: return

        TickScheduler += {
            if (autoBlock == "Pick") {
                player.inventory.selectedSlot = switchSlot - 36
               mc.interactionManager.syncSelectedSlot()
            } else {
                serverSlot = switchSlot - 36
            }
        }
    }

    private fun displaySafetyLinesIfEnabled() {
        if (!safetyLines || !isGodBridgeEnabled) {
            return
        }

        val player = mc.player ?: return

        // If player is not walking diagonally then continue
        if (round(abs(MathHelper.wrapDegrees(player.yaw)).roundToInt() / 45f) * 45f !in arrayOf(
                135f,
                45f
            ) || player.input.movementForward == 0f || player.input.movementSideways != 0f
        ) {
            val (posX, posY, posZ) = player.interpolatedPosition()

            GL11.glPushMatrix()
            GL11.glTranslated(-posX, -posY, -posZ)
            GL11.glLineWidth(5.5f)
            GL11.glDisable(GL11.GL_TEXTURE_2D)

            val (yawX, yawZ) = player.horizontalFacing.directionVec.x * 1.5 to player.horizontalFacing.directionVec.z * 1.5

            // The target rotation will either be the module's placeRotation or a forced rotation (usually that's where the GodBridge mode aims)
            val targetRotation = run {
                val yaw = floatArrayOf(-135f, -45f, 45f, 135f).minByOrNull {
                    abs(
                        RotationUtils.getAngleDifference(
                            it,
                            MathHelper.wrapDegrees(currRotation.yaw)
                        )
                    )
                } ?: return

                placeRotation?.rotation ?: Rotation(yaw, 73f)
            }

            // Calculate color based on rotation difference
            val color = getColorForRotationDifference(
                getRotationDifference(
                    targetRotation,
                    currRotation
                )
            )

            val main = BlockPos(player).down()

            val pos = if (canBeClicked(main)) {
                main
            } else {
                (-1..1).flatMap { x ->
                    (-1..1).map { z ->
                        val neighbor = main.add(x, 0, z)

                        neighbor to BlockUtils.getCenterDistance(neighbor)
                    }
                }.filter { canBeClicked(it.first) }.minByOrNull { it.second }?.first ?: main
            }.up().getVec()

            for (offset in 0..1) {
                for (i in -1..1 step 2) {
                    for (x1 in 0.25..0.5 step 0.01) {
                        val opposite = offset == 1

                        val (offsetX, offsetZ) = if (opposite) 0.0 to x1 * i else x1 * i to 0.0
                        val (lineX, lineZ) = if (opposite) yawX to 0.0 else 0.0 to yawZ

                        val (x, y, z) = pos.add(Vec3d(offsetX, -0.99, offsetZ))

                        GL11.glBegin(GL11.GL_LINES)

                        GL11.glColor3f(color.x, color.y, color.z)
                        GL11.glVertex3d(x - lineX, y + 0.5, z - lineZ)
                        GL11.glVertex3d(x + lineX, y + 0.5, z + lineZ)

                        GL11.glEnd()
                    }
                }
            }
            GL11.glEnable(GL11.GL_TEXTURE_2D)
            GL11.glPopMatrix()
        }
    }

    private fun getColorForRotationDifference(rotationDifference: Float): Color3f {
        val maxDifferenceForGreen = 10.0f
        val maxDifferenceForYellow = 40.0f

        val interpolationFactor = when {
            rotationDifference <= maxDifferenceForGreen -> 0.0f
            rotationDifference <= maxDifferenceForYellow -> (rotationDifference - maxDifferenceForGreen) / (maxDifferenceForYellow - maxDifferenceForGreen)
            else -> 1.0f
        }

        val green = 1.0f - interpolationFactor
        val blue = 0.0f

        return Color3f(interpolationFactor, green, blue)
    }

    private fun updatePlacedBlocksForTelly() {
        if (blocksUntilAxisChange > horizontalPlacements + verticalPlacements) {
            blocksUntilAxisChange = 0

            horizontalPlacements =
                TimeUtils.randomDelay(minHorizontalPlacements.get(), maxHorizontalPlacements.get())
            verticalPlacements =
                TimeUtils.randomDelay(minVerticalPlacements.get(), maxVerticalPlacements.get())
            return
        }

        blocksUntilAxisChange++
    }

    private fun tryToPlaceBlock(
        stack: ItemStack,
        clickPos: BlockPos,
        side: Direction,
        pos: Vec3d,
        attempt: Boolean = false,
    ): Boolean {
        val player = mc.player ?: return false

        val prevSize = stack.count

        val clickedSuccessfully = player.onPlayerRightClick(clickPos, side, pos, stack)

        if (clickedSuccessfully) {
            if (!attempt) {
                delayTimer.reset()

                if (player.onGround) {
                    player.velocityX *= speedModifier
                    player.velocityZ *= speedModifier
                }
            }

            if (swing) player.swingHand()
            else sendPacket(HandSwingC2SPacket())

            if (isManualJumpOptionActive && autoJump)
                blocksPlacedUntilJump++

            updatePlacedBlocksForTelly()

            if (stack.count <= 0) {
                player.inventory.main[serverSlot] = null
                ForgeEventFactory.onPlayerDestroyItem(player, stack)
            } else if (stack.count != prevSize || mc.interactionManager.currentGameMode.isCreative)
                mc.entityRenderer.itemRenderer.resetEquippedProgress()
        } else {
            if (player.sendUseItem(stack))
                mc.entityRenderDispatcher.itemRenderer.resetEquippedProgress2()
        }

        return clickedSuccessfully
    }

    fun handleMovementOptions(input: Input) {
        val player = mc.player ?: return

        if (!state) {
            return
        }

        if (!slow && speedLimiter && MovementUtils.speed > speedLimit) {
            input.movementSideways = 0f
            input.movementForward = 0f
            return
        }

        when (zitterMode.lowercase()) {
            "off" -> {
                return
            }

            "smooth" -> {
                val notOnGround = !player.onGround || !player.verticalCollision

                if (player.onGround) {
                    mc.options.sneakKey.pressed =
                        eagleSneaking || GameOptions.isPressed(mc.options.sneakKey)
                }

                if (input.jump || mc.options.jumpKey.isPressed || notOnGround) {
                    zitterTickTimer.reset()

                    if (useSneakMidAir) {
                        mc.options.sneakKey.pressed = true
                    }

                    if (!notOnGround && !input.jump) {
                        // Attempt to move against the direction
                        input.movementSideways = if (zitterDirection) 1f else -1f
                    } else {
                        input.movementSideways = 0f
                    }

                    zitterDirection = !zitterDirection

                    // Recreate input in case the user was indeed pressing inputs
                    if (mc.options.leftKey.isPressed) {
                        input.movementSideways++
                    }

                    if (mc.options.rightKey.isPressed) {
                        input.movementSideways--
                    }
                    return
                }

                if (zitterTickTimer.hasTimePassed()) {
                    zitterDirection = !zitterDirection
                    zitterTickTimer.reset()
                } else {
                    zitterTickTimer.update()
                }

                if (zitterDirection) {
                    input.movementSideways = -1f
                } else {
                    input.movementSideways = 1f
                }
            }

            "teleport" -> {
                MovementUtils.strafe(zitterSpeed)
                val yaw = (player.yaw + if (zitterDirection) 90.0 else -90.0).toRadians()
                player.velocityX -= sin(yaw) * zitterStrength
                player.velocityZ += cos(yaw) * zitterStrength
                zitterDirection = !zitterDirection
            }
        }
    }

    /**
     * Returns the amount of blocks
     */
    val blocksAmount: Int
        get() {
            var amount = 0
            for (i in 36..44) {
                val stack = mc.player.playerScreenHandler.getSlot(i).stack ?: continue
                val item = stack.item
                if (item is BlockItem) {
                    val block = item.block
                    val mainHandStack = mc.player.mainHandStack
                    if (mainHandStack != null && mainHandStack == stack || block !in InventoryUtils.BLOCK_BLACKLIST && block !is DeadBushBlock) {
                        amount += stack.count
                    }
                }
            }
            return amount
        }

    override val tag
        get() = if (towerMode != "None") ("$scaffoldMode | $towerMode") else scaffoldMode

    data class ExtraClickInfo(val delay: Int, val lastClick: Long, var clicks: Int)
}
