/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.item

import net.ccbluex.liquidbounce.api.minecraft.item.IArmorMaterial
import net.ccbluex.liquidbounce.api.minecraft.item.IItemArmor
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.minecraft.item.ItemArmor

class ItemArmorImpl(wrapped: ItemArmor) : ItemImpl<ItemArmor>(wrapped), IItemArmor
{
    override val damageReduceAmount: Int
        get() = wrapped.damageReduceAmount
    override val armorMaterial: IArmorMaterial
        get() = wrapped.armorMaterial.wrap()
    override val armorType: Int
        get() = wrapped.armorType
    override val unlocalizedName: String
        get() = wrapped.unlocalizedName

    override fun getColor(item: IItemStack): Int = wrapped.getColor(item.unwrap())
}
