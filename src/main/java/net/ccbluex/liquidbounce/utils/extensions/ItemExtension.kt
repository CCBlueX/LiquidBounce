package net.ccbluex.liquidbounce.utils.extensions

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.enchantments.IEnchantment
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack

val IItemStack?.enchantmentCount: Int
	get()
	{
		val enchTagList = this?.enchantmentTagList
		return if (enchTagList == null || enchTagList.hasNoTags()) 0 else (0 until enchTagList.tagCount()).map(enchTagList::getCompoundTagAt).filter { it.hasKey("ench") }.count { it.hasKey("id") }
	}

fun IItemStack?.getEnchantmentLevel(enchantment: IEnchantment): Int
{
	val enchTagList = this?.enchantmentTagList
	return if (enchTagList == null || enchTagList.hasNoTags()) 0 else (0 until enchTagList.tagCount()).map(enchTagList::getCompoundTagAt).filter { it.hasKey("ench") || it.hasKey("id") }.filter { it.getShort("ench").toInt() == enchantment.effectId || it.getShort("id").toInt() == enchantment.effectId }.sumBy { it.getShort("lvl").toInt() }
}

val IItemStack?.isEmpty: Boolean
	get() = this == null || LiquidBounce.wrapper.classProvider.isItemAir(this.item)
