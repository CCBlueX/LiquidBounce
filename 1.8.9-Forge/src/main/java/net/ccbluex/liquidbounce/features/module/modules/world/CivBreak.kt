package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.minecraft.block.BlockAir
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import java.awt.Color

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@ModuleInfo(name = "CivBrechen", description = "Allows you to break blocks instantly.", category = ModuleCategory.WORLD)
class CivBreak : Module() {

    private var blockPos: BlockPos? = null
    private var enumFacing: EnumFacing? = null

    private val airResetValue = BoolValue("Air-Reset", true)
    private val range = FloatValue("Range", 5F, 1F, 6F)
    private val rangeResetValue = BoolValue("Range-Reset", true)
    private val rotationsValue = BoolValue("Rotations", true)

    @EventTarget
    fun onBlockClick(event: ClickBlockEvent) {
        if (BlockUtils.getBlock(event.clickedBlock) == Blocks.bedrock)
            return

        blockPos = event.clickedBlock
        enumFacing = event.enumFacing

        // Break
        mc.netHandler.addToSendQueue(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, blockPos, enumFacing))
        mc.netHandler.addToSendQueue(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, blockPos, enumFacing))
    }

    @EventTarget
    fun onUpdate(event: MotionEvent) {
        if (event.eventState != EventState.POST)
            return

        blockPos ?: return

        if (BlockUtils.getBlock(blockPos) is BlockAir &&
                BlockUtils.getCenterDistance(blockPos!!) <= range.get()) {
            mc.thePlayer.swingItem()

            if (rotationsValue.get())
                RotationUtils.faceBlock(blockPos)

            // Break
            mc.netHandler.addToSendQueue(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, blockPos, enumFacing))
            mc.netHandler.addToSendQueue(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, blockPos, enumFacing))
            mc.playerController.clickBlock(blockPos, enumFacing)
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        blockPos ?: return

        if (airResetValue.get() && BlockUtils.getBlock(blockPos) is BlockAir ||
                rangeResetValue.get() && BlockUtils.getCenterDistance(blockPos!!) >= range.get()) {
            blockPos = null
            return
        }

        RenderUtils.drawBlockBox(blockPos, Color.RED, true)
    }
}