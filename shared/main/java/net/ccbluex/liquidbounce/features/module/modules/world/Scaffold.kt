/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.enums.BlockType
import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.minecraft.client.block.IBlock
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketEntityAction
import net.ccbluex.liquidbounce.api.minecraft.util.IMovingObjectPosition
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper.wrapAngleTo180_float
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
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
            if (minDelay > newValue) set(minDelay)
        }
    }

    private val minDelayValue: IntegerValue = object : IntegerValue("MinDelay", 0, 0, 1000) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val maxDelay = maxDelayValue.get()
            if (maxDelay < newValue) set(maxDelay)
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

// Variables

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
    private var eagleSneaking: Boolean = false

    // Downwards
    private var shouldGoDown: Boolean = false

    // ENABLING MODULE
    override fun onEnable() {
        if (mc.thePlayer == null) return
        launchY = mc.thePlayer!!.posY.toInt()
        slot = mc.thePlayer!!.inventory.currentItem
        facesBlock = false
    }

// UPDATE EVENTS

    /** @param */

    @EventTarget
    private fun onUpdate(event: UpdateEvent) {
        mc.timer.timerSpeed = timerValue.get()
        shouldGoDown =
            downValue.get() && !sameYValue.get() && mc.gameSettings.isKeyDown(mc.gameSettings.keyBindSneak) && blocksAmount > 1
        if (shouldGoDown) {
            mc.gameSettings.keyBindSneak.pressed = false
        }
        if (slowValue.get()) {
            mc.thePlayer!!.motionX = mc.thePlayer!!.motionX * slowSpeed.get()
            mc.thePlayer!!.motionZ = mc.thePlayer!!.motionZ * slowSpeed.get()
        }
        if (mc.thePlayer!!.onGround) {
            when (modeValue.get().toLowerCase()) {
                "rewinside" -> {
                    MovementUtils.strafe(0.2F)
                    mc.thePlayer!!.motionY = 0.0
                }
            }
            when (zitterMode.get().toLowerCase()) {
                "off" -> return
                "smooth" -> {
                    if (!mc.gameSettings.isKeyDown(mc.gameSettings.keyBindRight)) {
                        mc.gameSettings.keyBindRight.pressed = false
                    }
                    if (!mc.gameSettings.isKeyDown(mc.gameSettings.keyBindLeft)) {
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
                    val yaw: Double =
                        Math.toRadians(mc.thePlayer!!.rotationYaw + if (zitterDirection) 90.0 else -90.0)
                    mc.thePlayer!!.motionX = mc.thePlayer!!.motionX - sin(yaw) * zitterStrength.get()
                    mc.thePlayer!!.motionZ = mc.thePlayer!!.motionZ + cos(yaw) * zitterStrength.get()
                    zitterDirection = !zitterDirection
                }
            }
        }
        // Eagle
        if (!eagleValue.get().equals("Off", true) && !shouldGoDown) {
            var dif = 0.5
            if (edgeDistanceValue.get() > 0 && !shouldGoDown) {
                for (facingType in EnumFacingType.values()) {
                    val side = classProvider.getEnumFacing(facingType)
                    if (!side.isNorth() && !side.isEast() && !side.isSouth() && !side.isWest())
                        continue
                    val blockPosition = WBlockPos(
                        mc.thePlayer!!.posX,
                        mc.thePlayer!!.posY - 1.0,
                        mc.thePlayer!!.posZ
                    )
                    val neighbor = blockPosition.offset(side, 1)
                    if (mc.theWorld!!.getBlockState(neighbor).block == (classProvider.getBlockEnum(BlockType.AIR))) {
                        val calcDif = (if (side.isNorth() || side.isSouth())
                            abs((neighbor.z + 0.5) - mc.thePlayer!!.posZ) else
                            abs((neighbor.x + 0.5) - mc.thePlayer!!.posX)) - 0.5
                        if (calcDif < dif)
                            dif = calcDif
                    }
                }
            }
            if (placedBlocksWithoutEagle >= blocksToEagleValue.get()) {
                val shouldEagle: Boolean = mc.theWorld!!.getBlockState(
                    WBlockPos(
                        mc.thePlayer!!.posX,
                        mc.thePlayer!!.posY - 1.0,
                        mc.thePlayer!!.posZ
                    )
                ).block == (classProvider.getBlockEnum(BlockType.AIR)) || dif < edgeDistanceValue.get()
                if (eagleValue.get().equals("Silent", true) && !shouldGoDown) {
                    if (eagleSneaking != shouldEagle) {
                        mc.netHandler.addToSendQueue(
                            classProvider.createCPacketEntityAction(
                                mc.thePlayer!!, if (shouldEagle)
                                    ICPacketEntityAction.WAction.START_SNEAKING
                                else
                                    ICPacketEntityAction.WAction.STOP_SNEAKING
                            )
                        )
                    }
                    eagleSneaking = shouldEagle
                } else {
                    mc.gameSettings.keyBindSneak.pressed = shouldEagle
                    placedBlocksWithoutEagle = 0
                }
            } else {
                placedBlocksWithoutEagle++
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (mc.thePlayer == null) return
        val packet = event.packet
        if (classProvider.isCPacketHeldItemChange(packet)) {
            slot = packet.asCPacketHeldItemChange().slotId
        }
    }

    @EventTarget
    fun onStrafe(event: StrafeEvent) {
        if (strafeMode.get().equals("Off", true))
            return

        update()
        if (rotationsValue.get()
            && (keepRotationValue.get() || !lockRotationTimer.hasTimePassed(keepLengthValue.get()))
            && lockRotation != null
        ) {
            if (targetPlace == null) {
                var yaw = 0F
                for (i in 0..7) {
                    if (abs(
                            RotationUtils.getAngleDifference(
                                lockRotation!!.yaw,
                                (i * 45).toFloat()
                            )
                        ) < abs(RotationUtils.getAngleDifference(lockRotation!!.yaw, yaw))
                    ) {
                        yaw = wrapAngleTo180_float((i * 45).toFloat())
                    }
                }
                lockRotation!!.yaw = yaw
            }
            setRotation(lockRotation!!)
            lockRotationTimer.update()
        }
        lockRotation!!.applyStrafeToPlayer(event)
        event.cancelEvent()
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        val eventState: EventState = event.eventState

        // Lock Rotation
        if (rotationsValue.get()
            && (keepRotationValue.get() || !lockRotationTimer.hasTimePassed(keepLengthValue.get()))
            && lockRotation != null
            && strafeMode.get().equals("Off", true)
        ) {
            setRotation(lockRotation!!)
            if (eventState == EventState.POST)
                lockRotationTimer.update()
        }

        // Face block
        if ((facesBlock || !rotationsValue.get()) && placeModeValue.get()
                .equals(eventState.stateName, true)
        )
            place()

        // Update and search for a new block
        if (eventState == EventState.PRE && strafeMode.get().equals("Off", true))
            update()

        // Reset placeable delay
        if (targetPlace == null && placeDelay.get())
            delayTimer.reset()
    }

    fun update() {
        val isHeldItemBlock: Boolean =
            mc.thePlayer!!.heldItem != null && classProvider.isItemBlock(mc.thePlayer!!.heldItem!!.item)
        if (if (!autoBlockValue.get()
                    .equals("Off", true)
            ) InventoryUtils.findAutoBlockBlock() == -1 && !isHeldItemBlock else !isHeldItemBlock
        )
            return
        findBlock(modeValue.get().equals("expand", true))
    }

    private fun setRotation(rotation: Rotation) {
        if (silentRotationValue.get()) {
            RotationUtils.setTargetRotation(rotation, 0)
        } else {
            mc.thePlayer!!.rotationYaw = rotation.yaw
            mc.thePlayer!!.rotationPitch = rotation.pitch
        }
    }

    // Search for new target block
    private fun findBlock(expand: Boolean) {
        val blockPosition: WBlockPos =
            if (shouldGoDown) (if (mc.thePlayer!!.posY == mc.thePlayer!!.posY.toInt() + 0.5) WBlockPos(
                mc.thePlayer!!.posX,
                mc.thePlayer!!.posY - 0.6,
                mc.thePlayer!!.posZ
            )
            else WBlockPos(mc.thePlayer!!.posX, mc.thePlayer!!.posY - 0.6, mc.thePlayer!!.posZ).down())
            else
                (if (sameYValue.get() && launchY <= mc.thePlayer!!.posY) WBlockPos(
                    mc.thePlayer!!.posX,
                    launchY - 1.0,
                    mc.thePlayer!!.posZ
                ) else (if (mc.thePlayer!!.posY == mc.thePlayer!!.posY.toInt() + 0.5) WBlockPos(mc.thePlayer!!) else WBlockPos(
                    mc.thePlayer!!.posX,
                    mc.thePlayer!!.posY,
                    mc.thePlayer!!.posZ
                ).down()))
        if (!expand && (!isReplaceable(blockPosition) || search(blockPosition, !shouldGoDown)))
            return

        if (expand) {
            for (i in 0 until expandLengthValue.get()) {
                if (search(
                        blockPosition.add(
                            when (mc.thePlayer!!.horizontalFacing) {
                                classProvider.getEnumFacing(EnumFacingType.WEST) -> -i
                                classProvider.getEnumFacing(EnumFacingType.EAST) -> i
                                else -> 0
                            }, 0,
                            when (mc.thePlayer!!.horizontalFacing) {
                                classProvider.getEnumFacing(EnumFacingType.NORTH) -> -i
                                classProvider.getEnumFacing(EnumFacingType.SOUTH) -> i
                                else -> 0
                            }
                        ), false
                    )
                )
                    return
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
        if (targetPlace == null) {
            if (placeDelay.get())
                delayTimer.reset()
            return
        }

        if (!delayTimer.hasTimePassed(delay) || sameYValue.get() && launchY - 1 != targetPlace!!.vec3.yCoord.toInt())
            return

        var itemStack: IItemStack? = mc.thePlayer!!.heldItem
        if (itemStack == null || !classProvider.isItemBlock(itemStack.item) ||
            classProvider.isBlockBush(itemStack.item!!.asItemBlock().block) || mc.thePlayer!!.heldItem!!.stackSize <= 0
        ) {

            val blockSlot = InventoryUtils.findAutoBlockBlock()

            if (blockSlot == -1)
                return

            when (autoBlockValue.get().toLowerCase()) {
                "off" -> return
                "pick" -> {
                    mc.thePlayer!!.inventory.currentItem = blockSlot - 36
                    mc.playerController.updateController()
                }
                "spoof" -> {
                    if (blockSlot - 36 != slot) {
                        mc.netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(blockSlot - 36))
                    }
                }
                "switch" -> {
                    if (blockSlot - 36 != slot) {
                        mc.netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(blockSlot - 36))
                    }
                }
            }
            itemStack = mc.thePlayer!!.inventoryContainer.getSlot(blockSlot).stack
        }

        if (mc.playerController.onPlayerRightClick(
                mc.thePlayer!!,
                mc.theWorld!!,
                itemStack,
                targetPlace!!.blockPos,
                targetPlace!!.enumFacing,
                targetPlace!!.vec3
            )
        ) {
            delayTimer.reset()
            delay = if (!placeDelay.get()) 0 else TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())

            if (mc.thePlayer!!.onGround) {
                val modifier: Float = speedModifierValue.get()
                mc.thePlayer!!.motionX = mc.thePlayer!!.motionX * modifier
                mc.thePlayer!!.motionZ = mc.thePlayer!!.motionZ * modifier
            }

            if (swingValue.get()) {
                mc.thePlayer!!.swingItem()
            } else {
                mc.netHandler.addToSendQueue(classProvider.createCPacketAnimation())
            }
        }
        if (autoBlockValue.get().equals("Switch", true)) {
            if (slot != mc.thePlayer!!.inventory.currentItem) {
                mc.netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(mc.thePlayer!!.inventory.currentItem))
            }
        }
        targetPlace = null
    }

    // DISABLING MODULE
    override fun onDisable() {
        if (mc.thePlayer == null) return
        if (!mc.gameSettings.isKeyDown(mc.gameSettings.keyBindSneak)) {
            mc.gameSettings.keyBindSneak.pressed = false
            if (eagleSneaking)
                mc.netHandler.addToSendQueue(
                    classProvider.createCPacketEntityAction(
                        mc.thePlayer!!,
                        ICPacketEntityAction.WAction.STOP_SNEAKING
                    )
                )
        }
        if (!mc.gameSettings.isKeyDown(mc.gameSettings.keyBindRight))
            mc.gameSettings.keyBindRight.pressed = false
        if (!mc.gameSettings.isKeyDown(mc.gameSettings.keyBindLeft))
            mc.gameSettings.keyBindLeft.pressed = false

        lockRotation = null
        facesBlock = false
        mc.timer.timerSpeed = 1f
        shouldGoDown = false

        if (slot != mc.thePlayer!!.inventory.currentItem) {
            mc.netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(mc.thePlayer!!.inventory.currentItem))
        }
    }

    // Entity movement event
    /** @param event */
    @EventTarget
    fun onMove(event: MoveEvent) {
        if (!safeWalkValue.get() || shouldGoDown)
            return
        if (airSafeValue.get() || mc.thePlayer!!.onGround)
            event.isSafeWalk = true
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
            val scaledResolution = classProvider.createScaledResolution(mc)

            RenderUtils.drawBorderedRect(
                scaledResolution.scaledWidth / 2 - 2.toFloat(),
                scaledResolution.scaledHeight / 2 + 5.toFloat(),
                scaledResolution.scaledWidth / 2 + Fonts.font40.getStringWidth(info) + 2.toFloat(),
                scaledResolution.scaledHeight / 2 + 16.toFloat(), 3f, Color.BLACK.rgb, Color.BLACK.rgb
            )

            classProvider.getGlStateManager().resetColor()

            Fonts.font40.drawString(
                info, scaledResolution.scaledWidth / 2.toFloat(),
                scaledResolution.scaledHeight / 2 + 7.toFloat(), Color.WHITE.rgb
            )
            GL11.glPopMatrix()
        }
    }
// SCAFFOLD VISUALS
    /** @param  */
    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (!markValue.get()) return
        for (i in 0 until if (modeValue.get().equals("Expand", true)) expandLengthValue.get() + 1 else 2) {
            val blockPos = WBlockPos(
                mc.thePlayer!!.posX + when (mc.thePlayer!!.horizontalFacing) {
                    classProvider.getEnumFacing(EnumFacingType.WEST) -> -i.toDouble()
                    classProvider.getEnumFacing(EnumFacingType.EAST) -> i.toDouble()
                    else -> 0.0
                },
                if (sameYValue.get() && launchY <= mc.thePlayer!!.posY) launchY - 1.0 else mc.thePlayer!!.posY - (if (mc.thePlayer!!.posY == mc.thePlayer!!.posY + 0.5) 0.0 else 1.0) - if (shouldGoDown) 1.0 else 0.0,
                mc.thePlayer!!.posZ + when (mc.thePlayer!!.horizontalFacing) {
                    classProvider.getEnumFacing(EnumFacingType.NORTH) -> -i.toDouble()
                    classProvider.getEnumFacing(EnumFacingType.SOUTH) -> i.toDouble()
                    else -> 0.0
                }
            )
            val placeInfo: PlaceInfo? = PlaceInfo.get(blockPos)
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
     * @param checks        visible
     * @return
     */

    private fun search(blockPosition: WBlockPos, checks: Boolean): Boolean {
        facesBlock = false
        if (!isReplaceable(blockPosition)) return false

        // Search Ranges
        val xzRV = xzRangeValue.get().toDouble()
        val xzSSV = calcStepSize(xzRV.toFloat())
        val yRV = yRangeValue.get().toDouble()
        val ySSV = calcStepSize(yRV.toFloat())
        val eyesPos = WVec3(
            mc.thePlayer!!.posX,
            mc.thePlayer!!.entityBoundingBox.minY + mc.thePlayer!!.eyeHeight,
            mc.thePlayer!!.posZ
        )
        var placeRotation: PlaceRotation? = null
        for (facingType in EnumFacingType.values()) {
            val side = classProvider.getEnumFacing(facingType)
            val neighbor = blockPosition.offset(side)
            if (!canBeClicked(neighbor)) continue
            val dirVec = WVec3(side.directionVec)
            val auto = searchMode.get().equals("Auto", true)
            val center = searchMode.get().equals("AutoCenter", true)
            var xSearch = if (auto) 0.1 else 0.5 - xzRV / 2
            while (xSearch <= if (auto) 0.9 else 0.5 + xzRV / 2) {
                var ySearch = if (auto) 0.1 else 0.5 - yRV / 2
                while (ySearch <= if (auto) 0.9 else 0.5 + yRV / 2) {
                    var zSearch = if (auto) 0.1 else 0.5 - xzRV / 2
                    while (zSearch <= if (auto) 0.9 else 0.5 + xzRV / 2) {
                        val posVec = WVec3(blockPosition).addVector(
                            if (center) 0.5 else xSearch,
                            if (center) 0.5 else ySearch,
                            if (center) 0.5 else zSearch
                        )
                        val distanceSqPosVec = eyesPos.squareDistanceTo(posVec)
                        val hitVec = posVec.add(WVec3(dirVec.xCoord * 0.5, dirVec.yCoord * 0.5, dirVec.zCoord * 0.5))
                        if (checks && (eyesPos.squareDistanceTo(hitVec) > 18.0 || distanceSqPosVec > eyesPos.squareDistanceTo(
                                posVec.add(dirVec)
                            ) || mc.theWorld!!.rayTraceBlocks(
                                eyesPos, hitVec,
                                stopOnLiquid = false,
                                ignoreBlockWithoutBoundingBox = true,
                                returnLastUncollidableBlock = false
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
                        if ((side.isNorth() || side.isEast() || side.isSouth() || side.isWest()) && minDistValue.get() > 0) {
                            val diff: Double = abs(if (side.isNorth() || side.isSouth()) diffZ else diffX)
                            if (diff < minDistValue.get() || diff > 0.3f) {
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
                            rotationVector.xCoord * distanceSqPosVec,
                            rotationVector.yCoord * distanceSqPosVec,
                            rotationVector.zCoord * distanceSqPosVec
                        )
                        val obj = mc.theWorld!!.rayTraceBlocks(
                            eyesPos, vector,
                            stopOnLiquid = false,
                            ignoreBlockWithoutBoundingBox = false,
                            returnLastUncollidableBlock = true
                        )
                        if (obj!!.typeOfHit != IMovingObjectPosition.WMovingObjectType.BLOCK || obj.blockPos!! != neighbor) {
                            zSearch += if (auto) 0.1 else xzSSV
                            continue
                        }
                        if (placeRotation == null || RotationUtils.getRotationDifference(rotation) < RotationUtils.getRotationDifference(
                                placeRotation.rotation
                            )
                        ) {
                            placeRotation = PlaceRotation(PlaceInfo(neighbor, side.opposite, hitVec), rotation)
                        }

                        zSearch += if (auto) 0.1 else xzSSV
                    }
                    ySearch += if (auto) 0.1 else ySSV
                }
                xSearch += if (auto) 0.1 else xzSSV
            }
        }
        if (placeRotation == null) return false
        if (rotationsValue.get()) {
            if (minTurnSpeedValue.get() < 180) {
                val limitedRotation = RotationUtils.limitAngleChange(
                    RotationUtils.serverRotation,
                    placeRotation.rotation,
                    (Math.random() * (maxTurnSpeedValue.get() - minTurnSpeedValue.get()) + minTurnSpeedValue.get()).toFloat()
                )

                if ((10 * wrapAngleTo180_float(limitedRotation.yaw)).roundToInt() == (10 * wrapAngleTo180_float(
                        placeRotation.rotation.yaw
                    )).roundToInt() &&
                    (10 * wrapAngleTo180_float(limitedRotation.pitch)).roundToInt() == (10 * wrapAngleTo180_float(
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
        var accuracy: Double = searchAccuracyValue.get().toDouble()
        accuracy += accuracy % 2 // If it is set to uneven it changes it to even. Fixes a bug
        return if (range / accuracy < 0.01) 0.01 else (range / accuracy)
    }

    // RETURN HOTBAR AMOUNT
    private val blocksAmount: Int
        get() {
            var amount = 0
            for (i in 36..44) {
                val itemStack: IItemStack? = mc.thePlayer!!.inventoryContainer.getSlot(i).stack
                if (itemStack != null && classProvider.isItemBlock(itemStack.item)) {
                    val block: IBlock = (itemStack.item!!.asItemBlock()).block
                    val heldItem: IItemStack? = mc.thePlayer!!.heldItem
                    if (heldItem != null && heldItem == itemStack || !InventoryUtils.BLOCK_BLACKLIST.contains(block) && !classProvider.isBlockBush(
                            block
                        )
                    ) {
                        amount += itemStack.stackSize
                    }
                }
            }
            return amount
        }
    override val tag: String
        get() = modeValue.get()
}