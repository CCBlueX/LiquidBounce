/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.item

import net.ccbluex.liquidbounce.api.minecraft.block.state.IIBlockState
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.enchantments.IEnchantment
import net.ccbluex.liquidbounce.api.minecraft.entity.ai.attributes.IAttributeModifier
import net.ccbluex.liquidbounce.api.minecraft.item.IItem
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.minecraft.nbt.INBTBase
import net.ccbluex.liquidbounce.api.minecraft.nbt.INBTTagCompound
import net.ccbluex.liquidbounce.api.minecraft.nbt.INBTTagList
import net.ccbluex.liquidbounce.api.util.WrappedCollection
import net.ccbluex.liquidbounce.injection.backend.minecraft.client.block.unwrap
import net.ccbluex.liquidbounce.injection.backend.minecraft.enchantments.unwrap
import net.ccbluex.liquidbounce.injection.backend.minecraft.entity.ai.attributes.unwrap
import net.ccbluex.liquidbounce.injection.backend.minecraft.entity.ai.attributes.wrap
import net.ccbluex.liquidbounce.injection.backend.minecraft.entity.player.unwrap
import net.ccbluex.liquidbounce.injection.backend.minecraft.nbt.unwrap
import net.ccbluex.liquidbounce.injection.backend.minecraft.nbt.wrap
import net.ccbluex.liquidbounce.injection.implementations.IMixinItemStack
import net.minecraft.entity.ai.attributes.AttributeModifier
import net.minecraft.item.ItemPotion
import net.minecraft.item.ItemStack

class ItemStackImpl(val wrapped: ItemStack) : IItemStack
{
	override val animationsToGo: Int
		get() = wrapped.animationsToGo
	override val isItemEnchanted: Boolean
		get() = wrapped.isItemEnchanted
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
	override var stackSize: Int
		get() = wrapped.stackSize
		set(value)
		{
			wrapped.stackSize = value
		}
	override var itemDamage: Int
		get() = wrapped.itemDamage
		set(value)
		{
			wrapped.itemDamage = value
		}
	override val item: IItem?
		get() = wrapped.item?.wrap()

	@Suppress("CAST_NEVER_SUCCEEDS")
	override val itemDelay: Long
		get() = (wrapped as IMixinItemStack).itemDelay

	override val maxDamage: Int
		get() = wrapped.maxDamage

	override fun setTagInfo(key: String, nbt: INBTBase) = wrapped.setTagInfo(key, nbt.unwrap())

	override fun setStackDisplayName(displayName: String): IItemStack = wrapped.setStackDisplayName(displayName).wrap()

	override fun addEnchantment(enchantment: IEnchantment, level: Int) = wrapped.addEnchantment(enchantment.unwrap(), level)
	override fun getAttributeModifier(key: String): Collection<IAttributeModifier> = WrappedCollection(wrapped.attributeModifiers[key], IAttributeModifier::unwrap, AttributeModifier::wrap)
	override fun isSplash(): Boolean = ItemPotion.isSplash(wrapped.metadata)

	override fun getTooltip(thePlayer: IEntityPlayer, advanced: Boolean): List<String> = wrapped.getTooltip(thePlayer.unwrap(), advanced)

	override fun getStrVsBlock(block: IIBlockState): Float = wrapped.getStrVsBlock(block.block.unwrap())

	override fun equals(other: Any?): Boolean = other is ItemStackImpl && wrapped.isItemEqual(other.wrapped)
}

fun IItemStack.unwrap(): ItemStack = (this as ItemStackImpl).wrapped
fun ItemStack.wrap(): IItemStack = ItemStackImpl(this)
