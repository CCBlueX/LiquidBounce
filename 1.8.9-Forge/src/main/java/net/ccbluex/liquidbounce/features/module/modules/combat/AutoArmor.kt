package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.injection.implementations.IItemStack
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.item.Armor.getArmorArray
import net.ccbluex.liquidbounce.utils.item.Armor.getArmorSlot
import net.ccbluex.liquidbounce.utils.item.ItemUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.enchantment.Enchantment
import net.minecraft.item.Item
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.client.C16PacketClientStatus

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @author CCBlueX
 * @game Minecraft
 */
@ModuleInfo(name = "AutoArmor", description = "Automatically equips the best armor in your inventory.", category = ModuleCategory.COMBAT)
class AutoArmor : Module() {
    private val minDelayValue: IntegerValue = object : IntegerValue("MinDelay", 100, 0, 400) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val maxDelay = maxDelayValue.get()
            if (maxDelay < newValue) set(maxDelay)
        }
    }

    private val maxDelayValue: IntegerValue = object : IntegerValue("MaxDelay", 200, 0, 400) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val minDelay = minDelayValue.get()
            if (minDelay > newValue) set(minDelay)
        }
    }

    private val invOpenValue = BoolValue("InvOpen", false)
    private val simulateInventory = BoolValue("SimulateInventory", true)
    private val noMoveValue = BoolValue("NoMove", false)
    private val itemDelayValue = IntegerValue("ItemDelay", 0, 0, 5000)
    private val hotbarValue = BoolValue("Hotbar", true)
    private val msTimer = MSTimer()
    private var delay: Long = 0

    @EventTarget
    fun onRender3D(event: Render3DEvent?) {
        if (!msTimer.hasTimePassed(delay) || mc.thePlayer.openContainer != null && mc.thePlayer.openContainer.windowId != 0) return
        var item = -1
        var hotbarItem = -1

        for (slot in 0..3) {
            val idArray = getArmorArray(slot)

            if (mc.thePlayer.inventory.armorInventory[slot] == null) {
                item = getArmor(idArray, 9, 45, false)
                hotbarItem = getArmor(idArray, 36, 45, true)
            } else if (hasBetter(slot, idArray))
                item = getArmorSlot(slot)

            if (item != -1) break
        }
        if (!(noMoveValue.get() && MovementUtils.isMoving()) && (!invOpenValue.get() || mc.currentScreen is GuiInventory) && item != -1) {
            val openInventory = simulateInventory.get() && mc.currentScreen !is GuiInventory

            if (openInventory)
                mc.netHandler.addToSendQueue(C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT))

            mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, item, 0, 1, mc.thePlayer)
            msTimer.reset()

            delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())

            if (openInventory)
                mc.netHandler.addToSendQueue(C0DPacketCloseWindow())
        } else if (hotbarValue.get() && hotbarItem != -1) {
            mc.netHandler.addToSendQueue(C09PacketHeldItemChange(hotbarItem - 36))
            mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.inventoryContainer.getSlot(hotbarItem).stack))
            mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))

            msTimer.reset()
            delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())
        }
    }

    private fun hasBetter(slot: Int, type: IntArray?): Boolean {
        var armorIndex = -1
        var inventoryIndex = -1
        var inventorySlot = -1

        for (i in type!!.indices) {
            if (mc.thePlayer.inventory.armorInventory[slot] != null && Item.getIdFromItem(mc.thePlayer.inventory.armorInventory[slot].item) == type[i]) {
                armorIndex = i
                break
            }
        }
        for (i in type.indices) {
            if (getArmorItem(type[i], 9, 45, false).also { inventorySlot = it } != -1) {
                inventoryIndex = i
                break
            }
        }

        return if (inventoryIndex <= -1) false else inventoryIndex < armorIndex || inventoryIndex == armorIndex && ItemUtils.getEnchantment(mc.thePlayer.inventory.armorInventory[slot], Enchantment.protection) < ItemUtils.getEnchantment(mc.thePlayer.inventoryContainer.getSlot(inventorySlot).stack, Enchantment.protection)
    }

    private fun getArmor(ids: IntArray?, startSlot: Int, endSlot: Int, hotbar: Boolean): Int {
        for (id in ids!!) {
            val i = getArmorItem(id, startSlot, endSlot, hotbar)
            if (i != -1) return i
        }

        return -1
    }

    private fun getArmorItem(id: Int, startSlot: Int, endSlot: Int, hotbar: Boolean): Int {
        var bestSlot = -1

        for (index in startSlot until endSlot) {
            val itemStack = mc.thePlayer.inventoryContainer.getSlot(index).stack

            if (itemStack == null || !hotbar && System.currentTimeMillis() - (itemStack as Any as IItemStack).itemDelay < itemDelayValue.get()) continue

            if (Item.getIdFromItem(itemStack.item) == id && (bestSlot == -1 || ItemUtils.getEnchantment(itemStack, Enchantment.protection) >= ItemUtils.getEnchantment(mc.thePlayer.inventoryContainer.getSlot(bestSlot).stack, Enchantment.protection))) bestSlot = index
        }

        return bestSlot
    }
}