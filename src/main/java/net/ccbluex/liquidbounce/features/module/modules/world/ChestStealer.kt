/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.InventoryCleaner
import net.ccbluex.liquidbounce.utils.item.ItemUtils
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextInt
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils.randomDelay
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.Slot
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.network.play.server.S30PacketWindowItems
import net.minecraft.util.ResourceLocation

object ChestStealer : Module() {

    /**
     * OPTIONS
     */

    private val maxDelayValue: IntegerValue = object : IntegerValue("MaxDelay", 200, 0..400) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minDelay)

        override fun onChanged(oldValue: Int, newValue: Int) {
            nextDelay = randomDelay(minDelay, newValue)
        }
    }
    private val maxDelay by maxDelayValue

    private val minDelay: Int by object : IntegerValue("MinDelay", 150, 0..400) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(maxDelay)

        override fun onChanged(oldValue: Int, newValue: Int) {
            nextDelay = randomDelay(newValue, maxDelay)
        }

        override fun isSupported() = !maxDelayValue.isMinimal()
    }
    private val delayOnFirst by BoolValue("DelayOnFirst", false)

    private val takeRandomized by BoolValue("TakeRandomized", false)
    private val onlyItems by BoolValue("OnlyItems", false)
    private val noCompass by BoolValue("NoCompass", false)
    private val autoClose by BoolValue("AutoClose", true)

    private val autoCloseMaxDelayValue: IntegerValue = object : IntegerValue("AutoCloseMaxDelay", 0, 0..400) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(autoCloseMinDelay)

        override fun onChanged(oldValue: Int, newValue: Int) {
            nextCloseDelay = randomDelay(autoCloseMinDelay, newValue)
        }

        override fun isSupported() = autoClose
    }
    private val autoCloseMaxDelay by autoCloseMaxDelayValue

    private val autoCloseMinDelay: Int by object : IntegerValue("AutoCloseMinDelay", 0, 0..400) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(autoCloseMaxDelay)

        override fun onChanged(oldValue: Int, newValue: Int) {
            nextCloseDelay = randomDelay(newValue, autoCloseMaxDelay)
        }

        override fun isSupported() = autoClose && !autoCloseMaxDelayValue.isMinimal()
    }

    private val closeOnFull by BoolValue("CloseOnFull", true)
    private val chestTitle by BoolValue("ChestTitle", false)

    /**
     * VALUES
     */

    private val delayTimer = MSTimer()
    private var nextDelay = randomDelay(minDelay, maxDelay)

    private val autoCloseTimer = MSTimer()
    private var nextCloseDelay = randomDelay(autoCloseMinDelay, autoCloseMaxDelay)

    private var contentReceived = 0

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        val thePlayer = mc.thePlayer

        val screen = mc.currentScreen ?: return

        if (screen !is GuiChest || mc.currentScreen == null) {
            if (delayOnFirst)
                delayTimer.reset()
            autoCloseTimer.reset()
            return
        }

        if (!delayTimer.hasTimePassed(nextDelay)) {
            autoCloseTimer.reset()
            return
        }



        // No Compass
        if (noCompass && thePlayer.inventory.getCurrentItem()?.item?.unlocalizedName == "item.compass")
            return

        // Chest title
        if (chestTitle && (screen.lowerChestInventory == null ||
            ItemStack(Item.itemRegistry.getObject(ResourceLocation("minecraft:chest"))).displayName !in screen.lowerChestInventory.name))
            return

        // Is empty?
        if (!isEmpty(screen) && (!closeOnFull || !fullInventory)) {
            autoCloseTimer.reset()

            // Randomized
            if (takeRandomized) {
                do {
                    val items = mutableListOf<Slot>()

                    for (slotIndex in 0 until screen.inventoryRows * 9) {
                        val slot = screen.inventorySlots.getSlot(slotIndex)

                        val stack = slot.stack

                        if (stack != null && (!onlyItems || stack.item !is ItemBlock) && (!InventoryCleaner.state || InventoryCleaner.isUseful(stack, -1)))
                            items.add(slot)
                    }

                    val randomSlot = nextInt(endExclusive = items.size)
                    val slot = items[randomSlot]

                    move(screen, slot)
                } while (delayTimer.hasTimePassed(nextDelay) && items.isNotEmpty())
                return
            }

            // Non randomized
            for (slotIndex in 0 until screen.inventoryRows * 9) {
                val slot = screen.inventorySlots.getSlot(slotIndex)

                val stack = slot.stack

                if (delayTimer.hasTimePassed(nextDelay) && shouldTake(stack)) {
                    move(screen, slot)
                }
            }
        } else if (autoClose && screen.inventorySlots.windowId == contentReceived && autoCloseTimer.hasTimePassed(nextCloseDelay)) {
            thePlayer.closeScreen()
            nextCloseDelay = randomDelay(autoCloseMinDelay, autoCloseMaxDelay)
        }
    }

    @EventTarget
    private fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (packet is S30PacketWindowItems) {
            contentReceived = packet.func_148911_c()
        }
    }

    private fun shouldTake(stack: ItemStack?): Boolean {
        return stack != null && !ItemUtils.isStackEmpty(stack) && (!onlyItems || stack.item !is ItemBlock)
                && (!InventoryCleaner.state || InventoryCleaner.isUseful(stack, -1))
    }

    private fun move(screen: GuiChest, slot: Slot) {
        screen.handleMouseClick(slot, slot.slotNumber, 0, 1)
        delayTimer.reset()
        nextDelay = randomDelay(minDelay, maxDelay)
    }

    private fun isEmpty(chest: GuiChest): Boolean {
        for (i in 0 until chest.inventoryRows * 9) {
            val slot = chest.inventorySlots.getSlot(i)

            val stack = slot.stack

            if (shouldTake(stack))
                return false
        }

        return true
    }

    private val fullInventory
        get() = mc.thePlayer?.inventory?.mainInventory?.none(ItemUtils::isStackEmpty) ?: false
}