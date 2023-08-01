/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.RotationUtils.faceBlock
import net.ccbluex.liquidbounce.utils.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getCenterDistance
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBlockBox
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.minecraft.init.Blocks.air
import net.minecraft.init.Blocks.bedrock
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action.START_DESTROY_BLOCK
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import java.awt.Color

object CivBreak : Module("CivBreak", ModuleCategory.WORLD) {

    private var blockPos: BlockPos? = null
    private var enumFacing: EnumFacing? = null

    private val range by FloatValue("Range", 5F, 1F..6F)
    private val rotations by BoolValue("Rotations", true)
    private val visualSwing by BoolValue("VisualSwing", true)

    private val airReset by BoolValue("Air-Reset", true)
    private val rangeReset by BoolValue("Range-Reset", true)


    @EventTarget
    fun onBlockClick(event: ClickBlockEvent) {
        if (event.clickedBlock?.let { getBlock(it) } == bedrock)
            return

        blockPos = event.clickedBlock ?: return
        enumFacing = event.WEnumFacing ?: return

        // Break
        sendPackets(
            C07PacketPlayerDigging(START_DESTROY_BLOCK, blockPos, enumFacing),
            C07PacketPlayerDigging(STOP_DESTROY_BLOCK, blockPos, enumFacing)
        )
    }

    @EventTarget
    fun onUpdate(event: MotionEvent) {
        val pos = blockPos ?: return
        val isAirBlock = getBlock(pos) == air

        if (airReset && isAirBlock ||
                rangeReset && getCenterDistance(pos) > range) {
            blockPos = null
            return
        }

        if (isAirBlock || getCenterDistance(pos) > range)
            return

        when (event.eventState) {
            EventState.PRE -> if (rotations)
                setTargetRotation((faceBlock(pos) ?: return).rotation)

            EventState.POST -> {
                if (visualSwing)
                    mc.thePlayer.swingItem()
                else
                    sendPacket(C0APacketAnimation())

                // Break
                sendPackets(
                    C07PacketPlayerDigging(START_DESTROY_BLOCK, blockPos, enumFacing),
                    C07PacketPlayerDigging(STOP_DESTROY_BLOCK, blockPos, enumFacing)
                )
                mc.playerController.clickBlock(blockPos, enumFacing)
            }
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        drawBlockBox(blockPos ?: return, Color.RED, true)
    }
}