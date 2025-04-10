package net.ccbluex.liquidbounce.utils.inventory

import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.item.isNothing
import net.minecraft.block.BlockState
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import kotlin.collections.filter
import kotlin.math.abs

fun <T : HotbarItemSlot> SlotGroup<T>.findClosestSlot(item: Item): T? =
    findClosestSlot { it.item === items }

fun <T : HotbarItemSlot> SlotGroup<T>.findClosestSlot(vararg items: Item): T? =
    findClosestSlot { it.item in items }

/**
 * Distance order:
 * current hand -> offhand -> other slots
 */
inline fun <T : HotbarItemSlot> SlotGroup<T>.findClosestSlot(predicate: (ItemStack) -> Boolean): T? {
    return if (mc.player == null) {
        null
    } else {
        val selected = player.inventory.selectedSlot
        this.filter { predicate(it.itemStack) }.minByOrNull {
            when {
                it is OffHandSlot -> Int.MIN_VALUE + 1
                it.hotbarSlotForServer == selected -> Int.MIN_VALUE
                else -> abs(selected - it.hotbarSlotForServer)
            }
        }
    }
}

fun SlotGroup<*>.hasItem(item: Item): Boolean = any { it.itemStack.item === item }

fun <T : ItemSlot> SlotGroup<T>.findBestToolToMineBlock(
    blockState: BlockState,
    ignoreDurability: Boolean = true
): T? {
    val player = mc.player ?: return null

    val slot = filter {
        val stack = it.itemStack
        val durabilityCheck = (ignoreDurability || stack.damage < (stack.maxDamage - 2))
        stack.isNothing() || (!player.isCreative && durabilityCheck)
    }.maxByOrNull {
        it.itemStack.getMiningSpeedMultiplier(blockState)
    } ?: return null

    val miningSpeedMultiplier = slot.itemStack.getMiningSpeedMultiplier(blockState)

    // The current slot already matches the best
    if (miningSpeedMultiplier == player.inventory.mainHandStack.getMiningSpeedMultiplier(blockState)) {
        return null
    }

    return slot
}

object Slots {

    /**
     * Hotbar 0~8
     */
    @JvmField
    val Hotbar = SlotGroup(
        List(9) { HotbarItemSlot(it) }
    )

    /**
     * Inventory 0~26
     */
    @JvmField
    val Inventory = SlotGroup(
        List(27) { InventoryItemSlot(it) }
    )

    /**
     * Offhand (singleton list)
     */
    @JvmField
    val OffHand = SlotGroup(
        listOf(OffHandSlot)
    )

    /**
     * Armor slots 0~3
     *
     * Boots/Leggings/Chestplate/Helmet
     */
    @JvmField
    val Armor = SlotGroup(
        List(4) { ArmorItemSlot(it) }
    )

    /**
     * Offhand + Hotbar
     */
    @Suppress("UNCHECKED_CAST")
    @JvmField
    val OffhandWithHotbar = (OffHand + Hotbar) as SlotGroup<HotbarItemSlot>

    /**
     * Hotbar + OffHand + Inventory + Armor
     */
    @JvmField
    val All = Hotbar + OffHand + Inventory + Armor
}

class SlotGroup<T : ItemSlot>(val slots: List<T>) : List<T> by slots {
    val items: List<Item>
        get() = slots.map { it.itemStack.item }

    fun findSlot(item: Item): T? {
        return findSlot { it.item === item }
    }

    inline fun findSlot(predicate: (ItemStack) -> Boolean): T? {
        return if (mc.player == null) null else find { predicate(it.itemStack) }
    }

    operator fun plus(other: SlotGroup<*>): SlotGroup<ItemSlot> {
        val newList = ArrayList<ItemSlot>(this.size + other.size)
        newList.addAll(this)
        newList.addAll(other)
        return SlotGroup(newList)
    }

    operator fun plus(other: ItemSlot): SlotGroup<ItemSlot> {
        val newList = ArrayList<ItemSlot>(this.size + 1)
        newList.addAll(this)
        newList.add(other)
        return SlotGroup(newList)
    }
}
