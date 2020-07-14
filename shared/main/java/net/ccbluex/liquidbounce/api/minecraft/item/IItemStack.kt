/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.item

import net.ccbluex.liquidbounce.api.minecraft.block.state.IIBlockState
import net.ccbluex.liquidbounce.api.minecraft.enchantments.IEnchantment
import net.ccbluex.liquidbounce.api.minecraft.entity.ai.attributes.IAttributeModifier
import net.ccbluex.liquidbounce.api.minecraft.nbt.INBTBase
import net.ccbluex.liquidbounce.api.minecraft.nbt.INBTTagCompound
import net.ccbluex.liquidbounce.api.minecraft.nbt.INBTTagList

interface IItemStack {
    val displayName: String
    val unlocalizedName: String
    val maxItemUseDuration: Int
    val enchantmentTagList: INBTTagList?
    var tagCompound: INBTTagCompound?
    val stackSize: Int
    var itemDamage: Int
    val item: IItem?
    val itemDelay: Long

    fun getStrVsBlock(block: IIBlockState): Float
    fun setTagInfo(key: String, nbt: INBTBase)
    fun setStackDisplayName(displayName: String): IItemStack
    fun addEnchantment(enchantment: IEnchantment, level: Int)
    fun getAttributeModifier(key: String): Collection<IAttributeModifier>
    fun isSplash(): Boolean
}