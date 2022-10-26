/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.render.BlockOverlay
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.block.BlockUtils.canBeClicked
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isReplaceable
import net.ccbluex.liquidbounce.utils.block.PlaceInfo
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TickTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.BlockBush
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.settings.GameSettings
import net.minecraft.item.ItemBlock
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.MathHelper.wrapAngleTo180_float
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.*

@ModuleInfo(
    name = "Scaffold",
    description = "Automatically places blocks beneath your feet.",
    category = ModuleCategory.WORLD,
    keyBind = Keyboard.KEY_I
)
class Scaffold : Module() {

    private val modeValue = ListValue("Mode", arrayOf("Normal", "Rewinside", "Expand"), "Normal")

    // Delay
    private val maxDelayValue: IntegerValue = object : IntegerValue("MaxDelay", 0, 0, 1000) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val minDelay = minDelayValue.get()
            if (minDelay > newValue) {
                set(minDelay)
            }
        }
    }

    private val minDelayValue: IntegerValue = object : IntegerValue("MinDelay", 0, 0, 1000) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val maxDelay = maxDelayValue.get()
            if (maxDelay < newValue) {
                set(maxDelay)
            }
        }
    }

    // Placeable delay
    private val placeDelay = BoolValue("PlaceDelay", true)

    // Autoblock
    private val autoBlockValue = ListValue("AutoBlock", arrayOf("Off", "Pick", "Spoof", "Switch"), "Spoof")

    // Basic stuff
    @JvmField
    val sprintValue = BoolValue("Sprint", false)
    private val swingValue = BoolValue("Swing", true)
    private val searchValue = BoolValue("Search", true)
    private val downValue = BoolValue("Down", true)
    private val placeModeValue = ListValue("PlaceTiming", arrayOf("Pre", "Post"), "Post")

    // Eagle
    private val eagleValue = ListValue("Eagle", arrayOf("Normal", "Silent", "Off"), "Normal")
    private val blocksToEagleValue = IntegerValue("BlocksToEagle", 0, 0, 10)
    private val edgeDistanceValue = FloatValue("EagleEdgeDistance", 0f, 0f, 0.5f)

    // Expand
    private val omniDirectionalExpand = BoolValue("OmniDirectionalExpand", false)
    private val expandLengthValue = IntegerValue("ExpandLength", 1, 1, 6)

    // Rotation Options
    private val strafeMode = ListValue("Strafe", arrayOf("Off", "AAC"), "Off")
    private val rotationsValue = BoolValue("Rotations", true)
    private val silentRotationValue = BoolValue("SilentRotation", true)
    private val keepRotationValue = BoolValue("KeepRotation", true)
    private val keepLengthValue = IntegerValue("KeepRotationLength", 0, 0, 20)

    // XZ/Y range
    private val searchMode = ListValue("XYZSearch", arrayOf("Auto", "AutoCenter", "Manual"), "AutoCenter")
    private val xzRangeValue = FloatValue("xzRange", 0.8f, 0f, 1f)
    private var yRangeValue = FloatValue("yRange", 0.8f, 0f, 1f)
    private val minDistValue = FloatValue("MinDist", 0.0f, 0.0f, 0.2f)

    // Search Accuracy
    private val searchAccuracyValue: IntegerValue = object : IntegerValue("SearchAccuracy", 8, 1, 16) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            if (maximum < newValue) {
                set(maximum)
            } else if (minimum > newValue) {
                set(minimum)
            }
        }
    }

    // Turn Speed
    private val maxTurnSpeedValue: FloatValue = object : FloatValue("MaxTurnSpeed", 180f, 1f, 180f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val v = minTurnSpeedValue.get()
            if (v > newValue) set(v)
            if (maximum < newValue) {
                set(maximum)
            } else if (minimum > newValue) {
                set(minimum)
            }
        }
    }
    private val minTurnSpeedValue: FloatValue = object : FloatValue("MinTurnSpeed", 180f, 1f, 180f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val v = maxTurnSpeedValue.get()
            if (v < newValue) set(v)
            if (maximum < newValue) {
                set(maximum)
            } else if (minimum > newValue) {
                set(minimum)
            }
        }
    }

    // Zitter
    private val zitterMode = ListValue("Zitter", arrayOf("Off", "Teleport", "Smooth"), "Off")
    private val zitterSpeed = FloatValue("ZitterSpeed", 0.13f, 0.1f, 0.3f)
    private val zitterStrength = FloatValue("ZitterStrength", 0.05f, 0f, 0.2f)

    // Game
    private val timerValue = FloatValue("Timer", 1f, 0.1f, 10f)
    private val speedModifierValue = FloatValue("SpeedModifier", 1f, 0f, 2f)
    private val slowValue = BoolValue("Slow", false)
    private val slowSpeed = FloatValue("SlowSpeed", 0.6f, 0.2f, 0.8f)

    // Safety
    private val sameYValue = BoolValue("SameY", false)
    private val safeWalkValue = BoolValue("SafeWalk", true)
    private val airSafeValue = BoolValue("AirSafe", false)

    // Visuals
    private val counterDisplayValue = BoolValue("Counter", true)
    private val markValue = BoolValue("Mark", false)

    // Target block
    private var targetPlace: PlaceInfo? = null

    // Rotation lock
    private var lockRotation: Rotation? = null
    private var lockRotationTimer = TickTimer()

    // Launch position
    private var launchY = 0
    private var facesBlock = false

    // AutoBlock
    private var slot = 0

    // Zitter Direction
    private var zitterDirection = false

    // Delay
    private val delayTimer = MSTimer()
    private val zitterTimer = MSTimer()
    private var delay = 0L

    // Eagle
    private var placedBlocksWithoutEagle = 0
    private var eagleSneaking = false

    // Downwards
    private var shouldGoDown = false

    // Enabling module
    override fun onEnable() {
        val player = mc.thePlayer ?: return

        launchY = player.posY.roundToInt()
        slot = player.inventory.currentItem
        facesBlock = false
    }

    // Events
    @EventTarget
    private fun onUpdate(event: UpdateEvent) {
        val player = mc.thePlayer ?: return

        mc.timer.timerSpeed = timerValue.get()
        shouldGoDown =
            downValue.get() && !sameYValue.get() && GameSettings.isKeyDown(mc.gameSettings.keyBindSneak) && blocksAmount > 1
        if (shouldGoDown) {
            mc.gameSettings.keyBindSneak.pressed = false
        }
        if (slowValue.get()) {
            player.motionX = player.motionX * slowSpeed.get()
            player.motionZ = player.motionZ * slowSpeed.get()
        }
        // Eagle
        if (!eagleValue.get().equals("Off", true) && !shouldGoDown) {
            var dif = 0.5
            val blockPos = BlockPos(player.posX, player.posY - 1.0, player.posZ)
            if (edgeDistanceValue.get() > 0) {
                for (facingType in EnumFacing.values()) {
                    if (facingType == EnumFacing.UP || facingType == EnumFacing.DOWN) {
                        continue
                    }
                    val neighbor = blockPos.offset(facingType)
                    if (isReplaceable(neighbor)) {
                        val calcDif = (if (facingType == EnumFacing.NORTH || facingType == EnumFacing.SOUTH) {
                            abs((neighbor.z + 0.5) - player.posZ)
                        } else {
                            abs((neighbor.x + 0.5) - player.posX)
                        }) - 0.5

                        if (calcDif < dif) {
                            dif = calcDif
                        }
                    }
                }
            }
            if (placedBlocksWithoutEagle >= blocksToEagleValue.get()) {
                val shouldEagle =
                    isReplaceable(blockPos) || (edgeDistanceValue.get() > 0 && dif < edgeDistanceValue.get())
                if (eagleValue.get().equals("Silent", true)) {
                    if (eagleSneaking != shouldEagle) {
                        mc.netHandler.addToSendQueue(
                            C0BPacketEntityAction(
                                player, if (shouldEagle) {
                                    C0BPacketEntityAction.Action.START_SNEAKING
                                } else {
                                    C0BPacketEntityAction.Action.STOP_SNEAKING
                                }
                            )
                        )
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
            when (modeValue.get().lowercase()) {
                "rewinside" -> {
                    MovementUtils.strafe(0.2F)
                    player.motionY = 0.0
                }
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
                    MovementUtils.strafe(zitterSpeed.get())
                    val yaw = Math.toRadians(player.rotationYaw + if (zitterDirection) 90.0 else -90.0)
                    player.motionX = player.motionX - sin(yaw) * zitterStrength.get()
                    player.motionZ = player.motionZ + cos(yaw) * zitterStrength.get()
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
            slot = packet.slotId
        }
    }

    @EventTarget
    fun onStrafe(event: StrafeEvent) {
        if (strafeMode.get().equals("Off", true)) {
            return
        }

        update()
        val rotation = lockRotation ?: return

        if (rotationsValue.get() && (keepRotationValue.get() || !lockRotationTimer.hasTimePassed(keepLengthValue.get()))) {
            if (targetPlace == null) {
                rotation.yaw = wrapAngleTo180_float((rotation.yaw / 45f).roundToInt() * 45f)
            }
            setRotation(rotation)
            lockRotationTimer.update()

            rotation.applyStrafeToPlayer(event)
            event.cancelEvent()
            return
        }

        val targetRotation = RotationUtils.targetRotation ?: return
        targetRotation.applyStrafeToPlayer(event)
        event.cancelEvent()
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        val eventState = event.eventState

        // Lock Rotation
        if (rotationsValue.get() && (keepRotationValue.get() || !lockRotationTimer.hasTimePassed(keepLengthValue.get())) && lockRotation != null && strafeMode.get()
                .equals("Off", true)
        ) {
            setRotation(lockRotation!!)
            if (eventState == EventState.POST) {
                lockRotationTimer.update()
            }
        }

        // Face block
        if ((facesBlock || !rotationsValue.get()) && placeModeValue.get().equals(eventState.stateName, true)) {
            place()
        }

        // Update and search for a new block
        if (eventState == EventState.PRE && strafeMode.get().equals("Off", true)) {
            update()
        }

        // Reset placeable delay
        if (targetPlace == null && placeDelay.get()) {
            delayTimer.reset()
        }
    }

    fun update() {
        val player = mc.thePlayer ?: return

        val holdingItem = player.heldItem != null && player.heldItem!!.item is ItemBlock
        if (if (!autoBlockValue.get()
                    .equals("off", true)
            ) InventoryUtils.findAutoBlockBlock() == -1 && !holdingItem else !holdingItem
        ) {
            return
        }

        findBlock(modeValue.get().equals("expand", true))
    }

    private fun setRotation(rotation: Rotation) {
        val player = mc.thePlayer ?: return

        if (silentRotationValue.get()) {
            RotationUtils.setTargetRotation(rotation, 0)
        } else {
            player.rotationYaw = rotation.yaw
            player.rotationPitch = rotation.pitch
        }
    }

    // Search for new target block
    private fun findBlock(expand: Boolean) {
        val player = mc.thePlayer ?: return

        val blockPosition = if (shouldGoDown) {
            (if (player.posY == player.posY.roundToInt() + 0.5) {
                BlockPos(player.posX, player.posY - 0.6, player.posZ)
            } else {
                BlockPos(player.posX, player.posY - 0.6, player.posZ).down()
            })
        } else (if (sameYValue.get() && launchY <= player.posY) {
            BlockPos(player.posX, launchY - 1.0, player.posZ)
        } else (if (player.posY == player.posY.roundToInt() + 0.5) {
            BlockPos(player)
        } else {
            BlockPos(player.posX, player.posY, player.posZ).down()
        }))
        if (!expand && (!isReplaceable(blockPosition) || search(blockPosition, !shouldGoDown))) {
            return
        }

        if (expand) {
            val yaw = Math.toRadians(player.rotationYaw.toDouble())
            val x = if (omniDirectionalExpand.get()) -sin(yaw).roundToInt() else player.horizontalFacing.directionVec.x
            val z = if (omniDirectionalExpand.get()) cos(yaw).roundToInt() else player.horizontalFacing.directionVec.z
            for (i in 0 until expandLengthValue.get()) {
                if (search(blockPosition.add(x * i, 0, z * i), false)) {
                    return
                }
            }
        } else if (searchValue.get()) {
            for (x in -1..1) {
                for (z in -1..1) {
                    if (search(blockPosition.add(x, 0, z), !shouldGoDown)) {
                        return
                    }
                }
            }
        }
    }

    fun place() {
        val player = mc.thePlayer ?: return
        val world = mc.theWorld ?: return

        if (targetPlace == null) {
            if (placeDelay.get()) {
                delayTimer.reset()
            }
            return
        }

        if (!delayTimer.hasTimePassed(delay) || sameYValue.get() && launchY - 1 != targetPlace!!.vec3.yCoord.toInt()) {
            return
        }

        var itemStack = player.heldItem
        if (itemStack == null || itemStack.item !is ItemBlock|| (itemStack.item!! as ItemBlock).block is BlockBush || player.heldItem!!.stackSize <= 0) {
            val blockSlot = InventoryUtils.findAutoBlockBlock()

            if (blockSlot == -1) {
                return
            }

            when (autoBlockValue.get().lowercase()) {
                "off" -> {
                    return
                }
                "pick" -> {
                    player.inventory.currentItem = blockSlot - 36
                    mc.playerController.updateController()
                }
                "spoof" -> {
                    if (blockSlot - 36 != slot) {
                        mc.netHandler.addToSendQueue(C09PacketHeldItemChange(blockSlot - 36))
                    }
                }
                "switch" -> {
                    if (blockSlot - 36 != slot) {
                        mc.netHandler.addToSendQueue(C09PacketHeldItemChange(blockSlot - 36))
                    }
                }
            }
            itemStack = player.inventoryContainer.getSlot(blockSlot).stack
        }

        if (mc.playerController.onPlayerRightClick(
                player, world, itemStack, targetPlace!!.blockPos, targetPlace!!.enumFacing, targetPlace!!.vec3
            )
        ) {
            delayTimer.reset()
            delay = if (!placeDelay.get()) 0 else TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())

            if (player.onGround) {
                val modifier = speedModifierValue.get()
                player.motionX = player.motionX * modifier
                player.motionZ = player.motionZ * modifier
            }

            if (swingValue.get()) {
                player.swingItem()
            } else {
                mc.netHandler.addToSendQueue(C0APacketAnimation())
            }
        }
        if (autoBlockValue.get().equals("Switch", true)) {
            if (slot != player.inventory.currentItem) {
                mc.netHandler.addToSendQueue(C09PacketHeldItemChange(player.inventory.currentItem))
            }
        }
        targetPlace = null
    }

    // Disabling module
    override fun onDisable() {
        val player = mc.thePlayer ?: return

        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)) {
            mc.gameSettings.keyBindSneak.pressed = false
            if (eagleSneaking) {
                mc.netHandler.addToSendQueue(
                    C0BPacketEntityAction(
                        player, C0BPacketEntityAction.Action.STOP_SNEAKING
                    )
                )
            }
        }
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindRight)) {
            mc.gameSettings.keyBindRight.pressed = false
        }
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindLeft)) {
            mc.gameSettings.keyBindLeft.pressed = false
        }

        lockRotation = null
        facesBlock = false
        mc.timer.timerSpeed = 1f
        shouldGoDown = false

        if (slot != player.inventory.currentItem) {
            mc.netHandler.addToSendQueue(C09PacketHeldItemChange(player.inventory.currentItem))
        }
    }

    // Entity movement event
    @EventTarget
    fun onMove(event: MoveEvent) {
        val player = mc.thePlayer ?: return

        if (!safeWalkValue.get() || shouldGoDown) {
            return
        }
        if (airSafeValue.get() || player.onGround) {
            event.isSafeWalk = true
        }
    }

    // Scaffold visuals
    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (counterDisplayValue.get()) {
            GL11.glPushMatrix()
            val blockOverlay = LiquidBounce.moduleManager.getModule(BlockOverlay::class.java) as BlockOverlay
            if (blockOverlay.state && blockOverlay.infoValue.get() && blockOverlay.currentBlock != null) {
                GL11.glTranslatef(0f, 15f, 0f)
            }
            val info = "Blocks: §7$blocksAmount"
            val scaledResolution = ScaledResolution(mc)

            RenderUtils.drawBorderedRect(
                scaledResolution.scaledWidth / 2 - 2.toFloat(),
                scaledResolution.scaledHeight / 2 + 5.toFloat(),
                scaledResolution.scaledWidth / 2 + Fonts.font40.getStringWidth(info) + 2.toFloat(),
                scaledResolution.scaledHeight / 2 + 16.toFloat(),
                3f,
                Color.BLACK.rgb,
                Color.BLACK.rgb
            )

            GlStateManager.resetColor()

            Fonts.font40.drawString(
                info,
                scaledResolution.scaledWidth / 2.toFloat(),
                scaledResolution.scaledHeight / 2 + 7.toFloat(),
                Color.WHITE.rgb
            )
            GL11.glPopMatrix()
        }
    }

    // Visuals
    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        val player = mc.thePlayer ?: return
        if (!markValue.get()) {
            return
        }
        for (i in 0 until if (modeValue.get().equals("Expand", true)) expandLengthValue.get() + 1 else 2) {
            val yaw = Math.toRadians(player.rotationYaw.toDouble())
            val x = if (omniDirectionalExpand.get()) -sin(yaw).roundToInt() else player.horizontalFacing.directionVec.x
            val z = if (omniDirectionalExpand.get()) cos(yaw).roundToInt() else player.horizontalFacing.directionVec.z
            val blockPos = BlockPos(
                player.posX + x * i,
                if (sameYValue.get() && launchY <= player.posY) launchY - 1.0 else player.posY - (if (player.posY == player.posY + 0.5) 0.0 else 1.0) - if (shouldGoDown) 1.0 else 0.0,
                player.posZ + z * i
            )
            val placeInfo = PlaceInfo.get(blockPos)
            if (isReplaceable(blockPos) && placeInfo != null) {
                RenderUtils.drawBlockBox(blockPos, Color(68, 117, 255, 100), false)
                break
            }
        }
    }

    /**
     * Search for placeable block
     *
     * @param blockPosition pos
     * @param raycast visible
     * @return
     */

    private fun search(blockPosition: BlockPos, raycast: Boolean): Boolean {
        facesBlock = false
        val player = mc.thePlayer ?: return false
        val world = mc.theWorld ?: return false

        if (!isReplaceable(blockPosition)) {
            return false
        }

        // Search Ranges
        val xzRV = xzRangeValue.get().toDouble()
        val xzSSV = calcStepSize(xzRV.toFloat())
        val yRV = yRangeValue.get().toDouble()
        val ySSV = calcStepSize(yRV.toFloat())
        val eyesPos = Vec3(player.posX, player.entityBoundingBox.minY + player.eyeHeight, player.posZ)
        var placeRotation: PlaceRotation? = null
        for (facingType in EnumFacing.values()) {
            val neighbor = blockPosition.offset(facingType)
            if (!canBeClicked(neighbor)) {
                continue
            }
            val dirVec = Vec3(facingType.directionVec)
            val auto = searchMode.get().equals("Auto", true)
            val center = searchMode.get().equals("AutoCenter", true)
            var xSearch = if (auto) 0.1 else 0.5 - xzRV / 2
            while (xSearch <= if (auto) 0.9 else 0.5 + xzRV / 2) {
                var ySearch = if (auto) 0.1 else 0.5 - yRV / 2
                while (ySearch <= if (auto) 0.9 else 0.5 + yRV / 2) {
                    var zSearch = if (auto) 0.1 else 0.5 - xzRV / 2
                    while (zSearch <= if (auto) 0.9 else 0.5 + xzRV / 2) {
                        val posVec = Vec3(blockPosition).addVector(
                            if (center) 0.5 else xSearch, if (center) 0.5 else ySearch, if (center) 0.5 else zSearch
                        )
                        val distanceSqPosVec = eyesPos.squareDistanceTo(posVec)
                        val hitVec = posVec.add(Vec3(dirVec.xCoord * 0.5, dirVec.yCoord * 0.5, dirVec.zCoord * 0.5))
                        if (raycast && (eyesPos.distanceTo(hitVec) > 4.25 || distanceSqPosVec > eyesPos.squareDistanceTo(
                                posVec.add(dirVec)
                            ) || world.rayTraceBlocks(
                                eyesPos,
                                hitVec,
                                false,
                                true,
                                false
                            ) != null)
                        ) {
                            zSearch += if (auto) 0.1 else xzSSV
                            continue
                        }

                        // Face block
                        val diffX = hitVec.xCoord - eyesPos.xCoord
                        val diffY = hitVec.yCoord - eyesPos.yCoord
                        val diffZ = hitVec.zCoord - eyesPos.zCoord
                        val diffXZ = sqrt(diffX * diffX + diffZ * diffZ)
                        if (facingType != EnumFacing.UP && facingType != EnumFacing.DOWN) {
                            val diff = abs(if (facingType == EnumFacing.NORTH || facingType == EnumFacing.SOUTH) diffZ else diffX)
                            if (diff < minDistValue.get()) {
                                zSearch += if (auto) 0.1 else xzSSV
                                continue
                            }
                        }
                        val rotation = Rotation(
                            wrapAngleTo180_float(Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90f),
                            wrapAngleTo180_float(-Math.toDegrees(atan2(diffY, diffXZ)).toFloat())
                        )
                        val rotationVector = RotationUtils.getVectorForRotation(rotation)
                        val vector = eyesPos.addVector(
                            rotationVector.xCoord * 4.25, rotationVector.yCoord * 4.25, rotationVector.zCoord * 4.25
                        )

                        val obj = world.rayTraceBlocks(
                            eyesPos,
                            vector,
                            false,
                            false,
                            true
                        ) ?: continue

                        if (obj.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || obj.blockPos != neighbor) {
                            zSearch += if (auto) 0.1 else xzSSV
                            continue
                        }
                        if (placeRotation == null || RotationUtils.getRotationDifference(rotation) < RotationUtils.getRotationDifference(
                                placeRotation.rotation
                            )
                        ) {
                            placeRotation = PlaceRotation(PlaceInfo(neighbor, facingType.opposite, hitVec), rotation)
                        }

                        zSearch += if (auto) 0.1 else xzSSV
                    }
                    ySearch += if (auto) 0.1 else ySSV
                }
                xSearch += if (auto) 0.1 else xzSSV
            }
        }
        if (placeRotation == null) {
            return false
        }
        if (rotationsValue.get()) {
            if (minTurnSpeedValue.get() < 180) {
                val limitedRotation = RotationUtils.limitAngleChange(
                    RotationUtils.serverRotation,
                    placeRotation.rotation,
                    (Math.random() * (maxTurnSpeedValue.get() - minTurnSpeedValue.get()) + minTurnSpeedValue.get()).toFloat()
                )

                if ((10 * wrapAngleTo180_float(limitedRotation.yaw)).roundToInt() == (10 * wrapAngleTo180_float(
                        placeRotation.rotation.yaw
                    )).roundToInt() && (10 * wrapAngleTo180_float(limitedRotation.pitch)).roundToInt() == (10 * wrapAngleTo180_float(
                        placeRotation.rotation.pitch
                    )).roundToInt()
                ) {
                    setRotation(placeRotation.rotation)
                    lockRotation = placeRotation.rotation
                    facesBlock = true
                } else {
                    setRotation(limitedRotation)
                    lockRotation = limitedRotation
                    facesBlock = false
                }
            } else {
                setRotation(placeRotation.rotation)
                lockRotation = placeRotation.rotation
                facesBlock = true
            }
            lockRotationTimer.reset()
        }
        targetPlace = placeRotation.placeInfo
        return true
    }

    private fun calcStepSize(range: Float): Double {
        var accuracy = searchAccuracyValue.get().toDouble()
        accuracy += accuracy % 2 // If it is set to uneven it changes it to even. Fixes a bug
        return if (range / accuracy < 0.01) 0.01 else (range / accuracy)
    }

    // Return hotbar amount
    private val blocksAmount: Int
        get() {
            var amount = 0
            for (i in 36..44) {
                val itemStack = mc.thePlayer!!.inventoryContainer.getSlot(i).stack
                val itemStackItem = itemStack?.item
                if (itemStackItem is ItemBlock) {
                    val block = itemStackItem.block
                    val heldItem = mc.thePlayer!!.heldItem
                    if (heldItem != null && heldItem == itemStack || !InventoryUtils.BLOCK_BLACKLIST.contains(block) && block !is BlockBush) {
                        amount += itemStack.stackSize
                    }
                }
            }
            return amount
        }
    override val tag: String
        get() = modeValue.get()
}