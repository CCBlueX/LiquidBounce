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
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.InventoryUtils.serverSlot
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.PlaceRotation
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils.getRotationDifference
import net.ccbluex.liquidbounce.utils.RotationUtils.getVectorForRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.limitAngleChange
import net.ccbluex.liquidbounce.utils.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.targetRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.block.BlockUtils.canBeClicked
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isReplaceable
import net.ccbluex.liquidbounce.utils.block.PlaceInfo
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBlockBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBorderedRect
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils.randomClickDelay
import net.ccbluex.liquidbounce.utils.timer.TimeUtils.randomDelay
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.BlockBush
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager.resetColor
import net.minecraft.client.settings.GameSettings
import net.minecraft.item.ItemBlock
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.client.C0BPacketEntityAction.Action.START_SNEAKING
import net.minecraft.network.play.client.C0BPacketEntityAction.Action.STOP_SNEAKING
import net.minecraft.util.*
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import kotlin.math.*

object Scaffold : Module("Scaffold", ModuleCategory.WORLD, Keyboard.KEY_I) {

    private val mode by ListValue("Mode", arrayOf("Normal", "Rewinside", "Expand", "GodBridge"), "Normal")

    // Expand
    private val omniDirectionalExpand by BoolValue("OmniDirectionalExpand", false) { mode == "Expand" }
    private val expandLength by IntegerValue("ExpandLength", 1, 1..6) { mode == "Expand" }

    // Placeable delay
    private val placeDelay: BoolValue = object : BoolValue("PlaceDelay", true) {
        override fun isSupported() = mode != "GodBridge"
    }

    private val maxDelayValue: IntegerValue = object : IntegerValue("MaxDelay", 0, 0..1000) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minDelay)

        override fun isSupported() = placeDelay.isActive()
    }
    private val maxDelay by maxDelayValue

    private val minDelay by object : IntegerValue("MinDelay", 0, 0..1000) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(maxDelay)

        override fun isSupported() = placeDelay.isActive() && !maxDelayValue.isMinimal()
    }

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

    // Basic stuff
    val sprint by BoolValue("Sprint", false)
    private val swing by BoolValue("Swing", true)
    private val search by BoolValue("Search", true)
    private val down by BoolValue("Down", true) { mode != "GodBridge" }

    private val jumpAutomatically by BoolValue("JumpAutomatically", true) { mode == "GodBridge" }
    private val preFallChanceBlocksJump by IntegerValue(
        "PreFallChanceBlocksJump", 4, 1..8
    ) { mode == "GodBridge" && !jumpAutomatically }

    // Eagle
    private val eagleValue: ListValue = object : ListValue("Eagle", arrayOf("Normal", "Silent", "Off"), "Normal") {
        override fun isSupported() = mode != "GodBridge"
    }

    val eagle by eagleValue

    private val eagleSpeed by FloatValue("EagleSpeed", 0.3f, 0.3f..1.0f) { eagleValue.isSupported() && eagle != "Off" }
    val eagleSprint by BoolValue("EagleSprint", false) { eagleValue.isSupported() && eagle == "Normal" }
    private val blocksToEagle by IntegerValue("BlocksToEagle", 0, 0..10) { eagleValue.isSupported() && eagle != "Off" }
    private val edgeDistance by FloatValue(
        "EagleEdgeDistance", 0f, 0f..0.5f
    ) { eagleValue.isSupported() && eagle != "Off" }

    // Rotation Options
    private val rotations by BoolValue("Rotations", true)
    private val strafe by BoolValue("Strafe", false)
    private val stabilizedRotation by BoolValue("StabilizedRotation", false) { mode != "GodBridge" }
    private val silentRotation by BoolValue("SilentRotation", true)
    private val keepRotation by BoolValue("KeepRotation", true)
    private val keepTicks by object : IntegerValue("KeepTicks", 1, 1..20) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minimum)
    }

    // Search options
    private val searchMode by ListValue("SearchMode", arrayOf("Area", "Center"), "Area") { mode != "GodBridge" }
    private val minDist by FloatValue("MinDist", 0f, 0f..0.2f) { mode != "GodBridge" }

    // Turn Speed
    private val maxTurnSpeedValue: FloatValue = object : FloatValue("MaxTurnSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minTurnSpeed)
    }
    private val maxTurnSpeed by maxTurnSpeedValue
    private val minTurnSpeed by object : FloatValue("MinTurnSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxTurnSpeed)

        override fun isSupported() = !maxTurnSpeedValue.isMinimal()
    }

    private val angleThresholdUntilReset by FloatValue("AngleThresholdUntilReset", 5f, 0.1f..180f)

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
    private val safeWalk: BoolValue = object : BoolValue("SafeWalk", true) {
        override fun isSupported() = mode != "GodBridge"
    }

    private val airSafe by BoolValue("AirSafe", false) { safeWalk.isActive() }

    // Visuals
    private val counterDisplay by BoolValue("Counter", true)
    private val mark by BoolValue("Mark", false)
    private val trackCPS by BoolValue("TrackCPS", false)

    // Target placement
    private var targetPlace: PlaceInfo? = null

    // Launch position
    private var launchY = 0
    private val shouldKeepLaunchPosition
        get() = sameY && mode != "GodBridge"

    // Zitter
    private var zitterDirection = false
    private var ticksSinceZitter = 0

    // Delay
    private val delayTimer = MSTimer()
    private val zitterTimer = MSTimer()
    private var zitterTicks = randomDelay(minZitterTicks, maxZitterTicks)
    private var delay = 0

    // Eagle
    private var placedBlocksWithoutEagle = 0
    var eagleSneaking = false
    private val isEagleEnabled
        get() = eagle != "Off" && !shouldGoDown && mode != "GodBridge"

    // Downwards
    private val shouldGoDown
        get() = down && !sameY && GameSettings.isKeyDown(mc.gameSettings.keyBindSneak) && mode != "GodBridge" && blocksAmount > 1

    // Current rotation
    private val currRotation
        get() = targetRotation ?: mc.thePlayer.rotation

    // Extra clicks
    private var extraClick = ExtraClickInfo(randomClickDelay(extraClickMinCPS, extraClickMaxCPS), 0L, 0)

    // GodBridge
    private var blocksPlacedUntilJump = 0
    private val isManualJumpOptionActive
        get() = mode == "GodBridge" && !jumpAutomatically

    // Enabling module
    override fun onEnable() {
        val player = mc.thePlayer ?: return

        launchY = player.posY.roundToInt()
    }

    // Events
    @EventTarget
    private fun onUpdate(event: UpdateEvent) {
        val player = mc.thePlayer ?: return

        mc.timer.timerSpeed = timer

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

                    if (ticksSinceZitter >= zitterTicks) {
                        ticksSinceZitter = 0
                        zitterDirection = !zitterDirection
                        zitterTicks = randomDelay(minZitterTicks, maxZitterTicks)
                    } else {
                        ticksSinceZitter++
                        zitterTimer.reset()
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
    fun onMotion(event: MotionEvent) {
        val rotation = targetRotation

        if (rotations && keepRotation && rotation != null) {
            setRotation(rotation, 1)
        }

        if (event.eventState == EventState.POST) {
            update()
        }
    }

    @EventTarget
    fun onTick(event: TickEvent) {
        val target = targetPlace

        if (extraClicks) {
            while (extraClick.clicks > 0) {
                extraClick.clicks--

                doPlaceAttempt()
            }
        }

        if (target == null) {
            if (placeDelay.isActive()) {
                delayTimer.reset()
            }
            return
        }

        val raycastProperly = !(mode == "Expand" && expandLength > 1 || shouldGoDown)

        performBlockRaytrace(currRotation, mc.playerController.blockReachDistance).let {
            if (it != null && it.blockPos == target.blockPos && (!raycastProperly || it.sideHit == target.enumFacing)) {
                val result = if (raycastProperly) {
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

        if (!expand && (!isReplaceable(blockPosition) || search(blockPosition, !shouldGoDown, area))) {
            return
        }

        if (expand) {
            val yaw = player.rotationYaw.toRadiansD()
            val x = if (omniDirectionalExpand) -sin(yaw).roundToInt() else player.horizontalFacing.directionVec.x
            val z = if (omniDirectionalExpand) cos(yaw).roundToInt() else player.horizontalFacing.directionVec.z
            for (i in 0 until expandLength) {
                if (search(blockPosition.add(x * i, 0, z * i), false, area)) {
                    return
                }
            }
        } else if (search) {
            for (x in -1..1) {
                if (search(blockPosition.add(x, 0, 0), !shouldGoDown, area)) {
                    return
                }
            }
            for (z in -1..1) {
                if (search(blockPosition.add(0, 0, z), !shouldGoDown, area)) {
                    return
                }
            }
        }
    }

    private fun place(placeInfo: PlaceInfo) {
        val player = mc.thePlayer ?: return
        val world = mc.theWorld ?: return

        if (!delayTimer.hasTimePassed(delay) || shouldKeepLaunchPosition && launchY - 1 != placeInfo.vec3.yCoord.toInt()) {
            return
        }

        var itemStack = player.heldItem

        //TODO: blacklist more blocks than only bushes
        if (itemStack == null || itemStack.item !is ItemBlock || (itemStack.item as ItemBlock).block is BlockBush || player.heldItem.stackSize <= 0) {
            val blockSlot = InventoryUtils.findBlockInHotbar() ?: return

            when (autoBlock.lowercase()) {
                "off" -> return

                "pick" -> {
                    player.inventory.currentItem = blockSlot - 36
                    mc.playerController.updateController()
                }

                "spoof", "switch" -> {
                    if (blockSlot - 36 != serverSlot) {
                        sendPacket(C09PacketHeldItemChange(blockSlot - 36))
                    }
                }
            }
            itemStack = player.inventoryContainer.getSlot(blockSlot).stack
        }

        if (mc.playerController.onPlayerRightClick(
                player, world, itemStack, placeInfo.blockPos, placeInfo.enumFacing, placeInfo.vec3
            )
        ) {
            delayTimer.reset()
            delay = if (!placeDelay.isActive()) 0 else randomDelay(minDelay, maxDelay)

            if (player.onGround) {
                player.motionX *= speedModifier
                player.motionZ *= speedModifier
            }

            if (swing) {
                player.swingItem()
            } else {
                sendPacket(C0APacketAnimation())
            }

            if (isManualJumpOptionActive) {
                blocksPlacedUntilJump++
            }
        } else {
            if (mc.playerController.sendUseItem(player, world, itemStack)) {
                mc.entityRenderer.itemRenderer.resetEquippedProgress2()
            }
        }

        if (autoBlock == "Switch") {
            if (serverSlot != player.inventory.currentItem) {
                sendPacket(C09PacketHeldItemChange(player.inventory.currentItem))
            }
        }

        if (trackCPS) {
            CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)
        }

        targetPlace = null
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

        val isOnTheSamePos = raytrace.blockPos.x == player.posX.toInt() && raytrace.blockPos.z == player.posZ.toInt()

        val isBlockBelowPlayer = if (shouldKeepLaunchPosition) {
            raytrace.blockPos.y == launchY - 1 && !block.canPlaceBlockOnSide(
                world, raytrace.blockPos, EnumFacing.UP, player, stack
            )
        } else {
            raytrace.blockPos.y <= player.posY - 1 && (placementAttempt == "Independent" && isOnTheSamePos || !block.canPlaceBlockOnSide(
                world, raytrace.blockPos, EnumFacing.UP, player, stack
            ))
        }

        val shouldPlace = placementAttempt == "Independent" || !block.canPlaceBlockOnSide(
            world, raytrace.blockPos, raytrace.sideHit, player, stack
        )

        if (raytrace.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || !isBlockBelowPlayer || !shouldPlace) {
            return
        }

        if (mc.playerController.onPlayerRightClick(
                player, world, stack, raytrace.blockPos, raytrace.sideHit, raytrace.hitVec
            )
        ) {
            if (swing) {
                player.swingItem()
            } else {
                sendPacket(C0APacketAnimation())
            }

            if (isManualJumpOptionActive) {
                blocksPlacedUntilJump++
            }
        } else {
            if (mc.playerController.sendUseItem(player, world, stack)) {
                mc.entityRenderer.itemRenderer.resetEquippedProgress2()
            }
        }

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

        targetPlace = null
        mc.timer.timerSpeed = 1f

        if (serverSlot != player.inventory.currentItem) {
            sendPacket(C09PacketHeldItemChange(player.inventory.currentItem))
        }
    }

    // Entity movement event
    @EventTarget
    fun onMove(event: MoveEvent) {
        val player = mc.thePlayer ?: return

        if (!safeWalk.isActive() || shouldGoDown) {
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

            if (BlockOverlay.state && BlockOverlay.info && BlockOverlay.currentBlock != null) glTranslatef(0f, 15f, 0f)

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

        if (!mark) {
            return
        }

        for (i in 0 until if (mode == "Expand") expandLength + 1 else 2) {
            val yaw = player.rotationYaw.toRadiansD()
            val x = if (omniDirectionalExpand) -sin(yaw).roundToInt() else player.horizontalFacing.directionVec.x
            val z = if (omniDirectionalExpand) cos(yaw).roundToInt() else player.horizontalFacing.directionVec.z
            val blockPos = BlockPos(
                player.posX + x * i,
                if (shouldKeepLaunchPosition && launchY <= player.posY) launchY - 1.0 else player.posY - (if (player.posY == player.posY + 0.5) 0.0 else 1.0) - if (shouldGoDown) 1.0 else 0.0,
                player.posZ + z * i
            )
            val placeInfo = PlaceInfo.get(blockPos)

            if (isReplaceable(blockPos) && placeInfo != null) {
                drawBlockBox(blockPos, Color(68, 117, 255, 100), false)
                break
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

    private fun search(blockPosition: BlockPos, raycast: Boolean, area: Boolean): Boolean {
        val player = mc.thePlayer ?: return false

        if (!isReplaceable(blockPosition)) {
            return false
        }

        val maxReach = mc.playerController.blockReachDistance

        val eyes = player.eyes
        var placeRotation: PlaceRotation? = null

        var currPlaceRotation: PlaceRotation?

        var considerStablePitch: PlaceRotation? = null

        val isLookingDiagonally = run {
            val yaw = abs(MathHelper.wrapAngleTo180_float(player.rotationYaw))

            arrayOf(45f, 135f).any { yaw in it - 10f..it + 10f } && player.movementInput.moveStrafe == 0f
        }

        for (side in EnumFacing.values()) {
            val neighbor = blockPosition.offset(side)

            if (!canBeClicked(neighbor)) {
                continue
            }

            if (mode == "GodBridge") {
                // Selection of these values only. Mostly used by Godbridgers.
                val list = arrayOf(-135f, -45f, 45f, 135f)

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
                                considerStablePitch = compareDifferences(currPlaceRotation, considerStablePitch)
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

        placeRotation = considerStablePitch ?: placeRotation

        placeRotation ?: return false

        if (rotations) {
            var targetRotation = placeRotation.rotation

            val info = placeRotation.placeInfo

            if (mode == "GodBridge") {
                val shouldJumpForcefully = isManualJumpOptionActive && blocksPlacedUntilJump >= preFallChanceBlocksJump

                performBlockRaytrace(currRotation, maxReach)?.let {
                    if (it.blockPos == info.blockPos && (it.sideHit != info.enumFacing || shouldJumpForcefully) && isMoving && currRotation.yaw.roundToInt() % 45f == 0f) {
                        if (player.onGround && !isLookingDiagonally) {
                            player.jump()
                        }

                        if (shouldJumpForcefully) {
                            blocksPlacedUntilJump = 0
                        }

                        targetRotation = currRotation
                    }
                }
            }

            val limitedRotation = limitAngleChange(
                currRotation, targetRotation, nextFloat(minTurnSpeed, maxTurnSpeed)
            )

            setRotation(limitedRotation, keepTicks)
        }
        targetPlace = placeRotation.placeInfo
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

            if (dist < minDist) {
                return null
            }
        }

        var rotation = toRotation(vec, false)

        rotation = if (stabilizedRotation) {
            Rotation(round(rotation.yaw / 45f) * 45f, rotation.pitch)
        } else {
            rotation
        }

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
