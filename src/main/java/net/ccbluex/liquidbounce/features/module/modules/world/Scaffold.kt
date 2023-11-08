/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.render.BlockOverlay
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.CPSCounter
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.PlaceRotation
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.getAngleDifference
import net.ccbluex.liquidbounce.utils.RotationUtils.getRotationDifference
import net.ccbluex.liquidbounce.utils.RotationUtils.getVectorForRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.limitAngleChange
import net.ccbluex.liquidbounce.utils.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.block.BlockUtils.canBeClicked
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getCenterDistance
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getState
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isReplaceable
import net.ccbluex.liquidbounce.utils.block.PlaceInfo
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverSlot
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBlockBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBorderedRect
import net.ccbluex.liquidbounce.utils.timing.DelayTimer
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.timing.TickDelayTimer
import net.ccbluex.liquidbounce.utils.timing.TimeUtils.randomClickDelay
import net.ccbluex.liquidbounce.utils.timing.TimeUtils.randomDelay
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.BlockBush
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager.resetColor
import net.minecraft.client.settings.GameSettings
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.client.C0BPacketEntityAction.Action.START_SNEAKING
import net.minecraft.network.play.client.C0BPacketEntityAction.Action.STOP_SNEAKING
import net.minecraft.util.*
import net.minecraft.world.WorldSettings
import net.minecraftforge.event.ForgeEventFactory
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import javax.vecmath.Color3f
import kotlin.math.*


object Scaffold : Module("Scaffold", ModuleCategory.WORLD, Keyboard.KEY_I) {

    private val mode by ListValue("Mode", arrayOf("Normal", "Rewinside", "Expand", "Telly", "GodBridge"), "Normal")

    // Expand
    private val omniDirectionalExpand by BoolValue("OmniDirectionalExpand", false) { mode == "Expand" }
    private val expandLength by IntegerValue("ExpandLength", 1, 1..6) { mode == "Expand" }

    // Placeable delay
    private val placeDelayValue = BoolValue("PlaceDelay", true) { mode != "GodBridge" }

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
        "PlacementAttempt", arrayOf("Fail", "Independent"), "Fail"
    ) { extraClicks }

    // Autoblock
    private val autoBlock by ListValue("AutoBlock", arrayOf("Off", "Pick", "Spoof", "Switch"), "Spoof")
    private val sortByHighestAmount by BoolValue("SortByHighestAmount", false, subjective = true) { autoBlock != "Off" }

    // Basic stuff
    val sprint by BoolValue("Sprint", false)
    private val swing by BoolValue("Swing", true, subjective = true)
    private val down by BoolValue("Down", true) { mode !in arrayOf("GodBridge", "Telly") }

    private val ticksUntilRotation: IntegerValue = object : IntegerValue("TicksUntilRotation", 3, 1..5) {
        override fun isSupported() = mode == "Telly"

        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceIn(minimum, maximum)
    }

    private val jumpAutomatically by BoolValue("JumpAutomatically", true) { mode == "GodBridge" }
    private val maxBlocksToJump: IntegerValue = object : IntegerValue("MaxBlocksToJump", 4, 1..8) {
        override fun isSupported() = mode == "GodBridge" && !jumpAutomatically

        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minBlocksToJump.get())
    }

    private val minBlocksToJump: IntegerValue = object : IntegerValue("MinBlocksToJump", 4, 1..8) {
        override fun isSupported() = mode == "GodBridge" && !jumpAutomatically && !maxBlocksToJump.isMinimal()

        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(maxBlocksToJump.get())
    }

    private val startHorizontally by BoolValue("StartHorizontally", true) { mode == "Telly" }

    private val maxHorizontalPlacements: IntegerValue = object : IntegerValue("MaxHorizontalPlacements", 1, 1..10) {
        override fun isSupported() = mode == "Telly"

        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minHorizontalPlacements.get())
    }

    private val minHorizontalPlacements: IntegerValue = object : IntegerValue("MinHorizontalPlacements", 1, 1..10) {
        override fun isSupported() = mode == "Telly"

        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(maxHorizontalPlacements.get())
    }

    private val maxVerticalPlacements: IntegerValue = object : IntegerValue("MaxVerticalPlacements", 1, 1..10) {
        override fun isSupported() = mode == "Telly"

        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minVerticalPlacements.get())
    }

    private val minVerticalPlacements: IntegerValue = object : IntegerValue("MinVerticalPlacements", 1, 1..10) {
        override fun isSupported() = mode == "Telly"

        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(maxVerticalPlacements.get())
    }

    private val maxJumpTicks: IntegerValue = object : IntegerValue("MaxJumpTicks", 0, 0..10) {
        override fun isSupported() = mode == "Telly"

        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minJumpTicks.get())
    }

    private val minJumpTicks: IntegerValue = object : IntegerValue("MinJumpTicks", 0, 0..10) {
        override fun isSupported() = mode == "Telly"

        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(maxJumpTicks.get())
    }

    private val allowClutching by BoolValue("AllowClutching", true) { mode !in arrayOf("Telly", "Expand") }

    private val horizontalClutchBlocks: IntegerValue = object : IntegerValue("HorizontalClutchBlocks", 3, 1..5) {
        override fun isSupported() = allowClutching && mode !in arrayOf("Telly", "Expand")

        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceIn(minimum, maximum)
    }

    private val verticalClutchBlocks: IntegerValue = object : IntegerValue("VerticalClutchBlocks", 2, 1..3) {
        override fun isSupported() = allowClutching && mode !in arrayOf("Telly", "Expand")

        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceIn(minimum, maximum)
    }

    // Eagle
    private val eagleValue = ListValue("Eagle", arrayOf("Normal", "Silent", "Off"), "Normal") { mode != "GodBridge" }

    val eagle by eagleValue

    private val eagleSpeed by FloatValue("EagleSpeed", 0.3f, 0.3f..1.0f) { eagleValue.isSupported() && eagle != "Off" }
    val eagleSprint by BoolValue("EagleSprint", false) { eagleValue.isSupported() && eagle == "Normal" }
    private val blocksToEagle by IntegerValue("BlocksToEagle", 0, 0..10) { eagleValue.isSupported() && eagle != "Off" }
    private val edgeDistance by FloatValue("EagleEdgeDistance", 0f, 0f..0.5f)
    { eagleValue.isSupported() && eagle != "Off" }

    // Rotation Options
    private val rotationMode by ListValue("Rotations", arrayOf("Off", "Normal", "Stabilized", "GodBridge"), "Normal")
    private val strafe by BoolValue("Strafe", false) { rotationMode != "Off" && silentRotation }
    private val silentRotation by BoolValue("SilentRotation", true) { rotationMode != "Off" }
    private val keepRotation by BoolValue("KeepRotation", true) { rotationMode != "Off" }
    private val keepTicks by object : IntegerValue("KeepTicks", 1, 1..20) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minimum)
        override fun isSupported() = rotationMode != "Off" && mode != "Telly"
    }

    // Search options
    private val searchMode by ListValue("SearchMode", arrayOf("Area", "Center"), "Area") { mode != "GodBridge" }
    private val minDist by FloatValue("MinDist", 0f, 0f..0.2f) { mode !in arrayOf("GodBridge", "Telly") }

    // Turn Speed
    private val maxTurnSpeedValue: FloatValue = object : FloatValue("MaxTurnSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minTurnSpeed)
        override fun isSupported() = rotationMode != "Off"
    }
    private val maxTurnSpeed by maxTurnSpeedValue
    private val minTurnSpeed by object : FloatValue("MinTurnSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceIn(minimum, maxTurnSpeed)

        override fun isSupported() = !maxTurnSpeedValue.isMinimal() && rotationMode != "Off"
    }

    private val angleThresholdUntilReset by FloatValue("AngleThresholdUntilReset", 5f, 0.1f..180f)
    { rotationMode != "Off" }

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

    // Game
    private val timer by FloatValue("Timer", 1f, 0.1f..10f)
    private val speedModifier by FloatValue("SpeedModifier", 1f, 0f..2f)
    private val slow by BoolValue("Slow", false)
    private val slowSpeed by FloatValue("SlowSpeed", 0.6f, 0.2f..0.8f) { slow }

    // Safety
    private val sameY by BoolValue("SameY", false) { mode != "GodBridge" }
    private val safeWalkValue = BoolValue("SafeWalk", true) { mode != "GodBridge" }

    private val airSafe by BoolValue("AirSafe", false) { safeWalkValue.isActive() }

    // Visuals
    private val counterDisplay by BoolValue("Counter", true, subjective = true)
    private val mark by BoolValue("Mark", false, subjective = true)
    private val trackCPS by BoolValue("TrackCPS", false, subjective = true)
    private val safetyLines by BoolValue("SafetyLines", false, subjective = true) { isGodBridgeEnabled }

    // Target placement
    private var placeRotation: PlaceRotation? = null

    // Launch position
    private var launchY = 0
    private val shouldKeepLaunchPosition
        get() = sameY && mode != "GodBridge"

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
        get() = eagle != "Off" && !shouldGoDown && mode != "GodBridge"

    // Downwards
    private val shouldGoDown
        get() = down && !sameY
            && GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)
            && mode !in arrayOf("GodBridge", "Telly")
            && blocksAmount > 1

    // Current rotation
    private val currRotation
        get() = currentRotation ?: mc.thePlayer.rotation

    // Extra clicks
    private var extraClick = ExtraClickInfo(randomClickDelay(extraClickMinCPS, extraClickMaxCPS), 0L, 0)

    // GodBridge
    private var blocksPlacedUntilJump = 0

    private val isManualJumpOptionActive
        get() = mode == "GodBridge" && !jumpAutomatically

    private var blocksToJump = randomDelay(minBlocksToJump.get(), maxBlocksToJump.get())

    private val isGodBridgeEnabled
        get() = mode == "GodBridge" || mode == "Normal" && rotationMode == "GodBridge"

    // Telly
    private var offGroundTicks = 0
    private var ticksUntilJump = 0
    private var blocksUntilAxisChange = 0
    private var jumpTicks = randomDelay(minJumpTicks.get(), maxJumpTicks.get())
    private var horizontalPlacements = randomDelay(minHorizontalPlacements.get(), maxHorizontalPlacements.get())
    private var verticalPlacements = randomDelay(minVerticalPlacements.get(), maxVerticalPlacements.get())
    private val shouldPlaceHorizontally
        get() = mode == "Telly" && isMoving && (startHorizontally && blocksUntilAxisChange <= horizontalPlacements || !startHorizontally && blocksUntilAxisChange > verticalPlacements)

    // Enabling module
    override fun onEnable() {
        val player = mc.thePlayer ?: return

        launchY = player.posY.roundToInt()
        blocksUntilAxisChange = 0
    }

    // Events
    @EventTarget
    private fun onUpdate(event: UpdateEvent) {
        val player = mc.thePlayer ?: return

        if (mc.playerController.currentGameType == WorldSettings.GameType.SPECTATOR)
            return

        mc.timer.timerSpeed = timer

        // Telly
        if (mc.thePlayer.onGround) {
            offGroundTicks = 0
            ticksUntilJump++
        } else {
            offGroundTicks++
        }

        if (shouldGoDown) {
            mc.gameSettings.keyBindSneak.pressed = false
        }

        if (slow) {
            player.motionX *= slowSpeed
            player.motionZ *= slowSpeed
        }

        // Eagle
        if (isEagleEnabled) {
            var dif = 0.5
            val blockPos = BlockPos(player).down()

            for (side in EnumFacing.values()) {
                if (side.axis == EnumFacing.Axis.Y) {
                    continue
                }

                val neighbor = blockPos.offset(side)

                if (isReplaceable(neighbor)) {
                    val calcDif = (if (side.axis == EnumFacing.Axis.Z) {
                        abs(neighbor.z + 0.5 - player.posZ)
                    } else {
                        abs(neighbor.x + 0.5 - player.posX)
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
                        sendPacket(C0BPacketEntityAction(player, if (shouldEagle) START_SNEAKING else STOP_SNEAKING))
                    }
                    eagleSneaking = shouldEagle
                } else {
                    mc.gameSettings.keyBindSneak.pressed = shouldEagle
                    eagleSneaking = shouldEagle
                }
                placedBlocksWithoutEagle = 0
            } else {
                placedBlocksWithoutEagle++
            }
        }

        if (player.onGround) {
            // Still a thing?
            if (mode == "Rewinside") {
                strafe(0.2F)
                player.motionY = 0.0
            }

            when (zitterMode.lowercase()) {
                "off" -> {
                    return
                }

                "smooth" -> {
                    if (!GameSettings.isKeyDown(mc.gameSettings.keyBindRight)) {
                        mc.gameSettings.keyBindRight.pressed = false
                    }
                    if (!GameSettings.isKeyDown(mc.gameSettings.keyBindLeft)) {
                        mc.gameSettings.keyBindLeft.pressed = false
                    }

                    if (zitterTickTimer.hasTimePassed()) {
                        zitterDirection = !zitterDirection
                        zitterTickTimer.reset()
                    } else {
                        zitterTickTimer.update()
                    }

                    if (zitterDirection) {
                        mc.gameSettings.keyBindRight.pressed = true
                        mc.gameSettings.keyBindLeft.pressed = false
                    } else {
                        mc.gameSettings.keyBindRight.pressed = false
                        mc.gameSettings.keyBindLeft.pressed = true
                    }
                }

                "teleport" -> {
                    strafe(zitterSpeed)
                    val yaw = (player.rotationYaw + if (zitterDirection) 90.0 else -90.0).toRadians()
                    player.motionX -= sin(yaw) * zitterStrength
                    player.motionZ += cos(yaw) * zitterStrength
                    zitterDirection = !zitterDirection
                }
            }
        }
    }

    @EventTarget
    fun onStrafe(event: StrafeEvent) {
        val player = mc.thePlayer

        // Jumping needs to be done here, so it doesn't get detected by movement-sensitive anti-cheats.
        if (mode == "Telly" && player.onGround && isMoving && currRotation == player.rotation && !mc.gameSettings.keyBindJump.isKeyDown && ticksUntilJump >= jumpTicks) {
            player.jump()

            ticksUntilJump = 0
            jumpTicks = randomDelay(minJumpTicks.get(), maxJumpTicks.get())
        }
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        val rotation = currentRotation

        if (rotationMode != "Off" && keepRotation && rotation != null) {
            setRotation(rotation, 1)
        }

        if (event.eventState == EventState.POST) {
            update()
        }
    }

    @EventTarget
    fun onTick(event: TickEvent) {
        val target = placeRotation?.placeInfo

        if (extraClicks) {
            while (extraClick.clicks > 0) {
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

        val raycastProperly = !(mode == "Expand" && expandLength > 1 || shouldGoDown) && rotationMode != "Off"

        performBlockRaytrace(currRotation, mc.playerController.blockReachDistance).let {
            if (rotationMode == "Off" || it != null && it.blockPos == target.blockPos && (!raycastProperly || it.sideHit == target.enumFacing)) {
                val result = if (raycastProperly && it != null) {
                    PlaceInfo(it.blockPos, it.sideHit, it.hitVec)
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
        val player = mc.thePlayer ?: return
        val holdingItem = player.heldItem?.item is ItemBlock

        if (!holdingItem && (autoBlock == "Off" || InventoryUtils.findBlockInHotbar() == null)) {
            return
        }

        findBlock(mode == "Expand" && expandLength > 1, searchMode == "Area")
    }

    private fun setRotation(rotation: Rotation, ticks: Int) {
        val player = mc.thePlayer ?: return

        if (silentRotation) {
            if (mode == "Telly" && isMoving) {
                if (offGroundTicks < ticksUntilRotation.get() && ticksUntilJump >= jumpTicks) {
                    return
                }
            }

            setTargetRotation(
                rotation,
                ticks,
                strafe,
                resetSpeed = minTurnSpeed to maxTurnSpeed,
                angleThresholdForReset = angleThresholdUntilReset
            )

        } else {
            rotation.toPlayer(player)
        }
    }

    // Search for new target block
    private fun findBlock(expand: Boolean, area: Boolean) {
        val player = mc.thePlayer ?: return

        val blockPosition = if (shouldGoDown) {
            if (player.posY == player.posY.roundToInt() + 0.5) {
                BlockPos(player.posX, player.posY - 0.6, player.posZ)
            } else {
                BlockPos(player.posX, player.posY - 0.6, player.posZ).down()
            }
        } else if (shouldKeepLaunchPosition && launchY <= player.posY) {
            BlockPos(player.posX, launchY - 1.0, player.posZ)
        } else if (player.posY == player.posY.roundToInt() + 0.5) {
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
            val yaw = player.rotationYaw.toRadiansD()
            val x = if (omniDirectionalExpand) -sin(yaw).roundToInt() else player.horizontalFacing.directionVec.x
            val z = if (omniDirectionalExpand) cos(yaw).roundToInt() else player.horizontalFacing.directionVec.z

            repeat(expandLength) {
                if (search(blockPosition.add(x * it, 0, z * it), false, area))
                    return
            }
            return
        }

        val (f, g) = if (mode == "Telly") 5 to 3 else if (allowClutching) horizontalClutchBlocks.get() to verticalClutchBlocks.get() else 1 to 1

        (-f..f).flatMap { x ->
            (0 downTo -g).flatMap { y ->
                (-f..f).map { z ->
                    Vec3i(x, y, z)
                }
            }
        }.sortedBy {
            getCenterDistance(blockPosition.add(it))
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

    private fun place(placeInfo: PlaceInfo) {
        val player = mc.thePlayer ?: return
        val world = mc.theWorld ?: return

        if (!delayTimer.hasTimePassed() || shouldKeepLaunchPosition && launchY - 1 != placeInfo.vec3.yCoord.toInt())
            return

        var stack = player.inventoryContainer.getSlot(serverSlot + 36).stack

        //TODO: blacklist more blocks than only bushes
        if (stack == null || stack.item !is ItemBlock || (stack.item as ItemBlock).block is BlockBush || stack.stackSize <= 0 || sortByHighestAmount) {
            val blockSlot = if (sortByHighestAmount) {
                InventoryUtils.findLargestBlockStackInHotbar() ?: return
            } else {
                InventoryUtils.findBlockInHotbar() ?: return
            }

            when (autoBlock.lowercase()) {
                "off" -> return

                "pick" -> {
                    player.inventory.currentItem = blockSlot - 36
                    mc.playerController.updateController()
                }

                "spoof", "switch" -> serverSlot = blockSlot - 36
            }
            stack = player.inventoryContainer.getSlot(blockSlot).stack
        }

        tryToPlaceBlock(stack, placeInfo.blockPos, placeInfo.enumFacing, placeInfo.vec3)

        if (autoBlock == "Switch")
            serverSlot = player.inventory.currentItem

        // Since we violate vanilla slot switch logic if we send the packets now, we arrange them for the next tick
        switchBlockNextTickIfPossible(stack)

        if (trackCPS) {
            CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)
        }

        placeRotation = null
    }

    private fun doPlaceAttempt() {
        val player = mc.thePlayer ?: return
        val world = mc.theWorld ?: return

        val stack = player.inventoryContainer.getSlot(serverSlot + 36).stack ?: return

        if (stack.item !is ItemBlock || InventoryUtils.BLOCK_BLACKLIST.contains((stack.item as ItemBlock).block)) {
            return
        }

        val block = stack.item as ItemBlock

        val raytrace = performBlockRaytrace(currRotation, mc.playerController.blockReachDistance) ?: return

        val canPlaceOnUpperFace = block.canPlaceBlockOnSide(
            world, raytrace.blockPos, EnumFacing.UP, player, stack
        )

        val shouldPlace = if (placementAttempt == "Fail") {
            !block.canPlaceBlockOnSide(world, raytrace.blockPos, raytrace.sideHit, player, stack)
        } else {
            if (shouldKeepLaunchPosition) {
                raytrace.blockPos.y == launchY - 1 && !canPlaceOnUpperFace
            } else if (shouldPlaceHorizontally) {
                !canPlaceOnUpperFace
            } else {
                raytrace.blockPos.y <= player.posY.toInt() - 1 && !(raytrace.blockPos.y == player.posY.toInt() - 1 && canPlaceOnUpperFace && raytrace.sideHit == EnumFacing.UP)
            }
        }

        if (raytrace.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || !shouldPlace) {
            return
        }

        tryToPlaceBlock(stack, raytrace.blockPos, raytrace.sideHit, raytrace.hitVec, attempt = true)

        // Since we violate vanilla slot switch logic if we send the packets now, we arrange them for the next tick
        switchBlockNextTickIfPossible(stack)

        if (trackCPS) {
            CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)
        }
    }

    // Disabling module
    override fun onDisable() {
        val player = mc.thePlayer ?: return

        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)) {
            mc.gameSettings.keyBindSneak.pressed = false
            if (eagleSneaking) {
                sendPacket(C0BPacketEntityAction(player, STOP_SNEAKING))
            }
        }

        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindRight)) {
            mc.gameSettings.keyBindRight.pressed = false
        }
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindLeft)) {
            mc.gameSettings.keyBindLeft.pressed = false
        }

        placeRotation = null
        mc.timer.timerSpeed = 1f

        serverSlot = player.inventory.currentItem

        TickScheduler.clear()
    }

    // Entity movement event
    @EventTarget
    fun onMove(event: MoveEvent) {
        val player = mc.thePlayer ?: return

        if (!safeWalkValue.isActive() || shouldGoDown) {
            return
        }

        if (airSafe || player.onGround) {
            event.isSafeWalk = true
        }
    }

    // Scaffold visuals
    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (counterDisplay) {
            glPushMatrix()

            if (BlockOverlay.handleEvents() && BlockOverlay.info && BlockOverlay.currentBlock != null) glTranslatef(0f, 15f, 0f)

            val info = "Blocks: §7$blocksAmount"
            val (width, height) = ScaledResolution(mc)

            drawBorderedRect(
                width / 2 - 2,
                height / 2 + 5,
                width / 2 + Fonts.font40.getStringWidth(info) + 2,
                height / 2 + 16,
                3,
                Color.BLACK.rgb,
                Color.BLACK.rgb
            )

            resetColor()

            Fonts.font40.drawString(
                info, width / 2, height / 2 + 7, Color.WHITE.rgb
            )
            glPopMatrix()
        }
    }

    // Visuals
    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        val player = mc.thePlayer ?: return

        val shouldBother = !(shouldGoDown || mode == "Expand" && expandLength > 1) && extraClicks && isMoving

        if (shouldBother) {
            currRotation.let {
                performBlockRaytrace(it, mc.playerController.blockReachDistance)?.let { raytrace ->
                    val timePassed = System.currentTimeMillis() - extraClick.lastClick >= extraClick.delay

                    if (raytrace.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && timePassed) {
                        extraClick = ExtraClickInfo(
                            randomClickDelay(extraClickMinCPS, extraClickMaxCPS),
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

        repeat(if (mode == "Expand") expandLength + 1 else 2) {
            val yaw = player.rotationYaw.toRadiansD()
            val x = if (omniDirectionalExpand) -sin(yaw).roundToInt() else player.horizontalFacing.directionVec.x
            val z = if (omniDirectionalExpand) cos(yaw).roundToInt() else player.horizontalFacing.directionVec.z
            val blockPos = BlockPos(
                player.posX + x * it,
                if (shouldKeepLaunchPosition && launchY <= player.posY) launchY - 1.0 else player.posY - (if (player.posY == player.posY + 0.5) 0.0 else 1.0) - if (shouldGoDown) 1.0 else 0.0,
                player.posZ + z * it
            )
            val placeInfo = PlaceInfo.get(blockPos)

            if (isReplaceable(blockPos) && placeInfo != null) {
                drawBlockBox(blockPos, Color(68, 117, 255, 100), false)
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
        horizontalOnly: Boolean = false
    ): Boolean {
        val player = mc.thePlayer ?: return false

        if (!isReplaceable(blockPosition)) {
            return false
        }

        val maxReach = mc.playerController.blockReachDistance

        val eyes = player.eyes
        var placeRotation: PlaceRotation? = null

        var currPlaceRotation: PlaceRotation?

        var considerStableRotation: PlaceRotation? = null

        val isLookingDiagonally = run {
            // Round the rotation to the nearest multiple of 45 degrees so that way we check if the player faces diagonally
            val yaw = round(abs(MathHelper.wrapAngleTo180_float(player.rotationYaw)).roundToInt() / 45f) * 45f

            floatArrayOf(45f, 135f).any { yaw == it } && player.movementInput.moveStrafe == 0f
        }

        for (side in EnumFacing.values().filter { !horizontalOnly || it.axis != EnumFacing.Axis.Y }) {
            val neighbor = blockPosition.offset(side)

            if (!canBeClicked(neighbor)) {
                continue
            }

            if (isGodBridgeEnabled) {
                // Selection of these values only. Mostly used by Godbridgers.
                val list = floatArrayOf(-135f, -45f, 45f, 135f)

                // Selection of pitch values that should be OK in non-complex situations.
                val pitchList = 55.0..75.7 + if (isLookingDiagonally) 1.0 else 0.0

                for (yaw in list) {
                    for (pitch in pitchList step 0.1) {
                        val rotation = Rotation(yaw, pitch.toFloat())

                        val raytrace = performBlockRaytrace(rotation, maxReach) ?: continue

                        currPlaceRotation =
                            PlaceRotation(PlaceInfo(raytrace.blockPos, raytrace.sideHit, raytrace.hitVec), rotation)

                        if (raytrace.blockPos == neighbor && raytrace.sideHit == side.opposite) {
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
                    findTargetPlace(blockPosition, neighbor, Vec3(0.5, 0.5, 0.5), side, eyes, maxReach, raycast)
                        ?: continue

                placeRotation = compareDifferences(currPlaceRotation, placeRotation)
            } else {
                for (x in 0.1..0.9) {
                    for (y in 0.1..0.9) {
                        for (z in 0.1..0.9) {
                            currPlaceRotation =
                                findTargetPlace(blockPosition, neighbor, Vec3(x, y, z), side, eyes, maxReach, raycast)
                                    ?: continue

                            placeRotation = compareDifferences(currPlaceRotation, placeRotation)
                        }
                    }
                }
            }
        }

        placeRotation = considerStableRotation ?: placeRotation

        placeRotation ?: return false

        if (rotationMode != "Off") {
            var targetRotation = placeRotation.rotation

            val info = placeRotation.placeInfo

            if (mode == "GodBridge") {
                val shouldJumpForcefully = isManualJumpOptionActive && blocksPlacedUntilJump >= blocksToJump

                performBlockRaytrace(currRotation, maxReach)?.let {
                    if (it.blockPos == info.blockPos && (it.sideHit != info.enumFacing || shouldJumpForcefully) && isMoving && currRotation.yaw.roundToInt() % 45f == 0f) {
                        if (player.onGround && !isLookingDiagonally) {
                            player.jump()
                        }

                        if (shouldJumpForcefully) {
                            blocksPlacedUntilJump = 0
                            blocksToJump = randomDelay(minBlocksToJump.get(), maxBlocksToJump.get())
                        }

                        targetRotation = currRotation
                    }
                }
            }

            val limitedRotation = limitAngleChange(
                currRotation, targetRotation, nextFloat(minTurnSpeed, maxTurnSpeed)
            )

            setRotation(limitedRotation, if (mode == "Telly") 1 else keepTicks)
        }
        this.placeRotation = placeRotation
        return true
    }

    /**
     * For expand scaffold, fixes vector values that should match according to direction vector
     */
    private fun modifyVec(original: Vec3, direction: EnumFacing, pos: Vec3, shouldModify: Boolean): Vec3 {
        if (!shouldModify) {
            return original
        }

        val x = original.xCoord
        val y = original.yCoord
        val z = original.zCoord

        val side = direction.opposite

        return when (side.axis ?: return original) {
            EnumFacing.Axis.Y -> Vec3(x, pos.yCoord + side.directionVec.y.coerceAtLeast(0), z)
            EnumFacing.Axis.X -> Vec3(pos.xCoord + side.directionVec.x.coerceAtLeast(0), y, z)
            EnumFacing.Axis.Z -> Vec3(x, y, pos.zCoord + side.directionVec.z.coerceAtLeast(0))
        }

    }

    private fun findTargetPlace(
        pos: BlockPos, offsetPos: BlockPos, vec3: Vec3, side: EnumFacing, eyes: Vec3, maxReach: Float, raycast: Boolean
    ): PlaceRotation? {
        val world = mc.theWorld ?: return null

        val vec = (Vec3(pos) + vec3).addVector(
            side.directionVec.x * vec3.xCoord, side.directionVec.y * vec3.yCoord, side.directionVec.z * vec3.zCoord
        )

        val distance = eyes.distanceTo(vec)

        if (raycast && (distance > maxReach || world.rayTraceBlocks(eyes, vec, false, true, false) != null)) {
            return null
        }

        val diff = vec - eyes

        if (side.axis != EnumFacing.Axis.Y) {
            val dist = abs(if (side.axis == EnumFacing.Axis.Z) diff.zCoord else diff.xCoord)

            if (dist < minDist && mode != "Telly") {
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
            if (raytrace.blockPos == offsetPos && (!raycast || raytrace.sideHit == side.opposite)) {
                return PlaceRotation(
                    PlaceInfo(
                        raytrace.blockPos, side.opposite, modifyVec(raytrace.hitVec, side, Vec3(offsetPos), !raycast)
                    ), currRotation
                )
            }
        }

        val raytrace = performBlockRaytrace(rotation, maxReach) ?: return null

        if (raytrace.blockPos == offsetPos && (!raycast || raytrace.sideHit == side.opposite)) {
            return PlaceRotation(
                PlaceInfo(
                    raytrace.blockPos, side.opposite, modifyVec(raytrace.hitVec, side, Vec3(offsetPos), !raycast)
                ), rotation
            )
        }

        return null
    }

    private fun performBlockRaytrace(rotation: Rotation, maxReach: Float): MovingObjectPosition? {
        val player = mc.thePlayer ?: return null
        val world = mc.theWorld ?: return null

        val eyes = player.eyes
        val rotationVec = getVectorForRotation(rotation)

        val reach = eyes + (rotationVec * maxReach.toDouble())

        return world.rayTraceBlocks(eyes, reach, false, false, true)
    }

    private fun compareDifferences(
        new: PlaceRotation, old: PlaceRotation?, rotation: Rotation = currRotation
    ): PlaceRotation {
        if (old == null || getRotationDifference(new.rotation, rotation) < getRotationDifference(
                old.rotation, rotation
            )
        ) {
            return new
        }

        return old
    }

    private fun switchBlockNextTickIfPossible(stack: ItemStack) {
        val player = mc.thePlayer ?: return

        if (autoBlock !in arrayOf("Off", "Switch") && stack.stackSize <= 0) {
            InventoryUtils.findBlockInHotbar()?.let {
                TickScheduler += {
                    if (autoBlock == "Pick") {
                        player.inventory.currentItem = it - 36
                        mc.playerController.updateController()
                    } else {
                        serverSlot = it - 36
                    }
                }
            }
        }
    }

    private fun displaySafetyLinesIfEnabled() {
        if (!safetyLines || !isGodBridgeEnabled) {
            return
        }

        val player = mc.thePlayer ?: return

        // If player is not walking diagonally then continue
        if (round(abs(MathHelper.wrapAngleTo180_float(player.rotationYaw)).roundToInt() / 45f) * 45f !in arrayOf(
                135f,
                45f
            ) || player.movementInput.moveForward == 0f || player.movementInput.moveStrafe != 0f
        ) {
            val (posX, posY, posZ) = player.interpolatedPosition()

            glPushMatrix()
            glTranslated(-posX, -posY, -posZ)
            glLineWidth(5.5f)
            glDisable(GL_TEXTURE_2D)

            val (yawX, yawZ) = player.horizontalFacing.directionVec.x * 1.5 to player.horizontalFacing.directionVec.z * 1.5

            // The target rotation will either be the module's placeRotation or a forced rotation (usually that's where the GodBridge mode aims)
            val targetRotation = run {
                val yaw = floatArrayOf(-135f, -45f, 45f, 135f).minByOrNull {
                    abs(getAngleDifference(it, MathHelper.wrapAngleTo180_float(currRotation.yaw)))
                } ?: return

                placeRotation?.rotation ?: Rotation(yaw, 73f)
            }

            // Calculate color based on rotation difference
            val color = getColorForRotationDifference(getRotationDifference(targetRotation, currRotation))

            val main = BlockPos(player).down()

            val pos = if (canBeClicked(main)) {
                main
            } else {
                (-1..1).flatMap { x ->
                    (-1..1).map { z ->
                        val neighbor = main.add(x, 0, z)

                        neighbor to getCenterDistance(neighbor)
                    }
                }.filter { canBeClicked(it.first) }.minByOrNull { it.second }?.first ?: main
            }.up().getVec()

            for (offset in 0..1) {
                for (i in -1..1 step 2) {
                    for (x1 in 0.25..0.5 step 0.01) {
                        val opposite = offset == 1

                        val (offsetX, offsetZ) = if (opposite) 0.0 to x1 * i else x1 * i to 0.0
                        val (lineX, lineZ) = if (opposite) yawX to 0.0 else 0.0 to yawZ

                        val (x, y, z) = pos.add(Vec3(offsetX, -0.99, offsetZ))

                        glBegin(GL_LINES)

                        glColor3f(color.x, color.y, color.z)
                        glVertex3d(x - lineX, y + 0.5, z - lineZ)
                        glVertex3d(x + lineX, y + 0.5, z + lineZ)

                        glEnd()
                    }
                }
            }
            glEnable(GL_TEXTURE_2D)
            glPopMatrix()
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

            horizontalPlacements = randomDelay(minHorizontalPlacements.get(), maxHorizontalPlacements.get())
            verticalPlacements = randomDelay(minVerticalPlacements.get(), maxVerticalPlacements.get())
            return
        }

        blocksUntilAxisChange++
    }

    private fun tryToPlaceBlock(
        stack: ItemStack,
        hitPos: BlockPos,
        side: EnumFacing,
        hitVec: Vec3,
        attempt: Boolean = false
    ): Boolean {
        val (facingX, facingY, facingZ) = (hitVec - hitPos.toVec()).toFloatTriple()
        val hitState = getState(hitPos)
        val item = stack.item
        val prevSize = stack.stackSize

        // Adjusted mc.playerController.onPlayerRightClick() that sends correct stack in its C08
        val clickedSuccessfully = run {
            if (item !is ItemBlock)
                return@run false

            if (!mc.theWorld.worldBorder.contains(hitPos))
                return@run false

            if (!item.canPlaceBlockOnSide(mc.theWorld, hitPos, side, mc.thePlayer, stack))
                return@run false

            sendPacket(
                C08PacketPlayerBlockPlacement(hitPos, side.index, stack, facingX, facingY, facingZ)
            )

            if (!mc.thePlayer.isSneaking && hitState?.block?.onBlockActivated(
                    mc.theWorld, hitPos, hitState, mc.thePlayer,
                    side, facingX, facingY, facingZ
                ) == true
            )
                return@run true

            val prevMetadata = stack.metadata

            stack.onItemUse(mc.thePlayer, mc.theWorld, hitPos, side, facingX, facingY, facingZ).also {
                if (mc.playerController.isInCreativeMode) {
                    stack.itemDamage = prevMetadata
                    stack.stackSize = prevSize
                }
            }
        }

        if (clickedSuccessfully) {
            if (!attempt) {
                delayTimer.reset()

                if (mc.thePlayer.onGround) {
                    mc.thePlayer.motionX *= speedModifier
                    mc.thePlayer.motionZ *= speedModifier
                }
            }

            if (swing) mc.thePlayer.swingItem()
            else sendPacket(C0APacketAnimation())

            if (isManualJumpOptionActive)
                blocksPlacedUntilJump++

            updatePlacedBlocksForTelly()

            if (stack.stackSize <= 0) {
                mc.thePlayer.inventory.mainInventory[serverSlot] = null
                ForgeEventFactory.onPlayerDestroyItem(mc.thePlayer, stack)
            } else if (stack.stackSize != prevSize || mc.playerController.isInCreativeMode)
                mc.entityRenderer.itemRenderer.resetEquippedProgress()

        } else {
            // Adjusted mc.playerController.sendUseItem() that sends correct stack in its C08
            sendPacket(C08PacketPlayerBlockPlacement(stack))
            val newStack = stack.useItemRightClick(mc.theWorld, mc.thePlayer)

            if (newStack != stack || newStack.stackSize != prevSize) {
                if (newStack.stackSize <= 0) {
                    mc.thePlayer.inventory.mainInventory[serverSlot] = null
                    ForgeEventFactory.onPlayerDestroyItem(mc.thePlayer, newStack)
                } else
                    mc.thePlayer.inventory.mainInventory[serverSlot] = newStack

                mc.entityRenderer.itemRenderer.resetEquippedProgress2()
            }
        }

        return clickedSuccessfully
    }

    /**
     * Returns the amount of blocks
     */
    private val blocksAmount: Int
        get() {
            var amount = 0
            for (i in 36..44) {
                val stack = mc.thePlayer.inventoryContainer.getSlot(i).stack ?: continue
                val item = stack.item
                if (item is ItemBlock) {
                    val block = item.block
                    val heldItem = mc.thePlayer.heldItem
                    if (heldItem != null && heldItem == stack || block !in InventoryUtils.BLOCK_BLACKLIST && block !is BlockBush) {
                        amount += stack.stackSize
                    }
                }
            }
            return amount
        }

    override val tag
        get() = mode

    data class ExtraClickInfo(val delay: Int, val lastClick: Long, var clicks: Int)
}
