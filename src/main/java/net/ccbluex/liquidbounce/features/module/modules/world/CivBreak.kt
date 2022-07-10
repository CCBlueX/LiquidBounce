/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.ClickBlockEvent
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.distanceToCenter
import net.ccbluex.liquidbounce.utils.extensions.getBlock
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.minecraft.block.BlockAir
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing

@ModuleInfo(name = "CivBreak", description = "Allows you to break blocks instantly.", category = ModuleCategory.WORLD)
class CivBreak : Module()
{
    private val range = FloatValue("Range", 5F, 1F, 6F)

    private val rotationsValue = BoolValue("Rotations", true)
    private val visualSwingValue = BoolValue("VisualSwing", true)
    private val airResetValue = BoolValue("Air-Reset", true)

    private val rangeResetValue = BoolValue("Range-Reset", true)

    var blockPos: BlockPos? = null
    private var enumFacing: EnumFacing? = null

    @EventTarget
    fun onBlockClick(event: ClickBlockEvent)
    {
        val theWorld = mc.theWorld ?: return

        if (event.clickedBlock?.let(theWorld::getBlock) == Blocks.bedrock) return

        val netHandler = mc.netHandler

        val enumFacing = (event.WEnumFacing ?: return).also { enumFacing = it }
        val blockPos = (event.clickedBlock ?: return).also { blockPos = it }

        // Break
        netHandler.addToSendQueue(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, blockPos, enumFacing))
        netHandler.addToSendQueue(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, blockPos, enumFacing))
    }

    @EventTarget
    fun onUpdate(event: MotionEvent)
    {
        val pos = blockPos ?: return

        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        if (airResetValue.get() && theWorld.getBlock(pos) is BlockAir || rangeResetValue.get() && thePlayer.distanceToCenter(pos) > range.get())
        {
            blockPos = null
            return
        }

        val netHandler = mc.netHandler

        if (theWorld.getBlock(pos) is BlockAir || thePlayer.distanceToCenter(pos) > range.get()) return

        when (event.eventState)
        {
            EventState.PRE -> if (rotationsValue.get()) RotationUtils.setTargetRotation((RotationUtils.faceBlock(theWorld, thePlayer, pos) ?: return).rotation)

            EventState.POST ->
            {
                val facing = enumFacing ?: return

                if (visualSwingValue.get()) thePlayer.swingItem()
                else netHandler.addToSendQueue(C0APacketAnimation())

                // Break
                netHandler.addToSendQueue(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, facing))
                netHandler.addToSendQueue(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, facing))
                mc.playerController.clickBlock(pos, facing)
            }
        }
    }

    @EventTarget
    fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
    {
        RenderUtils.drawBlockBox(mc.theWorld ?: return, mc.thePlayer ?: return, blockPos ?: return, -65536, 0, false, event.partialTicks)
    }

    override val tag: String
        get() = "${range.get()}"
}
