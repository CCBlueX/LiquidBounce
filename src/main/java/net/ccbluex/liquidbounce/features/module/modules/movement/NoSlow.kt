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
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.item.*
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action.RELEASE_USE_ITEM
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing

object NoSlow : Module("NoSlow", ModuleCategory.MOVEMENT) {

    private val swordMode by ListValue("SwordMode", arrayOf("None", "NCP", "AAC5", "SwitchItem"), "None")

    private val blockForwardMultiplier by FloatValue("BlockForwardMultiplier", 1f, 0.2F..1f)
    private val blockStrafeMultiplier by FloatValue("BlockStrafeMultiplier", 1f, 0.2F..1f)

    private val consumePacket by ListValue("ConsumeMode", arrayOf("None", "AAC5"), "None")

    private val consumeForwardMultiplier by FloatValue("ConsumeForwardMultiplier", 1f, 0.2F..1f)
    private val consumeStrafeMultiplier by FloatValue("ConsumeStrafeMultiplier", 1f, 0.2F..1f)

    private val bowForwardMultiplier by FloatValue("BowForwardMultiplier", 1f, 0.2F..1f)
    private val bowStrafeMultiplier by FloatValue("BowStrafeMultiplier", 1f, 0.2F..1f)


    // Blocks
    val soulsand by BoolValue("Soulsand", true)
    val liquidPush by BoolValue("LiquidPush", true)

    @EventTarget
    fun onMotion(event: MotionEvent) {
        val thePlayer = mc.thePlayer ?: return
        val heldItem = thePlayer.heldItem ?: return

        if (!isMoving) {
            return
        }

        if ((heldItem.item is ItemFood || heldItem.item is ItemPotion || heldItem.item is ItemBucketMilk) && thePlayer.isUsingItem) {
            when (consumePacket.lowercase()) {
                "aac5" -> {
                    sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, thePlayer.heldItem, 0f, 0f, 0f))
                }

                else -> {
                    return
                }
            }
        }

        if (heldItem.item is ItemSword && thePlayer.isBlocking) {
            when (swordMode.lowercase()) {
                "none" -> {
                    return
                }

                "ncp" -> {
                    when (event.eventState) {
                        EventState.PRE -> sendPacket(
                            C07PacketPlayerDigging(
                                RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN
                            )
                        )

                        EventState.POST -> sendPacket(
                            C08PacketPlayerBlockPlacement(
                                BlockPos(-1, -1, -1), 255, thePlayer.heldItem, 0f, 0f, 0f
                            )
                        )

                        else -> {}
                    }
                }

                "aac5" -> {
                    if (event.eventState == EventState.POST) {
                        sendPacket(
                            C08PacketPlayerBlockPlacement(
                                BlockPos(-1, -1, -1), 255, thePlayer.heldItem, 0f, 0f, 0f
                            )
                        )
                    }
                }

                "switchitem" -> {
                    when (event.eventState) {
                        EventState.PRE -> sendPackets(
                            C09PacketHeldItemChange(thePlayer.inventory.currentItem % 8 + 1),
                            C09PacketHeldItemChange(thePlayer.inventory.currentItem)
                        )
                        else -> {}
                    }
                }
            }
        }
    }

    @EventTarget
    fun onSlowDown(event: SlowDownEvent) {
        val heldItem = mc.thePlayer.heldItem?.item

        event.forward = getMultiplier(heldItem, true)
        event.strafe = getMultiplier(heldItem, false)
    }

    private fun getMultiplier(item: Item?, isForward: Boolean) = when (item) {
        is ItemFood, is ItemPotion, is ItemBucketMilk -> if (isForward) consumeForwardMultiplier else consumeStrafeMultiplier

        is ItemSword -> if (isForward) blockForwardMultiplier else blockStrafeMultiplier

        is ItemBow -> if (isForward) bowForwardMultiplier else bowStrafeMultiplier

        else -> 0.2F
    }
}
