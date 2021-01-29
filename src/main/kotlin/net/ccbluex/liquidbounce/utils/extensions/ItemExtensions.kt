package net.ccbluex.liquidbounce.utils.extensions

import com.mojang.brigadier.StringReader
import net.minecraft.command.argument.ItemStackArgument
import net.minecraft.command.argument.ItemStringReader
import net.minecraft.item.ItemStack

/**
 * Create item with NBT tags
 *
 * @docs https://minecraft.gamepedia.com/Commands/give
 */
fun createItem(stack: String, amount: Int = 1): ItemStack = ItemStringReader(StringReader(stack), true).consume().let {
    ItemStackArgument(it.item, it.tag).createStack(amount, false)
}

/**
 * Check if a stack is nothing (means empty slot)
 */
fun ItemStack?.isNothing() = this?.isEmpty == true
