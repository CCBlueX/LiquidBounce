/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.item.IItemPotion
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.minecraft.potion.IPotionEffect
import net.ccbluex.liquidbounce.api.util.WrappedCollection
import net.minecraft.item.ItemPotion
import net.minecraft.potion.PotionEffect
import net.minecraft.potion.PotionUtils

class ItemPotionImpl(wrapped: ItemPotion) : ItemImpl<ItemPotion>(wrapped), IItemPotion {
    override fun getEffects(stack: IItemStack): Collection<IPotionEffect> {
        return WrappedCollection(
                PotionUtils.getEffectsFromStack(stack.unwrap()),
                IPotionEffect::unwrap,
                PotionEffect::wrap
        )
    }
}


