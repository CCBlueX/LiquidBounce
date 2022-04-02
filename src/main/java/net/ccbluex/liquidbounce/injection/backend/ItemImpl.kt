/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.item.*
import net.minecraft.item.*

open class ItemImpl<T : Item>(val wrapped: T) : IItem {
    override val unlocalizedName: String
        get() = wrapped.unlocalizedName

    override fun asItemArmor(): IItemArmor = ItemArmorImpl(wrapped as ItemArmor)
    override fun asItemPotion(): IItemPotion = ItemPotionImpl(wrapped as ItemPotion)
    override fun asItemBlock(): IItemBlock = ItemBlockImpl(wrapped as ItemBlock)
    override fun asItemSword(): IItemSword = ItemSwordImpl(wrapped as ItemSword)
    override fun asItemBucket(): IItemBucket = ItemBucketImpl(wrapped as ItemBucket)

    override fun equals(other: Any?): Boolean {
        return other is ItemImpl<*> && other.wrapped == this.wrapped
    }
}

inline fun IItem.unwrap(): Item = (this as ItemImpl<*>).wrapped
inline fun Item.wrap(): IItem = ItemImpl(this)