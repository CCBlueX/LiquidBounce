/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.combat.AutoArmor
import net.ccbluex.liquidbounce.injection.implementations.IMixinItemStack
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.item.ArmorPiece
import net.ccbluex.liquidbounce.utils.item.ItemUtils
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.BlockBush
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.enchantment.Enchantment
import net.minecraft.init.Blocks
import net.minecraft.item.*
import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.client.C16PacketClientStatus


@ModuleInfo(
    name = "InventoryCleaner",
    description = "Automatically throws away useless items.",
    category = ModuleCategory.PLAYER
)
class InventoryCleaner : Module() {

    /**
     * OPTIONS
     */

    private val maxDelayValue: IntegerValue = object : IntegerValue("MaxDelay", 600, 0, 1000) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val minCPS = minDelayValue.get()
            if (minCPS > newValue) set(minCPS)
        }
    }

    private val minDelayValue: IntegerValue = object : IntegerValue("MinDelay", 400, 0, 1000) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val maxDelay = maxDelayValue.get()
            if (maxDelay < newValue) set(maxDelay)
        }
    }

    private val invOpenValue = BoolValue("InvOpen", false)
    private val simulateInventory = BoolValue("SimulateInventory", true)
    private val noMoveValue = BoolValue("NoMove", false)
    private val ignoreVehiclesValue = BoolValue("IgnoreVehicles", false)
    private val hotbarValue = BoolValue("Hotbar", true)
    private val randomSlotValue = BoolValue("RandomSlot", false)
    private val sortValue = BoolValue("Sort", true)
    private val itemDelayValue = IntegerValue("ItemDelay", 0, 0, 5000)

    private val items =
        arrayOf("None", "Ignore", "Sword", "Bow", "Pickaxe", "Axe", "Food", "Block", "Water", "Gapple", "Pearl")
    private val sortSlot1Value = ListValue("SortSlot-1", items, "Sword")
    private val sortSlot2Value = ListValue("SortSlot-2", items, "Bow")
    private val sortSlot3Value = ListValue("SortSlot-3", items, "Pickaxe")
    private val sortSlot4Value = ListValue("SortSlot-4", items, "Axe")
    private val sortSlot5Value = ListValue("SortSlot-5", items, "None")
    private val sortSlot6Value = ListValue("SortSlot-6", items, "None")
    private val sortSlot7Value = ListValue("SortSlot-7", items, "Food")
    private val sortSlot8Value = ListValue("SortSlot-8", items, "Block")
    private val sortSlot9Value = ListValue("SortSlot-9", items, "Block")

    /**
     * VALUES
     */

    private var delay = 0L

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.thePlayer ?: return

        if (!InventoryUtils.CLICK_TIMER.hasTimePassed(delay) || mc.currentScreen !is GuiInventory && invOpenValue.get() || noMoveValue.get() && MovementUtils.isMoving || thePlayer.openContainer != null && thePlayer.openContainer.windowId != 0 || (LiquidBounce.moduleManager[AutoArmor::class.java] as AutoArmor).isLocked) {
            return
        }

        if (sortValue.get()) {
            sortHotbar()
        }

        while (InventoryUtils.CLICK_TIMER.hasTimePassed(delay)) {
            val garbageItems =
                items(9, if (hotbarValue.get()) 45 else 36).filter { !isUseful(it.value, it.key) }.keys.toMutableList()

            // Shuffle items
            if (randomSlotValue.get()) {
                garbageItems.shuffle()
            }

            val garbageItem = garbageItems.firstOrNull() ?: break

            // Drop all useless items
            val openInventory = mc.currentScreen !is GuiInventory && simulateInventory.get()

            if (openInventory) {
                mc.netHandler.addToSendQueue(C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT))
            }

            mc.playerController.windowClick(thePlayer.openContainer.windowId, garbageItem, 1, 4, thePlayer)

            if (openInventory) {
                mc.netHandler.addToSendQueue(C0DPacketCloseWindow())
            }

            delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())
        }
    }

    /**
     * Checks if the item is useful
     *
     * @param slot Slot id of the item. If the item isn't in the inventory -1
     * @return Returns true when the item is useful
     */
    fun isUseful(itemStack: ItemStack, slot: Int): Boolean {
        return try {
            val item = itemStack.item

            if (item is ItemSword || item is ItemTool) {
                val thePlayer = mc.thePlayer ?: return true

                if (slot >= 36 && findBetterItem(
                        slot - 36, thePlayer.inventory.getStackInSlot(slot - 36)
                    ) == slot - 36
                ) return true

                for (i in 0..8) {
                    if (type(i).equals("sword", true) && item is ItemSword || type(i).equals(
                            "pickaxe", true
                        ) && item is ItemPickaxe || type(i).equals(
                            "axe", true
                        ) && item is ItemAxe
                    ) {
                        if (findBetterItem(i, thePlayer.inventory.getStackInSlot(i)) == null) {
                            return true
                        }
                    }
                }

                val damage = (itemStack.attributeModifiers["generic.attackDamage"].firstOrNull()?.amount
                    ?: 0.0) + 1.25 * ItemUtils.getEnchantment(
                    itemStack, Enchantment.sharpness
                )

                items(0, 45).none { (_, stack) ->
                    stack != itemStack && stack.javaClass == itemStack.javaClass && damage < (stack.attributeModifiers["generic.attackDamage"].firstOrNull()?.amount
                        ?: 0.0) + 1.25 * ItemUtils.getEnchantment(stack, Enchantment.sharpness)
                }
            } else if (item is ItemBow) {
                val currPower = ItemUtils.getEnchantment(itemStack, Enchantment.power)

                items().none { (_, stack) ->
                    itemStack != stack && stack.item is ItemBow && currPower < ItemUtils.getEnchantment(
                        stack, Enchantment.power
                    )
                }
            } else if (item is ItemArmor) {
                val currArmor = ArmorPiece(itemStack, slot)

                items().none { (slot, stack) ->
                    if (stack != itemStack && stack.item is ItemArmor) {
                        val armor = ArmorPiece(stack, slot)

                        if (armor.armorType != currArmor.armorType) false
                        else AutoArmor.ARMOR_COMPARATOR.compare(currArmor, armor) <= 0
                    } else false
                }
            } else if (itemStack.unlocalizedName == "item.compass") {
                items(0, 45).none { (_, stack) -> itemStack != stack && stack.unlocalizedName == "item.compass" }
            } else item is ItemFood || itemStack.unlocalizedName == "item.arrow" || item is ItemBlock && item.block !is BlockBush || item is ItemBed || itemStack.unlocalizedName == "item.diamond" || itemStack.unlocalizedName == "item.ingotIron" || item is ItemPotion || item is ItemEnderPearl || item is ItemEnchantedBook || item is ItemBucket || itemStack.unlocalizedName == "item.stick" || ignoreVehiclesValue.get() && (item is ItemBoat || item is ItemMinecart)
        } catch (ex: Exception) {
            ClientUtils.getLogger().error("(InventoryCleaner) Failed to check item: ${itemStack.unlocalizedName}.", ex)

            true
        }
    }

    /**
     * INVENTORY SORTER
     */

    /**
     * Sort hotbar
     */
    private fun sortHotbar() {
        for (index in 0..8) {
            val thePlayer = mc.thePlayer ?: return

            val bestItem = findBetterItem(index, thePlayer.inventory.getStackInSlot(index)) ?: continue

            if (bestItem != index) {
                val openInventory = mc.currentScreen !is GuiInventory && simulateInventory.get()

                if (openInventory) mc.netHandler.addToSendQueue(C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT))

                mc.playerController.windowClick(
                    0, if (bestItem < 9) bestItem + 36 else bestItem, index, 2, thePlayer
                )

                if (openInventory) mc.netHandler.addToSendQueue(C0DPacketCloseWindow())

                delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())
                break
            }
        }
    }

    private fun findBetterItem(targetSlot: Int, slotStack: ItemStack?): Int? {
        val type = type(targetSlot)

        val thePlayer = mc.thePlayer ?: return null

        when (type.lowercase()) {
            "sword", "pickaxe", "axe" -> {
                val currentTypeChecker: ((Item?) -> Boolean) = when {
                    type.equals("Sword", ignoreCase = true) -> { item: Item? -> item is ItemSword }
                    type.equals("Pickaxe", ignoreCase = true) -> { obj: Item? -> obj is ItemPickaxe }
                    type.equals("Axe", ignoreCase = true) -> { obj: Item? -> obj is ItemAxe }
                    else -> return null
                }

                var bestWeapon = if (currentTypeChecker(slotStack?.item)) targetSlot else -1

                thePlayer.inventory.mainInventory.forEachIndexed { index, itemStack ->
                    if (itemStack != null && currentTypeChecker(itemStack.item) && !type(index).equals(
                            type, ignoreCase = true
                        )
                    ) {
                        if (bestWeapon == -1) {
                            bestWeapon = index
                        } else {
                            val currDamage = (itemStack.attributeModifiers["generic.attackDamage"].firstOrNull()?.amount
                                ?: 0.0) + 1.25 * ItemUtils.getEnchantment(itemStack, Enchantment.sharpness)

                            val bestStack = thePlayer.inventory.getStackInSlot(bestWeapon) ?: return@forEachIndexed
                            val bestDamage = (bestStack.attributeModifiers["generic.attackDamage"].firstOrNull()?.amount
                                ?: 0.0) + 1.25 * ItemUtils.getEnchantment(bestStack, Enchantment.sharpness)

                            if (bestDamage < currDamage) bestWeapon = index
                        }
                    }
                }

                return if (bestWeapon != -1 || bestWeapon == targetSlot) bestWeapon else null
            }

            "bow" -> {
                var bestBow = if (slotStack?.item is ItemBow) targetSlot else -1
                var bestPower = if (bestBow != -1) ItemUtils.getEnchantment(slotStack, Enchantment.power) else 0

                thePlayer.inventory.mainInventory.forEachIndexed { index, itemStack ->
                    if (itemStack?.item is ItemBow && !type(index).equals(type, ignoreCase = true)) {
                        if (bestBow == -1) {
                            bestBow = index
                        } else {
                            val power = ItemUtils.getEnchantment(itemStack, Enchantment.power)

                            if (ItemUtils.getEnchantment(itemStack, Enchantment.power) > bestPower) {
                                bestBow = index
                                bestPower = power
                            }
                        }
                    }
                }

                return if (bestBow != -1) bestBow else null
            }

            "food" -> {
                thePlayer.inventory.mainInventory.forEachIndexed { index, stack ->
                    if (stack != null) {
                        val item = stack.item

                        if (item is ItemFood && item !is ItemAppleGold && !type(index).equals(
                                "Food", ignoreCase = true
                            )
                        ) {
                            val replaceCurr = ItemUtils.isStackEmpty(slotStack) || slotStack?.item !is ItemFood

                            return if (replaceCurr) index else null
                        }
                    }
                }
            }

            "block" -> {
                thePlayer.inventory.mainInventory.forEachIndexed { index, stack ->
                    if (stack != null) {
                        val item = stack.item

                        if (item is ItemBlock && !InventoryUtils.BLOCK_BLACKLIST.contains(item.block) && !type(index).equals(
                                "Block", ignoreCase = true
                            )
                        ) {
                            val replaceCurr = ItemUtils.isStackEmpty(slotStack) || slotStack?.item !is ItemBlock

                            return if (replaceCurr) index else null
                        }
                    }
                }
            }

            "water" -> {
                thePlayer.inventory.mainInventory.forEachIndexed { index, stack ->
                    if (stack != null) {
                        val item = stack.item

                        if (item is ItemBucket && item.isFull == Blocks.flowing_water && !type(index).equals(
                                "Water", ignoreCase = true
                            )
                        ) {
                            val replaceCurr =
                                ItemUtils.isStackEmpty(slotStack) || slotStack?.item !is ItemBucket || (slotStack.item as ItemBucket).isFull != Blocks.flowing_water

                            return if (replaceCurr) index else null
                        }
                    }
                }
            }

            "gapple" -> {
                thePlayer.inventory.mainInventory.forEachIndexed { index, stack ->
                    if (stack != null) {
                        val item = stack.item

                        if (item is ItemAppleGold && !type(index).equals("Gapple", ignoreCase = true)) {
                            val replaceCurr = ItemUtils.isStackEmpty(slotStack) || slotStack?.item !is ItemAppleGold

                            return if (replaceCurr) index else null
                        }
                    }
                }
            }

            "pearl" -> {
                thePlayer.inventory.mainInventory.forEachIndexed { index, stack ->
                    if (stack != null) {
                        val item = stack.item

                        if (item is ItemEnderPearl && !type(index).equals("Pearl", ignoreCase = true)) {
                            val replaceCurr = ItemUtils.isStackEmpty(slotStack) || slotStack?.item !is ItemEnderPearl

                            return if (replaceCurr) index else null
                        }
                    }
                }
            }
        }

        return null
    }

    /**
     * Get items in inventory
     */
    private fun items(start: Int = 0, end: Int = 45): Map<Int, ItemStack> {
        val items = mutableMapOf<Int, ItemStack>()

        for (i in end - 1 downTo start) {
            val itemStack = mc.thePlayer.inventoryContainer.getSlot(i).stack ?: continue

            if (ItemUtils.isStackEmpty(itemStack)) {
                continue
            }

            if (type(i).equals("Ignore", ignoreCase = true)) {
                continue
            }

            if (System.currentTimeMillis() - (itemStack as IMixinItemStack).itemDelay >= itemDelayValue.get()) {
                items[i] = itemStack
            }
        }

        return items
    }

    /**
     * Get type of [targetSlot]
     */
    private fun type(targetSlot: Int) = when (targetSlot) {
        0 -> sortSlot1Value.get()
        1 -> sortSlot2Value.get()
        2 -> sortSlot3Value.get()
        3 -> sortSlot4Value.get()
        4 -> sortSlot5Value.get()
        5 -> sortSlot6Value.get()
        6 -> sortSlot7Value.get()
        7 -> sortSlot8Value.get()
        8 -> sortSlot9Value.get()
        else -> ""
    }
}
