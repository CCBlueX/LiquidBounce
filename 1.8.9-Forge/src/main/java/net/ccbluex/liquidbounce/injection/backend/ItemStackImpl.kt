/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import com.google.common.collect.Multimap
import net.ccbluex.liquidbounce.api.minecraft.client.block.IBlock
import net.ccbluex.liquidbounce.api.minecraft.enchantments.IEnchantment
import net.ccbluex.liquidbounce.api.minecraft.entity.ai.attributes.IAttributeModifier
import net.ccbluex.liquidbounce.api.minecraft.item.IItem
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.minecraft.nbt.INBTBase
import net.ccbluex.liquidbounce.api.minecraft.nbt.INBTTagCompound
import net.ccbluex.liquidbounce.api.minecraft.nbt.INBTTagList
import net.ccbluex.liquidbounce.injection.implementations.IMixinItemStack
import net.minecraft.item.ItemStack

class ItemStackImpl(val wrapped: ItemStack) : IItemStack {
    override fun getStrVsBlock(block: IBlock): Float = wrapped.getStrVsBlock(block.unwrap())
    override fun setTagInfo(key: String, nbt: INBTBase) = wrapped.setTagInfo(key, nbt.unwrap())

    override fun setStackDisplayName(displayName: String): IItemStack = wrapped.setStackDisplayName(displayName).wrap()

    override fun addEnchantment(enchantment: IEnchantment, level: Int) = wrapped.addEnchantment(enchantment.unwrap(), level)

    override val displayName: String
        get() = wrapped.displayName

    override val unlocalizedName: String
        get() = wrapped.unlocalizedName
    override val maxItemUseDuration: Int
        get() = wrapped.maxItemUseDuration
    override val attributeModifiers: Multimap<String, IAttributeModifier>
        get() = TODO("Not yet implemented")
    override val enchantmentTagList: INBTTagList?
        get() = wrapped.enchantmentTagList?.wrap()
    override var tagCompound: INBTTagCompound?
        get() = wrapped.tagCompound?.wrap()
        set(value) {
            wrapped.tagCompound = value?.unwrap()
        }
    override val stackSize: Int
        get() = wrapped.stackSize
    override var itemDamage: Int
        get() = wrapped.itemDamage
        set(value) {
            wrapped.itemDamage = value
        }
    override val item: IItem?
        get() = wrapped.item?.wrap()
    override val itemDelay: Long
        get() = (wrapped as IMixinItemStack).itemDelay
}

inline fun IItemStack.unwrap(): ItemStack = (this as ItemStackImpl).wrapped
inline fun ItemStack.wrap(): IItemStack = ItemStackImpl(this)