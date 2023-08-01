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
import net.ccbluex.liquidbounce.utils.InventoryUtils.serverSlot
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.PlaceRotation
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils.faceBlock
import net.ccbluex.liquidbounce.utils.RotationUtils.getRotationDifference
import net.ccbluex.liquidbounce.utils.RotationUtils.getVectorForRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.block.BlockUtils.canBeClicked
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isReplaceable
import net.ccbluex.liquidbounce.utils.block.PlaceInfo
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBorderedRect
import net.ccbluex.liquidbounce.utils.timer.TickTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.BlockBush
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager.resetColor
import net.minecraft.init.Blocks.air
import net.minecraft.item.ItemBlock
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.stats.StatList
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import kotlin.math.truncate

object Tower : Module("Tower", ModuleCategory.WORLD, Keyboard.KEY_O) {
    /**
     * OPTIONS
     */
    private val mode by ListValue(
        "Mode",
        arrayOf("Jump", "Motion", "ConstantMotion", "MotionTP", "Packet", "Teleport", "AAC3.3.9", "AAC3.6.4"),
        "Motion"
    )
    private val autoBlock by ListValue("AutoBlock", arrayOf("Off", "Pick", "Spoof", "Switch"), "Spoof")
    private val swing by BoolValue("Swing", true)
    private val stopWhenBlockAbove by BoolValue("StopWhenBlockAbove", false)
    private val rotations by BoolValue("Rotations", true)
    private val keepRotation by BoolValue("KeepRotation", false) { rotations }
    private val onJump by BoolValue("OnJump", false)
    private val matrix by BoolValue("Matrix", false)
    private val placeMode by ListValue("PlaceTiming", arrayOf("Pre", "Post"), "Post") { mode != "Packet" }
    private val timer by FloatValue("Timer", 1f, 0.01f..10f)

    // Jump mode
    private val jumpMotion by FloatValue("JumpMotion", 0.42f, 0.3681289f..0.79f) { mode == "Jump" }
    private val jumpDelay by IntegerValue("JumpDelay", 0, 0..20) { mode == "Jump" }

    // ConstantMotion
    private val constantMotion by FloatValue("ConstantMotion", 0.42f, 0.1f..1f) { mode == "ConstantMotion" }
    private val constantMotionJumpGround by FloatValue("ConstantMotionJumpGround", 0.79f, 0.76f..1f) { mode == "ConstantMotion" }

    // Teleport
    private val teleportHeight by FloatValue("TeleportHeight", 1.15f, 0.1f..5f) { mode == "Teleport" }
    private val teleportDelay by IntegerValue("TeleportDelay", 0, 0..20) { mode == "Teleport" }
    private val teleportGround by BoolValue("TeleportGround", true) { mode == "Teleport" }
    private val teleportNoMotion by BoolValue("TeleportNoMotion", false) { mode == "Teleport" }

    // Render
    private val counterDisplay by BoolValue("Counter", true)

    /**
     * MODULE
     */
    // Target block
    private var placeInfo: PlaceInfo? = null

    // Rotation lock
    private var lockRotation: Rotation? = null

    // Mode stuff
    private val tickTimer = TickTimer()
    private var jumpGround = 0.0

    override fun onDisable() {
        val thePlayer = mc.thePlayer ?: return

        mc.timer.timerSpeed = 1f
        lockRotation = null

        if (serverSlot != thePlayer.inventory.currentItem) {
            sendPacket(C09PacketHeldItemChange(thePlayer.inventory.currentItem))
        }
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (onJump && !mc.gameSettings.keyBindJump.isKeyDown) return
        val thePlayer = mc.thePlayer ?: return

        // Lock Rotation
        if (rotations && keepRotation && lockRotation != null) setTargetRotation(lockRotation!!)

        mc.timer.timerSpeed = timer
        val eventState = event.eventState

        // Force use of POST event when Packet mode is selected, it doesn't work with PRE mode
        if (eventState.stateName == (if (mode == "Packet") "POST" else placeMode.uppercase()))
            place()

        if (eventState == EventState.PRE) {
            placeInfo = null
            tickTimer.update()

            val update =
                (autoBlock != "Off" && InventoryUtils.findBlockInHotbar() != null) || thePlayer.heldItem?.item is ItemBlock

            if (update) {
                if (!stopWhenBlockAbove || getBlock(BlockPos(thePlayer).up(2)) == air) move()

                val blockPos = BlockPos(thePlayer).down()
                if (blockPos.getBlock() == air) {
                    if (search(blockPos) && rotations) {
                        val vecRotation = faceBlock(blockPos)
                        if (vecRotation != null) {
                            setTargetRotation(vecRotation.rotation)
                            placeInfo!!.vec3 = vecRotation.vec
                        }
                    }
                }
            }
        }
    }

    //Send jump packets, bypasses Hypixel.
    private fun fakeJump() {
        mc.thePlayer.isAirBorne = true
        mc.thePlayer.triggerAchievement(StatList.jumpStat)
    }

    /**
     * Move player
     */
    private fun move() {
        val thePlayer = mc.thePlayer ?: return

        when (mode.lowercase()) {
            "jump" -> if (thePlayer.onGround && tickTimer.hasTimePassed(jumpDelay)) {
                fakeJump()
                thePlayer.motionY = jumpMotion.toDouble()
                tickTimer.reset()
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

            "packet" -> if (thePlayer.onGround && tickTimer.hasTimePassed(2)) {
                fakeJump()
                sendPackets(
                    C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY + 0.42, thePlayer.posZ, false),
                    C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY + 0.753, thePlayer.posZ, false)
                )
                thePlayer.setPosition(thePlayer.posX, thePlayer.posY + 1.0, thePlayer.posZ)
                tickTimer.reset()
            }

            "teleport" -> {
                if (teleportNoMotion) {
                    thePlayer.motionY = 0.0
                }
                if ((thePlayer.onGround || !teleportGround) && tickTimer.hasTimePassed(teleportDelay)) {
                    fakeJump()
                    thePlayer.setPositionAndUpdate(
                        thePlayer.posX, thePlayer.posY + teleportHeight, thePlayer.posZ
                    )
                    tickTimer.reset()
                }
            }

            "constantmotion" -> {
                if (thePlayer.onGround) {
                    fakeJump()
                    jumpGround = thePlayer.posY
                    thePlayer.motionY = constantMotion.toDouble()
                }
                if (thePlayer.posY > jumpGround + constantMotionJumpGround) {
                    fakeJump()
                    thePlayer.setPosition(
                        thePlayer.posX, truncate(thePlayer.posY), thePlayer.posZ
                    ) // TODO: toInt() required?
                    thePlayer.motionY = constantMotion.toDouble()
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
        var itemStack = thePlayer.heldItem
        if (itemStack == null || itemStack.item !is ItemBlock || (itemStack.item as ItemBlock).block is BlockBush) {
            val blockSlot = InventoryUtils.findBlockInHotbar() ?: return

            when (autoBlock) {
                "Off" -> return
                "Pick" -> {
                    mc.thePlayer.inventory.currentItem = blockSlot - 36
                    mc.playerController.updateController()
                }

                "Spoof", "Switch" -> {
                    if (blockSlot - 36 != serverSlot) {
                        sendPacket(C09PacketHeldItemChange(blockSlot - 36))
                    }
                }
            }
            itemStack = thePlayer.inventoryContainer.getSlot(blockSlot).stack
        }

        // Place block
        if (mc.playerController.onPlayerRightClick(
                thePlayer, mc.theWorld!!, itemStack!!, placeInfo!!.blockPos, placeInfo!!.enumFacing, placeInfo!!.vec3
            )
        ) {
            if (swing) {
                thePlayer.swingItem()
            } else {
                sendPacket(C0APacketAnimation())
            }
        }
        if (autoBlock == "Switch" && serverSlot != mc.thePlayer.inventory.currentItem)
            sendPacket(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))

        placeInfo = null
    }

    /**
     * Search for placeable block
     *
     * @param blockPosition pos
     * @return
     */
    private fun search(blockPosition: BlockPos): Boolean {
        val thePlayer = mc.thePlayer ?: return false
        if (!isReplaceable(blockPosition)) {
            return false
        }

        val eyesPos = thePlayer.eyes
        var placeRotation: PlaceRotation? = null
        for (facingType in EnumFacing.values()) {
            val neighbor = blockPosition.offset(facingType)
            if (!canBeClicked(neighbor)) {
                continue
            }
            val dirVec = Vec3(facingType.directionVec)

            for (x in 0.1..0.9) {
                for (y in 0.1..0.9) {
                    for (z in 0.1..0.9) {
                        val posVec = Vec3(blockPosition).addVector(
                            if (matrix) 0.5 else x, if (matrix) 0.5 else y, if (matrix) 0.5 else z
                        )

                        val distanceSqPosVec = eyesPos.squareDistanceTo(posVec)
                        val hitVec = posVec + (dirVec * 0.5)

                        if (eyesPos.distanceTo(hitVec) > 4.25
	                        || distanceSqPosVec > eyesPos.squareDistanceTo(posVec + dirVec)
                            || mc.theWorld.rayTraceBlocks(eyesPos, hitVec, false, true, false) != null
                        ) continue

                        // face block
                        val rotation = toRotation(hitVec, false)

                        val rotationVector = getVectorForRotation(rotation)
                        val vector = eyesPos + (rotationVector * 4.25)

                        val obj = mc.theWorld.rayTraceBlocks(eyesPos, vector, false, false, true) ?: continue

                        if (obj.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || obj.blockPos != neighbor)
                            continue

                        if (placeRotation == null || getRotationDifference(rotation) < getRotationDifference(placeRotation.rotation))
                            placeRotation = PlaceRotation(PlaceInfo(neighbor, facingType.opposite, hitVec), rotation)
                    }
                }
            }
        }

        placeRotation ?: return false

        if (rotations) {
            setTargetRotation(placeRotation.rotation)
            lockRotation = placeRotation.rotation
        }
        placeInfo = placeRotation.placeInfo
        return true
    }

    /**
     * Tower visuals
     *
     * @param event
     */
    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (counterDisplay) {
            glPushMatrix()

            if (BlockOverlay.state && BlockOverlay.info && BlockOverlay.currentBlock != null)
                glTranslatef(0f, 15f, 0f)

            val info = "Blocks: §7$blocksAmount"
            val (width, height) = ScaledResolution(mc)

            drawBorderedRect(
                width / 2f - 2,
                height / 2f + 5,
                width / 2f + Fonts.font40.getStringWidth(info) + 2,
                height / 2f + 16,
                3f,
                Color.BLACK.rgb,
                Color.BLACK.rgb
            )

            resetColor()

            Fonts.font40.drawString(
                info,
                width / 2.toFloat(),
                height / 2 + 7.toFloat(),
                Color.WHITE.rgb
            )
            glPopMatrix()
        }
    }

    @EventTarget
    fun onJump(event: JumpEvent) {
        if (onJump) event.cancelEvent()
    }

    /**
     * @return hotbar blocks amount
     */
    private val blocksAmount: Int
        get() {
            var amount = 0
            for (i in 36..44) {
                val itemStack = mc.thePlayer.inventoryContainer.getSlot(i).stack
                val item = itemStack?.item ?: continue
                if (item is ItemBlock) {
                    val block = item.block
                    if (mc.thePlayer.heldItem == itemStack || block !in InventoryUtils.BLOCK_BLACKLIST) {
                        amount += itemStack.stackSize
                    }
                }
            }
            return amount
        }

    override val tag
        get() = mode
}