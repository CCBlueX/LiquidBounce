/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.enums.StatType
import net.ccbluex.liquidbounce.api.minecraft.util.IMovingObjectPosition
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.render.BlockOverlay
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.PlaceRotation
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.canBeClicked
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isReplaceable
import net.ccbluex.liquidbounce.utils.block.PlaceInfo
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.TickTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.math.truncate

@ModuleInfo(name = "Tower", description = "Automatically builds a tower beneath you.", category = ModuleCategory.WORLD, keyBind = Keyboard.KEY_O)
class Tower : Module() {
    /**
     * OPTIONS
     */
    private val modeValue = ListValue("Mode", arrayOf(
            "Jump", "Motion", "ConstantMotion", "MotionTP", "Packet", "Teleport", "AAC3.3.9", "AAC3.6.4"
    ), "Motion")
    private val autoBlockValue = BoolValue("AutoBlock", true)
    private val stayAutoBlock = BoolValue("StayAutoBlock", false)
    private val swingValue = BoolValue("Swing", true)
    private val stopWhenBlockAbove = BoolValue("StopWhenBlockAbove", false)
    private val rotationsValue = BoolValue("Rotations", true)
    private val keepRotationValue = BoolValue("KeepRotation", false)
    private val onJumpValue = BoolValue("OnJump", false)
    private val placeModeValue = ListValue("PlaceTiming", arrayOf("Pre", "Post"), "Post")
    private val timerValue = FloatValue("Timer", 1f, 0.01f, 10f)

    // Jump mode
    private val jumpMotionValue = FloatValue("JumpMotion", 0.42f, 0.3681289f, 0.79f)
    private val jumpDelayValue = IntegerValue("JumpDelay", 0, 0, 20)

    // ConstantMotion
    private val constantMotionValue = FloatValue("ConstantMotion", 0.42f, 0.1f, 1f)
    private val constantMotionJumpGroundValue = FloatValue("ConstantMotionJumpGround", 0.79f, 0.76f, 1f)

    // Teleport
    private val teleportHeightValue = FloatValue("TeleportHeight", 1.15f, 0.1f, 5f)
    private val teleportDelayValue = IntegerValue("TeleportDelay", 0, 0, 20)
    private val teleportGroundValue = BoolValue("TeleportGround", true)
    private val teleportNoMotionValue = BoolValue("TeleportNoMotion", false)

    // Render
    private val counterDisplayValue = BoolValue("Counter", true)

    /**
     * MODULE
     */
    // Target block
    private var placeInfo: PlaceInfo? = null

    // Rotation lock
    private var lockRotation: Rotation? = null

    // Mode stuff
    private val timer = TickTimer()
    private var jumpGround = 0.0

    // AutoBlock
    private var slot = 0
    override fun onDisable() {
        val thePlayer = mc.thePlayer ?: return

        mc.timer.timerSpeed = 1f
        lockRotation = null

        if (slot != thePlayer.inventory.currentItem) {
            mc.netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(thePlayer.inventory.currentItem))
        }
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (onJumpValue.get() && !mc.gameSettings.keyBindJump.isKeyDown) return
        val thePlayer = mc.thePlayer ?: return

        // Lock Rotation
        if (rotationsValue.get() && keepRotationValue.get() && lockRotation != null) {
            RotationUtils.setTargetRotation(lockRotation)
        }

        mc.timer.timerSpeed = timerValue.get()
        val eventState = event.eventState

        if (placeModeValue.get().equals(eventState.stateName, ignoreCase = true)) {
            place()
        }

        if (eventState == EventState.PRE) {
            placeInfo = null
            timer.update()

            val update = if (autoBlockValue.get()) {
                InventoryUtils.findAutoBlockBlock() != -1 || thePlayer.heldItem != null && classProvider.isItemBlock(thePlayer.heldItem!!.item)
            } else {
                thePlayer.heldItem != null && classProvider.isItemBlock(thePlayer.heldItem!!.item)
            }

            if (update) {
                if (!stopWhenBlockAbove.get() || classProvider.isBlockAir(getBlock(WBlockPos(thePlayer.posX, thePlayer.posY + 2, thePlayer.posZ)))) {
                    move()
                }
                val blockPos = WBlockPos(thePlayer.posX, thePlayer.posY - 1.0, thePlayer.posZ)
                if (classProvider.isBlockAir(mc.theWorld!!.getBlockState(blockPos).block)) {
                    if (search(blockPos) && rotationsValue.get()) {
                        val vecRotation = RotationUtils.faceBlock(blockPos)
                        if (vecRotation != null) {
                            RotationUtils.setTargetRotation(vecRotation.rotation)
                            placeInfo!!.vec3 = vecRotation.vec
                        }
                    }
                }
            }
        }
    }

    //Send jump packets, bypasses Hypixel.
    private fun fakeJump() {
        mc.thePlayer!!.isAirBorne = true
        mc.thePlayer!!.triggerAchievement(classProvider.getStatEnum(StatType.JUMP_STAT))
    }

    /**
     * Move player
     */
    private fun move() {
        val thePlayer = mc.thePlayer ?: return

        when (modeValue.get().toLowerCase()) {
            "jump" -> if (thePlayer.onGround && timer.hasTimePassed(jumpDelayValue.get())) {
                fakeJump()
                thePlayer.motionY = jumpMotionValue.get().toDouble()
                timer.reset()
            }
            "motion" -> if (thePlayer.onGround) {
                fakeJump()
                thePlayer.motionY = 0.42
            } else if (thePlayer.motionY < 0.1) {
                thePlayer.motionY = -0.3
            }
            "motiontp" -> if (thePlayer.onGround) {
                fakeJump()
                thePlayer.motionY = 0.42
            } else if (thePlayer.motionY < 0.23) {
                thePlayer.setPosition(thePlayer.posX, truncate(thePlayer.posY), thePlayer.posZ)
            }
            "packet" -> if (thePlayer.onGround && timer.hasTimePassed(2)) {
                fakeJump()
                mc.netHandler.addToSendQueue(classProvider.createCPacketPlayerPosition(thePlayer.posX,
                        thePlayer.posY + 0.42, thePlayer.posZ, false))
                mc.netHandler.addToSendQueue(classProvider.createCPacketPlayerPosition(thePlayer.posX,
                        thePlayer.posY + 0.753, thePlayer.posZ, false))
                thePlayer.setPosition(thePlayer.posX, thePlayer.posY + 1.0, thePlayer.posZ)
                timer.reset()
            }
            "teleport" -> {
                if (teleportNoMotionValue.get()) {
                    thePlayer.motionY = 0.0
                }
                if ((thePlayer.onGround || !teleportGroundValue.get()) && timer.hasTimePassed(teleportDelayValue.get())) {
                    fakeJump()
                    thePlayer.setPositionAndUpdate(thePlayer.posX, thePlayer.posY + teleportHeightValue.get(), thePlayer.posZ)
                    timer.reset()
                }
            }
            "constantmotion" -> {
                if (thePlayer.onGround) {
                    fakeJump()
                    jumpGround = thePlayer.posY
                    thePlayer.motionY = constantMotionValue.get().toDouble()
                }
                if (thePlayer.posY > jumpGround + constantMotionJumpGroundValue.get()) {
                    fakeJump()
                    thePlayer.setPosition(thePlayer.posX, truncate(thePlayer.posY), thePlayer.posZ) // TODO: toInt() required?
                    thePlayer.motionY = constantMotionValue.get().toDouble()
                    jumpGround = thePlayer.posY
                }
            }
            "aac3.3.9" -> {
                if (thePlayer.onGround) {
                    fakeJump()
                    thePlayer.motionY = 0.4001
                }
                mc.timer.timerSpeed = 1f
                if (thePlayer.motionY < 0) {
                    thePlayer.motionY -= 0.00000945
                    mc.timer.timerSpeed = 1.6f
                }
            }
            "aac3.6.4" -> if (thePlayer.ticksExisted % 4 == 1) {
                thePlayer.motionY = 0.4195464
                thePlayer.setPosition(thePlayer.posX - 0.035, thePlayer.posY, thePlayer.posZ)
            } else if (thePlayer.ticksExisted % 4 == 0) {
                thePlayer.motionY = -0.5
                thePlayer.setPosition(thePlayer.posX + 0.035, thePlayer.posY, thePlayer.posZ)
            }
        }
    }

    /**
     * Place target block
     */
    private fun place() {
        if (placeInfo == null) return
        val thePlayer = mc.thePlayer ?: return

        // AutoBlock
        var blockSlot = -1
        var itemStack = thePlayer.heldItem
        if (itemStack == null || !classProvider.isItemBlock(itemStack.item) || classProvider.isBlockBush(itemStack.item?.asItemBlock()?.block)) {
            if (!autoBlockValue.get()) return

            blockSlot = InventoryUtils.findAutoBlockBlock()
            if (blockSlot == -1) return

            mc.netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(blockSlot - 36))
            itemStack = thePlayer.inventoryContainer.getSlot(blockSlot).stack
        }

        // Place block
        if (mc.playerController.onPlayerRightClick(thePlayer, mc.theWorld!!, itemStack!!, placeInfo!!.blockPos, placeInfo!!.enumFacing, placeInfo!!.vec3)) {
            if (swingValue.get()) {
                thePlayer.swingItem()
            } else {
                mc.netHandler.addToSendQueue(classProvider.createCPacketAnimation())
            }
        }
        placeInfo = null

        // Switch back to old slot when using auto block
        if (!stayAutoBlock.get() && blockSlot >= 0) {
            mc.netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(thePlayer.inventory.currentItem))
        }
    }

    /**
     * Search for placeable block
     *
     * @param blockPosition pos
     * @return
     */
    private fun search(blockPosition: WBlockPos): Boolean {
        val thePlayer = mc.thePlayer ?: return false
        if (!isReplaceable(blockPosition)) return false

        val eyesPos = WVec3(thePlayer.posX, thePlayer.entityBoundingBox.minY + thePlayer.eyeHeight, thePlayer.posZ)
        var placeRotation: PlaceRotation? = null
        for (facingType in EnumFacingType.values()) {
            val side = classProvider.getEnumFacing(facingType)
            val neighbor = blockPosition.offset(side)

            if (!canBeClicked(neighbor))
                continue

            val dirVec = WVec3(side.directionVec)

            var xSearch = 0.1
            while (xSearch < 0.9) {
                var ySearch = 0.1
                while (ySearch < 0.9) {
                    var zSearch = 0.1
                    while (zSearch < 0.9) {
                        val posVec = WVec3(blockPosition).addVector(xSearch, ySearch, zSearch)
                        val distanceSqPosVec = eyesPos.squareDistanceTo(posVec)
                        val hitVec = posVec.add(WVec3(dirVec.xCoord * 0.5, dirVec.yCoord * 0.5, dirVec.zCoord * 0.5))
                        if (eyesPos.squareDistanceTo(hitVec) > 18.0 || distanceSqPosVec > eyesPos.squareDistanceTo(posVec.add(dirVec)) || mc.theWorld!!.rayTraceBlocks(eyesPos, hitVec, stopOnLiquid = false,
                                        ignoreBlockWithoutBoundingBox = true, returnLastUncollidableBlock = false) != null) {
                            zSearch += 0.1
                            continue
                        }

                        // face block
                        val diffX = hitVec.xCoord - eyesPos.xCoord
                        val diffY = hitVec.yCoord - eyesPos.yCoord
                        val diffZ = hitVec.zCoord - eyesPos.zCoord
                        val diffXZ = sqrt(diffX * diffX + diffZ * diffZ)

                        val rotation = Rotation(
                                WMathHelper.wrapAngleTo180_float(Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90f),
                                WMathHelper.wrapAngleTo180_float((-Math.toDegrees(atan2(diffY, diffXZ))).toFloat())
                        )
                        val rotationVector = RotationUtils.getVectorForRotation(rotation)
                        val vector = eyesPos.addVector(rotationVector.xCoord * 4, rotationVector.yCoord * 4, rotationVector.zCoord * 4)
                        val obj = mc.theWorld!!.rayTraceBlocks(eyesPos, vector, stopOnLiquid = false,
                                ignoreBlockWithoutBoundingBox = false, returnLastUncollidableBlock = true)
                        if (!(obj!!.typeOfHit == IMovingObjectPosition.WMovingObjectType.BLOCK && obj.blockPos == neighbor)) {
                            zSearch += 0.1
                            continue
                        }
                        if (placeRotation == null || RotationUtils.getRotationDifference(rotation) <
                                RotationUtils.getRotationDifference(placeRotation.rotation)) placeRotation = PlaceRotation(PlaceInfo(neighbor, side.opposite, hitVec), rotation)
                        zSearch += 0.1
                    }
                    ySearch += 0.1
                }
                xSearch += 0.1
            }
        }
        if (placeRotation == null) return false
        if (rotationsValue.get()) {
            RotationUtils.setTargetRotation(placeRotation.rotation, 0)
            lockRotation = placeRotation.rotation
        }
        placeInfo = placeRotation.placeInfo
        return true
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (mc.thePlayer == null) return
        val packet = event.packet
        if (classProvider.isCPacketHeldItemChange(packet)) {
            slot = packet.asCPacketHeldItemChange().slotId
        }
    }

    /**
     * Tower visuals
     *
     * @param event
     */
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

            RenderUtils.drawBorderedRect(scaledResolution.scaledWidth / 2 - 2.toFloat(),
                    scaledResolution.scaledHeight / 2 + 5.toFloat(),
                    scaledResolution.scaledWidth / 2 + Fonts.font40.getStringWidth(info) + 2.toFloat(),
                    scaledResolution.scaledHeight / 2 + 16.toFloat(), 3f, Color.BLACK.rgb, Color.BLACK.rgb)

            classProvider.getGlStateManager().resetColor()

            Fonts.font40.drawString(info, scaledResolution.scaledWidth / 2.toFloat(),
                    scaledResolution.scaledHeight / 2 + 7.toFloat(), Color.WHITE.rgb)
            GL11.glPopMatrix()
        }
    }

    @EventTarget
    fun onJump(event: JumpEvent) {
        if (onJumpValue.get()) event.cancelEvent()
    }

    /**
     * @return hotbar blocks amount
     */
    private val blocksAmount: Int
        get() {
            var amount = 0
            for (i in 36..44) {
                val itemStack = mc.thePlayer!!.inventoryContainer.getSlot(i).stack
                if (itemStack != null && classProvider.isItemBlock(itemStack.item)) {
                    val block = itemStack.item!!.asItemBlock().block
                    if (mc.thePlayer!!.heldItem == itemStack || !InventoryUtils.BLOCK_BLACKLIST.contains(block)) {
                        amount += itemStack.stackSize
                    }
                }
            }
            return amount
        }

    override val tag: String
        get() = modeValue.get()
}
