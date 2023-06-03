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
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.InventoryUtils.sendSlotChange
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.PlaceRotation
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils.getRotationDifference
import net.ccbluex.liquidbounce.utils.RotationUtils.getVectorForRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.limitAngleChange
import net.ccbluex.liquidbounce.utils.RotationUtils.serverRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.targetRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.block.BlockUtils.canBeClicked
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isReplaceable
import net.ccbluex.liquidbounce.utils.block.PlaceInfo
import net.ccbluex.liquidbounce.utils.extensions.eyes
import net.ccbluex.liquidbounce.utils.extensions.rotation
import net.ccbluex.liquidbounce.utils.extensions.toRadians
import net.ccbluex.liquidbounce.utils.extensions.toRadiansD
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
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

object Scaffold : Module("Scaffold", ModuleCategory.WORLD, Keyboard.KEY_I) {

    private val mode by ListValue("Mode", arrayOf("Normal", "Rewinside", "Expand"), "Normal")

    // Placeable delay
    private val placeDelay = BoolValue("PlaceDelay", true)

    private val extraClicks = BoolValue("DoExtraClicks", false)

    private val extraClickMaxCPSValue: IntegerValue = object : IntegerValue("ExtraClickMaxCPS", 7, 0..20) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(extraClickMinCPS)

        override fun isSupported() = extraClicks.isActive()
    }
    private val extraClickMaxCPS by extraClickMaxCPSValue

    private val extraClickMinCPS by object : IntegerValue("ExtraClickMinCPS", 3, 0..20) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(extraClickMaxCPS)

        override fun isSupported() = extraClicks.isActive() && !extraClickMaxCPSValue.isMinimal()
    }

    // Delay
    private val maxDelayValue: IntegerValue = object : IntegerValue("MaxDelay", 0, 0..1000) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minDelay)

        override fun isSupported() = placeDelay.get()
    }
    private val maxDelay by maxDelayValue

    private val minDelay by object : IntegerValue("MinDelay", 0, 0..1000) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(maxDelay)

        override fun isSupported() = placeDelay.get() && !maxDelayValue.isMinimal()
    }

    // Autoblock
    private val autoBlock by ListValue("AutoBlock", arrayOf("Off", "Pick", "Spoof", "Switch"), "Spoof")

    // Basic stuff
    val sprint by BoolValue("Sprint", false)
    private val swing by BoolValue("Swing", true)
    private val search by BoolValue("Search", true)
    private val down by BoolValue("Down", true)

    // Eagle
    private val eagle by ListValue("Eagle", arrayOf("Normal", "Silent", "Off"), "Normal")
    private val blocksToEagle by IntegerValue("BlocksToEagle", 0, 0..10) { eagle != "Off" }
    private val edgeDistance by FloatValue("EagleEdgeDistance", 0f, 0f..0.5f) { eagle != "Off" }

    // Expand
    private val omniDirectionalExpand = BoolValue("OmniDirectionalExpand", false)
    private val expandLength by IntegerValue("ExpandLength", 1, 1..6)

    // Rotation Options
    private val rotations by BoolValue("Rotations", true)
    private val strafe by BoolValue("Strafe", false)
    private val stabilizedRotation = BoolValue("StabilizedRotation", false)
    private val silentRotation by BoolValue("SilentRotation", true)
    private val keepRotation by BoolValue("KeepRotation", true)
    private val keepTicksValue = object : IntegerValue("KeepTicks", 1, 1..20) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minimum)
    }

    // Search options
    private val searchMode = ListValue("SearchMode", arrayOf("Area", "Center"), "Area")
    private val minDist by FloatValue("MinDist", 0f, 0f..0.2f)

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
    private val zitterMode = ListValue("Zitter", arrayOf("Off", "Teleport", "Smooth"), "Off")
    private val zitterSpeed = FloatValue("ZitterSpeed", 0.13f, 0.1f..0.3f) { zitterMode.get() == "Teleport" }
    private val zitterStrength = FloatValue("ZitterStrength", 0.05f, 0f..0.2f) { zitterMode.get() == "Teleport" }

    // Game
    private val timer by FloatValue("Timer", 1f, 0.1f..10f)
    private val speedModifier by FloatValue("SpeedModifier", 1f, 0f..2f)
    private val slow by BoolValue("Slow", false)
    private val slowSpeed = FloatValue("SlowSpeed", 0.6f, 0.2f..0.8f) { slow }

    // Safety
    private val sameY by BoolValue("SameY", false)
    private val safeWalk by BoolValue("SafeWalk", true)
    private val airSafe by BoolValue("AirSafe", false)

    // Visuals
    private val counterDisplay by BoolValue("Counter", true)
    private val mark by BoolValue("Mark", false)

    // Target placement
    private var targetPlace: PlaceInfo? = null

    // Launch position
    private var launchY = 0

    // AutoBlock
    private var slot = -1

    // Zitter Direction
    private var zitterDirection = false

    // Delay
    private val delayTimer = MSTimer()
    private val zitterTimer = MSTimer()
    private var delay = 0

    // Eagle
    private var placedBlocksWithoutEagle = 0
    private var eagleSneaking = false

    // Downwards
    private val shouldGoDown
        get() = down && !sameY && GameSettings.isKeyDown(mc.gameSettings.keyBindSneak) && blocksAmount > 1

    // Current rotation
    private val currRotation
        get() = targetRotation ?: mc.thePlayer?.rotation ?: serverRotation

    // Extra clicks
    private var extraClick = ExtraClickInfo(randomClickDelay(extraClickMinCPS, extraClickMaxCPS), 0L, 0)

    // Enabling module
    override fun onEnable() {
        val player = mc.thePlayer ?: return

        launchY = player.posY.roundToInt()
        slot = player.inventory.currentItem
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
            player.motionX *= slowSpeed.get()
            player.motionZ *= slowSpeed.get()
        }

        // Eagle
        if (eagle != "Off" && !shouldGoDown) {
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
                }
                placedBlocksWithoutEagle = 0
            } else {
                placedBlocksWithoutEagle++
            }
        }

        if (player.onGround) {
            if (mode == "Rewinside") {
                strafe(0.2F)
                player.motionY = 0.0
            }
            when (zitterMode.get().lowercase()) {
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

                    if (zitterTimer.hasTimePassed(100)) {
                        zitterDirection = !zitterDirection
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
                    strafe(zitterSpeed.get())
                    val yaw = (player.rotationYaw + if (zitterDirection) 90.0 else -90.0).toRadians()
                    player.motionX -= sin(yaw) * zitterStrength.get()
                    player.motionZ += cos(yaw) * zitterStrength.get()
                    zitterDirection = !zitterDirection
                }
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (mc.thePlayer == null) {
            return
        }

        val packet = event.packet

        if (packet is C09PacketHeldItemChange) {
            if (slot == packet.slotId) {
                event.cancelEvent()
                return
            }

            slot = packet.slotId
        }
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        val rotation = targetRotation

        if (rotations && keepRotation && rotation != null) {
            setRotation(rotation, keepTicksValue.minimum)
        }

        if (event.eventState == EventState.POST) {
            update()
        }
    }

    @EventTarget
    fun onTick(event: TickEvent) {
        val target = targetPlace

        if (extraClicks.get()) {
            while (extraClick.clicks > 0) {
                extraClick.clicks--

                doPlaceAttempt()
            }
        }

        if (target == null) {
            if (placeDelay.get()) {
                delayTimer.reset()
            }
            return
        }

        val raycastProperly = !(mode == "Expand" && expandLength > 1 || shouldGoDown)

        performBlockRaytrace(currRotation, mc.playerController.blockReachDistance).let {
            if (it != null && it.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && it.blockPos == target.blockPos && (!raycastProperly || it.sideHit == target.enumFacing)) {
                val result = if (raycastProperly) {
                    PlaceInfo(it.blockPos, it.sideHit, it.hitVec)
                } else {
                    target
                }

                place(result)
            }
        }
    }

    fun update() {
        val player = mc.thePlayer ?: return
        val holdingItem = player.heldItem?.item is ItemBlock

        if (!holdingItem && (autoBlock == "Off" || InventoryUtils.findAutoBlockBlock() == -1)) {
            return
        }

        findBlock(mode == "Expand" && expandLength > 1, searchMode.get() == "Area")
    }

    private fun setRotation(rotation: Rotation, ticks: Int) {
        val player = mc.thePlayer ?: return

        if (silentRotation) {
            setTargetRotation(
                rotation,
                ticks,
                strafe,
                resetSpeed = Pair(minTurnSpeed, maxTurnSpeed),
                angleThresholdForReset = angleThresholdUntilReset
            )
        } else {
            rotation.fixedSensitivity().let {
                player.rotationYaw = it.yaw
                player.rotationPitch = it.pitch
            }
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
        } else if (sameY && launchY <= player.posY) {
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
            val x = if (omniDirectionalExpand.get()) -sin(yaw).roundToInt() else player.horizontalFacing.directionVec.x
            val z = if (omniDirectionalExpand.get()) cos(yaw).roundToInt() else player.horizontalFacing.directionVec.z
            for (i in 0 until expandLength) {
                if (search(blockPosition.add(x * i, 0, z * i), false, area)) {
                    return
                }
            }
        } else if (search) {
            for (x in -1..1) {
                for (z in -1..1) {
                    if (search(blockPosition.add(x, 0, z), !shouldGoDown, area)) {
                        return
                    }
                }
            }
        }
    }

    private fun place(placeInfo: PlaceInfo) {
        val player = mc.thePlayer ?: return
        val world = mc.theWorld ?: return

        if (!delayTimer.hasTimePassed(delay) || sameY && launchY - 1 != placeInfo.vec3.yCoord.toInt()) {
            return
        }

        var itemStack = player.heldItem
        //TODO: blacklist more blocks than only bushes
        if (itemStack == null || itemStack.item !is ItemBlock || (itemStack.item as ItemBlock).block is BlockBush || player.heldItem.stackSize <= 0) {
            val blockSlot = InventoryUtils.findAutoBlockBlock()

            if (blockSlot == -1) {
                return
            }

            when (autoBlock.lowercase()) {
                "off" -> return

                "pick" -> {
                    player.inventory.currentItem = blockSlot - 36
                    mc.playerController.updateController()
                }

                "spoof", "switch" -> {
                    sendPacket(sendSlotChange(slot, blockSlot - 36))
                }
            }
            itemStack = player.inventoryContainer.getSlot(blockSlot).stack
        }

        if (mc.playerController.onPlayerRightClick(
                player, world, itemStack, placeInfo.blockPos, placeInfo.enumFacing, placeInfo.vec3
            )
        ) {
            delayTimer.reset()
            delay = if (!placeDelay.get()) 0 else randomDelay(minDelay, maxDelay)

            if (player.onGround) {
                player.motionX *= speedModifier
                player.motionZ *= speedModifier
            }

            if (swing) {
                player.swingItem()
            } else {
                sendPacket(C0APacketAnimation())
            }
        }

        if (autoBlock == "Switch") {
            sendSlotChange(slot, mc.thePlayer.inventory.currentItem)
        }

        targetPlace = null
    }

    private fun doPlaceAttempt() {
        val player = mc.thePlayer ?: return
        val world = mc.theWorld ?: return

        if (slot == -1) {
            return
        }

        val stack = player.inventoryContainer.getSlot(slot + 36).stack ?: return

        if (stack.item !is ItemBlock || InventoryUtils.BLOCK_BLACKLIST.contains((stack.item as ItemBlock).block)) {
            return
        }

        val rotation = targetRotation ?: return

        val raytrace = performBlockRaytrace(rotation, mc.playerController.blockReachDistance) ?: return

        val shouldHelpWithDelay =
            !delayTimer.hasTimePassed(delay) && (raytrace.sideHit.axis != EnumFacing.Axis.Y || !sameY && raytrace.blockPos.y < player.posY - 1)

        if (raytrace.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || (stack.item as ItemBlock).canPlaceBlockOnSide(
                world, raytrace.blockPos, raytrace.sideHit, player, stack
            ) && !shouldHelpWithDelay
        ) {
            return
        }

        // This should only occur when delay is not catching up
        if (mc.playerController.onPlayerRightClick(
                player, world, stack, raytrace.blockPos, raytrace.sideHit, raytrace.hitVec
            )
        ) {
            if (shouldHelpWithDelay) {
                delayTimer.reset()
                delay = randomDelay(minDelay, maxDelay)
            }

            if (swing) {
                player.swingItem()
            } else {
                sendPacket(C0APacketAnimation())
            }
        }

        // This however must occur.
        if (mc.playerController.sendUseItem(player, world, stack)) {
            mc.entityRenderer.itemRenderer.resetEquippedProgress2()
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

        sendSlotChange(slot, mc.thePlayer.inventory.currentItem)
    }

    // Entity movement event
    @EventTarget
    fun onMove(event: MoveEvent) {
        val player = mc.thePlayer ?: return

        if (!safeWalk || shouldGoDown) {
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
            val scaledResolution = ScaledResolution(mc)

            drawBorderedRect(
                scaledResolution.scaledWidth / 2 - 2,
                scaledResolution.scaledHeight / 2 + 5,
                scaledResolution.scaledWidth / 2 + Fonts.font40.getStringWidth(info) + 2,
                scaledResolution.scaledHeight / 2 + 16,
                3,
                Color.BLACK.rgb,
                Color.BLACK.rgb
            )

            resetColor()

            Fonts.font40.drawString(
                info, scaledResolution.scaledWidth / 2, scaledResolution.scaledHeight / 2 + 7, Color.WHITE.rgb
            )
            glPopMatrix()
        }
    }

    // Visuals
    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        val player = mc.thePlayer ?: return

        val shouldBother =
            !(shouldGoDown || mode == "Expand" && expandLength > 1) && extraClicks.get() && MovementUtils.isMoving

        if (shouldBother) {
            targetRotation?.let {
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
            val x = if (omniDirectionalExpand.get()) -sin(yaw).roundToInt() else player.horizontalFacing.directionVec.x
            val z = if (omniDirectionalExpand.get()) cos(yaw).roundToInt() else player.horizontalFacing.directionVec.z
            val blockPos = BlockPos(
                player.posX + x * i,
                if (sameY && launchY <= player.posY) launchY - 1.0 else player.posY - (if (player.posY == player.posY + 0.5) 0.0 else 1.0) - if (shouldGoDown) 1.0 else 0.0,
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

        for (side in EnumFacing.values()) {
            val neighbor = blockPosition.offset(side)

            if (!canBeClicked(neighbor)) {
                continue
            }
            if (!area) {
                currPlaceRotation =
                    findTargetPlace(blockPosition, neighbor, Vec3(0.5, 0.5, 0.5), side, eyes, maxReach, raycast)
                        ?: continue

                if (placeRotation == null || getRotationDifference(
                        currPlaceRotation.rotation, currRotation
                    ) < getRotationDifference(placeRotation.rotation, currRotation)
                ) {
                    placeRotation = currPlaceRotation
                }
            } else {
                var x = 0.1
                while (x < 0.9) {
                    var y = 0.1
                    while (y < 0.9) {
                        var z = 0.1
                        while (z < 0.9) {
                            currPlaceRotation =
                                findTargetPlace(blockPosition, neighbor, Vec3(x, y, z), side, eyes, maxReach, raycast)

                            if (currPlaceRotation == null) {
                                z += 0.1
                                continue
                            }

                            if (placeRotation == null || getRotationDifference(
                                    currPlaceRotation.rotation, currRotation
                                ) < getRotationDifference(placeRotation.rotation, currRotation)
                            ) {
                                placeRotation = currPlaceRotation
                            }

                            z += 0.1
                        }
                        y += 0.1
                    }
                    x += 0.1
                }
            }
        }

        placeRotation ?: return false

        if (rotations) {
            val limitedRotation = limitAngleChange(
                currRotation, placeRotation.rotation, nextFloat(minTurnSpeed, maxTurnSpeed)
            )

            setRotation(limitedRotation, keepTicksValue.get())
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

        val vec = Vec3(pos).add(vec3).addVector(
            side.directionVec.x * vec3.xCoord, side.directionVec.y * vec3.yCoord, side.directionVec.z * vec3.zCoord
        )

        val distance = eyes.distanceTo(vec)

        if (raycast && (distance > maxReach || world.rayTraceBlocks(eyes, vec, false, true, false) != null)) {
            return null
        }

        val diff = vec.subtract(eyes)

        if (side.axis != EnumFacing.Axis.Y) {
            val dist = abs(if (side.axis == EnumFacing.Axis.Z) diff.zCoord else diff.xCoord)

            if (dist < minDist) {
                return null
            }
        }

        var rotation = toRotation(vec, false)

        rotation = if (stabilizedRotation.get()) {
            Rotation((rotation.yaw / 45f).roundToInt() * 45f, rotation.pitch)
        } else {
            rotation
        }

        val raytrace = performBlockRaytrace(rotation, maxReach) ?: return null

        if (raytrace.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && raytrace.blockPos == offsetPos && (!raycast || raytrace.sideHit == side.opposite)) {
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

        val reach =
            eyes.addVector(rotationVec.xCoord * maxReach, rotationVec.yCoord * maxReach, rotationVec.zCoord * maxReach)

        return world.rayTraceBlocks(eyes, reach, false, false, true)
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