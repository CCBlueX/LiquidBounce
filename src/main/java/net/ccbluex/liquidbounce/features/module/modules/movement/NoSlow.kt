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
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.item.*
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action.RELEASE_USE_ITEM
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing

object NoSlow : Module("NoSlow", ModuleCategory.MOVEMENT) {

    // Highly customizable values

    val packetMode by ListValue("Packet-Mode", arrayOf("None", "NCP", "AAC5", "Spoof", "OldIntave"), "None")
    
    private val blockForwardMultiplier by FloatValue("BlockForwardMultiplier", 1f, 0.2F..1f)
    private val blockStrafeMultiplier by FloatValue("BlockStrafeMultiplier", 1f, 0.2F..1f)

    val consumePacket by ListValue("ConsumePacket", arrayOf("None", "AAC5", "SpoofNoSwitch", "SpamPlace", "SpamEmptyPlace", "Spoof", "Glitch"), "None")
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
        
        if (heldItem.item is ItemFood || heldItem.item is ItemPotion || heldItem.item is ItemBucketMilk && thePlayer.isUsingItem()) {

            when (consumePacket.lowercase()) {
                "none" -> {
                    null
                }

                "glitch" -> {
                    mc.netHandler.addToSendQueue(C09PacketHeldItemChange((mc.thePlayer.inventory.currentItem + 1) % 9))
                    mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
                }

                "aac5" -> {
                    mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, mc.thePlayer.inventory.getCurrentItem(), 0f, 0f, 0f))
                }

                "spoofnoswitch" -> {
                    mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
                }

                "spamplace" -> {
                    mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getCurrentItem()))
                }

                "spamemptyplace" -> {
                    mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement())
                }

                "spoof" -> {
                    sendPacket(C09PacketHeldItemChange(thePlayer.inventory.currentItem  % 8 + 1))
                    sendPacket(C09PacketHeldItemChange(thePlayer.inventory.currentItem))
                    
                }

                else -> {
                    null
                }

            }
        }
            
        if (heldItem.item is ItemSword && thePlayer.isBlocking()) {

            when (packetMode.lowercase()) {
                "none" -> {
                    null
                }

                "ncp" -> {
                    when (event.eventState) {
                        EventState.PRE ->
                            sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos(0, 0, 0), EnumFacing.DOWN))
                        EventState.POST ->
                            sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, thePlayer.heldItem, 0f, 0f, 0f))
                    }
                }
                
                "aac5" -> {
                    if (event.eventState == EventState.POST) {
                        mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, thePlayer.inventory.getCurrentItem(), 0f, 0f, 0f))
                    }
                }

                "spoof" -> {
                    sendPacket(C09PacketHeldItemChange(thePlayer.inventory.currentItem  % 8 + 1))
                    sendPacket(C09PacketHeldItemChange(thePlayer.inventory.currentItem))
                    
                }

                "oldintave" -> {
                    if(mc.thePlayer.isUsingItem){
                        if (event.eventState == EventState.PRE){
                            mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem % 8 + 1))
                            mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
                        }
                        if(event.eventState == EventState.POST){
                                mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.inventoryContainer.getSlot(mc.thePlayer.inventory.currentItem + 36).stack))
                        }
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
