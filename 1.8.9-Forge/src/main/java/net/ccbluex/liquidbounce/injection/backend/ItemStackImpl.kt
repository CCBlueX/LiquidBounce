/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import com.google.common.collect.Multimap
import net.ccbluex.liquidbounce.api.minecraft.client.block.IBlock
import net.ccbluex.liquidbounce.api.minecraft.entity.ai.attributes.IAttributeModifier
import net.ccbluex.liquidbounce.api.minecraft.item.IItem
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.minecraft.nbt.INBTTagCompound
import net.ccbluex.liquidbounce.api.minecraft.nbt.INBTTagList
import net.ccbluex.liquidbounce.injection.backend.utils.unwrap
import net.ccbluex.liquidbounce.injection.backend.utils.wrap
import net.ccbluex.liquidbounce.injection.implementations.IMixinItemStack
import net.minecraft.item.ItemStack

class ItemStackImpl(val wrapped: ItemStack) : IItemStack {
    override fun getStrVsBlock(block: IBlock): Float = wrapped.getStrVsBlock(block.unwrap())

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
    override val itemDamage: Int
        get() = wrapped.itemDamage
    override val item: IItem?
        get() = wrapped.item?.wrap()
    override val itemDelay: Long
        get() = (wrapped as IMixinItemStack).itemDelay
}