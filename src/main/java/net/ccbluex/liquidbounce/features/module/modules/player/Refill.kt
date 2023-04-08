package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.injection.implementations.IMixinItemStack
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.client.C0EPacketClickWindow
import net.minecraft.network.play.client.C16PacketClientStatus
import net.minecraft.network.play.server.S2EPacketCloseWindow

@ModuleInfo(name = "Refill", description = "Refills items such as blocks and food from inventory to hotbar.", category = ModuleCategory.PLAYER)
object Refill : Module() {
    private val delayValue = IntegerValue("Delay", 400, 10, 1000)

    private val itemDelayValue = IntegerValue("ItemDelay", 400, 0, 1000)

    private val modeValue = ListValue("Mode", arrayOf("Swap", "Merge"), "Swap")

    private val invOpenValue = BoolValue("InvOpen", false)
    private val simulateInventoryValue = object : BoolValue("SimulateInventory", false) {
        override fun isSupported() = !invOpenValue.get()
    }

    private val noMoveValue = BoolValue("NoMoveClicks", false)
    private val noMoveAirValue = object : BoolValue("NoClicksInAir", false) {
        override fun isSupported() = noMoveValue.get()
    }
    private val noMoveGroundValue = object : BoolValue("NoClicksOnGround", true) {
        override fun isSupported() = noMoveValue.get()
    }

    private val timer = MSTimer()

    private var openInv = false

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (!timer.hasTimePassed(delayValue.get()))
            return

        if (invOpenValue.get() && mc.currentScreen !is GuiInventory)
            return

        if (noMoveValue.get() && isMoving && if (mc.thePlayer.onGround) noMoveGroundValue.get() else noMoveAirValue.get())
            return

        for (slot in 36..44) {
            val stack = mc.thePlayer.inventoryContainer.getSlot(slot).stack ?: continue
            if (stack.stackSize == stack.maxStackSize
                || (System.currentTimeMillis() - (stack as IMixinItemStack).itemDelay) < itemDelayValue.get()) continue

            when (modeValue.get()) {
                "Swap" -> {
                    val bestOption = mc.thePlayer.inventoryContainer.inventory.withIndex()
                        .filter { (index, searchStack) ->
                            index < 36 && searchStack != null && searchStack.stackSize > stack.stackSize
                                    && (ItemStack.areItemsEqual(stack, searchStack)
                                    || searchStack.item.javaClass.isAssignableFrom(stack.item.javaClass)
                                    || stack.item.javaClass.isAssignableFrom(searchStack.item.javaClass))
                        }.maxByOrNull { it.value.stackSize }

                    if (bestOption != null) {
                        val (index, betterStack) = bestOption

                        click(index, slot - 36, 2, betterStack)
                        break
                    }
                }

                "Merge" -> {
                    val bestOption = mc.thePlayer.inventoryContainer.inventory.withIndex()
                        .filter { (index, searchStack) ->
                            index < 36 && searchStack != null && ItemStack.areItemsEqual(stack, searchStack)
                        }.minByOrNull { it.value.stackSize }

                    if (bestOption != null) {
                        val (otherSlot, otherStack) = bestOption

                        click(otherSlot, 0, 0, otherStack)
                        click(slot, 0, 0, stack)

                        // Return items that couldn't fit into hotbar slot
                        if (stack.stackSize + otherStack.stackSize > stack.maxStackSize)
                            click(otherSlot, 0, 0, otherStack)

                        break
                    }
                }
            }
        }

        if (simulateInventoryValue.get() && openInv && mc.currentScreen !is GuiInventory)
            mc.netHandler.addToSendQueue(C0DPacketCloseWindow(mc.thePlayer.openContainer.windowId))
    }

    @EventTarget(ignoreCondition = true)
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (event.isCancelled) return

        when (packet) {
            is C16PacketClientStatus ->
                if (packet.status == C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT)
                    openInv = true
            is C0DPacketCloseWindow, is S2EPacketCloseWindow -> openInv = false
        }
    }

    fun click(slot: Int, button: Int, mode: Int, stack: ItemStack) {
        if (simulateInventoryValue.get() && !openInv)
            mc.netHandler.addToSendQueue(C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT))

        mc.netHandler.addToSendQueue(
            C0EPacketClickWindow(mc.thePlayer.openContainer.windowId, slot, button, mode, stack,
                mc.thePlayer.openContainer.getNextTransactionID(mc.thePlayer.inventory))
        )

        timer.reset()
    }
}