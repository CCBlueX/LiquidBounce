package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.InventoryUtils.serverOpenInventory
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.item.hasItemDelayPassed
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.client.C0EPacketClickWindow
import net.minecraft.network.play.client.C16PacketClientStatus
import net.minecraft.network.play.client.C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT

object Refill : Module("Refill", ModuleCategory.PLAYER) {
    private val delay by IntegerValue("Delay", 400, 10..1000)

    private val itemDelay by IntegerValue("ItemDelay", 400, 0..1000)

    private val mode by ListValue("Mode", arrayOf("Swap", "Merge"), "Swap")

    private val invOpen by BoolValue("InvOpen", false)
    private val simulateInventory by BoolValue("SimulateInventory", false) { !invOpen }

    private val noMove by BoolValue("NoMoveClicks", false)
    private val noMoveAir by BoolValue("NoClicksInAir", false) { noMove }
    private val noMoveGround by BoolValue("NoClicksOnGround", true) { noMove }

    private val timer = MSTimer()

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (!timer.hasTimePassed(delay))
            return

        if (invOpen && mc.currentScreen !is GuiInventory)
            return

        if (noMove && isMoving && if (mc.thePlayer.onGround) noMoveGround else noMoveAir)
            return

        for (slot in 36..44) {
            val stack = mc.thePlayer.inventoryContainer.getSlot(slot).stack ?: continue
            if (stack.stackSize == stack.maxStackSize || !stack.hasItemDelayPassed(itemDelay)) continue

            when (mode) {
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

        if (simulateInventory && serverOpenInventory && mc.currentScreen !is GuiInventory)
            sendPacket(C0DPacketCloseWindow(mc.thePlayer.openContainer.windowId))
    }

    fun click(slot: Int, button: Int, mode: Int, stack: ItemStack) {
        if (simulateInventory && !serverOpenInventory)
            sendPacket(C16PacketClientStatus(OPEN_INVENTORY_ACHIEVEMENT))

        sendPacket(
            C0EPacketClickWindow(mc.thePlayer.openContainer.windowId, slot, button, mode, stack,
                mc.thePlayer.openContainer.getNextTransactionID(mc.thePlayer.inventory))
        )

        timer.reset()
    }
}