/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.render.FreeCam
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.VecRotation
import net.ccbluex.liquidbounce.utils.block.BlockUtils.collideBlock
import net.ccbluex.liquidbounce.utils.misc.FallingPlayer
import net.ccbluex.liquidbounce.utils.timer.TickTimer
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.BlockLiquid
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemBucket
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import kotlin.math.ceil
import kotlin.math.sqrt

@ModuleInfo(name = "NoFall", description = "Prevents you from taking fall damage.", category = ModuleCategory.PLAYER)
class NoFall : Module() {
    @JvmField
    val modeValue = ListValue(
        "Mode", arrayOf(
            "SpoofGround",
            "NoGround",
            "Packet",
            "MLG",
            "AAC",
            "LAAC",
            "AAC3.3.11",
            "AAC3.3.15",
            "Spartan",
            "CubeCraft",
            "Hypixel"
        ), "SpoofGround"
    )
    private val minFallDistance = FloatValue("MinMLGHeight", 5f, 2f, 50f)
    private val spartanTimer = TickTimer()
    private val mlgTimer = TickTimer()
    private var currentState = 0
    private var jumped = false
    private var currentMlgRotation: VecRotation? = null
    private var currentMlgItemIndex = 0
    private var currentMlgBlock: BlockPos? = null

    @EventTarget(ignoreCondition = true)
    fun onUpdate(event: UpdateEvent?) {
        if (mc.thePlayer!!.onGround) jumped = false

        if (mc.thePlayer!!.motionY > 0) jumped = true

        if (!state || LiquidBounce.moduleManager.getModule(FreeCam::class.java).state) return

        if (collideBlock(mc.thePlayer!!.entityBoundingBox) { it is BlockLiquid } || collideBlock(
                AxisAlignedBB.fromBounds(
                    mc.thePlayer!!.entityBoundingBox.maxX,
                    mc.thePlayer!!.entityBoundingBox.maxY,
                    mc.thePlayer!!.entityBoundingBox.maxZ,
                    mc.thePlayer!!.entityBoundingBox.minX,
                    mc.thePlayer!!.entityBoundingBox.minY - 0.01,
                    mc.thePlayer!!.entityBoundingBox.minZ
                )
            ) { it is BlockLiquid }
        ) return

        when (modeValue.get().lowercase()) {
            "packet" -> {
                if (mc.thePlayer!!.fallDistance > 2f) {
                    mc.netHandler.addToSendQueue(C03PacketPlayer(true))
                }
            }
            "cubecraft" -> if (mc.thePlayer!!.fallDistance > 2f) {
                mc.thePlayer!!.onGround = false
                mc.thePlayer!!.sendQueue.addToSendQueue(C03PacketPlayer(true))
            }
            "aac" -> {
                if (mc.thePlayer!!.fallDistance > 2f) {
                    mc.netHandler.addToSendQueue(C03PacketPlayer(true))
                    currentState = 2
                } else if (currentState == 2 && mc.thePlayer!!.fallDistance < 2) {
                    mc.thePlayer!!.motionY = 0.1
                    currentState = 3
                    return
                }
                when (currentState) {
                    3 -> {
                        mc.thePlayer!!.motionY = 0.1
                        currentState = 4
                    }
                    4 -> {
                        mc.thePlayer!!.motionY = 0.1
                        currentState = 5
                    }
                    5 -> {
                        mc.thePlayer!!.motionY = 0.1
                        currentState = 1
                    }
                }
            }
            "laac" -> if (!jumped && mc.thePlayer!!.onGround && !mc.thePlayer!!.isOnLadder && !mc.thePlayer!!.isInWater && !mc.thePlayer!!.isInWeb) mc.thePlayer!!.motionY =
                (-6).toDouble()
            "aac3.3.11" -> if (mc.thePlayer!!.fallDistance > 2) {
                mc.thePlayer!!.motionZ = 0.0
                mc.thePlayer!!.motionX = mc.thePlayer!!.motionZ
                mc.netHandler.addToSendQueue(
                    C03PacketPlayer.C04PacketPlayerPosition(
                        mc.thePlayer!!.posX, mc.thePlayer!!.posY - 10E-4, mc.thePlayer!!.posZ, mc.thePlayer!!.onGround
                    )
                )
                mc.netHandler.addToSendQueue(C03PacketPlayer(true))
            }
            "aac3.3.15" -> if (mc.thePlayer!!.fallDistance > 2) {
                if (!mc.isIntegratedServerRunning) mc.netHandler.addToSendQueue(
                    C03PacketPlayer.C04PacketPlayerPosition(
                        mc.thePlayer!!.posX, Double.NaN, mc.thePlayer!!.posZ, false
                    )
                )
                mc.thePlayer!!.fallDistance = (-9999).toFloat()
            }
            "spartan" -> {
                spartanTimer.update()
                if (mc.thePlayer!!.fallDistance > 1.5 && spartanTimer.hasTimePassed(10)) {
                    mc.netHandler.addToSendQueue(
                        C03PacketPlayer.C04PacketPlayerPosition(
                            mc.thePlayer!!.posX, mc.thePlayer!!.posY + 10, mc.thePlayer!!.posZ, true
                        )
                    )
                    mc.netHandler.addToSendQueue(
                        C03PacketPlayer.C04PacketPlayerPosition(
                            mc.thePlayer!!.posX, mc.thePlayer!!.posY - 10, mc.thePlayer!!.posZ, true
                        )
                    )
                    spartanTimer.reset()
                }
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        val mode = modeValue.get()
        if (packet is C03PacketPlayer) {
            if (mode.equals("SpoofGround", ignoreCase = true)) packet.onGround = true
            if (mode.equals("NoGround", ignoreCase = true)) packet.onGround = false
            if (mode.equals(
                    "Hypixel", ignoreCase = true
                ) && mc.thePlayer != null && mc.thePlayer!!.fallDistance > 1.5
            ) packet.onGround = mc.thePlayer!!.ticksExisted % 2 == 0
        }
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        if (collideBlock(
                mc.thePlayer!!.entityBoundingBox
            ) { it is BlockLiquid } || collideBlock(
                AxisAlignedBB.fromBounds(
                    mc.thePlayer!!.entityBoundingBox.maxX,
                    mc.thePlayer!!.entityBoundingBox.maxY,
                    mc.thePlayer!!.entityBoundingBox.maxZ,
                    mc.thePlayer!!.entityBoundingBox.minX,
                    mc.thePlayer!!.entityBoundingBox.minY - 0.01,
                    mc.thePlayer!!.entityBoundingBox.minZ
                )
            ) { it is BlockLiquid }
        ) return

        if (modeValue.get().equals("laac", ignoreCase = true)) {
            if (!jumped && !mc.thePlayer!!.onGround && !mc.thePlayer!!.isOnLadder && !mc.thePlayer!!.isInWater && !mc.thePlayer!!.isInWeb && mc.thePlayer!!.motionY < 0.0) {
                event.x = 0.0
                event.z = 0.0
            }
        }
    }

    @EventTarget
    private fun onMotionUpdate(event: MotionEvent) {
        if (!modeValue.get().equals("MLG", ignoreCase = true)) return

        if (event.eventState == EventState.PRE) {
            currentMlgRotation = null

            mlgTimer.update()

            if (!mlgTimer.hasTimePassed(10)) return

            if (mc.thePlayer!!.fallDistance > minFallDistance.get()) {
                val fallingPlayer = FallingPlayer(
                    mc.thePlayer!!.posX,
                    mc.thePlayer!!.posY,
                    mc.thePlayer!!.posZ,
                    mc.thePlayer!!.motionX,
                    mc.thePlayer!!.motionY,
                    mc.thePlayer!!.motionZ,
                    mc.thePlayer!!.rotationYaw,
                    mc.thePlayer!!.moveStrafing,
                    mc.thePlayer!!.moveForward
                )

                val maxDist: Double = mc.playerController.blockReachDistance + 1.5

                val collision =
                    fallingPlayer.findCollision(ceil(1.0 / mc.thePlayer!!.motionY * -maxDist).toInt()) ?: return

                var ok: Boolean = Vec3(
                    mc.thePlayer!!.posX, mc.thePlayer!!.posY + mc.thePlayer!!.eyeHeight, mc.thePlayer!!.posZ
                ).distanceTo(
                    Vec3(collision.pos).addVector(
                        0.5, 0.5, 0.5
                    )
                ) < mc.playerController.blockReachDistance + sqrt(0.75)

                if (mc.thePlayer!!.motionY < collision.pos.y + 1 - mc.thePlayer!!.posY) {
                    ok = true
                }

                if (!ok) return

                var index = -1

                for (i in 36..44) {
                    val itemStack = mc.thePlayer!!.inventoryContainer.getSlot(i).stack

                    if (itemStack != null && (itemStack.item == Items.water_bucket || itemStack.item is ItemBlock && (itemStack.item as ItemBlock).block == Blocks.web)
                    ) {
                        index = i - 36

                        if (mc.thePlayer!!.inventory.currentItem == index) break
                    }
                }
                if (index == -1) return

                currentMlgItemIndex = index
                currentMlgBlock = collision.pos

                if (mc.thePlayer!!.inventory.currentItem != index) {
                    mc.thePlayer!!.sendQueue.addToSendQueue(C09PacketHeldItemChange(index))
                }

                currentMlgRotation = RotationUtils.faceBlock(collision.pos)
                currentMlgRotation!!.rotation.toPlayer(mc.thePlayer!!)
            }
        } else if (currentMlgRotation != null) {
            val stack = mc.thePlayer!!.inventory.getStackInSlot(currentMlgItemIndex)

            if (stack!!.item is ItemBucket) {
                mc.playerController.sendUseItem(mc.thePlayer!!, mc.theWorld!!, stack)
            } else {
                if (mc.playerController.sendUseItem(mc.thePlayer!!, mc.theWorld!!, stack)) {
                    mlgTimer.reset()
                }
            }
            if (mc.thePlayer!!.inventory.currentItem != currentMlgItemIndex) mc.thePlayer!!.sendQueue.addToSendQueue(
                C09PacketHeldItemChange(mc.thePlayer!!.inventory.currentItem)
            )
        }
    }

    @EventTarget(ignoreCondition = true)
    fun onJump(event: JumpEvent?) {
        jumped = true
    }

    override val tag: String
        get() = modeValue.get()
}