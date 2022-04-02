/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.item.IItemSword
import net.minecraft.item.ItemSword

class ItemSwordImpl(wrapped: ItemSword) : ItemImpl<ItemSword>(wrapped), IItemSword {
    override val damageVsEntity: Float
        get() = wrapped.damageVsEntity

}