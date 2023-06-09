/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.SlowDownEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.minecraft.item.*
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action.RELEASE_USE_ITEM
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing

object NoSlow : Module("NoSlow", ModuleCategory.MOVEMENT) {

    // Highly customizable values

    private val blockForwardMultiplier by FloatValue("BlockForwardMultiplier", 1f, 0.2F..1f)
    private val blockStrafeMultiplier by FloatValue("BlockStrafeMultiplier", 1f, 0.2F..1f)

    private val consumeForwardMultiplier by FloatValue("ConsumeForwardMultiplier", 1f, 0.2F..1f)
    private val consumeStrafeMultiplier by FloatValue("ConsumeStrafeMultiplier", 1f, 0.2F..1f)

    private val bowForwardMultiplier by FloatValue("BowForwardMultiplier", 1f, 0.2F..1f)
    private val bowStrafeMultiplier by FloatValue("BowStrafeMultiplier", 1f, 0.2F..1f)

    // NCP mode
    private val packet by BoolValue("Packet", true)

    // Blocks
    val soulsand by BoolValue("Soulsand", true)
    val liquidPush by BoolValue("LiquidPush", true)

    @EventTarget
    fun onMotion(event: MotionEvent) {
        val thePlayer = mc.thePlayer ?: return
        val heldItem = thePlayer.heldItem ?: return

        if (heldItem.item !is ItemSword || !isMoving)
            return

        if (!thePlayer.isBlocking && !KillAura.blockStatus)
            return

        if (packet) {
            when (event.eventState) {
                EventState.PRE ->
                    sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos(0, 0, 0), EnumFacing.DOWN))
                EventState.POST ->
                    sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, mc.thePlayer.heldItem, 0f, 0f, 0f))
            }
        }
    }

    @EventTarget
    fun onSlowDown(event: SlowDownEvent) {
        val heldItem = mc.thePlayer.heldItem?.item

        event.forward = getMultiplier(heldItem, true)
        event.strafe = getMultiplier(heldItem, false)
    }

    private fun getMultiplier(item: Item?, isForward: Boolean) =
        when (item) {
            is ItemFood, is ItemPotion, is ItemBucketMilk ->
                if (isForward) consumeForwardMultiplier else consumeStrafeMultiplier

            is ItemSword ->
                if (isForward) blockForwardMultiplier else blockStrafeMultiplier

            is ItemBow ->
                if (isForward) bowForwardMultiplier else bowStrafeMultiplier

            else -> 0.2F
        }
}
