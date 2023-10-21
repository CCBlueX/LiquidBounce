package net.ccbluex.liquidbounce.utils.item

import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList

class enchantUtils {
}
fun addEnchantment(item: ItemStack, enchantment: Enchantment, level: Int?) {
    val nbt = item.orCreateNbt
    if (nbt?.contains(ItemStack.ENCHANTMENTS_KEY, NbtElement.LIST_TYPE.toInt()) == false) {
        nbt.put(ItemStack.ENCHANTMENTS_KEY, NbtList())
    }
    val nbtList = nbt?.getList(ItemStack.ENCHANTMENTS_KEY, NbtElement.COMPOUND_TYPE.toInt())
    nbtList?.add(EnchantmentHelper.createNbt(EnchantmentHelper.getEnchantmentId(enchantment), level ?: enchantment.maxLevel))
}

fun removeEnchantment(item: ItemStack, enchantment: Enchantment){
    val nbt = item.nbt ?: return
    if (!nbt.contains(ItemStack.ENCHANTMENTS_KEY, NbtElement.LIST_TYPE.toInt())) {
        return
    }
    val nbtList = nbt.getList(ItemStack.ENCHANTMENTS_KEY, NbtElement.COMPOUND_TYPE.toInt())
    nbtList.removeIf { (it as NbtCompound).getString("id") == EnchantmentHelper.getEnchantmentId(enchantment).toString() }
}

fun clearEnchantments(item: ItemStack) {
    val nbt = item.nbt ?: return
    nbt.remove(ItemStack.ENCHANTMENTS_KEY)
}
