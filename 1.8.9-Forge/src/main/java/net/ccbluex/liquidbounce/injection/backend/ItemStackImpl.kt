/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.block.state.IIBlockState
import net.ccbluex.liquidbounce.api.minecraft.enchantments.IEnchantment
import net.ccbluex.liquidbounce.api.minecraft.entity.ai.attributes.IAttributeModifier
import net.ccbluex.liquidbounce.api.minecraft.item.IItem
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.minecraft.nbt.INBTBase
import net.ccbluex.liquidbounce.api.minecraft.nbt.INBTTagCompound
import net.ccbluex.liquidbounce.api.minecraft.nbt.INBTTagList
import net.ccbluex.liquidbounce.api.util.WrappedCollection
import net.minecraft.entity.ai.attributes.AttributeModifier
import net.minecraft.item.ItemPotion
import net.minecraft.item.ItemStack

import net.ccbluex.liquidbounce.injection.implementations.IMixinItemStack

class ItemStackImpl(val wrapped: ItemStack) : IItemStack
{
	override fun setTagInfo(key: String, nbt: INBTBase) = wrapped.setTagInfo(key, nbt.unwrap())

	override fun setStackDisplayName(displayName: String): IItemStack = wrapped.setStackDisplayName(displayName).wrap()

	override fun addEnchantment(enchantment: IEnchantment, level: Int) = wrapped.addEnchantment(enchantment.unwrap(), level)
	override fun getAttributeModifier(key: String): Collection<IAttributeModifier> = WrappedCollection(wrapped.attributeModifiers[key], IAttributeModifier::unwrap, AttributeModifier::wrap)
	override fun isSplash(): Boolean = ItemPotion.isSplash(wrapped.metadata)

	override val displayName: String
		get() = wrapped.displayName

	override val unlocalizedName: String
		get() = wrapped.unlocalizedName
	override val maxItemUseDuration: Int
		get() = wrapped.maxItemUseDuration
	override val enchantmentTagList: INBTTagList?
		get() = wrapped.enchantmentTagList?.wrap()
	override var tagCompound: INBTTagCompound?
		get() = wrapped.tagCompound?.wrap()
		set(value)
		{
			wrapped.tagCompound = value?.unwrap()
		}
	override val stackSize: Int
		get() = wrapped.stackSize
	override var itemDamage: Int
		get() = wrapped.itemDamage
		set(value)
		{
			wrapped.itemDamage = value
		}
	override val item: IItem?
		get() = wrapped.item?.wrap()
	override val itemDelay: Long
		get() = (wrapped as IMixinItemStack).itemDelay

	override fun getStrVsBlock(block: IIBlockState): Float = wrapped.getStrVsBlock(block.block.unwrap())
}

inline fun IItemStack.unwrap(): ItemStack = (this as ItemStackImpl).wrapped
inline fun ItemStack.wrap(): IItemStack = ItemStackImpl(this)
