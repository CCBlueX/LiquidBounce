/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.item

import net.ccbluex.liquidbounce.api.minecraft.item.IItemFood
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.minecraft.item.ItemFood

class ItemFoodImpl(wrapped: ItemFood) : ItemImpl<ItemFood>(wrapped), IItemFood
{
	override fun getHealAmount(stack: IItemStack): Int = wrapped.getHealAmount(stack.unwrap())
	override fun getSaturationModifier(stack: IItemStack): Float = wrapped.getSaturationModifier(stack.unwrap())
}
