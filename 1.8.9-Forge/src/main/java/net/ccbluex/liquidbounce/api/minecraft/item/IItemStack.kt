/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.item

import com.google.common.collect.Multimap
import net.ccbluex.liquidbounce.api.minecraft.client.block.IBlock
import net.ccbluex.liquidbounce.api.minecraft.enchantments.IEnchantment
import net.ccbluex.liquidbounce.api.minecraft.entity.ai.attributes.IAttributeModifier
import net.ccbluex.liquidbounce.api.minecraft.nbt.INBTBase
import net.ccbluex.liquidbounce.api.minecraft.nbt.INBTTagCompound
import net.ccbluex.liquidbounce.api.minecraft.nbt.INBTTagList

interface IItemStack {
    val displayName: String
    val unlocalizedName: String
    val maxItemUseDuration: Int
    val attributeModifiers: Multimap<String, IAttributeModifier>
    val enchantmentTagList: INBTTagList?
    var tagCompound: INBTTagCompound?
    val stackSize: Int
    var itemDamage: Int
    val item: IItem?
    val itemDelay: Long

    fun getStrVsBlock(block: IBlock): Float
    fun setTagInfo(key: String, nbt: INBTBase)
    fun setStackDisplayName(displayName: String): IItemStack
    fun addEnchantment(enchantment: IEnchantment, level: Int)

    companion object {
        inline fun IItemStack.isSplash() = (this.itemDamage and 16384) != 0
    }
}