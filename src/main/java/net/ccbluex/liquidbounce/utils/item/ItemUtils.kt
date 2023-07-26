/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.item

import net.ccbluex.liquidbounce.injection.implementations.IMixinItemStack
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.minecraft.enchantment.Enchantment
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemFood
import net.minecraft.item.ItemPotion
import net.minecraft.item.ItemBucketMilk
import net.minecraft.nbt.JsonToNBT
import net.minecraft.util.ResourceLocation

object ItemUtils : MinecraftInstance() {
    /**
     * Allows you to create an item using the item json
     *
     * @param itemArguments arguments of item
     * @return created item
     */
    fun createItem(itemArguments: String): ItemStack? {
        return try {
            val args = itemArguments.replace('&', '§').split(" ")

            val amount = args.getOrNull(1)?.toInt() ?: 1
            val meta = args.getOrNull(2)?.toInt() ?: 0

            val resourceLocation = ResourceLocation(args[0])
            val item = Item.itemRegistry.getObject(resourceLocation) ?: return null

            val itemStack = ItemStack(item, amount, meta)

            if (args.size >= 4) {
                val nbt = args.drop(3).joinToString(" ")

                itemStack.tagCompound = JsonToNBT.getTagFromJson(nbt)
            }

            itemStack
        } catch (exception: Exception) {
            exception.printStackTrace()
            null
        }
    }

    fun getItems(startInclusive: Int = 0, endInclusive: Int = 44,
                 itemDelay: Int? = null, filter: ((ItemStack, Int) -> Boolean)? = null): Map<Int, ItemStack> {
        val items = mutableMapOf<Int, ItemStack>()

        for (i in startInclusive..endInclusive) {
            val itemStack = mc.thePlayer.inventoryContainer.getSlot(i).stack ?: continue

            if (itemStack.isEmpty)
                continue

            if (itemDelay != null && !itemStack.hasItemDelayPassed(itemDelay))
                continue

            if (filter?.invoke(itemStack, i) != false)
                items[i] = itemStack
        }

        return items
    }


    /**
     * Allows you to check if player is consuming item
     */
    fun isConsumingItem(): Boolean {
        if (!mc.thePlayer.isUsingItem) {
            return false
        }

        val usingItem = mc.thePlayer.itemInUse.item
        return usingItem is ItemFood || usingItem is ItemBucketMilk || usingItem is ItemPotion
    }
}

/**
 *
 * Item extensions
 *
 */

val ItemStack.durability
    get() = this.maxDamage - this.itemDamage

val ItemStack.enchantmentCount: Int
    get() {
        if (this.enchantmentTagList == null || this.enchantmentTagList.hasNoTags())
            return 0

        var count = 0
        for (i in 0 until this.enchantmentTagList.tagCount()) {
            val tagCompound = this.enchantmentTagList.getCompoundTagAt(i)
            if (tagCompound.hasKey("ench") || tagCompound.hasKey("id")) count++
        }

        return count
    }

fun ItemStack.getEnchantmentLevel(enchantment: Enchantment): Int {
    if (this.enchantmentTagList == null || this.enchantmentTagList.hasNoTags())
        return 0

    for (i in 0 until this.enchantmentTagList.tagCount()) {
        val tagCompound = this.enchantmentTagList.getCompoundTagAt(i)
        if (tagCompound.hasKey("ench") && tagCompound.getInteger("ench") == enchantment.effectId
            || tagCompound.hasKey("id") && tagCompound.getInteger("id") == enchantment.effectId
        ) return tagCompound.getInteger("lvl")
    }

    return 0
}

val ItemStack?.isEmpty
    get() = this == null || this.item == null

fun ItemStack.hasItemDelayPassed(delay: Int) =
    System.currentTimeMillis() - (this as IMixinItemStack).itemDelay >= delay

val ItemStack.attackDamage
    get() = (this.attributeModifiers["generic.attackDamage"].firstOrNull()?.amount ?: 0.0) +
            1.25 * this.getEnchantmentLevel(Enchantment.sharpness)