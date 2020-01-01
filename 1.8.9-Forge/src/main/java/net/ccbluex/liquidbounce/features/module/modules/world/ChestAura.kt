package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.player.Blink
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BlockValue
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.block.Block
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import java.util.*
import kotlin.math.atan2

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@ModuleInfo(name = "KistenAura", description = "Automatically opens chests around you.", category = ModuleCategory.WORLD)
class ChestAura : Module() {

    private val rangeValue = FloatValue("Range", 5F, 1F, 6F)
    private val delayValue = IntegerValue("Delay", 100, 50, 200)
    private val throughWallsValue = BoolValue("ThroughWalls", true)
    private val visualSwing = BoolValue("VisualSwing", true)
    private val chestValue = BlockValue("Chest", Block.getIdFromBlock(Blocks.chest))

    private var currentBlock: BlockPos? = null
    private val msTimer = MSTimer()

    companion object {
        val clickedBlocks: MutableList<BlockPos> = ArrayList()
    }

    override fun onDisable() {
        mc.thePlayer ?: return

        clickedBlocks.clear()
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (ModuleManager.getModule(Blink::class.java)?.state!!)
            return

        when (event.eventState) {
            EventState.PRE -> {
                if (mc.currentScreen is GuiContainer)
                    msTimer.reset()

                val eyesPos = Vec3(mc.thePlayer.posX, mc.thePlayer.entityBoundingBox.minY + mc.thePlayer.getEyeHeight(),
                        mc.thePlayer.posZ)
                val radius = rangeValue.get() + 1

                currentBlock = BlockUtils.searchBlocks(radius.toInt())
                        .filter {
                            Block.getIdFromBlock(it.value) == chestValue.get() && !clickedBlocks.contains(it.key)
                                    && BlockUtils.getCenterDistance(it.key) < rangeValue.get()
                        }
                        .filter {
                            if (throughWallsValue.get()) return@filter true
                            val blockPos = it.key
                            val movingObjectPosition = mc.theWorld.rayTraceBlocks(eyesPos,
                                    Vec3(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5), false, true, false)

                            movingObjectPosition != null && movingObjectPosition.blockPos == blockPos
                        }
                        .minBy { BlockUtils.getCenterDistance(it.key) }
                        ?.key
            }

            EventState.POST -> if (currentBlock != null) {
                if (msTimer.hasTimePassed(delayValue.get().toLong()) &&
                        mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.heldItem, currentBlock, EnumFacing.DOWN, Vec3(currentBlock!!.x.toDouble(), currentBlock!!.y.toDouble(), currentBlock!!.z.toDouble()))) {
                    if (visualSwing.get())
                        mc.thePlayer.swingItem()
                    else
                        mc.netHandler.addToSendQueue(C0APacketAnimation())

                    clickedBlocks.add(currentBlock!!)
                    currentBlock = null
                    msTimer.reset()
                }
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (packet is C03PacketPlayer && currentBlock != null) {
            val diffX = currentBlock!!.x + 0.5 - mc.thePlayer.posX
            val diffY = currentBlock!!.y + 0.5 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight())
            val diffZ = currentBlock!!.z + 0.5 - mc.thePlayer.posZ
            val dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ).toDouble()

            packet.yaw = (atan2(diffZ, diffX) * 180.0 / Math.PI).toFloat() - 90.0f
            packet.pitch = (-(atan2(diffY, dist) * 180.0 / Math.PI)).toFloat()
            packet.rotating = true
        }
    }
}