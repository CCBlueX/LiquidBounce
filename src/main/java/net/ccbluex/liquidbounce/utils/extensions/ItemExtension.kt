package net.ccbluex.liquidbounce.utils.extensions

import net.ccbluex.liquidbounce.injection.implementations.IMixinItemStack
import net.minecraft.enchantment.Enchantment
import net.minecraft.item.ItemStack

val ItemStack?.enchantmentCount: Int
    get()
    {
        val enchTagList = this?.enchantmentTagList
        return if (enchTagList == null || enchTagList.hasNoTags()) 0 else (0 until enchTagList.tagCount()).map(enchTagList::getCompoundTagAt).filter { it.hasKey("ench") }.count { it.hasKey("id") }
    }

fun ItemStack?.getEnchantmentLevel(enchantment: Enchantment): Int
{
    val enchTagList = this?.enchantmentTagList
    return if (enchTagList == null || enchTagList.hasNoTags()) 0 else (0 until enchTagList.tagCount()).map(enchTagList::getCompoundTagAt).filter { it.hasKey("ench") || it.hasKey("id") }.filter { it.getShort("ench").toInt() == enchantment.effectId || it.getShort("id").toInt() == enchantment.effectId }.sumOf { it.getShort("lvl").toInt() }
}

val ItemStack?.isEmpty: Boolean
    get() = this == null // || this.item is ItemAir

@Suppress("CAST_NEVER_SUCCEEDS")
val ItemStack.itemDelay: Long
    get() = (this as IMixinItemStack).itemDelay
