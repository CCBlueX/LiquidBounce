/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

@file:Suppress("BooleanLiteralArgument")

package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.enums.BlockType
import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.minecraft.client.block.IBlock
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.minecraft.network.IPacket
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketEntityAction
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketHeldItemChange
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
    private val placeableDelay = BoolValue("PlaceableDelay", true)

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
    private val eagleValue = ListValue("Eagle", arrayOf("Normal", "EdgeDistance", "Silent", "Off"), "Normal")
    private val blocksToEagleValue = IntegerValue("BlocksToEagle", 0, 0, 10)
    private val edgeDistanceValue = FloatValue("EagleEdgeDistance", 0.2f, 0f, 0.5f)

    // Expand
    private val expandLengthValue = IntegerValue("ExpandLength", 1, 1, 6)

    // Rotation Options
    private val rotationStrafeValue = BoolValue("RotationStrafe", false)
    private val rotationModeValue =
        ListValue("RotationMode", arrayOf("Normal", "Static", "StaticPitch", "StaticYaw", "Off"), "Normal")
    private val silentRotationValue = BoolValue("SilentRotation", true)
    private val keepRotationValue = BoolValue("KeepRotation", true)
    private val keepLengthValue = IntegerValue("KeepRotationLength", 0, 0, 20)
    private val staticPitchValue = FloatValue("StaticPitchOffSet", 86f, 70f, 90f)
    private val staticYawValue = FloatValue("StaticYawOffSet", 0f, 0f, 90f)

    // xz + y range
    private val xzRangeValue = FloatValue("xzRange", 0.8f, 0f, 1f)
    private val yRangeValue = FloatValue("yRange", 0.8f, 0f, 1f)
    private val minDiffValue = FloatValue("MinDiff", 0.0f, 0.0f, 0.2f)

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
    private val zitterValue = BoolValue("Zitter", false)
    private val zitterModeValue = ListValue("ZitterMode", arrayOf("Teleport", "Smooth"), "Teleport")
    private val zitterSpeed = FloatValue("ZitterSpeed", 0.13f, 0.1f, 2f)
    private val zitterStrength = FloatValue("ZitterStrength", 0.05f, 0f, 0.2f)

    // Game
    private val timerValue = FloatValue("Timer", 1f, 0.1f, 10f)
    private val speedModifierValue = FloatValue("SpeedModifier", 1f, 0f, 2f)
    private val slowValue = object : BoolValue("Slow", false) {
        override fun onChanged(oldValue: Boolean, newValue: Boolean) {
            if (newValue) {
                sprintValue.set(false)
            }
        }
    }

    private val slowSpeed = FloatValue("SlowSpeed", 0.6f, 0.2f, 0.8f)

    // Safety
    private val sameYValue = BoolValue("SameY", false)
    private val safeWalkValue = BoolValue("SafeWalk", true)
    private val airSafeValue = BoolValue("AirSafe", false)

    // Visuals
    private val counterDisplayValue = BoolValue("Counter", true)
    private val markValue = BoolValue("Mark", false)

// MODULE

    // Target block
    private var targetPlace: PlaceInfo? = null

    // Rotation lock
    private var lockRotation: Rotation? = null
    private var limitedRotation: Rotation? = null

    // Launch position
    private var launchY = 0
    private var facesBlock = false

    // AutoBlock
    private var slot = 0
    //private var oldslot = 0

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
        //oldslot = mc.thePlayer!!.inventory.currentItem
    }

// UPDATE EVENTS

    /** @param */

    @EventTarget
    private fun onUpdate(event: UpdateEvent) {
        mc.timer.timerSpeed = timerValue.get()
        shouldGoDown =
            downValue.get() && !sameYValue.get() && mc.gameSettings.isKeyDown(mc.gameSettings.keyBindSneak) && blocksAmount > 1
        if (shouldGoDown)
            mc.gameSettings.keyBindSneak.pressed = false
        if (slowValue.get()) {
            mc.thePlayer!!.motionX = mc.thePlayer!!.motionX * slowSpeed.get()
            mc.thePlayer!!.motionZ = mc.thePlayer!!.motionZ * slowSpeed.get()
        }
        if (sprintValue.get()) {
            if (!mc.gameSettings.isKeyDown(mc.gameSettings.keyBindSprint)) {
                mc.gameSettings.keyBindSprint.pressed = false
            }
            if (mc.gameSettings.isKeyDown(mc.gameSettings.keyBindSprint)) {
                mc.gameSettings.keyBindSprint.pressed = true
            }
            if (mc.gameSettings.keyBindSprint.isKeyDown) {
                mc.thePlayer!!.sprinting = true
            }
            if (!mc.gameSettings.keyBindSprint.isKeyDown) {
                mc.thePlayer!!.sprinting = false
            }
        }
        if (mc.thePlayer!!.onGround) {
            when (modeValue.get().toLowerCase()) {
                "rewinside" -> {
                    MovementUtils.strafe(0.2F)
                    mc.thePlayer!!.motionY = 0.0
                }
            }
            if (zitterValue.get() && zitterModeValue.get().equals("Smooth", true)) {
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
        }
        // Eagle
        if (!eagleValue.get().equals("Off", true) && !shouldGoDown) {
            var dif = 0.5
            if (eagleValue.get().equals("EdgeDistance", true) && !shouldGoDown) {
                for (i in 0..3) when (i) {
                    0 -> {
                        val blockPos = WBlockPos(
                            mc.thePlayer!!.posX - 1.0,
                            mc.thePlayer!!.posY - (if (mc.thePlayer!!.posY == mc.thePlayer!!.posY.toInt() + 0.5) 0.0 else 1.0),
                            mc.thePlayer!!.posZ
                        )
                        val placeInfo: PlaceInfo? = PlaceInfo.get(blockPos)
                        if (isReplaceable(blockPos) && placeInfo != null) {
                            var calcDif: Double = mc.thePlayer!!.posX - blockPos.x
                            calcDif -= 0.5

                            if (calcDif < 0) {
                                calcDif *= -1.0
                                calcDif -= 0.5
                            }
                            if (calcDif < dif) {
                                dif = calcDif
                            }
                        }
                    }
                    1 -> {
                        val blockPos = WBlockPos(
                            mc.thePlayer!!.posX + 1.0,
                            mc.thePlayer!!.posY - (if (mc.thePlayer!!.posY == mc.thePlayer!!.posY.toInt() + 0.5) 0.0 else 1.0),
                            mc.thePlayer!!.posZ
                        )
                        val placeInfo: PlaceInfo? = PlaceInfo.get(blockPos)

                        if (isReplaceable(blockPos) && placeInfo != null) {
                            var calcDif: Double = mc.thePlayer!!.posX - blockPos.x
                            calcDif -= 0.5

                            if (calcDif < 0) {
                                calcDif *= -1.0
                                calcDif -= 0.5
                            }
                            if (calcDif < dif) {
                                dif = calcDif
                            }
                        }
                    }
                    2 -> {
                        val blockPos = WBlockPos(
                            mc.thePlayer!!.posX,
                            mc.thePlayer!!.posY - (if (mc.thePlayer!!.posY == mc.thePlayer!!.posY.toInt() + 0.5) 0.0 else 1.0),
                            mc.thePlayer!!.posZ - 1.0
                        )
                        val placeInfo: PlaceInfo? = PlaceInfo.get(blockPos)

                        if (isReplaceable(blockPos) && placeInfo != null) {
                            var calcDif: Double = mc.thePlayer!!.posZ - blockPos.z
                            calcDif -= 0.5

                            if (calcDif < 0) {
                                calcDif *= -1.0
                                calcDif -= 0.5
                            }
                            if (calcDif < dif) {
                                dif = calcDif
                            }
                        }
                    }
                    3 -> {
                        val blockPos = WBlockPos(
                            mc.thePlayer!!.posX,
                            mc.thePlayer!!.posY - (if (mc.thePlayer!!.posY == mc.thePlayer!!.posY.toInt() + 0.5) 0.0 else 1.0),
                            mc.thePlayer!!.posZ + 1.0
                        )
                        val placeInfo: PlaceInfo? = PlaceInfo.get(blockPos)

                        if (isReplaceable(blockPos) && placeInfo != null) {
                            var calcDif: Double = mc.thePlayer!!.posZ - blockPos.z
                            calcDif -= 0.5

                            if (calcDif < 0) {
                                calcDif *= -1
                                calcDif -= 0.5
                            }
                            if (calcDif < dif) {
                                dif = calcDif
                            }
                        }
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
                ).block == (classProvider.getBlockEnum(BlockType.AIR)) || (dif < edgeDistanceValue.get() && eagleValue.get()
                    .equals("EdgeDistance", true))
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
            if (zitterValue.get() && zitterModeValue.get().equals("teleport", true)) {
                MovementUtils.strafe(zitterSpeed.get())
                val yaw: Double =
                    Math.toRadians(mc.thePlayer!!.rotationYaw + if (zitterDirection) 90.0 else -90.0)
                mc.thePlayer!!.motionX = mc.thePlayer!!.motionX - sin(yaw) * zitterStrength.get()
                mc.thePlayer!!.motionZ = mc.thePlayer!!.motionZ + cos(yaw) * zitterStrength.get()
                zitterDirection = !zitterDirection
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (mc.thePlayer == null) return
        val packet: IPacket = event.packet
        if (classProvider.isCPacketHeldItemChange(packet)) {
            val packetHeldItemChange: ICPacketHeldItemChange = packet.asCPacketHeldItemChange()
            slot = packetHeldItemChange.slotId
        }
    }

    @EventTarget
    fun onStrafe(event: StrafeEvent) {
        if (!rotationStrafeValue.get()) return
        RotationUtils.serverRotation.applyStrafeToPlayer(event)
        event.cancelEvent()
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        val eventState: EventState = event.eventState

        // Lock Rotation
        if (!rotationModeValue.get().equals("Off", true) && keepRotationValue.get() && lockRotation != null)
            setRotation(lockRotation!!)

        if ((facesBlock || rotationModeValue.get().equals("Off", true)) && placeModeValue.get()
                .equals(eventState.stateName, true)
        )
            place()

        // Update and search for a new block
        if (eventState == EventState.PRE)
            update()

        // Reset placeable delay
        if (targetPlace == null && placeableDelay.get())
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

    private fun setRotation(rotation: Rotation, keepRotation: Int) {
        if (silentRotationValue.get()) {
            RotationUtils.setTargetRotation(rotation, keepRotation)
        } else {
            mc.thePlayer!!.rotationYaw = rotation.yaw
            mc.thePlayer!!.rotationPitch = rotation.pitch
        }
    }

    private fun setRotation(rotation: Rotation) {
        setRotation(rotation, 0)
    }

    // Search for new target block
    private fun findBlock(expand: Boolean) {
        val blockPosition: WBlockPos =
            if (shouldGoDown) (if (mc.thePlayer!!.posY == mc.thePlayer!!.posY.toInt() + 0.5) WBlockPos(
                mc.thePlayer!!.posX,
                mc.thePlayer!!.posY - 0.6,
                mc.thePlayer!!.posZ
            ) else WBlockPos(
                mc.thePlayer!!.posX,
                mc.thePlayer!!.posY - 0.6,
                mc.thePlayer!!.posZ
            ).down()) else (if (sameYValue.get() && launchY <= mc.thePlayer!!.posY) WBlockPos(
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
                            if (mc.thePlayer!!.horizontalFacing == classProvider.getEnumFacing(
                                    EnumFacingType.WEST
                                )
                            ) -i else if (mc.thePlayer!!.horizontalFacing == classProvider.getEnumFacing(EnumFacingType.EAST)) i else 0,
                            0,
                            if (mc.thePlayer!!.horizontalFacing == classProvider.getEnumFacing(EnumFacingType.NORTH)) -i else if (mc.thePlayer!!.horizontalFacing == classProvider.getEnumFacing(
                                    EnumFacingType.SOUTH
                                )
                            ) i else 0
                        ), false
                    )
                )
                    return
            }
        } else if (searchValue.get()) {
            for (x in -1..1)
                for (z in -1..1)
                    if (search(blockPosition.add(x, 0, z), !shouldGoDown))
                        return
        }
    }

    fun place() {
        if (targetPlace == null) {
            if (placeableDelay.get()) delayTimer.reset()
            return
        }

        if (!delayTimer.hasTimePassed(delay) || sameYValue.get() && launchY - 1 != targetPlace!!.vec3.yCoord.toInt())
            return

        var itemStack: IItemStack? = mc.thePlayer!!.heldItem
        if (itemStack == null || !classProvider.isItemBlock(itemStack.item) ||
            classProvider.isBlockBush(itemStack.item!!.asItemBlock().block) || mc.thePlayer!!.heldItem!!.stackSize <= 0
        ) {
            if (autoBlockValue.get().equals("Off", true))
                return

            val blockSlot = InventoryUtils.findAutoBlockBlock()

            if (blockSlot == -1)
                return
            when (autoBlockValue.get()) {
                "Off" -> {
                    return
                }
                "Pick" -> {
                    mc.thePlayer!!.inventory.currentItem = blockSlot - 36
                    mc.playerController.updateController()
                }
                "Spoof" -> {
                    if (blockSlot - 36 != slot) {
                        mc.netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(blockSlot - 36))
                    }
                }
                "Switch" -> {
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
            delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())

            if (mc.thePlayer!!.onGround) {
                val modifier: Float = speedModifierValue.get()
                mc.thePlayer!!.motionX = mc.thePlayer!!.motionX * modifier
                mc.thePlayer!!.motionZ = mc.thePlayer!!.motionZ * modifier
            }

            if (swingValue.get())
                mc.thePlayer!!.swingItem()
            else
                mc.netHandler.addToSendQueue(classProvider.createCPacketAnimation())
        }
        if (autoBlockValue.get().equals("Switch", true)) {
            if (slot != mc.thePlayer!!.inventory.currentItem)
                mc.netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(mc.thePlayer!!.inventory.currentItem))
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

        /*if(autoBlockValue.get().equals("Switch", ignoreCase = true)) {
            mc.thePlayer!!.inventory.currentItem = oldslot
            mc.playerController.updateController()
        }*/

        if (!mc.gameSettings.isKeyDown(mc.gameSettings.keyBindRight))
            mc.gameSettings.keyBindRight.pressed = false
        if (!mc.gameSettings.isKeyDown(mc.gameSettings.keyBindLeft))
            mc.gameSettings.keyBindLeft.pressed = false

        lockRotation = null
        limitedRotation = null
        facesBlock = false
        mc.timer.timerSpeed = 1f
        shouldGoDown = false

        if (slot != mc.thePlayer!!.inventory.currentItem)
            mc.netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(mc.thePlayer!!.inventory.currentItem))
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
                mc.thePlayer!!.posX + if (mc.thePlayer!!.horizontalFacing == classProvider.getEnumFacing(EnumFacingType.WEST)) -i.toDouble() else if (mc.thePlayer!!.horizontalFacing == classProvider.getEnumFacing(
                        EnumFacingType.EAST
                    )
                ) i.toDouble() else 0.0,
                if (sameYValue.get() && launchY <= mc.thePlayer!!.posY) launchY - 1.0 else mc.thePlayer!!.posY - (if (mc.thePlayer!!.posY == mc.thePlayer!!.posY + 0.5) 0.0 else 1.0) - if (shouldGoDown) 1.0 else 0.0,
                mc.thePlayer!!.posZ + if (mc.thePlayer!!.horizontalFacing == classProvider.getEnumFacing(EnumFacingType.NORTH)) -i.toDouble() else if (mc.thePlayer!!.horizontalFacing == classProvider.getEnumFacing(
                        EnumFacingType.SOUTH
                    )
                ) i.toDouble() else 0.0
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
        if (!isReplaceable(blockPosition)) return false

        // Static Modes
        val staticMode = rotationModeValue.get().equals("Static", ignoreCase = true)
        val staticPitchMode = staticMode || rotationModeValue.get().equals("StaticPitch", ignoreCase = true)
        val staticYawMode = staticMode || rotationModeValue.get().equals("StaticYaw", ignoreCase = true)
        val staticPitch = staticPitchValue.get()
        val staticYawOffset = staticYawValue.get()

        // Search Ranges
        val xzRV = xzRangeValue.get().toDouble()
        val xzSSV = calcStepSize(xzRV.toFloat())
        val yRV = yRangeValue.get().toDouble()
        val ySSV = calcStepSize(yRV.toFloat())
        var xSearchFace = 0.0
        var ySearchFace = 0.0
        var zSearchFace = 0.0
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
            var xSearch = 0.5 - xzRV / 2
            while (xSearch <= 0.5 + xzRV / 2) {
                var ySearch = 0.5 - yRV / 2
                while (ySearch <= 0.5 + yRV / 2) {
                    var zSearch = 0.5 - xzRV / 2
                    while (zSearch <= 0.5 + xzRV / 2) {
                        val posVec = WVec3(blockPosition).addVector(xSearch, ySearch, zSearch)
                        val distanceSqPosVec = eyesPos.squareDistanceTo(posVec)
                        val hitVec = posVec.add(WVec3(dirVec.xCoord * 0.5, dirVec.yCoord * 0.5, dirVec.zCoord * 0.5))
                        if (checks && (eyesPos.squareDistanceTo(hitVec) > 18.0 || distanceSqPosVec > eyesPos.squareDistanceTo(
                                posVec.add(dirVec)
                            ) || mc.theWorld!!.rayTraceBlocks(eyesPos, hitVec, false, true, false) != null)
                        ) {
                            zSearch += xzSSV
                            continue
                        }

                        // Face block
                        for (i in 0 until if (staticYawMode) 2 else 1) {
                            val diffX: Double = if (staticYawMode && i == 0) 0.0 else hitVec.xCoord - eyesPos.xCoord
                            val diffY = hitVec.yCoord - eyesPos.yCoord
                            val diffZ: Double = if (staticYawMode && i == 1) 0.0 else hitVec.zCoord - eyesPos.zCoord
                            val diffXZ = sqrt(diffX * diffX + diffZ * diffZ)
                            if (!side.isUp() && minDiffValue.get() > 0) {
                                val diff: Double = abs(if (side.isNorth() || side.isSouth()) diffZ else diffX)
                                if (diff < minDiffValue.get() || diff > 0.3f)
                                    continue
                            }
                            val pitch = if (staticPitchMode) staticPitch else wrapAngleTo180_float(
                                (-Math.toDegrees(
                                    atan2(
                                        diffY,
                                        diffXZ
                                    )
                                )).toFloat()
                            )
                            val rotation = Rotation(
                                wrapAngleTo180_float(
                                    Math.toDegrees(atan2(diffZ, diffX))
                                        .toFloat() - 90f + if (staticYawMode) staticYawOffset else 0f
                                ), pitch
                            )
                            val rotationVector = RotationUtils.getVectorForRotation(rotation)
                            val vector = eyesPos.addVector(
                                rotationVector.xCoord * 4,
                                rotationVector.yCoord * 4,
                                rotationVector.zCoord * 4
                            )
                            val obj = mc.theWorld!!.rayTraceBlocks(eyesPos, vector, false, false, true)
                            if (obj!!.typeOfHit !== IMovingObjectPosition.WMovingObjectType.BLOCK || obj!!.blockPos!! != neighbor)
                                continue
                            if (placeRotation == null || RotationUtils.getRotationDifference(rotation) < RotationUtils.getRotationDifference(
                                    placeRotation.rotation
                                )
                            ) {
                                placeRotation = PlaceRotation(PlaceInfo(neighbor, side.opposite, hitVec), rotation)
                            }
                            xSearchFace = xSearch
                            ySearchFace = ySearch
                            zSearchFace = zSearch
                        }
                        zSearch += xzSSV
                    }
                    ySearch += ySSV
                }
                xSearch += xzSSV
            }
        }
        if (placeRotation == null) return false
        if (!rotationModeValue.get().equals("Off", ignoreCase = true)) {
            if (minTurnSpeedValue.get() < 180) {
                limitedRotation = RotationUtils.limitAngleChange(
                    RotationUtils.serverRotation,
                    placeRotation.rotation,
                    (Math.random() * (maxTurnSpeedValue.get() - minTurnSpeedValue.get()) + minTurnSpeedValue.get()).toFloat()
                )
                setRotation(limitedRotation!!, keepLengthValue.get())
                lockRotation = limitedRotation
                facesBlock = false
                for (facingType in EnumFacingType.values()) {
                    val side = classProvider.getEnumFacing(facingType)
                    val neighbor = blockPosition.offset(side)
                    if (!canBeClicked(neighbor)) continue
                    val dirVec = WVec3(side.directionVec)
                    val posVec = WVec3(blockPosition).addVector(xSearchFace, ySearchFace, zSearchFace)
                    val distanceSqPosVec = eyesPos.squareDistanceTo(posVec)
                    val hitVec = posVec.add(WVec3(dirVec.xCoord * 0.5, dirVec.yCoord * 0.5, dirVec.zCoord * 0.5))
                    if (checks && (eyesPos.squareDistanceTo(hitVec) > 18.0 || distanceSqPosVec > eyesPos.squareDistanceTo(
                            posVec.add(dirVec)
                        ) || mc.theWorld!!.rayTraceBlocks(eyesPos, hitVec, false, true, false) != null)
                    )
                        continue
                    val rotationVector = RotationUtils.getVectorForRotation(limitedRotation)
                    val vector = eyesPos.addVector(
                        rotationVector.xCoord * 4,
                        rotationVector.yCoord * 4,
                        rotationVector.zCoord * 4
                    )
                    val obj = mc.theWorld!!.rayTraceBlocks(eyesPos, vector, false, false, true)
                    if (!(obj!!.typeOfHit === IMovingObjectPosition.WMovingObjectType.BLOCK && obj!!.blockPos!! == neighbor))
                        continue
                    facesBlock = true
                    break
                }
            } else {
                setRotation(placeRotation.rotation, keepLengthValue.get())
                lockRotation = placeRotation.rotation
                facesBlock = true
            }
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
                    )
                        amount += itemStack.stackSize
                }
            }
            return amount
        }
    override val tag: String
        get() = modeValue.get()
}
