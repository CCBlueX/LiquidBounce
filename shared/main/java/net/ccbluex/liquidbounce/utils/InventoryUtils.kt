/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayerDigging
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos.Companion.ORIGIN
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.timer.MSTimer

class InventoryUtils : MinecraftInstance(), Listenable
{
    @EventTarget
    fun onClick(@Suppress("UNUSED_PARAMETER") event: ClickWindowEvent)
    {
        CLICK_TIMER.reset()
    }

    @EventTarget
    fun onTick(@Suppress("UNUSED_PARAMETER") event: TickEvent)
    {
        val thePlayer = mc.thePlayer ?: return

        if (targetSlot != null && slotKeepLength >= 0)
        {
            slotKeepLength--
            if (slotKeepLength <= 0) resetSlot(thePlayer)
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent)
    {
        val packet = event.packet

        val provider = classProvider

        if (provider.isCPacketPlayerDigging(packet))
        {
            val digging = packet.asCPacketPlayerDigging()

            if ((digging.status == ICPacketPlayerDigging.WAction.DROP_ITEM || digging.status == ICPacketPlayerDigging.WAction.DROP_ALL_ITEMS) && digging.position == ORIGIN && provider.getEnumFacing(EnumFacingType.DOWN) == digging.facing) CLICK_TIMER.reset() // Drop (all) item(s) in hotbar with Q (Ctrl+Q)
        }

        if (provider.isCPacketPlayerBlockPlacement(packet)) CLICK_TIMER.reset()

        if (provider.isCPacketHeldItemChange(packet) && targetSlot != null) event.cancelEvent()
    }

    override fun handleEvents(): Boolean = true

    companion object
    {
        // !! ---------------------------------------------------------------------------------------------------------------------------- !!
        // inventoryContainer.getSlot(i).stack is using different Slot ID system unlike inventory.getStackInSlot()
        // ID system can be found on
        // mc.thePlayer.inventoryContainer.getSlot(i).stack - https://wiki.vg/File:Inventory-slots.png
        // mc.thePlayer.inventory.getStackInSlot() (= mc.thePlayer.inventory.mainInventory) - https://minecraft.gamepedia.com/File:Items_slot_number.png
        // !! ---------------------------------------------------------------------------------------------------------------------------- !!

        @JvmField
        val CLICK_TIMER = MSTimer()

        @JvmField
        var targetSlot: Int? = null

        private var slotKeepLength = 0
        private var occupied = false

        fun tryHoldSlot(thePlayer: IEntityPlayerSP, slot: Int, keepLength: Int = 0, lock: Boolean = false): Boolean
        {
            if (occupied || slot !in 0..8) return false

            if (slot != (if (targetSlot == null) thePlayer.inventory.currentItem else targetSlot))
            {
                targetSlot = null

                mc.playerController.onStoppedUsingItem(thePlayer) // Stop using item before swap the slot
                mc.netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(slot))
            }

            targetSlot = slot
            slotKeepLength = keepLength
            occupied = lock

            return true
        }

        fun resetSlot(thePlayer: IEntityPlayer)
        {
            val slot = targetSlot ?: return
            slotKeepLength = 0
            occupied = false

            targetSlot = null

            val currentSlot = thePlayer.inventory.currentItem
            if (slot != currentSlot) mc.netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(currentSlot))
        }
    }
}
