package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.api.enums.WEnumHand
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.createOpenInventoryPacket
import net.ccbluex.liquidbounce.utils.createUseItemPacket
import net.ccbluex.liquidbounce.utils.item.ArmorComparator
import net.ccbluex.liquidbounce.utils.item.ArmorPiece
import net.ccbluex.liquidbounce.utils.item.ItemUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import java.util.stream.Collectors
import java.util.stream.IntStream

/*
* LiquidBounce Hacked Client
* A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
* https://github.com/CCBlueX/LiquidBounce/
*/
@ModuleInfo(name = "AutoArmor", description = "Automatically equips the best armor in your inventory.", category = ModuleCategory.PLAYER)
class AutoArmor : Module() {

    private val maxDelayValue: IntegerValue = object : IntegerValue("MaxDelay", 200, 0, 400) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val minDelay = minDelayValue.get()
            if (minDelay > newValue) set(minDelay)
        }
    }
    private val minDelayValue: IntegerValue = object : IntegerValue("MinDelay", 100, 0, 400) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val maxDelay = maxDelayValue.get()
            if (maxDelay < newValue) set(maxDelay)
        }
    }
    private val startDelayValue = IntegerValue("StartDelay", 0, 0, 5000)
    private val itemDelayValue = IntegerValue("ItemDelay", 0, 0, 5000)

    private val invOpenValue = BoolValue("InvOpen", false)
    private val simulateInventory = BoolValue("SimulateInventory", true)
    private val noMoveValue = BoolValue("NoMove", false)
    private val hotbarValue = BoolValue("Hotbar", true)
    private val dropOldValue = BoolValue("DropOld", true)

    private val START_TIMER = MSTimer()
    private var delay: Long = 0
    var DONE = false
    val ARMOR_COMPARATOR = ArmorComparator()


    @EventTarget
    fun onUpdate(event: UpdateEvent?) {
        if (mc.thePlayer == null || mc.theWorld == null || mc.thePlayer!!.openContainer != null && mc.thePlayer!!.openContainer!!.windowId != 0) return


        //Reset start timer
        if (noMoveValue.get() && isMoving || !classProvider.isGuiInventory(mc.currentScreen) && invOpenValue.get()) START_TIMER.reset()

        //Check if time has passed
        if (!START_TIMER.hasTimePassed(startDelayValue.get().toLong()) || !InventoryUtils.CLICK_TIMER.hasTimePassed(delay)) return


        // Find best armor
        val armorPieces = IntStream.range(0, 36)
                .filter { i: Int ->
                    val itemStack = mc.thePlayer!!.inventory.getStackInSlot(i)
                    (itemStack != null && classProvider.isItemArmor(itemStack.item)
                            && (i < 9 || System.currentTimeMillis() - itemStack.itemDelay >= itemDelayValue.get()))
                }
                .mapToObj { i: Int -> ArmorPiece(mc.thePlayer!!.inventory.getStackInSlot(i), i) }
                .collect(Collectors.groupingBy { obj: ArmorPiece -> obj.armorType })
        val bestArmor = arrayOfNulls<ArmorPiece>(4)
        for ((key, value) in armorPieces) {
            bestArmor[key!!] = value.stream()
                    .max(ARMOR_COMPARATOR).orElse(null)
        }

        // Swap armor
        for (i in 0..3) {
            val armorPiece = bestArmor[i] ?: continue
            val armorSlot = 3 - i
            val oldArmor = ArmorPiece(mc.thePlayer!!.inventory.armorItemInSlot(armorSlot), -1)
            if (ItemUtils.isStackEmpty(oldArmor.itemStack) || !classProvider.isItemArmor(oldArmor.itemStack.item) || ARMOR_COMPARATOR.compare(oldArmor, armorPiece) < 0) {
                if (!ItemUtils.isStackEmpty(oldArmor.itemStack) && move(8 - armorSlot, true)) {
                    DONE = false
                    return
                }
                if (ItemUtils.isStackEmpty(mc.thePlayer!!.inventory.armorItemInSlot(armorSlot)) && move(armorPiece.slot, false)) {
                    DONE = false
                    return
                }
            }
        }
        DONE = true
    }

    val isDone: Boolean
        get() = DONE || !state

    /**
     * Shift+Left clicks the specified item
     *
     * @param item        Slot of the item to click
     * @param isArmorSlot
     * @return True if it is unable to move the item
     */
    private fun move(item: Int, isArmorSlot: Boolean): Boolean {
        if (!isArmorSlot && item < 9 && hotbarValue.get()) {
            mc.netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(item))
            mc.netHandler.addToSendQueue(createUseItemPacket(mc.thePlayer!!.inventoryContainer.getSlot(item).stack, WEnumHand.MAIN_HAND))
            mc.netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(mc.thePlayer!!.inventory.currentItem))
            delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())
            return true
        } else if (item != -1) {
            val openInventory = simulateInventory.get() && !classProvider.isGuiInventory(mc.currentScreen)
            if (openInventory) mc.netHandler.addToSendQueue(createOpenInventoryPacket())
            mc.playerController.windowClick(mc.thePlayer!!.inventoryContainer.windowId, if (isArmorSlot) item else if (item < 9) item + 36 else item, 0, if (isArmorSlot && dropOldValue.get()) 4 else 1, mc.thePlayer!!)
            delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())
            if (openInventory) mc.netHandler.addToSendQueue(classProvider.createCPacketCloseWindow())
            return true
        }
        return false
    }
}